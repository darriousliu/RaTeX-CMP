//! Additive CMP bridge. The upstream submodule and its public ABI stay unchanged.
use std::{cell::RefCell, collections::HashMap, ffi::CString, os::raw::c_char, sync::Arc};

use ratex_layout::{
    layout_with_fallback_metrics, to_display_list, validate_layout, FallbackGlyphMetrics,
    LayoutOptions,
};
use ratex_types::{color::Color, math_style::MathStyle};
use serde::Deserialize;

#[cfg(target_os = "android")]
mod jni;
#[cfg(target_arch = "wasm32")]
mod wasm;

pub const MAX_ENTRIES: usize = 4096;
pub const MAX_METRIC_EM: f64 = 100.0;

#[repr(C)]
#[derive(Clone, Copy, Debug, Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
pub struct GlyphMetrics {
    pub code_point: u32,
    #[serde(default)]
    pub reserved: u32,
    pub width: f64,
    pub height: f64,
    pub depth: f64,
}

fn metrics_map(
    entries: &[GlyphMetrics],
) -> Result<Arc<HashMap<char, FallbackGlyphMetrics>>, String> {
    if entries.len() > MAX_ENTRIES {
        return Err(format!("fallback metrics exceed {MAX_ENTRIES} entries"));
    }
    let mut map = HashMap::with_capacity(entries.len());
    for entry in entries {
        let ch = char::from_u32(entry.code_point).ok_or("invalid fallback Unicode scalar")?;
        if entry.reserved != 0 {
            return Err("fallback metric reserved field must be zero".into());
        }
        for value in [entry.width, entry.height, entry.depth] {
            if !value.is_finite() || !(0.0..=MAX_METRIC_EM).contains(&value) {
                return Err(format!(
                    "fallback metrics must be finite and within 0..={MAX_METRIC_EM} em"
                ));
            }
        }
        if map
            .insert(
                ch,
                FallbackGlyphMetrics {
                    width: entry.width,
                    height: entry.height,
                    depth: entry.depth,
                },
            )
            .is_some()
        {
            return Err("duplicate fallback Unicode scalar".into());
        }
    }
    Ok(Arc::new(map))
}

pub fn render(
    latex: &str,
    display_mode: bool,
    rgba: [f32; 4],
    entries: &[GlyphMetrics],
) -> Result<String, String> {
    let metrics = metrics_map(entries)?;
    if rgba
        .iter()
        .any(|c| !c.is_finite() || !(0.0..=1.0).contains(c))
    {
        return Err("color must contain four finite components in 0..=1".into());
    }
    let ast = ratex_parser::parse(latex).map_err(|e| format!("parse error: {e}"))?;
    let options = LayoutOptions::default()
        .with_style(if display_mode {
            MathStyle::Display
        } else {
            MathStyle::Text
        })
        .with_color(Color::new(rgba[0], rgba[1], rgba[2], rgba[3]));
    let tree = layout_with_fallback_metrics(&ast, &options, metrics);
    validate_layout(&tree)?;
    let mut json = serde_json::to_value(to_display_list(&tree)).map_err(|e| e.to_string())?;
    json.as_object_mut()
        .ok_or("invalid display list")?
        .insert("version".into(), 1.into());
    serde_json::to_string(&json).map_err(|e| e.to_string())
}

thread_local! {
    static LAST_ERROR: RefCell<Option<CString>> = const { RefCell::new(None) };
}

fn finish(result: Result<String, String>) -> *mut c_char {
    LAST_ERROR.with(|slot| *slot.borrow_mut() = None);
    match result.and_then(|json| CString::new(json).map_err(|e| e.to_string())) {
        Ok(json) => json.into_raw(),
        Err(error) => {
            let error = CString::new(error.replace('\0', "")).expect("removed null bytes");
            LAST_ERROR.with(|slot| *slot.borrow_mut() = Some(error));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "C" fn ratex_cmp_bridge_version() -> u32 {
    1
}

/// # Safety
/// Non-null pointers must reference valid memory for the documented input sizes.
#[no_mangle]
pub unsafe extern "C" fn ratex_cmp_parse(
    latex: *const c_char,
    display_mode: i32,
    rgba: *const f32,
    entries: *const GlyphMetrics,
    count: usize,
) -> *mut c_char {
    finish((|| {
        if latex.is_null() {
            return Err("latex pointer is null".into());
        }
        if count > MAX_ENTRIES {
            return Err(format!("fallback metrics exceed {MAX_ENTRIES} entries"));
        }
        if count > 0 && entries.is_null() {
            return Err("fallback entries pointer is null".into());
        }
        if display_mode != 0 && display_mode != 1 {
            return Err("display mode must be 0 or 1".into());
        }
        let source = unsafe { std::ffi::CStr::from_ptr(latex) }
            .to_str()
            .map_err(|e| e.to_string())?;
        let color = if rgba.is_null() {
            [0.0, 0.0, 0.0, 1.0]
        } else {
            let values = unsafe { std::slice::from_raw_parts(rgba, 4) };
            [values[0], values[1], values[2], values[3]]
        };
        let entries = if count == 0 {
            &[]
        } else {
            unsafe { std::slice::from_raw_parts(entries, count) }
        };
        render(source, display_mode == 1, color, entries)
    })())
}

#[no_mangle]
pub extern "C" fn ratex_cmp_last_error() -> *const c_char {
    LAST_ERROR.with(|slot| {
        slot.borrow()
            .as_ref()
            .map_or(std::ptr::null(), |s| s.as_ptr())
    })
}

/// # Safety
/// Pointer must be NULL or an unfreed result from this bridge.
#[no_mangle]
pub unsafe extern "C" fn ratex_cmp_free(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe { drop(CString::from_raw(ptr)) };
    }
}

#[cfg(test)]
mod tests;

//! Raw WASM exports; no JS glue or second npm distribution is needed.
use super::*;

const MAX_INPUT_BYTES: usize = 32 * 1024 * 1024;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase", deny_unknown_fields)]
struct Request {
    latex: String,
    display_mode: bool,
    color: [f32; 4],
    metrics: Vec<GlyphMetrics>,
}

#[no_mangle]
pub extern "C" fn ratex_cmp_alloc(len: usize) -> *mut u8 {
    if len == 0 || len > MAX_INPUT_BYTES {
        return std::ptr::null_mut();
    }
    Box::into_raw(vec![0u8; len].into_boxed_slice()) as *mut u8
}

/// # Safety
/// Pointer and length must identify an allocation made by ratex_cmp_alloc.
#[no_mangle]
pub unsafe extern "C" fn ratex_cmp_dealloc(ptr: *mut u8, len: usize) {
    if !ptr.is_null() {
        unsafe { drop(Box::from_raw(std::ptr::slice_from_raw_parts_mut(ptr, len))) };
    }
}

/// # Safety
/// Input must reference len valid bytes; the output uses ratex_cmp_free.
#[no_mangle]
pub unsafe extern "C" fn ratex_cmp_parse_json(ptr: *const u8, len: usize) -> *mut c_char {
    finish((|| {
        if ptr.is_null() || len == 0 || len > MAX_INPUT_BYTES {
            return Err("invalid bridge request size".into());
        }
        let bytes = unsafe { std::slice::from_raw_parts(ptr, len) };
        let request: Request = serde_json::from_slice(bytes).map_err(|e| e.to_string())?;
        render(
            &request.latex,
            request.display_mode,
            request.color,
            &request.metrics,
        )
    })())
}

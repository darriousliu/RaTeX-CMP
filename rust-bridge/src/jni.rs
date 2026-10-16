use crate::{render, GlyphMetrics, MAX_ENTRIES};
use jni::{
    objects::{JClass, JDoubleArray, JFloatArray, JIntArray, JString},
    sys::{jboolean, jstring, JNI_TRUE},
    JNIEnv,
};

#[no_mangle]
pub extern "system" fn Java_io_ratex_RaTeXFallbackBridge_nativeParse(
    mut env: JNIEnv,
    _class: JClass,
    latex: JString,
    display_mode: jboolean,
    rgba: JFloatArray,
    code_points: JIntArray,
    dimensions: JDoubleArray,
) -> jstring {
    let result = (|| -> Result<String, String> {
        let count = env
            .get_array_length(&code_points)
            .map_err(|e| e.to_string())? as usize;
        if count > MAX_ENTRIES {
            return Err(format!("fallback metrics exceed {MAX_ENTRIES} entries"));
        }
        if env
            .get_array_length(&dimensions)
            .map_err(|e| e.to_string())? as usize
            != count * 3
        {
            return Err(
                "fallback dimensions length must be three times the code point count".into(),
            );
        }
        if env.get_array_length(&rgba).map_err(|e| e.to_string())? != 4 {
            return Err("color must contain four components".into());
        }
        let mut points = vec![0; count];
        let mut values = vec![0.0; count * 3];
        let mut color = [0.0; 4];
        env.get_int_array_region(&code_points, 0, &mut points)
            .map_err(|e| e.to_string())?;
        env.get_double_array_region(&dimensions, 0, &mut values)
            .map_err(|e| e.to_string())?;
        env.get_float_array_region(&rgba, 0, &mut color)
            .map_err(|e| e.to_string())?;
        let entries: Vec<_> = points
            .into_iter()
            .zip(values.chunks_exact(3))
            .map(|(cp, v)| GlyphMetrics {
                code_point: cp as u32,
                reserved: 0,
                width: v[0],
                height: v[1],
                depth: v[2],
            })
            .collect();
        let latex: String = env.get_string(&latex).map_err(|e| e.to_string())?.into();
        render(&latex, display_mode == JNI_TRUE, color, &entries)
    })();
    match result {
        Ok(json) => match env.new_string(json) {
            Ok(value) => value.into_raw(),
            Err(_) => std::ptr::null_mut(), // Preserve JNI's pending allocation exception.
        },
        Err(error) => {
            let _ = env.throw_new("io/ratex/RaTeXException", error);
            std::ptr::null_mut()
        }
    }
}

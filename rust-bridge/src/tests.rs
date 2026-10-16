use super::*;
use std::ffi::CStr;

fn entry(cp: u32) -> GlyphMetrics {
    GlyphMetrics {
        code_point: cp,
        reserved: 0,
        width: 0.82,
        height: 0.53,
        depth: 0.12,
    }
}
fn output(source: &str, entries: &[GlyphMetrics]) -> serde_json::Value {
    serde_json::from_str(&render(source, true, [0.0, 0.0, 1.0, 1.0], entries).unwrap()).unwrap()
}

#[test]
fn metrics_scale_and_support_non_bmp() {
    for (source, factor) in [
        (r"\text{ш}", 1.0),
        (r"\scriptstyle\text{ш}", 0.7),
        (r"\scriptscriptstyle\text{ш}", 0.5),
        (r"\Huge\text{ш}", 2.488),
    ] {
        let json = output(source, &[entry('ш' as u32)]);
        for (key, value) in [("width", 0.82), ("height", 0.53), ("depth", 0.12)] {
            assert!((json[key].as_f64().unwrap() - factor * value).abs() < 1e-9);
        }
    }
    assert_eq!(output(r"\text{😊}", &[entry(0x1f60a)])["width"], 0.82);
}

#[test]
fn empty_map_and_known_glyphs_preserve_legacy_layout() {
    for source in [r"x^2", r"\text{ш}", r"\frac{\text{ш}}{\sqrt{x}}"] {
        let options = LayoutOptions::default().with_color(Color::new(0.0, 0.0, 1.0, 1.0));
        let tree = ratex_layout::layout(&ratex_parser::parse(source).unwrap(), &options);
        let mut expected = serde_json::to_value(to_display_list(&tree)).unwrap();
        expected["version"] = 1.into();
        let expected: serde_json::Value =
            serde_json::from_str(&serde_json::to_string(&expected).unwrap()).unwrap();
        assert_eq!(output(source, &[]), expected);
    }
    assert_eq!(output("x", &[]), output("x", &[entry('x' as u32)]));
    assert_eq!(
        output(r"\text{Ж}", &[]),
        output(r"\text{Ж}", &[entry('ш' as u32)])
    );
}

#[test]
fn validates_entries_and_keeps_calls_isolated() {
    for cp in [0xd800, 0x110000, u32::MAX] {
        assert!(metrics_map(&[entry(cp)]).is_err());
    }
    for width in [f64::NAN, f64::INFINITY, -0.1, 100.1] {
        assert!(metrics_map(&[GlyphMetrics { width, ..entry(1) }]).is_err());
    }
    assert!(metrics_map(&[entry(1), entry(1)]).is_err());
    assert!(metrics_map(&vec![entry(1); MAX_ENTRIES + 1]).is_err());
    assert!(metrics_map(&[GlyphMetrics {
        reserved: 1,
        ..entry(1)
    }])
    .is_err());
    assert!(metrics_map(&[GlyphMetrics {
        width: 0.0,
        ..entry(1)
    }])
    .is_ok());
    let baseline = output(r"\text{ш}", &[]);
    let threads: Vec<_> = (1..8)
        .map(|n| {
            std::thread::spawn(move || {
                let value = n as f64 / 10.0;
                assert_eq!(
                    output(
                        r"\text{ш}",
                        &[GlyphMetrics {
                            width: value,
                            ..entry('ш' as u32)
                        }]
                    )["width"],
                    value
                );
            })
        })
        .collect();
    for thread in threads {
        thread.join().unwrap();
    }
    assert_eq!(output(r"\text{ш}", &[]), baseline);
}

#[test]
fn ffi_layout_errors_and_memory_ownership() {
    assert_eq!(std::mem::size_of::<GlyphMetrics>(), 32);
    assert_eq!(std::mem::offset_of!(GlyphMetrics, width), 8);
    assert_eq!(std::mem::offset_of!(GlyphMetrics, depth), 24);
    unsafe {
        let source = CString::new(r"\text{ш}").unwrap();
        assert!(
            ratex_cmp_parse(source.as_ptr(), 1, std::ptr::null(), std::ptr::null(), 1).is_null()
        );
        assert!(!ratex_cmp_last_error().is_null());
        let result = ratex_cmp_parse(source.as_ptr(), 1, std::ptr::null(), &entry('ш' as u32), 1);
        assert!(!result.is_null());
        assert!(ratex_cmp_last_error().is_null());
        assert!(CStr::from_ptr(result).to_str().unwrap().contains("0.82"));
        ratex_cmp_free(result);
        ratex_cmp_free(std::ptr::null_mut());
    }
}

#[test]
fn layout_budget_and_parse_failures_still_recover() {
    let source = format!(
        "{}{}{}",
        r"\pmb{".repeat(13),
        r"\begin{array}{:c:}\rule{1em}{10em}\end{array}",
        "}".repeat(13)
    );
    assert!(render(&source, true, [0.0, 0.0, 0.0, 1.0], &[entry(1)])
        .unwrap_err()
        .contains("Layout resource limit exceeded"));
    assert!(render(r"\char9999999999999999999", true, [0.0, 0.0, 0.0, 1.0], &[]).is_err());
    assert!(render("x+1", true, [0.0, 0.0, 0.0, 1.0], &[]).is_ok());
}

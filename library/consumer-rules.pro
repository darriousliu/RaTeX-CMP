# JNI resolves these class/method names, and creates the exception by its binary name.
-keepclasseswithmembernames,includedescriptorclasses class io.ratex.RaTeXFallbackBridge {
    native <methods>;
}
-keep,allowoptimization class io.ratex.RaTeXException {
    public <init>(java.lang.String);
}

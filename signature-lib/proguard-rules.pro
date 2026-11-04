# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep ANRSignatureValidator class and methods
-keep class com.anrstudio.signaturelib.ANRSignatureValidator {
    public *;
}

# Keep JNI method signatures
-keepclassmembers class com.anrstudio.signaturelib.ANRSignatureValidator {
    private native java.lang.String nativeGetSignatureHash(android.content.Context);
    private native boolean nativeVerifySignature(android.content.Context, java.lang.String);
    private native java.lang.String nativeGetAuthorKey(android.content.Context, long);
}

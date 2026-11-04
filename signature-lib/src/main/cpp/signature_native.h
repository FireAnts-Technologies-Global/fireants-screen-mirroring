#ifndef SIGNATURE_NATIVE_H
#define SIGNATURE_NATIVE_H

#include <jni.h>
#include <string>

extern "C" {
    JNIEXPORT jstring JNICALL
    Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeGetSignatureHash(
        JNIEnv* env, jobject thiz, jobject context);
    
    JNIEXPORT jboolean JNICALL
    Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeVerifySignature(
        JNIEnv* env, jobject thiz, jobject context, jstring expectedHash);
    
    JNIEXPORT jstring JNICALL
    Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeGetAuthorKey(
        JNIEnv* env, jobject thiz, jobject context, jlong timestamp);
}

// Helper functions
std::string getPackageSignature(JNIEnv* env, jobject context);
std::string getPackageName(JNIEnv* env, jobject context);
std::string calculateSHA256(const uint8_t* data, size_t length);
std::string calculateHMACSHA256(const std::string& key, const std::string& value);
std::string formatHashWithColons(const std::string& hash);

#endif // SIGNATURE_NATIVE_H

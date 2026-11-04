#include "signature_native.h"
#include <android/log.h>
#include <vector>

#define LOG_TAG "SignatureNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeGetSignatureHash(
    JNIEnv* env, jobject thiz, jobject context) {
    
    try {
        std::string signature = getPackageSignature(env, context);
        if (signature.empty()) {
            return nullptr;
        }
        return env->NewStringUTF(signature.c_str());
    } catch (...) {
        LOGE("Exception in nativeGetSignatureHash");
        return nullptr;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeVerifySignature(
    JNIEnv* env, jobject thiz, jobject context, jstring expectedHash) {
    
    try {
        const char* expected = env->GetStringUTFChars(expectedHash, nullptr);
        std::string signature = getPackageSignature(env, context);
        bool result = (signature == expected);
        env->ReleaseStringUTFChars(expectedHash, expected);
        return result;
    } catch (...) {
        LOGE("Exception in nativeVerifySignature");
        return false;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_anrstudio_signaturelib_ANRSignatureValidator_nativeGetAuthorKey(
    JNIEnv* env, jobject thiz, jobject context, jlong timestamp) {
    
    try {
        std::string signatureWithColons = getPackageSignature(env, context);
        if (signatureWithColons.empty()) {
            LOGE("Failed to get signature");
            return nullptr;
        }
        
        // 2. Get package name
        std::string packageName = getPackageName(env, context);
        if (packageName.empty()) {
            LOGE("Failed to get package name");
            return nullptr;
        }

        std::string value = packageName + "|" + std::to_string(timestamp);

        std::string hmac = calculateHMACSHA256(signatureWithColons, value);
        
        if (hmac.empty()) {
            LOGE("Failed to calculate HMAC");
            return nullptr;
        }
        
        return env->NewStringUTF(hmac.c_str());
    } catch (...) {
        LOGE("Exception in nativeGetAuthorKey");
        return nullptr;
    }
}

std::string getPackageName(JNIEnv* env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPackageName = env->GetMethodID(
        contextClass, "getPackageName", "()Ljava/lang/String;");
    
    if (getPackageName == nullptr) {
        LOGE("Failed to get getPackageName method");
        return "";
    }
    
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageName);
    if (packageName == nullptr) {
        LOGE("Failed to get package name");
        return "";
    }
    
    const char* packageNameStr = env->GetStringUTFChars(packageName, nullptr);
    std::string result(packageNameStr);
    env->ReleaseStringUTFChars(packageName, packageNameStr);
    
    return result;
}

std::string getPackageSignature(JNIEnv* env, jobject context) {
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getPackageManager = env->GetMethodID(
        contextClass, "getPackageManager", "()Landroid/content/pm/PackageManager;");
    
    if (getPackageManager == nullptr) {
        LOGE("Failed to get getPackageManager method");
        return "";
    }
    
    jobject packageManager = env->CallObjectMethod(context, getPackageManager);
    if (packageManager == nullptr) {
        LOGE("Failed to get PackageManager");
        return "";
    }

    jmethodID getPackageName = env->GetMethodID(
        contextClass, "getPackageName", "()Ljava/lang/String;");
    
    if (getPackageName == nullptr) {
        LOGE("Failed to get getPackageName method");
        return "";
    }
    
    jstring packageName = (jstring)env->CallObjectMethod(context, getPackageName);
    if (packageName == nullptr) {
        LOGE("Failed to get package name");
        return "";
    }
    
    // 3. Get PackageInfo with signatures
    jclass pmClass = env->GetObjectClass(packageManager);
    jmethodID getPackageInfo = env->GetMethodID(
        pmClass, "getPackageInfo", 
        "(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;");
    
    if (getPackageInfo == nullptr) {
        LOGE("Failed to get getPackageInfo method");
        return "";
    }

    const jint GET_SIGNING_CERTIFICATES = 0x08000000;
    jobject packageInfo = env->CallObjectMethod(
        packageManager, getPackageInfo, packageName, GET_SIGNING_CERTIFICATES);
    
    if (packageInfo == nullptr || env->ExceptionCheck()) {
        env->ExceptionClear();
        LOGE("Failed to get PackageInfo");
        return "";
    }

    jclass packageInfoClass = env->GetObjectClass(packageInfo);

    jfieldID signingInfoField = env->GetFieldID(
        packageInfoClass, "signingInfo", "Landroid/content/pm/SigningInfo;");
    
    jobjectArray signatures = nullptr;
    
    if (signingInfoField != nullptr && !env->ExceptionCheck()) {
        jobject signingInfo = env->GetObjectField(packageInfo, signingInfoField);
        
        if (signingInfo != nullptr) {
            jclass signingInfoClass = env->GetObjectClass(signingInfo);
            jmethodID getSigningCertHistory = env->GetMethodID(
                signingInfoClass, "getSigningCertificateHistory", 
                "()[Landroid/content/pm/Signature;");
            
            if (getSigningCertHistory != nullptr) {
                signatures = (jobjectArray)env->CallObjectMethod(
                    signingInfo, getSigningCertHistory);
            }
        }
    } else {
        env->ExceptionClear();
        jfieldID signaturesField = env->GetFieldID(
            packageInfoClass, "signatures", "[Landroid/content/pm/Signature;");
        
        if (signaturesField != nullptr) {
            signatures = (jobjectArray)env->GetObjectField(packageInfo, signaturesField);
        }
    }
    
    if (signatures == nullptr || env->GetArrayLength(signatures) == 0) {
        LOGE("No signatures found");
        return "";
    }

    jobject signature = env->GetObjectArrayElement(signatures, 0);
    if (signature == nullptr) {
        LOGE("Failed to get signature");
        return "";
    }
    
    jclass signatureClass = env->GetObjectClass(signature);
    jmethodID toByteArray = env->GetMethodID(
        signatureClass, "toByteArray", "()[B");
    
    if (toByteArray == nullptr) {
        LOGE("Failed to get toByteArray method");
        return "";
    }
    
    jbyteArray signatureBytes = (jbyteArray)env->CallObjectMethod(
        signature, toByteArray);
    
    if (signatureBytes == nullptr) {
        LOGE("Failed to get signature bytes");
        return "";
    }

    jsize length = env->GetArrayLength(signatureBytes);
    jbyte* bytes = env->GetByteArrayElements(signatureBytes, nullptr);
    
    std::string hash = calculateSHA256(
        reinterpret_cast<const uint8_t*>(bytes), length);
    
    env->ReleaseByteArrayElements(signatureBytes, bytes, JNI_ABORT);

    return formatHashWithColons(hash);
}

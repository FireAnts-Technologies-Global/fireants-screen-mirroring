package com.anrstudio.signaturelib

import android.content.Context

/**
 * Native signature validator using JNI for enhanced security.
 * All signature extraction logic is implemented in C++ to prevent easy reverse engineering.
 */
object ANRSignatureValidator {

    init {
        System.loadLibrary("signature_native")
    }

    /**
     * Get SHA-256 hash of the app's signing certificate.
     * 
     * @param context Application or Activity context
     * @return Signature hash in format "XX:XX:XX:..." or null if error occurs
     */
    fun getSignatureHash(context: Context): String? {
        return nativeGetSignatureHash(context)
    }

    /**
     * Verify if the app's signature matches the expected hash.
     * 
     * @param context Application or Activity context
     * @param expectedHash Expected signature hash in format "XX:XX:XX:..."
     * @return true if signature matches, false otherwise
     */
    fun verifySignature(context: Context, expectedHash: String): Boolean {
        return nativeVerifySignature(context, expectedHash)
    }

    /**
     * Generate authorization key using HMAC-SHA256.
     * Uses app signature as key and "packageName|timestamp" as value.
     * 
     * @param context Application or Activity context
     * @param timestamp Unix timestamp in milliseconds
     * @return HMAC-SHA256 hash in lowercase hex format or null if error occurs
     */
    fun getAuthorKey(context: Context, timestamp: Long): String? {
        return nativeGetAuthorKey(context, timestamp)
    }

    private external fun nativeGetSignatureHash(context: Context): String?
    private external fun nativeVerifySignature(context: Context, expectedHash: String): Boolean
    private external fun nativeGetAuthorKey(context: Context, timestamp: Long): String?
}

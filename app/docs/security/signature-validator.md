# ANR Studio Signature Validator

## Purpose

This document explains how to use the 3 methods from the `ANRSignatureValidator` AAR to:
- get the app signature hash,
- verify the app signature against an expected hash,
- generate an author key for API headers.

## Core Methods

### 1) `getSignatureHash(context: Context): String?`

- Calls native code to get the current app signature hash.
- The returned value is commonly used to:
  - debug/validate setup during app onboarding,
  - register the hash with the backend when required.

### 2) `verifySignature(context: Context, expectedHash: String): Boolean`

- Verifies whether the app signature matches `expectedHash`.
- `expectedHash` format: `"XX:XX:XX:..."`.
- Useful for runtime validation before allowing sensitive API calls.

### 3) `getAuthorKey(context: Context, timestamp: Long): String?`

- Generates an author key using HMAC-SHA256.
- Key: app signature.
- Message: `"packageName|timestamp"`.
- Output: lowercase hex string, or `null` if an error occurs.

## Suggested API Header Flow

Example flow:
1. Create `timestamp = System.currentTimeMillis()`.
2. Call `authorKey = getAuthorKey(context, timestamp)`.
3. Attach headers to authentic all request API Service:
   - `x-timestamp: <timestamp>`
   - `Authorization: <authorKey>`
   - ...

## Troubleshooting

- `getSignatureHash()` returns `null`: invalid context or native-side error.
- `verifySignature()` returns `false`: wrong `expectedHash` or app signed with a different key.
- `getAuthorKey()` returns `null`: invalid timestamp or native HMAC generation failure.
- Backend rejects key: re-check timestamp format and device time synchronization.

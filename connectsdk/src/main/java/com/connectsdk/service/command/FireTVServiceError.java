package com.connectsdk.service.command;

public class FireTVServiceError extends ServiceCommandError {
    public FireTVServiceError(String str) {
        super(str);
    }

    public FireTVServiceError(String str, Throwable th) {
        super(str);
        this.payload = th;
    }

}

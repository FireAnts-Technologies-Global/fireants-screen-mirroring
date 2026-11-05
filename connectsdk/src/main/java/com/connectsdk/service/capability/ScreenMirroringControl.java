package com.connectsdk.service.capability;

import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;

import java.util.ArrayList;
import java.util.List;

public interface ScreenMirroringControl extends CapabilityMethods {
    String Any = "ScreenMirroringControl.Any";
    String ScreenMirroring = "ScreenMirroringControl.ScreenMirroring";
    String[] Capabilities = {ScreenMirroring};


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Static functions

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    static int getSdkVersion(Context context) {
        return -1;
    }

    static boolean isCompatibleOsVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    static boolean isRunning(Context context) {
        return false;
    }

    static boolean isSupportScreenMirroring(String deviceId) {
        ConnectableDevice dvc = DiscoveryManager.getInstance().getDeviceById(deviceId);
        List<String> capabilities = (dvc != null) ? dvc.getCapabilities() : new ArrayList<>();
        return capabilities.contains(ScreenMirroringControl.ScreenMirroring);
    }

    static boolean isUibcEnabled(Context context) {
        return false;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Interfaces

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    ScreenMirroringControl getScreenMirroringControl();

    void startScreenMirroring(Context context, Intent projectionData, ScreenMirroringStartListener onStartListener);

    void startScreenMirroring(Context context, Intent projectionData, Class secondScreenClass, ScreenMirroringStartListener onStartListener);

    void stopScreenMirroring(Context context, ScreenMirroringStopListener stopListener);

    void setErrorListener(Context context, ScreenMirroringErrorListener errorListener);


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    enum ScreenMirroringError {
        ERROR_GENERIC,
        ERROR_CONNECTION_CLOSED,
        ERROR_DEVICE_SHUTDOWN,
        ERROR_RENDERER_TERMINATED,
        ERROR_STOPPED_BY_NOTIFICATION
    }

    interface ScreenMirroringStartListener {
        void onPairing();

        void onStart(boolean result, Presentation secondScreen);
    }

    interface ScreenMirroringStopListener {
        void onStop(boolean result);
    }

    interface ScreenMirroringErrorListener {
        void onError(ScreenMirroringError screenMirroringError);
    }
}

package com.anrstudio.template.data.screenmirror;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class NetworkUtils {
    public static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        return getIpAddress(ip);
    }

    public static String getIpAddress(int ip) {

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ip = Integer.reverseBytes(ip);
        }
        byte[] ipBytes = BigInteger.valueOf(ip).toByteArray();
        String ipAddress;

        try {
            ipAddress = InetAddress.getByAddress(ipBytes).getHostAddress();
        } catch (UnknownHostException e) {
            ipAddress = null;
        }
        return ipAddress;
    }
}

package com.connectsdk.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DeviceDatabase extends SQLiteOpenHelper {
    public static final String COUNTY = "country";
    public static final String CUSTOM_USER_DEVICE_NAME = "custom_user_device_name";
    public static final String DEVELOPER_ENABLED = "developer_enabled";
    public static final String DEVICES_TABLE_NAME = "devices";
    public static final String DEVICE_ID = "device_id";
    public static final String ETHERNET_MAC = "ethernet_mac";
    public static final String HEADPHONES_CONNECTED = "headphones_connected";
    public static final String HOST = "host";
    public static final String IS_STICK = "is_stick";
    public static final String IS_TV = "is_tv";
    public static final String KEYED_DEVELOPER_ID = "keyed_developer_id";
    public static final String LANGUAGE = "language";
    public static final String LOCALE = "locale";
    public static final String MODEL_NAME = "model_name";
    public static final String MODEL_NUMBER = "model_number";
    public static final String NETWORK_TYPE = "network_type";
    public static final String NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String NOTIFICATIONS_FIRST_USE = "notifications_first_use";
    public static final String POWER_MODE = "power_mode";
    public static final String SEARCH_ENABLED = "search_enabled";
    public static final String SECURE_DEVICE = "secure_device";
    public static final String SERIAL_NUMBER = "serial_number";
    public static final String SOFTWARE_BUILD = "software_build";
    public static final String SOFTWARE_VERSION = "software_version";
    public static final String SUPPORTS_AUDIO_GUIDE = "supports_audio_guide";
    public static final String SUPPORTS_FIND_REMOTE = "supports_find_remote";
    public static final String SUPPORTS_PRIVATE_LISTENING = "supports_private_listening";
    public static final String SUPPORTS_SUSPEND = "supports_suspend";
    public static final String TIME_ZONE = "time_zone";
    public static final String TIME_ZONE_OFFSET = "time_zone_offset";
    public static final String UDN = "udn";
    public static final String USER_DEVICE_NAME = "user_device_name";
    public static final String VENDOR_NAME = "vendor_name";
    public static final String VOICE_SEARCH_ENABLED = "voice_search_enabled";
    public static final String WIFI_MAC = "wifi_mac";
    private static final String DATABASE_NAME = "devices";
    private static final int DATABASE_VERSION = 4;
    private static final String[] DEVICES_TABLE_ALTER_VERSION_FOUR = {"ALTER TABLE devices ADD COLUMN custom_user_device_name TEXT;"};
    private static final String[] DEVICES_TABLE_ALTER_VERSION_THREE = {"ALTER TABLE devices ADD COLUMN is_tv TEXT;", "ALTER TABLE devices ADD COLUMN is_stick TEXT;"};
    private static final String[] DEVICES_TABLE_ALTER_VERSION_TWO = {"ALTER TABLE devices ADD COLUMN supports_suspend TEXT;", "ALTER TABLE devices ADD COLUMN supports_find_remote TEXT;", "ALTER TABLE devices ADD COLUMN supports_audio_guide TEXT;", "ALTER TABLE devices ADD COLUMN supports_private_listening TEXT;"};
    private static final String DEVICES_TABLE_CREATE = "CREATE TABLE devices (host TEXT,udn TEXT,serial_number TEXT PRIMARY KEY,device_id TEXT,vendor_name TEXT,model_number TEXT,model_name TEXT,wifi_mac TEXT,ethernet_mac TEXT,network_type TEXT,user_device_name TEXT,software_version TEXT,software_build TEXT,secure_device TEXT,language TEXT,country TEXT,locale TEXT,time_zone TEXT,time_zone_offset TEXT,power_mode TEXT,supports_suspend TEXT,supports_find_remote TEXT,supports_audio_guide TEXT,developer_enabled TEXT,keyed_developer_id TEXT,search_enabled TEXT,voice_search_enabled TEXT,notifications_enabled TEXT,notifications_first_use TEXT,supports_private_listening TEXT,headphones_connected TEXT,is_tv TEXT,is_stick TEXT,custom_user_device_name TEXT);";

    public DeviceDatabase(Context context) {
        super(context, "devices", null, 4);
    }

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(DEVICES_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        int i3 = 0;
        if (i2 <= 2) {
            int i4 = 0;
            while (true) {
                String[] strArr = DEVICES_TABLE_ALTER_VERSION_TWO;
                if (i4 >= strArr.length) {
                    break;
                }
                sQLiteDatabase.execSQL(strArr[i4]);
                i4++;
            }
        }
        if (i2 <= 3) {
            int i5 = 0;
            while (true) {
                String[] strArr2 = DEVICES_TABLE_ALTER_VERSION_THREE;
                if (i5 >= strArr2.length) {
                    break;
                }
                sQLiteDatabase.execSQL(strArr2[i5]);
                i5++;
            }
        }
        if (i2 <= 4) {
            while (true) {
                String[] strArr3 = DEVICES_TABLE_ALTER_VERSION_FOUR;
                if (i3 < strArr3.length) {
                    sQLiteDatabase.execSQL(strArr3[i3]);
                    i3++;
                } else {
                    return;
                }
            }
        }
    }
}
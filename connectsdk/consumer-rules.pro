# ConnectSDK registers services/providers by class name and uses reflection.
-keep class com.connectsdk.** { *; }
-keep interface com.connectsdk.** { *; }
-keep enum com.connectsdk.** { *; }
-keep class javax.jmdns.** { *; }
-keep class org.java_websocket.** { *; }
-keep class com.dd.plist.** { *; }
-keep class com.amazon.whisperplay.** { *; }

-dontwarn com.connectsdk.**
-dontwarn javax.jmdns.**
-dontwarn org.java_websocket.**
-dontwarn com.dd.plist.**
-dontwarn com.amazon.whisperplay.**
-dontwarn org.apache.http.**

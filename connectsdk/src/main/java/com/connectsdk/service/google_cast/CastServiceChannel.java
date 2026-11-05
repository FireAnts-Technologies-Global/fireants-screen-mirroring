package com.connectsdk.service.google_cast;

import com.connectsdk.core.Util;
import com.connectsdk.service.sessions.CastWebAppSession;
import com.connectsdk.service.sessions.WebAppSessionListener;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;

import org.json.JSONException;
import org.json.JSONObject;

public class CastServiceChannel implements Cast.MessageReceivedCallback {
    final CastWebAppSession session;
    final String webAppId;
    JSONObject jSONObject = null;

    public CastServiceChannel(String str, CastWebAppSession castWebAppSession) {
        this.webAppId = str;
        this.session = castWebAppSession;
    }

    public String getNamespace() {
        return "urn:x-cast:com.connectsdk";
    }

    public void onMessageReceived(CastDevice castDevice, String str, final String str2) {
        final WebAppSessionListener webAppSessionListener = this.session.getWebAppSessionListener();
        if (webAppSessionListener != null) {
            try {
                jSONObject = new JSONObject(str2);
            } catch (JSONException e2) {
                e2.printStackTrace();
            } catch (NullPointerException e3) {
                e3.printStackTrace();
            }
            Util.runOnUI(new Runnable() {
                public void run() {
                    if (jSONObject == null) {
                        webAppSessionListener.onReceiveMessage(CastServiceChannel.this.session, str2);
                    } else {
                        webAppSessionListener.onReceiveMessage(CastServiceChannel.this.session, jSONObject);
                    }
                }
            });
        }
    }
}
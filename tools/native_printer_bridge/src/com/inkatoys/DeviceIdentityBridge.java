package com.inkatoys;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import java.nio.charset.Charset;
import java.security.MessageDigest;

public class DeviceIdentityBridge {
    private final Context context;

    public DeviceIdentityBridge(Context context) {
        this.context = context.getApplicationContext();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private String androidId() {
        try {
            String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "" : id;
        } catch (Throwable ignored) {
            return "";
        }
    }

    @JavascriptInterface
    public String getDeviceKey() {
        try {
            String raw = androidId() + "|" + context.getPackageName();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(raw.getBytes(Charset.forName("UTF-8"))));
        } catch (Throwable ignored) {
            return "";
        }
    }

    @JavascriptInterface public String getManufacturer() { return Build.MANUFACTURER == null ? "" : Build.MANUFACTURER; }
    @JavascriptInterface public String getModel() { return Build.MODEL == null ? "" : Build.MODEL; }
    @JavascriptInterface public String getAndroidVersion() { return Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE; }
    @JavascriptInterface public int getSdkInt() { return Build.VERSION.SDK_INT; }
}

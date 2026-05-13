package com.hmdm.launcher.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager {
    private static final String TAG = "HotspotManager";

    /**
     * Enables Wi-Fi hotspot (tethering) on the device.
     * Uses ConnectivityManager reflection which works for Device Owner apps on Android 8+.
     * Waits 8 seconds after being called to ensure the system is fully booted.
     */
    public static void enableHotspot(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

                if (cm == null) {
                    Log.e(TAG, "ConnectivityManager is null — cannot enable hotspot");
                    return;
                }

                Method startTethering = cm.getClass().getDeclaredMethod(
                    "startTethering",
                    int.class,
                    boolean.class,
                    Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback"),
                    Handler.class
                );
                startTethering.setAccessible(true);

                Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[]{
                        Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback")
                    },
                    (proxy, method, args) -> {
                        Log.i(TAG, "Tethering callback fired: " + method.getName());
                        return null;
                    }
                );

                // 0 = TETHERING_WIFI
                startTethering.invoke(cm, 0, false, callback, null);
                Log.i(TAG, "Hotspot enable command sent successfully");

            } catch (Exception e) {
                Log.e(TAG, "Failed to enable hotspot: " + e.getMessage());
            }
        }, 8000); // 8 second delay to wait for full system boot
    }
}

package com.hmdm.launcher.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.TetheringManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public class HotspotManager {
    private static final String TAG = "HotspotManager";

    /**
     * Enables Wi-Fi hotspot (tethering) on every boot.
     * - Android 12+ (API 31+): uses the public TetheringManager API.
     * - Android 8-11           : uses ConnectivityManager reflection.
     * Waits 8 seconds after boot to ensure the Wi-Fi stack is fully ready.
     */
    public static void enableHotspot(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                enableHotspotModern(context);
            } else {
                enableHotspotLegacy(context);
            }
        }, 8000); // 8-second delay for full system boot
    }

    // ---- Android 12+ (API 31+) ----
    @RequiresApi(Build.VERSION_CODES.S)
    private static void enableHotspotModern(Context context) {
        try {
            TetheringManager tm = context.getSystemService(TetheringManager.class);
            if (tm == null) {
                Log.e(TAG, "TetheringManager is null — cannot enable hotspot");
                return;
            }

            TetheringManager.TetheringRequest request =
                new TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI)
                    .build();

            Executor executor = Runnable::run;

            tm.startTethering(request, executor, new TetheringManager.StartTetheringCallback() {
                @Override
                public void onTetheringStarted() {
                    Log.i(TAG, "Hotspot started successfully (TetheringManager)");
                }

                @Override
                public void onTetheringFailed(int error) {
                    Log.e(TAG, "Hotspot failed to start, error code: " + error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hotspot on Android 12+: " + e.getMessage());
        }
    }

    // ---- Android 8-11 (API 26-30) ----
    private static void enableHotspotLegacy(Context context) {
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
            Log.i(TAG, "Hotspot enable command sent successfully (legacy reflection)");

        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hotspot on Android 8-11: " + e.getMessage());
        }
    }
}

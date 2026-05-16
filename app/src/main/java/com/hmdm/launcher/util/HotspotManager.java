package com.hmdm.launcher.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager {
    private static final String TAG = "HotspotManager";

    /**
     * Enables Wi-Fi hotspot (tethering) on every boot.
     * - Android 12+ (API 31+): uses TetheringManager via reflection
     *   (TetheringManager is not in the standard SDK jar so it cannot be imported directly).
     * - Android 8-11: uses ConnectivityManager.startTethering() via reflection.
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

    // ---- Android 12+ (API 31+) — TetheringManager via reflection ----
    private static void enableHotspotModern(Context context) {
        try {
            // "tethering" is the system service name for TetheringManager on Android 12+
            Object tm = context.getSystemService("tethering");
            if (tm == null) {
                Log.e(TAG, "TetheringManager is null — cannot enable hotspot");
                return;
            }

            // Build TetheringRequest for TETHERING_WIFI (type = 0)
            Class<?> builderClass =
                Class.forName("android.net.TetheringManager$TetheringRequest$Builder");
            java.lang.reflect.Constructor<?> ctor =
                builderClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            Object requestBuilder = ctor.newInstance(0); // 0 = TETHERING_WIFI

            Method buildMethod = builderClass.getDeclaredMethod("build");
            buildMethod.setAccessible(true);
            Object request = buildMethod.invoke(requestBuilder);

            // Create a StartTetheringCallback proxy
            Class<?> callbackClass =
                Class.forName("android.net.TetheringManager$StartTetheringCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                context.getClassLoader(),
                new Class[]{callbackClass},
                (proxy, method, args) -> {
                    Log.i(TAG, "TetheringManager callback: " + method.getName());
                    return null;
                }
            );

            // Call startTethering(TetheringRequest, Executor, StartTetheringCallback)
            Method startTethering = tm.getClass().getMethod(
                "startTethering",
                Class.forName("android.net.TetheringManager$TetheringRequest"),
                java.util.concurrent.Executor.class,
                callbackClass
            );
            startTethering.invoke(
                tm,
                request,
                (java.util.concurrent.Executor) Runnable::run,
                callback
            );
            Log.i(TAG, "Hotspot enable command sent successfully (Android 12+ TetheringManager)");

        } catch (Exception e) {
            Log.e(TAG, "Failed to enable hotspot on Android 12+: " + e.getMessage());
        }
    }

    // ---- Android 8-11 (API 26-30) — ConnectivityManager via reflection ----
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

package com.hmdm.launcher.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager extends Service {
    private static final String TAG = "HotspotManager";

    public static void enableHotspot(Context context) {
        Log.i(TAG, "Starting HotspotService");
        Intent intent = new Intent(context, HotspotManager.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "HotspotService started — waiting 8 seconds");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.i(TAG, "Attempting to enable hotspot via TetheringManager");
            try {
                // Get TetheringManager — available as a system service on Android 10+
                Object tetheringManager = getSystemService("tethering");

                if (tetheringManager == null) {
                    Log.e(TAG, "TetheringManager is null — trying connectivity approach");
                    tryConnectivityManagerApproach();
                    return;
                }

                Log.i(TAG, "Got TetheringManager: " + tetheringManager.getClass().getName());

                // Log available methods for debugging
                for (Method m : tetheringManager.getClass().getDeclaredMethods()) {
                    if (m.getName().contains("tart") || m.getName().contains("ether")) {
                        Log.i(TAG, "Method: " + m.getName() + " params: " + m.getParameterCount());
                    }
                }

                // Find startTethering method
                Method startTethering = null;
                for (Method m : tetheringManager.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("startTethering")) {
                        startTethering = m;
                        Log.i(TAG, "Found startTethering with " + m.getParameterCount() + " params: " + java.util.Arrays.toString(m.getParameterTypes()));
                        break;
                    }
                }

                if (startTethering == null) {
                    Log.e(TAG, "startTethering not found on TetheringManager");
                    tryConnectivityManagerApproach();
                    return;
                }

                startTethering.setAccessible(true);

                // Build TetheringRequest via reflection
                Class<?> requestBuilderClass = Class.forName("android.net.TetheringManager$TetheringRequest$Builder");
                Object builder = requestBuilderClass.getDeclaredConstructor(int.class).newInstance(0); // 0 = TETHERING_WIFI

                // setShouldShowEntitlementUi(false)
                try {
                    Method setShouldShow = requestBuilderClass.getDeclaredMethod("setShouldShowEntitlementUi", boolean.class);
                    setShouldShow.invoke(builder, false);
                } catch (Exception e) {
                    Log.w(TAG, "setShouldShowEntitlementUi not found, continuing");
                }

                Method buildMethod = requestBuilderClass.getDeclaredMethod("build");
                Object request = buildMethod.invoke(builder);

                // Create callback proxy
                Class<?> callbackClass = Class.forName("android.net.TetheringManager$StartTetheringCallback");
                Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    getClassLoader(),
                    new Class[]{ callbackClass },
                    (proxy, method, args) -> {
                        Log.i(TAG, "TetheringCallback: " + method.getName());
                        stopSelf();
                        return null;
                    }
                );

                // startTethering(TetheringRequest, Executor, StartTetheringCallback)
                startTethering.invoke(tetheringManager, request, getMainExecutor(), callback);
                Log.i(TAG, "startTethering invoked successfully on TetheringManager");

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                stopSelf();
            }
        }, 8000);
        return START_STICKY;
    }

    private void tryConnectivityManagerApproach() {
        Log.i(TAG, "Trying ConnectivityManager approach");
        try {
            Object cm = getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) { Log.e(TAG, "CM null"); stopSelf(); return; }

            for (Method m : cm.getClass().getDeclaredMethods()) {
                if (m.getName().contains("ether") || m.getName().contains("Tether")) {
                    Log.i(TAG, "CM Method: " + m.getName() + " params: " + java.util.Arrays.toString(m.getParameterTypes()));
                }
            }
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "CM Exception: " + e.getMessage());
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

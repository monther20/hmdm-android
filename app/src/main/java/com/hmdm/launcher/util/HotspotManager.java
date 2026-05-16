package com.hmdm.launcher.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Log;

import java.lang.reflect.Field;
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
            Log.i(TAG, "Attempting to enable hotspot via IConnectivityManager");
            try {
                ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);

                if (cm == null) {
                    Log.e(TAG, "ConnectivityManager is null");
                    stopSelf();
                    return;
                }

                // Get the internal IConnectivityManager service
                Field mServiceField = ConnectivityManager.class.getDeclaredField("mService");
                mServiceField.setAccessible(true);
                Object internalCm = mServiceField.get(cm);

                if (internalCm == null) {
                    Log.e(TAG, "Internal ConnectivityManager service is null");
                    stopSelf();
                    return;
                }

                Log.i(TAG, "Got internal CM: " + internalCm.getClass().getName());

                Class<?> internalCmClass = Class.forName("android.net.IConnectivityManager");
                ResultReceiver dummyReceiver = new ResultReceiver(null);

                // Try 3-param version first
                try {
                    Method startTethering = internalCmClass.getDeclaredMethod(
                        "startTethering",
                        int.class,
                        ResultReceiver.class,
                        boolean.class
                    );
                    startTethering.invoke(internalCm, 0, dummyReceiver, false);
                    Log.i(TAG, "startTethering (3-param) invoked successfully");

                } catch (NoSuchMethodException e1) {
                    Log.i(TAG, "3-param not found, trying 4-param with package name");

                    // Try 4-param version (newer devices add callingPkg)
                    try {
                        Method startTethering = internalCmClass.getDeclaredMethod(
                            "startTethering",
                            int.class,
                            ResultReceiver.class,
                            boolean.class,
                            String.class
                        );
                        startTethering.invoke(internalCm, 0, dummyReceiver, false, getPackageName());
                        Log.i(TAG, "startTethering (4-param) invoked successfully");

                    } catch (NoSuchMethodException e2) {
                        Log.e(TAG, "Neither 3-param nor 4-param startTethering found");

                        // Log all available methods for debugging
                        for (Method m : internalCmClass.getDeclaredMethods()) {
                            if (m.getName().contains("ether") || m.getName().contains("Tether")) {
                                Log.i(TAG, "Available: " + m.getName() + " params: " + m.getParameterTypes().length);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            } finally {
                stopSelf();
            }
        }, 8000);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}

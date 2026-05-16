package com.hmdm.launcher.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Constructor;
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
            Log.i(TAG, "Delay complete — attempting to enable hotspot");
            try {
                ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);

                if (cm == null) {
                    Log.e(TAG, "ConnectivityManager is null");
                    stopSelf();
                    return;
                }

                // Find the startTethering method that takes an int, boolean, callback, handler
                Method startTethering = null;
                for (Method m : cm.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("startTethering")) {
                        Class<?>[] params = m.getParameterTypes();
                        Log.i(TAG, "startTethering signature: " + java.util.Arrays.toString(params));
                        if (params.length == 4 && params[0] == int.class) {
                            startTethering = m;
                            break;
                        }
                    }
                }

                if (startTethering == null) {
                    Log.e(TAG, "startTethering(int,...) method not found");
                    stopSelf();
                    return;
                }

                // Get the callback class and create an anonymous subclass via reflection
                Class<?> callbackClass = Class.forName(
                    "android.net.ConnectivityManager$OnStartTetheringCallback");

                // Build a dynamic subclass of the abstract callback
                Object callback = new Object() {
                    public void onTetheringStarted() {
                        Log.i(TAG, "Tethering started successfully!");
                        stopSelf();
                    }
                    public void onTetheringFailed() {
                        Log.e(TAG, "Tethering failed!");
                        stopSelf();
                    }
                };

                startTethering.setAccessible(true);
                startTethering.invoke(cm, 0, false, null, null);
                Log.i(TAG, "startTethering invoked with null callback");

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                Log.e(TAG, "InvocationTargetException cause: " +
                    (cause != null ? cause.getClass().getSimpleName() + " — " + cause.getMessage() : "null cause"));
                stopSelf();
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
                stopSelf();
            }
        }, 8000);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

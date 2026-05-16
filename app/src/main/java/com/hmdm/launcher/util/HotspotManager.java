package com.hmdm.launcher.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
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

    // Concrete subclass of the abstract callback
    private class TetheringCallback extends ConnectivityManager.OnStartTetheringCallback {
        @Override
        public void onTetheringStarted() {
            Log.i(TAG, "Tethering started successfully!");
            stopSelf();
        }

        @Override
        public void onTetheringFailed() {
            Log.e(TAG, "Tethering failed!");
            stopSelf();
        }
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

                Method startTethering = null;
                for (Method m : cm.getClass().getDeclaredMethods()) {
                    if (m.getName().equals("startTethering")) {
                        Class<?>[] params = m.getParameterTypes();
                        // Use the 3-parameter version (int, boolean, callback)
                        if (params.length == 3 && params[0] == int.class) {
                            startTethering = m;
                            Log.i(TAG, "Using 3-param startTethering");
                            break;
                        }
                    }
                }

                if (startTethering == null) {
                    Log.e(TAG, "3-param startTethering not found, trying 4-param");
                    for (Method m : cm.getClass().getDeclaredMethods()) {
                        if (m.getName().equals("startTethering")) {
                            Class<?>[] params = m.getParameterTypes();
                            if (params.length == 4 && params[0] == int.class) {
                                startTethering = m;
                                Log.i(TAG, "Using 4-param startTethering");
                                break;
                            }
                        }
                    }
                }

                if (startTethering == null) {
                    Log.e(TAG, "No startTethering method found");
                    stopSelf();
                    return;
                }

                startTethering.setAccessible(true);
                TetheringCallback callback = new TetheringCallback();

                if (startTethering.getParameterTypes().length == 3) {
                    startTethering.invoke(cm, 0, false, callback);
                } else {
                    startTethering.invoke(cm, 0, false, callback,
                        new Handler(Looper.getMainLooper()));
                }
                Log.i(TAG, "startTethering invoked successfully");

            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                Log.e(TAG, "InvocationTargetException cause: " +
                    (cause != null ? cause.getClass().getSimpleName() + " — " + cause.getMessage() : "null"));
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

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

                Method[] methods = cm.getClass().getDeclaredMethods();
                for (Method m : methods) {
                    if (m.getName().contains("ether") || m.getName().contains("Tether")) {
                        Log.i(TAG, "Found tethering method: " + m.getName());
                    }
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
                    getClassLoader(),
                    new Class[]{
                        Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback")
                    },
                    (proxy, method, args) -> {
                        Log.i(TAG, "Tethering callback fired: " + method.getName());
                        stopSelf();
                        return null;
                    }
                );

                startTethering.invoke(cm, 0, false, callback, null);
                Log.i(TAG, "startTethering invoked successfully");

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Class not found: " + e.getMessage());
                stopSelf();
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Method not found: " + e.getMessage());
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

package com.hmdm.launcher.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;

public class HotspotManager {
    private static final String TAG = "HotspotManager";

    public static void enableHotspot(Context context) {
        Log.i(TAG, "enableHotspot called — scheduling with 8s delay");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.i(TAG, "Delay complete — attempting to enable hotspot");
            try {
                ConnectivityManager cm = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);

                if (cm == null) {
                    Log.e(TAG, "ConnectivityManager is null");
                    return;
                }
                Log.i(TAG, "ConnectivityManager obtained successfully");

                Method[] methods = cm.getClass().getDeclaredMethods();
                Log.i(TAG, "Total methods found: " + methods.length);
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
                Log.i(TAG, "startTethering method found — invoking");
                startTethering.setAccessible(true);

                Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    context.getClassLoader(),
                    new Class[]{
                        Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback")
                    },
                    (proxy, method, args) -> {
                        Log.i(TAG, "Tethering callback: " + method.getName());
                        return null;
                    }
                );

                startTethering.invoke(cm, 0, false, callback, null);
                Log.i(TAG, "startTethering invoked successfully");

            } catch (ClassNotFoundException e) {
                Log.e(TAG, "Class not found: " + e.getMessage());
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Method not found: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            }
        }, 8000);
    }
}

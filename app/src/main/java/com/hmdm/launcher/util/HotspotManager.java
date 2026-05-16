package com.hmdm.launcher.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
            Log.i(TAG, "Attempting to enable hotspot via shell command");
            try {
                Process process = Runtime.getRuntime().exec(
                    new String[]{"cmd", "wifi", "start-softap", "", "open"}
                );

                BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));

                StringBuilder out = new StringBuilder();
                StringBuilder err = new StringBuilder();
                String line;

                while ((line = stdout.readLine()) != null) out.append(line).append("\n");
                while ((line = stderr.readLine()) != null) err.append(line).append("\n");

                int exitCode = process.waitFor();

                Log.i(TAG, "Exit code: " + exitCode);
                if (out.length() > 0) Log.i(TAG, "Output: " + out.toString().trim());
                if (err.length() > 0) Log.e(TAG, "Error: " + err.toString().trim());

                if (exitCode == 0) {
                    Log.i(TAG, "Hotspot enabled successfully");
                } else {
                    Log.e(TAG, "Command failed — exit code " + exitCode);
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

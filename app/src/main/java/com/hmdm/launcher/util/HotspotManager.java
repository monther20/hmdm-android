package com.hmdm.launcher.util;

import android.content.Context;
import android.net.TetheringManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class HotspotManager {
    private static final String TAG = "HotspotManager";

    public static void enableHotspot(Context context) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                TetheringManager tetheringManager =
                    context.getSystemService(TetheringManager.class);

                if (tetheringManager == null) {
                    Log.e(TAG, "TetheringManager is null");
                    return;
                }

                TetheringManager.TetheringRequest request =
                    new TetheringManager.TetheringRequest.Builder(
                        TetheringManager.TETHERING_WIFI)
                        .setShouldShowEntitlementUi(false)
                        .build();

                tetheringManager.startTethering(
                    request,
                    context.getMainExecutor(),
                    new TetheringManager.StartTetheringCallback() {
                        @Override
                        public void onTetheringStarted() {
                            Log.i(TAG, "Hotspot enabled successfully");
                        }
                        @Override
                        public void onTetheringFailed(int error) {
                            Log.e(TAG, "Hotspot failed with error: " + error);
                        }
                    }
                );

            } catch (Exception e) {
                Log.e(TAG, "Exception enabling hotspot: " + e.getMessage());
            }
        }, 8000);
    }
}

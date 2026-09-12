package com.saadtopup.smsgateway;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

/**
 * Background service — every 3 minutes silent SMS scan when Gateway is ONLINE.
 * User sees nothing while scanning. Only "Last auto scan" time updates in UI.
 */
public class AutoScanService extends Service {
    private static final String TAG = "SaadAutoScan";
    private static final long INTERVAL_MS = 3 * 60 * 1000L; // 3 minutes

    private HandlerThread workerThread;
    private Handler workerHandler;
    private PowerManager.WakeLock wakeLock;
    private boolean running = false;

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            try {
                if (AppPrefs.isOnline(getApplicationContext())) {
                    // Brief wake lock so scan finishes even if screen is off
                    acquireWakeLock();
                    try {
                        SilentScanner.scan(getApplicationContext());
                    } finally {
                        releaseWakeLock();
                    }
                } else {
                    Log.d(TAG, "Offline — waiting");
                }
            } catch (Exception e) {
                Log.e(TAG, "Scan task error", e);
            }

            // Schedule next run
            if (running && workerHandler != null) {
                workerHandler.postDelayed(this, INTERVAL_MS);
            }
        }
    };

    public static void start(Context context) {
        try {
            Intent i = new Intent(context, AutoScanService.class);
            context.startService(i);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
    }

    public static void stop(Context context) {
        try {
            Intent i = new Intent(context, AutoScanService.class);
            context.stopService(i);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop service", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        workerThread = new HandlerThread("SaadAutoScanWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            // First scan after 10 seconds, then every 3 min
            workerHandler.removeCallbacks(scanTask);
            workerHandler.postDelayed(scanTask, 10_000);
            Log.d(TAG, "Auto scan started (every 3 min)");
        }
        // Sticky so system restarts if killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (workerHandler != null) {
            workerHandler.removeCallbacks(scanTask);
        }
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        releaseWakeLock();
        Log.d(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void acquireWakeLock() {
        try {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (pm != null) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SaadGateway:ScanLock");
                    wakeLock.setReferenceCounted(false);
                }
            }
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(30_000); // max 30 sec
            }
        } catch (Exception ignored) {
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {
        }
    }
}

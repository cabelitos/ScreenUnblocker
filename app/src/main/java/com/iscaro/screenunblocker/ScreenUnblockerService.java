package com.iscaro.screenunblocker;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.iscaro.screenunblocker.provider.ScreenUnblockerContentProvider;
import com.iscaro.screenunblocker.provider.WifiNetwork;

public class ScreenUnblockerService extends Service {

    private class WifiStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast received. Action:" + action);
            if (Intent.ACTION_USER_PRESENT.equals(action) && mNeedsToDisableKeyguard) {
                mNeedsToDisableKeyguard = false;
                changeKeyguardStatus(true);
            } else {
                unlockScreenIfNeeded();
            }
        }
    }

    private class WifiNetworkObserver extends ContentObserver {
        public WifiNetworkObserver(Handler handler) {
            super(handler);
        }

        private void dispatchLoader() {
            unlockScreenIfNeeded();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(TAG, "Database changed");
            dispatchLoader();
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "Database changed");
            dispatchLoader();
        }
    }

    private class UnlockRunnable implements Runnable {
        @Override
        public void run() {
            KeyguardManager km =(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            mLock = km.newKeyguardLock(KEYGUARD_TAG);
            mLock.disableKeyguard();
        }
    }

    private static final String TAG = "ScreenUnblockerService";
    private WifiStatusReceiver mReceiver;
    private KeyguardManager.KeyguardLock mLock;
    private WifiNetworkObserver mObserver;
    private boolean mNeedsToDisableKeyguard;
    private static final String KEYGUARD_TAG = "com.iscaro.ScreenUnblocker";
    private static final String WIFI_UNBLOCKER_ENABLED_KEY = "com.iscaro.ScreenUnblocker.enabled_key";
    private static final String WIFI_UNBLOCKER_PREFS_FILE = "com.iscaro.ScreenUnblocker.prefs_file";

    public static final String DISABLE_SERVICE_ACTION = "com.iscaro.ScreenUnblocker.disable_action";
    public static final String ENABLE_SERVICE_ACTION = "com.iscaro.ScreenUnblocker.enable_action";
    private static final int NOTIFICATION_ID = 1;
    private Handler mHandler;
    private final UnlockRunnable mUnlockRunnable = new UnlockRunnable();

    public static boolean isServiceEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(WIFI_UNBLOCKER_PREFS_FILE,
                Context.MODE_PRIVATE);
        return prefs.getBoolean(WIFI_UNBLOCKER_ENABLED_KEY, true);
    }

    private void unlockScreenIfNeeded() {

        if (!isServiceEnabled(this)) {
            Log.d(TAG, "ScreenUnblocker is disabled");
            return;
        }

        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();

        if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI &&
                info.isConnected()) {

            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = wifiManager.getConnectionInfo();

            if (wInfo != null && isKnownWiFi(wInfo.getSSID())) {
                Log.d(TAG, "Disabling keyguard. Wifi ssid:"+wInfo.getSSID());
                changeKeyguardStatus(true);
            } else {
                changeKeyguardStatus(false);
            }
        } else {
            changeKeyguardStatus(false);
        }
    }

    private boolean isKnownWiFi(String ssid) {
        //Android adds "" to the ssid, remove it.
        return WifiNetwork.queryBySSIDAndAddress(this, ssid.replace("\"", "")) != null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not supported");
    }

    private void changeKeyguardStatus(boolean unlock) {
        KeyguardManager km =(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if (!unlock && mLock != null) {
            Log.d(TAG, "Enabling the keyguard");
            mHandler.removeCallbacks(mUnlockRunnable);
            mLock.reenableKeyguard();
            mLock = null;
            mNeedsToDisableKeyguard = false;
        } else if (unlock) {
            Log.d(TAG, "Disabling the keyguard");
            if (km.inKeyguardRestrictedInputMode()) {
                mNeedsToDisableKeyguard = true;
            } else {
                if (mLock != null) {
                    mLock.reenableKeyguard();
                } else {
                    mNeedsToDisableKeyguard = true;
                }
                mHandler.removeCallbacks(mUnlockRunnable);
                mHandler.postDelayed(mUnlockRunnable, 300);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            return START_STICKY;
        }

        SharedPreferences.Editor editor = getSharedPreferences(WIFI_UNBLOCKER_PREFS_FILE,
                Context.MODE_PRIVATE).edit();
        String action = intent.getAction();
        if (DISABLE_SERVICE_ACTION.equals(action)) {
            editor.putBoolean(WIFI_UNBLOCKER_ENABLED_KEY, false);
            editor.commit();
            changeKeyguardStatus(false);
            stopSelf();
        } else if (ENABLE_SERVICE_ACTION.equals(action)) {
            editor.putBoolean(WIFI_UNBLOCKER_ENABLED_KEY, true);
            editor.commit();
            unlockScreenIfNeeded();
        }

        return START_STICKY;
    }

    private void start() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.notif_text));
        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setContentIntent(pending);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mReceiver = new WifiStatusReceiver();
        registerReceiver(mReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
        mObserver = new WifiNetworkObserver(new Handler());
        getContentResolver().registerContentObserver(
                ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, true, mObserver);
        unlockScreenIfNeeded();
        start();
        Log.d(TAG,"Starting the service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        getContentResolver().unregisterContentObserver(mObserver);
        changeKeyguardStatus(false);
        stopForeground(true);
        Log.d(TAG,"Destroying the service");
    }
}

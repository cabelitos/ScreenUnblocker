package com.iscaro.screenunblocker;

import android.app.KeyguardManager;
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
import android.util.Log;

import com.iscaro.screenunblocker.provider.ScreenUnblockerContentProvider;
import com.iscaro.screenunblocker.provider.WifiNetwork;

public class ScreenUnblockerService extends Service {

    private class WifiStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
                unlockScreenIfNeeded();
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

    private static final String TAG = "ScreenUnblockerService";
    private WifiStatusReceiver mReceiver;
    private KeyguardManager.KeyguardLock mLock;
    private WifiNetworkObserver mObserver;
    private boolean mIsDisabled;
    private static final String KEYGUARD_TAG = "com.iscaro.ScreenUnblocker";
    private static final String WIFI_UNBLOCKER_ENABLED_KEY = "com.iscaro.ScreenUnblocker.enabled_key";
    private static final String WIFI_UNBLOCKER_PREFS_FILE = "com.iscaro.ScreenUnblocker.prefs_file";

    public static final String DISABLE_SERVICE_ACTION = "com.iscaro.ScreenUnblocker.disable_action";
    public static final String ENABLE_SERVICE_ACTION = "com.iscaro.ScreenUnblocker.enable_action";

    public static final boolean isServiceEnabled(Context context) {
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
                mLock.disableKeyguard();
                mIsDisabled = true;
            } else {
                lockScreenIfNeeded();
            }
        } else {
            lockScreenIfNeeded();
        }

    }

    private boolean isKnownWiFi(String ssid) {
        //Android adds "" to the ssid, remove it.
        if (WifiNetwork.queryBySSIDAndAddress(this, ssid.replace("\"", "")) != null) {
            return true;
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not supported");
    }

    private void lockScreenIfNeeded() {
        if (mIsDisabled) {
            mLock.reenableKeyguard();
            mIsDisabled = false;
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
            lockScreenIfNeeded();
            stopSelf();
        } else if (ENABLE_SERVICE_ACTION.equals(action)) {
            editor.putBoolean(WIFI_UNBLOCKER_ENABLED_KEY, true);
            editor.commit();
            unlockScreenIfNeeded();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new WifiStatusReceiver();
        registerReceiver(mReceiver, new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION));
        mObserver = new WifiNetworkObserver(new Handler());
        getContentResolver().registerContentObserver(
                ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, true, mObserver);
        KeyguardManager km =(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        mLock = km.newKeyguardLock(KEYGUARD_TAG);
        unlockScreenIfNeeded();
        Log.d(TAG,"Starting the service");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        getContentResolver().unregisterContentObserver(mObserver);
        lockScreenIfNeeded();
        Log.d(TAG,"Destroying the service");
    }
}

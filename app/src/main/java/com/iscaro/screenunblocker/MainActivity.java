package com.iscaro.screenunblocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.iscaro.screenunblocker.provider.ScreenUnblockerContentProvider;
import com.iscaro.screenunblocker.provider.WifiNetwork;

import java.util.List;


public class MainActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> results = wifi.getScanResults();
            mAdapter.removeUnknownNetworks();
            mAdapter.add(WifiNetwork.fromScanResult(results));
        }
    }

    private class WifiNetworkObserver extends ContentObserver {
        public WifiNetworkObserver(Handler handler) {
            super(handler);
        }

        private void dispatchLoader() {
            MainActivity.this.getSupportLoaderManager().restartLoader(LOADER_ID,
                    null, MainActivity.this);
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifi.startScan();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            dispatchLoader();
        }

        @Override
        public void onChange(boolean selfChange) {
            dispatchLoader();
        }
    }

    private static final String TAG = "MainActivity";
    private static final int LOADER_ID = 1;
    private WifiScanReceiver mReceiver;
    private WifiNetworkAdapter mAdapter;
    private WifiNetworkObserver mObserver;
    private boolean mRegistered;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return WifiNetwork.getCursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG, "Load finished");
        mAdapter.removeKnownNetworks();
        mAdapter.add(WifiNetwork.fromCursor(cursor));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG, "Resiting loader");
        mAdapter.removeKnownNetworks();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView list = (ListView) findViewById(android.R.id.list);
        list.setEmptyView(findViewById(android.R.id.empty));
        mAdapter = new WifiNetworkAdapter(this, R.layout.list_item,
                new WifiNetworkAdapter.WifiNetworkOnCheckedChanged() {
                    @Override
                    public void onCheckChanged(WifiNetwork network, boolean checked) {
                        if (checked) {
                            Log.d(TAG, "Inserting " + network);
                            WifiNetwork.addWifiNetwork(MainActivity.this.getContentResolver(),
                                    network);
                        } else {
                            Log.d(TAG, "Removing " + network);
                            WifiNetwork.removeWifiNetwork(MainActivity.this.getContentResolver(),
                                    network);
                        }
                    }
                });
        list.setAdapter(mAdapter);
        Switch s = (Switch)findViewById(R.id.switch_enable);
        boolean enabled = ScreenUnblockerService.isServiceEnabled(this);
        s.setChecked(enabled);
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                //Maybe I should bind the service...
                Intent i = new Intent(MainActivity.this, ScreenUnblockerService.class);
                if (b) {
                    i.setAction(ScreenUnblockerService.ENABLE_SERVICE_ACTION);
                    registerListeners();
                    getSupportLoaderManager().initLoader(LOADER_ID, null, MainActivity.this);
                } else {
                    i.setAction(ScreenUnblockerService.DISABLE_SERVICE_ACTION);
                    mAdapter.clear();
                    unregisterListeners();
                }
                startService(i);
            }
        });
        if (enabled) {
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        }
        mReceiver = new WifiScanReceiver();
        // Maybe the service is not running, start it.
        startService(new Intent(this, ScreenUnblockerService.class));
    }

    private void unregisterListeners() {
        if (mRegistered) {
            unregisterReceiver(mReceiver);
            getContentResolver().unregisterContentObserver(mObserver);
            mRegistered = false;
        }
    }

    private void registerListeners() {
        if (!mRegistered && ScreenUnblockerService.isServiceEnabled(this)) {
            registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            wifi.startScan();
            mObserver = new WifiNetworkObserver(new Handler());
            getContentResolver().registerContentObserver(
                    ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, true, mObserver);
            mRegistered = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerListeners();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterListeners();
    }
}

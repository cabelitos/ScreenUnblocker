package com.iscaro.screenunblocker.provider;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Created by iscaro on 10/19/14.
 */
public class WifiNetwork implements Comparable<WifiNetwork> {

    private static final String TAG = "WifiNetwork";
    private static final long ID_NOT_SET = -1;
    private long mId;
    private String mSsid;
    private boolean mKnown;

    private static class AsyncQuery extends AsyncQueryHandler {
        public AsyncQuery(ContentResolver resolver) {
            super(resolver);
        }
    }

    public static final class WifiNetworkComparator implements Comparator<WifiNetwork> {
        @Override
        public int compare(WifiNetwork wifiNetwork, WifiNetwork wifiNetwork2) {
            return wifiNetwork.compareTo(wifiNetwork2);
        }
    }

    private WifiNetwork(long id, String ssid, boolean known) {
        mId = id;
        mSsid = ssid;
        mKnown = known;
    }

    private WifiNetwork() {
    }

    public static void removeWifiNetwork(ContentResolver resolver, WifiNetwork network) {
        AsyncQuery query = new AsyncQuery(resolver);
        query.startDelete(-1, null,
                Uri.parse(ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI + "/"
                        + network.getId()), null, null);
    }

    public static void addWifiNetwork(ContentResolver resolver, WifiNetwork network) {
        AsyncQuery query = new AsyncQuery(resolver);
        ContentValues values = new ContentValues();
        values.put(SqlHelper.COLUMN_SSID, network.getSsid());
        query.startInsert(-1, null, ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, values);
    }

    public static WifiNetwork queryBySSIDAndAddress(Context context, String ssid) {
        Log.d(TAG, "searching for ssid:" + ssid);
        Cursor cursor = context.getContentResolver().query(
                ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, null,
                SqlHelper.COLUMN_SSID + " = ?",
                new String[] {ssid}, null);
        List<WifiNetwork> list = fromCursor(cursor);
        if (list.size() == 0) {
            return null;
        }
        return list.get(0);
    }

    public static List<WifiNetwork> fromScanResult(Collection<ScanResult> results) {
        List<WifiNetwork> networks = new ArrayList<WifiNetwork>();
        for (ScanResult result : results) {
            if (TextUtils.isEmpty(result.SSID)) {
                continue;
            }
            WifiNetwork wn = new WifiNetwork(ID_NOT_SET, result.SSID, false);
            Log.d(TAG, "Adding:" + wn);
            networks.add(wn);
        }
        return networks;
    }

    public static List<WifiNetwork> fromCursor(Cursor cursor) {
        List<WifiNetwork> networks = new ArrayList<WifiNetwork>();
        if (cursor == null) {
            return networks;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            networks.add(new WifiNetwork(cursor.getLong(cursor.getColumnIndex(SqlHelper.COLUMN_ID)),
                    cursor.getString(cursor.getColumnIndex(SqlHelper.COLUMN_SSID)), true));
            cursor.moveToNext();
        }
        return networks;
    }

    public void updateFrom(Context context, WifiNetwork network) {
        if (network.mKnown && !mKnown) {
            mKnown = true;
            mId = network.mId;
        } else if (mKnown && !network.mKnown &&
                queryBySSIDAndAddress(context, mSsid) == null) {
            mKnown = false;
            mId = ID_NOT_SET;
        }
    }

    public static CursorLoader getCursorLoader(Context context) {
        return new CursorLoader(context,
                ScreenUnblockerContentProvider.KNOWN_NETWORKS_URI, null, null, null, null);
    }

    public void setIsKnown(boolean known) {
        mKnown = known;
    }

    public long getId() {
        return mId;
    }

    public boolean isKnown() {
        return mKnown;
    }

    public String getSsid() {
        return mSsid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WifiNetwork that = (WifiNetwork) o;

        if (mSsid != null ? !mSsid.equals(that.mSsid) : that.mSsid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)mId;
        result = 31 * result + (mSsid != null ? mSsid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "WifiNetwork{" +
                "mId=" + mId +
                ", mSsid='" + mSsid + '\'' +
                '}';
    }

    @Override
    public int compareTo(WifiNetwork wifiNetwork) {
        /* Reverse sort */
        if (this.isKnown() == wifiNetwork.isKnown())
            return 0;
        if (this.isKnown())
            return -1;
        return 1;
    }
}

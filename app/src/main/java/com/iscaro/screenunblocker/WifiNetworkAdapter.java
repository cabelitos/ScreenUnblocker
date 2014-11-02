package com.iscaro.screenunblocker;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.iscaro.screenunblocker.provider.WifiNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by iscaro on 10/19/14.
 */
public class WifiNetworkAdapter extends BaseAdapter {

    // I should create a separator for known and unknown networks
    private static final int VIEW_TYPES = 1;
    private static final String TAG = "WifiNetworkAdapter";

    //Maybe I should another collection here (It needs to be sorted all the time).
    private final List<WifiNetwork> mNetworks;
    private final WifiNetwork.WifiNetworkComparator mComparator;
    private final Context mContext;
    private final int mListItem;
    private final WifiNetworkOnCheckedChanged mOnCheckedChangedListener;

    public interface WifiNetworkOnCheckedChanged {
        public void onCheckChanged(WifiNetwork network, boolean checked);
    }

    private static class ViewHolder {
        final TextView mSsid;
        final CheckBox mCheck;
        final WifiNetworkOnCheckedChanged mListener;
        WifiNetwork mNetwork;

        public ViewHolder(TextView ssid, CheckBox check,
                          final WifiNetworkOnCheckedChanged listener) {
            mSsid = ssid;
            mCheck = check;
            mListener = listener;
        }
    }

    public WifiNetworkAdapter(Context context, int listItem,
                              WifiNetworkOnCheckedChanged onCheckedChangedListener) {
        super();
        mListItem = listItem;
        mComparator = new WifiNetwork.WifiNetworkComparator();
        mNetworks = new ArrayList<WifiNetwork>();
        mContext = context.getApplicationContext();
        mOnCheckedChangedListener = onCheckedChangedListener;
    }

    public void clear() {
        mNetworks.clear();
        notifyDataSetChanged();
    }

    private void removeNetworks(boolean knownStatus) {
        Iterator<WifiNetwork> iterator = mNetworks.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isKnown() == knownStatus) {
                iterator.remove();
            }
        }
        notifyDataSetChanged();
    }

    public void removeKnownNetworks() {
        removeNetworks(true);
    }

    public void removeUnknownNetworks() {
        removeNetworks(false);
    }

    private WifiNetwork findSiblingNetwork(WifiNetwork network) {
        for (WifiNetwork wn : mNetworks) {
            if (network.equals(wn)) {
                return wn;
            }
        }
        return null;
    }

    public void add(Collection<WifiNetwork> networks) {
        for (WifiNetwork network : networks) {
            if (!mNetworks.contains(network)) {
                mNetworks.add(network);
            } else {
                WifiNetwork wn = findSiblingNetwork(network);
                if (wn == null) {
                    Log.e(TAG, "WifiNetwork is null. This is not supposed to happen!");
                    continue;
                }
                Log.d(TAG, "Updating:" + network);
                wn.updateFrom(mContext, network);
            }
        }
        Collections.sort(mNetworks, mComparator);
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPES;
    }

    @Override
    public int getCount() {
        return mNetworks.size();
    }

    @Override
    public WifiNetwork getItem(int i) {
        return mNetworks.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        WifiNetwork network = getItem(i);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final ViewHolder holder;

        if (view == null) {
            view = inflater.inflate(mListItem, viewGroup, false);
            holder = new ViewHolder((TextView) view.findViewById(R.id.ssid),
                    (CheckBox)view.findViewById(R.id.disable_keyguard), mOnCheckedChangedListener);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.mCheck.setOnCheckedChangeListener(null);
        holder.mSsid.setText(network.getSsid());
        holder.mCheck.setChecked(network.isKnown());
        holder.mNetwork = network;
        holder.mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                holder.mListener.onCheckChanged(holder.mNetwork, b);
            }
        });
        return view;
    }
}

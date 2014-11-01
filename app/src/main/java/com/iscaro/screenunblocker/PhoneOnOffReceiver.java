package com.iscaro.screenunblocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PhoneOnOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, ScreenUnblockerService.class);

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(i);
        } else {
            context.stopService(i);
        }
    }
}

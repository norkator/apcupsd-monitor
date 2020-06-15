package com.nitramite.apcupsdmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StatusServiceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = StatusServiceBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Service stop, restart again.");
        context.startService(new Intent(context.getApplicationContext(), StatusService.class));
    }

}

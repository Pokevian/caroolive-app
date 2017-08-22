package com.pokevian.app.smartfleet.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pokevian.app.smartfleet.service.DataUploadService;
import com.pokevian.app.smartfleet.util.NetworkUtils;

/**
 * Created by ian on 2016-05-16.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtils.isConnected(context)) {
            DataUploadService.setAlarm(context, 10000);
        }
    }
}

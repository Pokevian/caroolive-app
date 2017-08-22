package com.pokevian.app.fingerpush;

import android.content.Context;

/**
 * Created by ian on 2016-04-14.
 */
public class GCMBroadcastReceiver extends com.google.android.gcm.GCMBroadcastReceiver {

    @Override
    protected String getGCMIntentServiceClassName(Context context) {
        return GCMIntentService.class.getName();
    }
}

/*
 * Copyright (c) 2015. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.service.floatinghead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by dg.kim on 2015-04-13.
 */
public class SmsHandler {
    private static final String TAG = "SmsHandler";

    private final Context mContext;
    private final Callbacks mCallbacks;
    private final SmsManager mSmsManager;
    private BroadcastReceiver mSmsReceivedReceiver;

    public SmsHandler(Context context, Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;
        mSmsManager = SmsManager.getDefault();
        registerSmsReceivedReceiver();
    }

    public void shutdown() {
        unregisterSmsReceivedReceiver();
    }

    public void sendSms(String destinationAddress, String text) {
        if (!TextUtils.isEmpty(text)) {
            ArrayList<String> parts = mSmsManager.divideMessage(text);
            if (parts != null && parts.size() > 0) {
                mSmsManager.sendMultipartTextMessage(destinationAddress, null, parts, null, null);
            } else {
                mSmsManager.sendTextMessage(destinationAddress, null, text, null, null);
            }
        } else {
            Logger.getLogger(TAG).warn("empty sms message");
        }
    }

    private void registerSmsReceivedReceiver() {
        if (mSmsReceivedReceiver == null) {
            mSmsReceivedReceiver = new SmsReceivedReceiver();
            IntentFilter filter = new IntentFilter();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
            } else {
                filter.addAction("android.provider.Telephony.SMS_RECEIVED");
            }
            mContext.registerReceiver(mSmsReceivedReceiver, filter);
        }
    }

    private void unregisterSmsReceivedReceiver() {
        if (mSmsReceivedReceiver != null) {
            mContext.unregisterReceiver(mSmsReceivedReceiver);
            mSmsReceivedReceiver = null;
        }
    }

    private class SmsReceivedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    mCallbacks.onSmsReceived(messages);
                } else {
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        Object[] pdus = (Object[]) intent.getSerializableExtra("pdus");
                        int pduCount = pdus.length;
                        SmsMessage[] messages = new SmsMessage[pduCount];
                        for (int i = 0; i < pduCount; i++) {
                            byte[] pdu = (byte[]) pdus[i];
                            messages[i] = SmsMessage.createFromPdu(pdu);
                        }
                        mCallbacks.onSmsReceived(messages);
                    }
                }
            }
        }
    }

    public interface Callbacks {
        void onSmsReceived(SmsMessage[] messages);
    }
}

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
import android.media.AudioManager;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by dg.kim on 2015-04-13.
 */
public class CallHandler {

    private static final String TAG = "CallManager";

    private final Context mContext;
    private final Callbacks mCallbacks;
    private final TelephonyManager mTelephonyManager;
    private final AudioManager mAudioManager;
    private ITelephony mTelephonyService;
    private BroadcastReceiver mPhoneStateReceiver;

    public CallHandler(Context context, Callbacks callbacks) {
        mContext = context;
        mCallbacks = callbacks;

        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        try {
            Class<?> c = Class.forName(mTelephonyManager.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            mTelephonyService = (ITelephony) m.invoke(mTelephonyManager);
        } catch (ClassNotFoundException e) {
            Logger.getLogger(TAG).warn("cannot get telephony service");
        } catch (InvocationTargetException e) {
            Logger.getLogger(TAG).warn("cannot get telephony service");
        } catch (NoSuchMethodException e) {
            Logger.getLogger(TAG).warn("cannot get telephony service");
        } catch (IllegalAccessException e) {
            Logger.getLogger(TAG).warn("cannot get telephony service");
        }

        registerPhoneStateReceiver();
    }

    public void shutdown() {
        unregisterPhoneStateReceiver();
    }

    public void endCall() {
        if (mTelephonyService != null) {
            try {
                mTelephonyService.endCall();
            } catch (RemoteException e) {
                Logger.getLogger(TAG).error("failed to end call");
            }
        }
    }

    public boolean isSpeakerphoneOn() {
        return mAudioManager.isSpeakerphoneOn();
    }

    public void setSpeakerphoneOn(boolean on) {
        mAudioManager.setSpeakerphoneOn(on);
    }

    private void registerPhoneStateReceiver() {
        if (mPhoneStateReceiver == null) {
            mPhoneStateReceiver = new PhoneStateReceiver();
            IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            mContext.registerReceiver(mPhoneStateReceiver, filter);
        }
    }

    private void unregisterPhoneStateReceiver() {
        if (mPhoneStateReceiver != null) {
            mContext.unregisterReceiver(mPhoneStateReceiver);
            mPhoneStateReceiver = null;
        }
    }

    private class PhoneStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                int state = mTelephonyManager.getCallState();
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                onPhoneStateChanged(state, incomingNumber);
            }
        }
    }

    private void onPhoneStateChanged(int state, String incomingNumber) {
        Log.i(TAG, "phone state=" + state);

        mCallbacks.onCallStateChanged(state, incomingNumber);
    }

    public interface Callbacks {
        void onCallStateChanged(int state, String incomingNumber);
    }

}

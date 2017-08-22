/*
 * Copyright (c) 2014. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.ui.driving;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.YoutubeUploadService.VideoInfo;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.TelephonyUtils;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class ErsProcessor {

    static final String TAG = "ErsProcessor";
    final Logger logger = Logger.getLogger(TAG);

    private final Context mContext;
    private final VehicleService mVehicleService;
    private final ErsTarget mErsTarget;
    private final VideoInfo mVideoInfo;

    private final SettingsStore mSettingsStore;
    private final int mSimState;
    private final int mCallState;

    public ErsProcessor(Activity activity, VehicleService vehicleService, ErsTarget ersTarget, VideoInfo videoInfo) {
        mContext = activity;
        mVehicleService = vehicleService;
        mErsTarget = ersTarget;
        mVideoInfo = videoInfo;

        mSettingsStore = SettingsStore.getInstance();
        mSimState = TelephonyUtils.getSimState(mContext);
        mCallState = TelephonyUtils.getCallState(mContext);
    }

    public boolean process() {
        logger.debug("process(): target=" + mErsTarget);

        switch (mErsTarget) {
            case CALL:
                return makeCall();
            case SMS:
                return sendSms();
            case YOUTUBE:
                return uploadYoutube();
            default:
                return false;
        }
    }

    private boolean makeCall() {
        if (mSimState != TelephonyManager.SIM_STATE_READY) {
            logger.warn("makeCall(): SIM is not ready");
            return false;
        }
        if (mCallState != TelephonyManager.CALL_STATE_IDLE) {
            logger.warn("makeCall(): Call is busy");
            return false;
        }

        String phoneNumber = mSettingsStore.getErsCallContactPhoneNumber();
        if (!TextUtils.isEmpty(phoneNumber)) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                mContext.startActivity(intent);
                return true;
            } catch (Exception e) {
                logger.error("makeCall(): failed to make a call");
            }
        }
        return false;
    }

    private boolean sendSms() {
        if (mSimState != TelephonyManager.SIM_STATE_READY) {
            logger.warn("sendSms(): SIM is not ready");
            return false;
        }
        if (mCallState != TelephonyManager.CALL_STATE_IDLE) {
            logger.warn("sendSms(): Call is busy");
            return false;
        }

        String phoneNumber = mSettingsStore.getErsSmsContactPhoneNumber();
        StringBuilder messageBuffer = new StringBuilder(mSettingsStore.getErsSmsMessage());
        String message = messageBuffer.toString();

        if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(message)) {
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> multiParts = smsManager.divideMessage(message);

            try {
                if (multiParts != null && multiParts.size() > 0) {
                    smsManager.sendMultipartTextMessage(phoneNumber, null, multiParts, null, null);
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                }
                return true;
            } catch (Exception e) {
                logger.error("sendSms(): failed to send sms");
            }
        }
        return false;
    }

    private boolean uploadYoutube() {
        if (mVideoInfo != null) {
            if (!TextUtils.isEmpty(mVideoInfo.getAccountName())) {
                mVehicleService.uploadVideo(mVideoInfo);
                return true;
            } else {
                logger.warn("uploadYoutube(): Invalid youtube account!");
            }
        }
        return false;
    }

}

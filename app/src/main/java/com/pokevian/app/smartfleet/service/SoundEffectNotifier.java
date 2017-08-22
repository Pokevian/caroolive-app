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

package com.pokevian.app.smartfleet.service;

import android.content.Context;
import android.content.Intent;

import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;

public class SoundEffectNotifier {

    private Context mContext;

    private boolean mIsOverspeed;
    private boolean mSoundEffect;
    private int mVes = VehicleEngineStatus.UNKNOWN;

    public SoundEffectNotifier(Context context) {
        mContext = context;
        mContext.startService(new Intent(mContext, SoundEffectService.class));
    }

    public void release() {
        stopLoopSound();
        mContext.stopService(new Intent(mContext, SoundEffectService.class));
    }

    public void stopLoopSound() {
        stopSound(SoundEffectService.SID_OVERSPEED);
    }

    public void onObdVehicleEngineStatusChanged(int ves) {
        if (VehicleEngineStatus.isOnDriving(ves) && !VehicleEngineStatus.isOnDriving(mVes)) {
            playSound(SoundEffectService.SID_ENGINE_START, 0);
        } else if (VehicleEngineStatus.isOffDriving(ves) && VehicleEngineStatus.isOnDriving(mVes)) {
            playSound(SoundEffectService.SID_ENGINE_STOP, 0);
        }

        mVes = ves;
    }

    public void onObdDataReceived(ObdData data) {
        boolean isDuplicated = data.getBoolean(KEY.DATA_DUPL, false);
        if (isDuplicated) return;

        // mil
//        if (data.getBoolean(KEY.CALC_MIL, false) && !data.getBoolean(KEY.CALC_MIL_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_mil));
//        }

        // long-term idling
        /*if (data.getBoolean(KEY.CALC_LONG_TERM_IDLING, false)
                && !data.getBoolean(KEY.CALC_LONG_TERM_IDLING_CONTINUE, true)) {
            long dt = data.getLong(KEY.CALC_TIME, 0) - data.getLong(KEY.CALC_LONG_TERM_IDLING_TIME, 0);
            int minutes = (int) (dt / 60000);
            speak(mContext.getString(R.string.tts_long_term_idling, minutes));
        }*/

        // over-speed
        /*if (data.getBoolean(KEY.CALC_OVERSPEED, false)
                && !data.getBoolean(KEY.CALC_OVERSPEED_CONTINUE, true)
                && !mIsOverspeed) {
            speak(mContext.getString(R.string.tts_overspeed));
            playSound(SoundEffectService.SID_OVERSPEED, -1);
            mIsOverspeed = true;
        } else if (!data.getBoolean(KEY.CALC_OVERSPEED, false)
                && mIsOverspeed) {
            stopSound(SoundEffectService.SID_OVERSPEED);
            mIsOverspeed = false;
        }*/

        if (data.isValid(KEY.SAE_VSS)) {
            int speed = data.getInteger(KEY.SAE_VSS);
            if (speed >= SettingsStore.getInstance().getOverspeedThreshold() && !mIsOverspeed) {
                mIsOverspeed = true;
                playSound(SoundEffectService.SID_OVERSPEED, -1);
            } else if (mIsOverspeed && speed < SettingsStore.getInstance().getOverspeedThreshold()) {
                stopSound(SoundEffectService.SID_OVERSPEED);
                mIsOverspeed = false;
            }
        }

        if (data.getBoolean(KEY.CALC_HARSH_ACCEL, false) && !data.getBoolean(KEY.CALC_HARSH_ACCEL_CONTINUE, true)) {
            playSound(SoundEffectService.SID_HARSH, 0);
        }

        if (data.getBoolean(KEY.CALC_HARSH_BRAKE, false) && !data.getBoolean(KEY.CALC_HARSH_BRAKE_CONTINUE, true)) {
            playSound(SoundEffectService.SID_HARSH, 0);
        }

        // long-term over-speed
//        if (data.getBoolean(KEY.CALC_LONG_TERM_OVERSPEED, false)
//                && !data.getBoolean(KEY.CALC_LONG_TERM_OVERSPEED_CONTINUE, false)) {
//            speak(mContext.getString(R.string.tts_long_term_overspeed));
//        }

        // harsh accel.
//        if (data.getBoolean(KEY.CALC_HARSH_ACCEL, false)
//                && !data.getBoolean(KEY.CALC_HARSH_ACCEL_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_accel));
//        }

        // harsh brake
//        if (data.getBoolean(KEY.CALC_HARSH_BRAKE, false)
//                && !data.getBoolean(KEY.CALC_HARSH_BRAKE_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_brake));
//        }

        // harsh start
//        if (data.getBoolean(KEY.CALC_HARSH_START, false)
//                && !data.getBoolean(KEY.CALC_HARSH_START_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_start));
//        }

        // harsh stop
//        if (data.getBoolean(KEY.CALC_HARSH_STOP, false)
//                && !data.getBoolean(KEY.CALC_HARSH_STOP_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_stop));
//        }

        // harsh left turn
//        if (data.getBoolean(KEY.CALC_HARSH_LEFT_TURN, false)
//                && !data.getBoolean(KEY.CALC_HARSH_LEFT_TURN_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_left_turn));
//        }

        // harsh right turn
//        if (data.getBoolean(KEY.CALC_HARSH_RIGHT_TURN, false)
//                && !data.getBoolean(KEY.CALC_HARSH_RIGHT_TURN_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_right_turn));
//        }

        // harsh u turn
//        if (data.getBoolean(KEY.CALC_HARSH_U_TURN, false)
//                && !data.getBoolean(KEY.CALC_HARSH_U_TURN_CONTINUE, true)) {
//            speak(mContext.getString(R.string.tts_harsh_u_turn));
//        }

        // gear-shift N
//        if (data.getBoolean(KEY.CALC_GEAR_SHIFT_N, false)
//                && !data.getBoolean(KEY.CALC_GEAR_SHIFT_N_CONTINUE, true)) {
//            int count = data.getInteger(KEY.TRIP_GEAR_SHIFT_N_COUNT, 0);
//            if (count < Consts.DEFAULT_ECO_MAX_GEAR_SHIFT_ALARM_COUNT) {
//                speak(mContext.getString(R.string.tts_gear_shift_n));
//            }
//        }
    }

    private void playSound(int soundId, int loop) {
        Intent service = new Intent(mContext, SoundEffectService.class);
        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.SOUND_PLAY);
        service.putExtra(SoundEffectService.EXTRA_SOUND_ID, soundId);
        service.putExtra(SoundEffectService.EXTRA_LOOP, loop);
        mContext.startService(service);
    }

    private void stopSound(int soundId) {
        Intent service = new Intent(mContext, SoundEffectService.class);
        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.SOUND_STOP);
        service.putExtra(SoundEffectService.EXTRA_SOUND_ID, soundId);
        mContext.startService(service);
    }

    private void speak(String text) {
//        Intent service = new Intent(mContext, SoundEffectService.class);
//        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.TTS_SPEAK);
//        service.putExtra(SoundEffectService.EXTRA_TTS_TEXT, text);
//        mContext.startService(service);
    }

}

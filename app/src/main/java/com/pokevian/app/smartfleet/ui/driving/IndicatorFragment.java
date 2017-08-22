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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.FuelType;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

public class IndicatorFragment extends Fragment {

    public static float MAX_ENGINE_COOLANT_TEMPERATURE = 100;
    public static float MIN_AUX_BATTERY_VOLTAGE = 13.2f;
    public static float MAX_AUX_BATTERY_VOLTAGE = 14.8f;

    private boolean mInvalidData = false;

    private ImageView mMilLamp;
    private ImageView mObdLamp;

    private ImageView mCoolantLamp;
    private ImageView mBatteryLamp;
    private ImageView mEcoLamp;

    private ImageView mHarshAccelLamp;
    private ImageView mHarshDecelLamp;
    private ImageView mIdlingLamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driving_indicator, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMilLamp = (ImageView)view.findViewById(R.id.iv_mil);
        mObdLamp = (ImageView)view.findViewById(R.id.iv_obd);

        mCoolantLamp = (ImageView)view.findViewById(R.id.iv_coolant);
        mBatteryLamp = (ImageView)view.findViewById(R.id.iv_battery);
        mEcoLamp = (ImageView)view.findViewById(R.id.iv_eco);

        mHarshAccelLamp = (ImageView)view.findViewById(R.id.cb_harsh_accel);
        mHarshDecelLamp = (ImageView)view.findViewById(R.id.cb_harsh_brake);
        mIdlingLamp = (ImageView)view.findViewById(R.id.cb_idle);

        init();
    }

    @Override
    public void onDestroy() {
        mMilLamp.setImageDrawable(null);
        mMilLamp = null;
        mObdLamp.setImageDrawable(null);
        mObdLamp = null;

        mCoolantLamp.setImageDrawable(null);
        mCoolantLamp = null;
        mBatteryLamp.setImageDrawable(null);
        mCoolantLamp = null;
        mEcoLamp.setImageDrawable(null);
        mEcoLamp = null;

        mHarshAccelLamp.setImageDrawable(null);
        mHarshAccelLamp = null;
        mHarshDecelLamp.setImageDrawable(null);
        mHarshDecelLamp = null;
        mIdlingLamp.setImageDrawable(null);
        mIdlingLamp = null;
        super.onDestroy();
    }

    public void onObdStateChanged(ObdState obdState) {
        if (obdState == ObdState.SCANNING) {
            mInvalidData = false;
        }
    }

    public void onObdDataReceived(ObdData data) {
        if (!isVisible() || mInvalidData) return;

        mMilLamp.setEnabled(((DrivingActivity)getActivity()).isMilOn(data));
        mObdLamp.setEnabled(isEngineOn(data));

        if (data.isValid(KEY.SAE_ECT)) {
            setCoolantTemperatureLamp(data.getBoolean(KEY.WARN_OVERHEAT, false));
        } else {
            offCoolantTemperatureLamp();
        }

        if (data.isValid(KEY.CALC_AUX_BAT)) {
            setBatteryVoltageLamp(data.getBoolean(KEY.WARN_UNDER_AUX_BAT, false)
                    || data.getBoolean(KEY.WARN_OVER_AUX_BAT, false));
        } else {
            offBatteryVoltageLamp();
        }

        if (data.isValid(KEY.CALC_FUEL_ECONOMY)) {
            setEcoLamp(((DrivingActivity) getActivity()).calcEcoLevel(data));
        } else {
            offEcoLamp();
        }

        // Harsh acceleration
        mHarshAccelLamp.setEnabled(data.getBoolean(KEY.CALC_HARSH_ACCEL, false));

        // Harsh brake
        mHarshDecelLamp.setEnabled(data.getBoolean(KEY.CALC_HARSH_BRAKE, false));

        // Idling
        mIdlingLamp.setEnabled(data.getBoolean(KEY.CALC_IDLING, false));
    }

    public void onObdCannotConnect() {
        mInvalidData = true;

        init();
    }

    private boolean isEngineOn() {
        return ((DrivingActivity)getActivity()).isEngineOn();
    }

    private boolean isEngineOn(ObdData data) {
        return VehicleEngineStatus.isOnDriving(data.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN));
    }

    private void init() {
        mMilLamp.setEnabled(false);
        mObdLamp.setEnabled(false);

        offCoolantTemperatureLamp();
        offBatteryVoltageLamp();
        offEcoLamp();

        mHarshAccelLamp.setEnabled(false);
        mHarshDecelLamp.setEnabled(false);
        mIdlingLamp.setEnabled(false);
    }

    private void setCoolantTemperatureLamp(boolean warning) {
        if (warning) {
            mCoolantLamp.setImageResource(R.drawable.ic_temp_red);
        } else {
            mCoolantLamp.setImageResource(R.drawable.ic_temp_blue);
        }
    }

    private void offCoolantTemperatureLamp() {
        mCoolantLamp.setImageResource(R.drawable.ic_temp_off);
    }

    private void setBatteryVoltageLamp(boolean warning) {
        if (warning) {
            mBatteryLamp.setImageResource(R.drawable.ic_volt_red);
        } else {
            mBatteryLamp.setImageResource(R.drawable.ic_volt_blue);
        }
    }

    private void offBatteryVoltageLamp() {
        mBatteryLamp.setImageResource(R.drawable.ic_volt_off);
    }

    private void setEcoLamp(int level) {
        if (level == 1) {
            mEcoLamp.setImageResource(R.drawable.ic_eco_green);
        } else if ( level == 2 || level == 3) {
            mEcoLamp.setImageResource(R.drawable.ic_eco_yellow);
        } else if (level == 4 || level == 5) {
            mEcoLamp.setImageResource(R.drawable.ic_eco_red);
        } else {
            offEcoLamp();
        }
    }

    private void offEcoLamp() {
        mEcoLamp.setImageResource(R.drawable.ic_eco_off);
    }
}

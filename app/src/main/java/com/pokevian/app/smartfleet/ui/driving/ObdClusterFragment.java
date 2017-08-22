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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.util.TextViewUtils;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

public class ObdClusterFragment extends Fragment {

    private boolean mInvalidData = false;

    private ImageView mFuelCut;
    private TextView mRpmText;
    private ImageView mCluster;

    private TextView mFuelEconomyText;
    private ProgressBar mInstantFuel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driving_obd, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCluster = (ImageView)view.findViewById(R.id.bg_obd_cluster);
        mFuelCut = (ImageView)view.findViewById(R.id.iv_check_fuelcut);
        mRpmText = (TextView)view.findViewById(R.id.tv_rpm);

        mFuelEconomyText = (TextView)view.findViewById(R.id.tv_fuel_economy);
        mInstantFuel = (ProgressBar)view.findViewById(R.id.pb_instant_fuel);

        init();
    }

    @Override
    public void onDestroy() {
        mCluster.setImageDrawable(null);
        mCluster = null;
        mFuelCut.setImageDrawable(null);
        mFuelCut = null;
        super.onDestroy();
    }

    private void init() {
        setClusterBackground(0);
        mFuelCut.setEnabled(false);
        mRpmText.setText(getString(R.string.empty_int_value));
        mInstantFuel.setProgress(0);
    }

    public void onObdStateChanged(ObdState obdState) {
        if (obdState == ObdState.SCANNING) {
            mInvalidData = false;
        }
    }

    public void onObdDataReceived(ObdData data) {
        if (mInvalidData || !isVisible()) return;

        if (isEngineOn(data)) {
            setClusterBackground(data);
            if (data.getBoolean(KEY.CALC_FUEL_CUT, false)) {
                mFuelCut.setEnabled(true);
                mInstantFuel.setProgress(mInstantFuel.getMax());
            } else {
                mFuelCut.setEnabled(false);
                mInstantFuel.setProgress(data.getFloat(KEY.CALC_FUEL_ECONOMY, 0f).intValue());
            }

            if (data.isValid(KEY.TRIP_FUEL_ECONOMY)) {
                Float fuelEconomy = data.getFloat(KEY.TRIP_FUEL_ECONOMY);
                if (!fuelEconomy.isNaN() && !fuelEconomy.isInfinite()) {
                    if (fuelEconomy > 99.9f) fuelEconomy = 99.9f;
                    TextViewUtils.setFuelEconomyText(mFuelEconomyText, fuelEconomy);
                }
            } else {
                mFuelEconomyText.setText(getString(R.string.empty_float_value));
            }
        } else {
            init();
            if (isEngineOn()) {
               setRpmText(data.getFloat(KEY.SAE_RPM, 0f));
            }
        }
    }

    public void onObdExtraDataReceived(float rpm, int vss) {
        if (mInvalidData || !isVisible()) return;

        setRpmText(rpm);
    }

    private void setRpmText(float rpm) {
        if(rpm < 0) {
            mRpmText.setText(R.string.empty_int_value);
        } else {
            TextViewUtils.setIntegerFormatText(mRpmText, rpm);
        }
    }

    public void onObdCannotConnect() {
        mInvalidData = true;

        if (isVisible()) {
            mFuelCut.setEnabled(false);
            mRpmText.setText(R.string.empty_int_value);
            mInstantFuel.setProgress(0);

            setClusterBackground(0);
        }
    }

    private boolean isEngineOn(ObdData data) {
        return VehicleEngineStatus.isOnDriving(data.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN));
    }

    private boolean isEngineOn() {
        return ((DrivingActivity)getActivity()).isEngineOn();
    }

    private void setClusterBackground(ObdData data) {
        setClusterBackground(calcEcoLevel(data));
    }

    private void setClusterBackground(int level) {
        if (level == 1) {
            mCluster.setImageResource(R.drawable.bg_left_green);
        } else if ( level == 2 || level == 3) {
            mCluster.setImageResource(R.drawable.bg_left_yellow);
        } else if (level == 4 || level == 5) {
            mCluster.setImageResource(R.drawable.bg_left_red);
        } else {
            mCluster.setImageResource(R.drawable.bg_left_cluster);
        }
    }

    private int calcEcoLevel(ObdData data) {
        return ((DrivingActivity) getActivity()).calcEcoLevel(data);
    }
}

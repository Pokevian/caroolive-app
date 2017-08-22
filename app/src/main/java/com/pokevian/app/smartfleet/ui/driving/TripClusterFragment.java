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
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.TextViewUtils;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

public class TripClusterFragment extends Fragment  {

    private ImageView mOverspeedLamp;
    private TextView mSpeedText;
    private TextView mRunDistanceText;
    private TextView mRunTimeText;

    private boolean mIsOverspeedWarningEnabled;
    private int mOverspeedThreshold;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mOverspeedThreshold = SettingsStore.getInstance().getOverspeedThreshold();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driving_trip, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mOverspeedLamp = (ImageView)view.findViewById(R.id.cb_overspeed);

        mSpeedText = (TextView)view.findViewById(R.id.tv_vss);
        mRunDistanceText = (TextView) view.findViewById(R.id.run_distance);
        mRunTimeText = (TextView) view.findViewById(R.id.run_time);

        init();
    }

    @Override
    public void onDestroy() {
        mOverspeedLamp.setImageDrawable(null);
        mOverspeedLamp = null;
        super.onDestroy();
    }

    public void onObdStateChanged(ObdState obdState) {
        // Nothing to do
    }

    public void onObdDataReceived(ObdData data) {
        if (!isVisible()) return;

        // Over-speed
        mOverspeedLamp.setEnabled(isOverspeed(data));

        // Trip distance
        if (data.isValid(KEY.TRIP_DRIVING_DIST)) {
            TextViewUtils.setDistanceText(mRunDistanceText, data.getFloat(KEY.TRIP_DRIVING_DIST, 0));
//            mRunDistanceText.setText(String.format("%.1f", data.getFloat(KEY.TRIP_DRIVING_DIST, 0)));
        }

        // Trip time
        if (data.isValid(KEY.TRIP_DURATION)) {
            int time = data.getFloat(KEY.TRIP_DURATION, 0).intValue();
            TextViewUtils.setRunTimeText(mRunTimeText, time);
        }
    }

    public void onObdCannotConnect() {
        init();
    }

    public void onObdExtraDataReceived(float rpm, int speed) {
        setSpeedText(speed);
    }

    private void init() {
        mOverspeedLamp.setEnabled(false);
        mSpeedText.setText(R.string.empty_int_value);
    }

    private void setSpeedText(int value) {
        if (isVisible()) {
            if (value > -1) {
                mSpeedText.setText(String.valueOf(value));
            } else {
                mSpeedText.setText(R.string.empty_int_value);
            }
        }
    }

    private boolean isOverspeed(ObdData data) {
        int speed = 0;
        if (data.isValid(KEY.SAE_VSS)) {
            speed = data.getInteger(KEY.SAE_VSS);
        }
        return mOverspeedThreshold <= speed;
    }
}

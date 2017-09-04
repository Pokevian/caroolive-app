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

package com.pokevian.app.smartfleet.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.driving.DrivingActivity;
import com.pokevian.app.smartfleet.ui.main.DrivingOnDialogFragment;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;

import org.apache.log4j.Logger;

public class BaseDrivingOnActivity extends BaseActivity implements AlertDialogFragment.AlertDialogCallbacks {

    private static final String TAG = "driving-on";

    private static final int REQUEST_DRIVING = 1004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerVehicleReceiver();
    }

    @Override
    protected void onDestroy() {
        unregisterVehicleReceiver();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DRIVING) {
            Logger.getLogger(TAG).trace("onActivityResult(): REQUEST_DRIVING: data=" + data);
            setResult(resultCode, data);
            finish();
        }
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();
        Logger.getLogger(TAG).trace("onDialogButtonClick(): tag=" + tag);
        if (DrivingOnDialogFragment.TAG.equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                startDrivingActivity();
            }
        }
    }

    protected void finishIfNeeded() {
        finish();
    }

    private void onVehicleEngineStatusChanged(int ves) {
        if (VehicleEngineStatus.isOnDriving(ves)) {
            if (isPaused()) {
                finishIfNeeded();
            } else {
                FragmentManager fm = getSupportFragmentManager();
                DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(DrivingOnDialogFragment.TAG);
                if (fragment == null) {
                    fragment = DrivingOnDialogFragment.newInstance();
                    fm.beginTransaction().add(fragment, DrivingOnDialogFragment.TAG)
                            .commitAllowingStateLoss();
                }
            }
        } else if (ves == VehicleEngineStatus.OFF) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(DrivingOnDialogFragment.TAG);
            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
            }
        }
    }

    private void startDrivingActivity() {
        // Unregister vehicle receiver
        unregisterVehicleReceiver();

        Intent intent = new Intent(this, DrivingActivity.class);
        startActivityForResult(intent, REQUEST_DRIVING);

        overridePendingTransition(R.anim.slide_in_down, R.anim.fade_out);
    }

    private void registerVehicleReceiver() {
        unregisterVehicleReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
//        filter.addAction(DrivingActivity.ACTION_GOTO_DRIVING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mVehicleReceiver, filter);
    }

    private void unregisterVehicleReceiver() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            Logger.getLogger("drivingOn").error(e.getMessage());
        }
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (isFinishing()) return;

            final String action = intent.getAction();
            if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                onVehicleEngineStatusChanged(intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS,
                        VehicleEngineStatus.UNKNOWN));
            }
        }
    };
}

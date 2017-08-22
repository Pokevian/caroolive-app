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

package com.pokevian.app.smartfleet.ui.settings;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.app.ActionBar;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.setup.PickVehicleFragment;
import com.pokevian.app.smartfleet.ui.setup.SelectVehicleFragment;

public class VehiclesSettingActivity extends BaseActivity
        implements OnBackStackChangedListener, SelectVehicleFragment.RegisterVehicleCallbacks, PickVehicleFragment.PickVehicleCallbacks {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vehicle_setting);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(false);
        bar.setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = SelectVehicleFragment.newInstance();
            fm.beginTransaction().replace(R.id.container, fragment, SelectVehicleFragment.TAG)
                    .commit();
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        getSupportFragmentManager().removeOnBackStackChangedListener(this);

        super.onDestroy();
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getSupportFragmentManager();
        int count = fm.getBackStackEntryCount();
        // Check if RegisterVehicleFragment is shown
        if (count == 0) {
            // and then show RegisterVehicleFragment menu
            Fragment fragment = fm.findFragmentByTag(SelectVehicleFragment.TAG);
            fragment.setMenuVisibility(true);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            // Pop RegisterVehicleFragment's backs stack
            SelectVehicleFragment fragment = (SelectVehicleFragment) fm.findFragmentByTag(SelectVehicleFragment.TAG);
            if (fragment != null) {
//                if (fragment.popBackStack()) {
//                    return;
//                }
                if (fragment.onBackPressed()) {
                    return;
                }
            }
            super.onBackPressed();
        }
    }

    @Override
    public void onVehicleRegistered(Vehicle vehicle) {
        if (isFinishing()) return;

        DialogFragment fragment = new SetupSucceededDialogFragment();
        fragment.show(getSupportFragmentManager(), SetupSucceededDialogFragment.TAG);
    }

    @Override
    public void onVehiclePicked(Vehicle old, Vehicle picked) {

    }

    public static class SetupSucceededDialogFragment extends DialogFragment {

        public static final String TAG = "setup-succeeded-dialog";

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_message_vehicle_setup_successfully)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((VehiclesSettingActivity) getActivity()).onSetupSucceeded();
                        }
                    })
                    .show();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            ((VehiclesSettingActivity) getActivity()).onSetupSucceeded();
        }
    }

    private void onSetupSucceeded() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onVehicleRegisterCancel() {
        if (isFinishing()) return;

        finish();
    }

}

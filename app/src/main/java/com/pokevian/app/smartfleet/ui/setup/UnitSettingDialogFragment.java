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

package com.pokevian.app.smartfleet.ui.setup;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.lib.obd2.defs.Unit;

public class UnitSettingDialogFragment extends DialogFragment {

    public static final String TAG = "UnitSettingDialogFragment";

    private static final String KEY_DISTANCE_POS = "distane_pos";
    private static final String KEY_VOLUME_POS = "volume_pos";
    private static final String KEY_FUEL_ECONOMY_POS = "fuel_economy_pos";

    private Spinner mDistanceUnitSpinner;
    private Spinner mVolumeUnitSpinner;
    private Spinner mFuelEconomyUnitSpinner;

    private SettingsStore mSettingsStore;
    private String mUserData;
    private AlertDialogCallbacks mCallbacks;

    public static UnitSettingDialogFragment newInstance(String userData) {
        UnitSettingDialogFragment fragment = new UnitSettingDialogFragment();
        Bundle args = new Bundle();
        args.putString("user_data", userData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (AlertDialogCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (AlertDialogCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsStore = SettingsStore.getInstance();

        Bundle args = getArguments();
        mUserData = args.getString("user_data");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_DISTANCE_POS, mDistanceUnitSpinner.getSelectedItemPosition());
        outState.putInt(KEY_VOLUME_POS, mVolumeUnitSpinner.getSelectedItemPosition());
        outState.putInt(KEY_FUEL_ECONOMY_POS, mFuelEconomyUnitSpinner.getSelectedItemPosition());

        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_unit_setting, null);

        mDistanceUnitSpinner = (Spinner) view.findViewById(R.id.distance_unit_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.distance_unit_entries, R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mDistanceUnitSpinner.setAdapter(adapter);

        mVolumeUnitSpinner = (Spinner) view.findViewById(R.id.volume_unit_spinner);
        adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.volume_unit_entries, R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mVolumeUnitSpinner.setAdapter(adapter);

        mFuelEconomyUnitSpinner = (Spinner) view.findViewById(R.id.fuel_economy_unit_spinner);
        adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.fuel_economy_unit_entries, R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        mFuelEconomyUnitSpinner.setAdapter(adapter);

        if (savedInstanceState != null) {
            mDistanceUnitSpinner.setSelection(savedInstanceState.getInt(KEY_DISTANCE_POS));
            mVolumeUnitSpinner.setSelection(savedInstanceState.getInt(KEY_VOLUME_POS));
            mFuelEconomyUnitSpinner.setSelection(savedInstanceState.getInt(KEY_FUEL_ECONOMY_POS));
        } else {
            mDistanceUnitSpinner.setSelection(fromDistanceUnit(mSettingsStore.getDistanceUnit()));
            mVolumeUnitSpinner.setSelection(fromVolumeUnit(mSettingsStore.getVolumeUnit()));
            mFuelEconomyUnitSpinner.setSelection(fromFuelEconomyUnit(mSettingsStore.getFuelEconomyUnit()));
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.vehicle_setting_unit_setting)
                .setView(view)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSettingsStore.storeDistanceUnit(toDistanceUnit(mDistanceUnitSpinner.getSelectedItemPosition()));
                        mSettingsStore.storeVolumeUnit(toVolumeUnit(mVolumeUnitSpinner.getSelectedItemPosition()));
                        mSettingsStore.storeFuelEconomyUnit(toFuelEconomyUnit(mFuelEconomyUnitSpinner.getSelectedItemPosition()));

                        // Set speed unit according to distance unit
                        if (mSettingsStore.getDistanceUnit() == Unit.MI) {
                            mSettingsStore.storeSpeedUnit(Unit.MPH);
                        } else {
                            mSettingsStore.storeSpeedUnit(Unit.KPH);
                        }

                        mCallbacks.onDialogButtonClick(UnitSettingDialogFragment.this, which);
                    }
                })
                .create();
    }

    public String getUserData() {
        return mUserData;
    }

    private int fromDistanceUnit(Unit distanceUnit) {
        String[] values = getResources().getStringArray(R.array.distance_unit_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(distanceUnit.name())) {
                return i;
            }
        }
        return 0;
    }

    private Unit toDistanceUnit(int position) {
        String[] values = getResources().getStringArray(R.array.distance_unit_values);
        return Unit.valueOf(values[position]);
    }

    private int fromVolumeUnit(Unit volumeUnit) {
        String[] values = getResources().getStringArray(R.array.volume_unit_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(volumeUnit.name())) {
                return i;
            }
        }
        return 0;
    }

    private Unit toVolumeUnit(int position) {
        String[] values = getResources().getStringArray(R.array.volume_unit_values);
        return Unit.valueOf(values[position]);
    }

    private int fromFuelEconomyUnit(Unit fuelEconomyUnit) {
        String[] values = getResources().getStringArray(R.array.fuel_economy_unit_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(fuelEconomyUnit.name())) {
                return i;
            }
        }
        return 0;
    }

    private Unit toFuelEconomyUnit(int position) {
        String[] values = getResources().getStringArray(R.array.fuel_economy_unit_values);
        return Unit.valueOf(values[position]);
    }

}

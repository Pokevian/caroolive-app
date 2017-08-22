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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleList;
import com.pokevian.app.smartfleet.model.VehicleModel;
import com.pokevian.app.smartfleet.model.VehicleModelList;
import com.pokevian.app.smartfleet.request.GetVehicleModelRequest;
import com.pokevian.app.smartfleet.request.RegisterVehicleRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleDialogFragment.LoadVehicleDialogCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleDisplacementDialogFragment.LoadVehicleDisplacementCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleFuelDialogFragment.LoadVehicleFuelCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleMakerDialogFragment.LoadVehicleMakerCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleModelDialogFragment.LoadVehicleModelCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleReleaseYearDialogFragment.LoadVehicleReleaseYearCallbacks;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleTypeDialogFragment.LoadVehicleTypeCallbacks;
import com.pokevian.app.smartfleet.ui.setup.PickObdDeviceFragment.PickObdDeviceCallbacks;
import com.pokevian.app.smartfleet.ui.setup.UpdateVehicleDialogFragment.UpdateVehicleCallbacks;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.lib.obd2.defs.Unit;

import org.apache.log4j.Logger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RegisterVehicleFragment extends Fragment implements OnBackStackChangedListener, OnClickListener,
        OnItemClickListener, LoadVehicleDialogCallbacks, PickObdDeviceCallbacks, UpdateVehicleCallbacks,
        AlertDialogCallbacks {

    public static final String TAG = "RegisterVehicleFragment";
    final Logger logger = Logger.getLogger(TAG);

    private ListView mList;
    private ListAdapter mAdapter;

    private Button mPrevBtn;
    private Button mNextBtn;

    private Vehicle mVehicle;
    private String mInactiveVehicleId;
    private boolean mEditable;
    private boolean mEditing;
    private NumberFormat mDecimalFormatter = NumberFormat.getInstance();
    private SettingsStore mSettingsStore;

    private MenuItem mEditMenu;

    private RegisterVehicleCallbacks mCallbacks;

    public static RegisterVehicleFragment newInstance() {
        RegisterVehicleFragment fragment = new RegisterVehicleFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (RegisterVehicleCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement EditVehicleCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (RegisterVehicleCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement EditVehicleCallbacks");
            }
        }

        getChildFragmentManager().addOnBackStackChangedListener(this);
    }

    @Override
    public void onDetach() {
        getChildFragmentManager().removeOnBackStackChangedListener(this);

        super.onDetach();
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fm = getChildFragmentManager();
        int count = fm.getBackStackEntryCount();
        if (count == 0) {
            setMenuVisibility(true);

            // FIXME:
            View container = getView().findViewById(R.id.container);
            container.setClickable(false);
        } else {
            setMenuVisibility(false);

            // FIXME:
            View container = getView().findViewById(R.id.container);
            container.setClickable(true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsStore = SettingsStore.getInstance();

        if (savedInstanceState != null) {
            mVehicle = (Vehicle) savedInstanceState.getSerializable("vehicle");
            mEditable = savedInstanceState.getBoolean("editable");
            mEditing = savedInstanceState.getBoolean("editing");
        }

//        if (mVehicle != null) {
//            updateVehicleInfo();
//            addEditMenu();
//
//        } else {
//            FragmentManager fm = getChildFragmentManager();
//            LoadVehicleDialogFragment.newInstance().show(fm, LoadVehicleDialogFragment.TAG);
//        }

        // Has options menu ('unit setting' and 'edit')
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("vehicle", mVehicle);
        outState.putBoolean("editable", mEditable);
        outState.putBoolean("editing", mEditing);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register_vehicle, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mList = (ListView) view.findViewById(R.id.list);

        ArrayList<VehicleItem> items = new ArrayList<VehicleItem>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            items.add(new VehicleItem());
        }

        mAdapter = new ListAdapter(getActivity(), items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        mPrevBtn = (Button) view.findViewById(R.id.prev_btn);
        mPrevBtn.setText(R.string.btn_later);
        mPrevBtn.setOnClickListener(this);

        mNextBtn = (Button) view.findViewById(R.id.next_btn);
        mNextBtn.setText(R.string.btn_next);
        mNextBtn.setOnClickListener(this);

        setEditable(mEditing);

        if (mVehicle != null) {
            updateVehicleInfo();
            addEditMenu();
            mPrevBtn.setText(R.string.btn_cancel);
        } else {
            FragmentManager fm = getChildFragmentManager();
            LoadVehicleDialogFragment.newInstance().show(fm, LoadVehicleDialogFragment.TAG);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.register_vehicle, menu);

        mEditMenu = menu.findItem(R.id.action_edit);
        if (!mEditable || mEditing) {
            mEditMenu.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        /*if (itemId == R.id.action_unit_setting) {
            DialogFragment fragment = UnitSettingDialogFragment.getTop10Request("MENU");
            fragment.show(getChildFragmentManager(), UnitSettingDialogFragment.TAG);
            return true;
        } else*/ if (itemId == R.id.action_edit) {
            DialogFragment fragment = EditVehicleWarningDialogFragment.newInstance();
            fragment.show(getChildFragmentManager(), EditVehicleWarningDialogFragment.TAG);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public boolean popBackStack() {
        FragmentManager fm = getChildFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        VehicleItem item = mAdapter.getItem(position);
        if (item.disabled) {
            return;
        }

        if (position == ITEM_MAKER) {
            FragmentManager fm = getChildFragmentManager();
            if (fm.findFragmentByTag(PickMakerFragment.TAG) == null) {
                Fragment fragment = PickMakerFragment.newInstance();
                fm.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .add(R.id.container, fragment, PickMakerFragment.TAG)
                        .addToBackStack(null).commit();
            }
        } else if (ITEM_TYPE == position) {
            item = mAdapter.getItem(ITEM_MAKER);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_maker_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickTypeFragment.TAG) == null) {
                    Fragment fragment = PickTypeFragment.newInstance(mVehicle.getModel().getMakerCode());
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickTypeFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }
        } else if (position == ITEM_MODEL) {
            item = mAdapter.getItem(ITEM_TYPE);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_type_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickModelFragment.TAG) == null) {
                    Fragment fragment = PickModelFragment.newInstance(mVehicle.getModel().getMakerCode(), mVehicle.getModel().getTypeCode());
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickModelFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }
        } else if (position == ITEM_FUEL) {
            item = mAdapter.getItem(ITEM_MODEL);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_model_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickFuelFragment.TAG) == null) {
                    Fragment fragment = PickFuelFragment.newInstance(mVehicle.getModel().getMakerCode(),
                            mVehicle.getModel().getModelCode());
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickFuelFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }
        } else if (position == ITEM_DISPLACEMENT) {
            item = mAdapter.getItem(ITEM_FUEL);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_fuel_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickDisplacementFragment.TAG) == null) {
                    Fragment fragment = PickDisplacementFragment.newInstance(mVehicle.getModel().getMakerCode(),
                            mVehicle.getModel().getModelCode(), mVehicle.getModel().getEngineCode());
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickDisplacementFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }
        } else if (ITEM_MODEL_YEAR == position) {
            item = mAdapter.getItem(ITEM_DISPLACEMENT);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_displacement_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickReleaseYearFragment.TAG) == null) {
                    Fragment fragment = PickReleaseYearFragment.newInstance(mVehicle.getModel().getMakerCode(),
                            mVehicle.getModel().getModelCode(), mVehicle.getModel().getEngineCode(), mVehicle.getModel().getDisplacement());
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickReleaseYearFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }
        } else if (ITEM_ISG == position) {
            item = mAdapter.getItem(ITEM_MODEL_YEAR);
            if (TextUtils.isEmpty(item.value)) {
                Toast.makeText(getActivity(), R.string.vehicle_setting_pick_years_first, Toast.LENGTH_LONG).show();
            } else {
                FragmentManager fm = getChildFragmentManager();
                if (fm.findFragmentByTag(PickIsgSupportFragment.TAG) == null) {
                    Fragment fragment = PickIsgSupportFragment.newInstance();
                    fm.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                    R.anim.slide_in_left, R.anim.slide_out_right)
                            .add(R.id.container, fragment, PickIsgSupportFragment.TAG)
                            .addToBackStack(null).commit();
                }
            }

        } else if (position == ITEM_PLATE_NO) {
            FragmentManager fm = getChildFragmentManager();
            if (fm.findFragmentByTag(InputPlateNoFragment.TAG) == null) {
                Fragment fragment = InputPlateNoFragment.newInstance(mVehicle.getPlateNo());
                fm.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                R.anim.slide_in_left, R.anim.slide_out_right)
                        .add(R.id.container, fragment, InputPlateNoFragment.TAG)
                        .addToBackStack(null).commit();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.prev_btn) {
            mCallbacks.onVehicleRegisterCancel();
        } else if (id == R.id.next_btn) {
            if (validate()) {

                Logger.getLogger(TAG).debug(mVehicle.toString());

                FragmentManager fm = getChildFragmentManager();
                DialogFragment dialogFragment = RegisterVehicleProcessDialogFragment
                        .newInstance(mVehicle, mInactiveVehicleId);
                dialogFragment.show(fm, RegisterVehicleProcessDialogFragment.TAG);
            }
        }
    }


    @Override
    public void onLoadVehicleSuccess(VehicleList list) {
        logger.trace("onLoadVehicleSuccess(): list=" + list);

        Vehicle vehicle = null;

        if (list != null) {
            ArrayList<Vehicle> vehicles = list.getList();
            if (vehicles != null) {
                // Pick first active vehicle (should be only one active vehicle for now)
                for (Vehicle v : vehicles) {
                    if (TwoState.Y.name().equals(v.getActiveCode())) {
                        vehicle = v;
                        break;
                    }
                }
            }
        }

        if (vehicle != null) {
            // exist vehicle
            mVehicle = vehicle;
            updateVehicleInfo();

            /*// Update vehicle information
            VehicleModel model = vehicle.getModel();

            VehicleItem item = mAdapter.getItem(ITEM_MAKER);
            item.value = model.getMakerName();

            item = mAdapter.getItem(ITEM_TYPE);
            item.value = model.getTypeName();

            item = mAdapter.getItem(ITEM_MODEL);
            item.value = model.getModelName();

            item = mAdapter.getItem(ITEM_FUEL);
            item.value = model.getEngineName();

            item = mAdapter.getItem(ITEM_DISPLACEMENT);
            item.value = mDecimalFormatter.format(model.getDisplacement()) + Unit.CC;

            item = mAdapter.getItem(ITEM_MODEL_YEAR);
            if (vehicle.getProdYear() > 0) {
                item.value = Integer.toString(vehicle.getProdYear());
            }

            item = mAdapter.getItem(ITEM_ISG);
            if (TwoState.Y.name().equals(vehicle.getIsgCode())) {
                item.value = getString(R.string.vehicle_setting_isg_support);
            } else {
                item.value = getString(R.string.vehicle_setting_isg_not_support);
            }

            item = mAdapter.getItem(ITEM_PLATE_NO);
            item.value = vehicle.getPlateNo();

            mAdapter.notifyDataSetInvalidated();*/

            // Add 'Edit' menu
            /*if (validate()) {
                mEditable = true;
                mEditing = false;
            } else {
                mEditable = false;
                setEditable(true);
            }

            getActivity().invalidateOptionsMenu();*/

            mPrevBtn.setText(R.string.btn_cancel);
        } else {
            // new vehicle
            mVehicle = new Vehicle();
            mVehicle.setModel(new VehicleModel());

            /*// Remove 'Edit' menu
            mEditable = false;
            getActivity().invalidateOptionsMenu();

            setEditable(true);*/
        }
        addEditMenu();

    }

    private void addEditMenu() {
        if (validate()) {
            mEditable = true;
            mEditing = false;
        } else {
            mEditable = false;
            setEditable(true);
        }

        getActivity().invalidateOptionsMenu();
    }

    void init() {
        updateVehicleInfo();
        addEditMenu();
        mPrevBtn.setText(R.string.btn_cancel);
    }

    // Update vehicle information
    private void updateVehicleInfo() {
        if (mVehicle != null) {
            VehicleModel model = mVehicle.getModel();

            VehicleItem item = mAdapter.getItem(ITEM_MAKER);
            item.value = model.getMakerName();

            item = mAdapter.getItem(ITEM_TYPE);
            item.value = model.getTypeName();

            item = mAdapter.getItem(ITEM_MODEL);
            item.value = model.getModelName();

            item = mAdapter.getItem(ITEM_FUEL);
            item.value = model.getEngineName();

            item = mAdapter.getItem(ITEM_DISPLACEMENT);
            item.value = mDecimalFormatter.format(model.getDisplacement()) + Unit.CC;

            item = mAdapter.getItem(ITEM_MODEL_YEAR);
            if (mVehicle.getProdYear() > 0) {
                item.value = Integer.toString(mVehicle.getProdYear());
            }

            item = mAdapter.getItem(ITEM_ISG);
            if (TwoState.Y.name().equals(mVehicle.getIsgCode())) {
                item.value = getString(R.string.vehicle_setting_isg_support);
            } else {
                item.value = getString(R.string.vehicle_setting_isg_not_support);
            }

            item = mAdapter.getItem(ITEM_PLATE_NO);
            item.value = mVehicle.getPlateNo();

            mAdapter.notifyDataSetInvalidated();
        }
    }

    @Override
    public void onLoadVehicleFailure() {
        logger.debug("onLoadVehicleFailure()");

        mCallbacks.onVehicleRegisterCancel();
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();
        if (UnitSettingDialogFragment.TAG.equals(tag)) {
            mSettingsStore.storeUnitInitialied(true);

            String userData = ((UnitSettingDialogFragment) fragment).getUserData();
            if (!"MENU".equals(userData)) {
                LoadVehicleDialogFragment.newInstance()
                        .show(getChildFragmentManager(), LoadVehicleDialogFragment.TAG);
            }
        } else if (EditVehicleWarningDialogFragment.TAG.equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                // Store vehicle ID which will be deactivated
                mInactiveVehicleId = mVehicle.getVehicleId();

                // Clear vehicle ID (new vehicle)
                mVehicle.setVehicleId(null);

                // Remove 'Edit' menu
                mEditable = false;
                mEditing = true;
                mEditMenu.setVisible(false);

                setEditable(true);
            }
        }
    }

    public void onMakerSelected(Fragment fragment, VehicleModel model) {
        // only makerCode and makerName will be specified
        mVehicle.getModel().setMakerCode(model.getMakerCode());
        mVehicle.getModel().setMakerName(model.getMakerName());

        VehicleItem item = mAdapter.getItem(ITEM_MAKER);
        item.value = model.getMakerName();

        mVehicle.getModel().setTypeCode(null);
        mVehicle.getModel().setTypeCode(null);
        item = mAdapter.getItem(ITEM_TYPE);
        item.value = null;

        mVehicle.getModel().setModelCode(null);
        mVehicle.getModel().setModelName(null);
        item = mAdapter.getItem(ITEM_MODEL);
        item.value = null;

        mVehicle.getModel().setEngineCode(null);
        mVehicle.getModel().setEngineName(null);
        item = mAdapter.getItem(ITEM_FUEL);
        item.value = null;

        mVehicle.getModel().setDisplacement(0);
        item = mAdapter.getItem(ITEM_DISPLACEMENT);
        item.value = null;

        mVehicle.getModel().setReleaseYear(0);
        item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = null;

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickTypeFragment.newInstance(mVehicle.getModel().getMakerCode());
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickTypeFragment.TAG)
                .addToBackStack(PickTypeFragment.TAG)
                .commit();
    }

    public void onTypeSelected(Fragment fragment, VehicleModel model) {
        // only typeCode and typeName will be specified
        mVehicle.getModel().setTypeCode(model.getTypeCode());
        mVehicle.getModel().setTypeName(model.getTypeName());
        VehicleItem item = mAdapter.getItem(ITEM_TYPE);
        item.value = model.getTypeName();

        mVehicle.getModel().setModelCode(null);
        mVehicle.getModel().setModelName(null);
        item = mAdapter.getItem(ITEM_MODEL);
        item.value = null;

        mVehicle.getModel().setEngineCode(null);
        mVehicle.getModel().setEngineName(null);
        item = mAdapter.getItem(ITEM_FUEL);
        item.value = null;

        mVehicle.getModel().setDisplacement(0);
        item = mAdapter.getItem(ITEM_DISPLACEMENT);
        item.value = null;

        mVehicle.getModel().setReleaseYear(0);
        item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = null;

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickModelFragment.newInstance(mVehicle.getModel().getMakerCode(), mVehicle.getModel().getTypeCode());
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickModelFragment.TAG)
                .addToBackStack(PickModelFragment.TAG)
                .commit();
    }

    public void onModelSelected(Fragment fragment, VehicleModel model) {
        // only modelCode and modelName will be specified
        mVehicle.getModel().setModelCode(model.getModelCode());
        mVehicle.getModel().setModelName(model.getModelName());
        VehicleItem item = mAdapter.getItem(ITEM_MODEL);
        item.value = model.getModelName();

        mVehicle.getModel().setEngineCode(null);
        mVehicle.getModel().setEngineName(null);
        item = mAdapter.getItem(ITEM_FUEL);
        item.value = null;

        mVehicle.getModel().setDisplacement(0);
        item = mAdapter.getItem(ITEM_DISPLACEMENT);
        item.value = null;

        mVehicle.getModel().setReleaseYear(0);
        item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = null;

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickFuelFragment.newInstance(mVehicle.getModel().getMakerCode(),
                mVehicle.getModel().getModelCode());
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickFuelFragment.TAG)
                .addToBackStack(PickFuelFragment.TAG)
                .commit();
    }

    public void onFuelSelected(Fragment fragment, VehicleModel model) {
        // only fuelCode and fuelName will be specified
        mVehicle.getModel().setEngineCode(model.getEngineCode());
        mVehicle.getModel().setEngineName(model.getEngineName());
        VehicleItem item = mAdapter.getItem(ITEM_FUEL);
        item.value = model.getEngineName();

        mVehicle.getModel().setDisplacement(0);
        item = mAdapter.getItem(ITEM_DISPLACEMENT);
        item.value = null;

        mVehicle.getModel().setReleaseYear(0);
        item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = null;

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickDisplacementFragment.newInstance(mVehicle.getModel().getMakerCode(),
                mVehicle.getModel().getModelCode(), mVehicle.getModel().getEngineCode());
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickDisplacementFragment.TAG)
                .addToBackStack(PickDisplacementFragment.TAG)
                .commit();
    }

    public void onDisplacementSelected(Fragment fragment, VehicleModel model) {
        // displacement, type and fuelEconomy will be specified
        mVehicle.getModel().setDisplacement(model.getDisplacement());
        VehicleItem item = mAdapter.getItem(ITEM_DISPLACEMENT);
        item.value = mDecimalFormatter.format(model.getDisplacement()) + Unit.CC;

        mVehicle.getModel().setReleaseYear(0);
        item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = null;

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickReleaseYearFragment.newInstance(mVehicle.getModel().getMakerCode(),
                mVehicle.getModel().getModelCode(), mVehicle.getModel().getEngineCode(), mVehicle.getModel().getDisplacement());
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickReleaseYearFragment.TAG)
                .addToBackStack(PickReleaseYearFragment.TAG)
                .commit();
    }

    private void onReleaseYearSelected(Fragment fragment, int year) {
        mVehicle.setProdYear(year);
        VehicleItem item = mAdapter.getItem(ITEM_MODEL_YEAR);
        item.value = String.format("%d", year);

        mVehicle.setIsgCode(null);
        item = mAdapter.getItem(ITEM_ISG);
        item.value = null;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        FragmentManager fm = getChildFragmentManager();
        Fragment nextFragment = PickIsgSupportFragment.newInstance();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, nextFragment, PickIsgSupportFragment.TAG)
                .addToBackStack(PickIsgSupportFragment.TAG)
                .commit();
    }

    private void onIsgSupportSelected(Fragment fragment, String supported) {
        VehicleItem item = mAdapter.getItem(ITEM_ISG);
        if (getString(R.string.vehicle_setting_isg_support).equals(supported)) {
            mVehicle.setIsgCode(TwoState.Y.name());
        } else {
            mVehicle.setIsgCode(TwoState.N.name());
        }
        item.value = supported;

        mAdapter.notifyDataSetInvalidated();

        // Clear model ID
        mVehicle.getModel().setModelId(0);

        // Pop all stacked fragments
        FragmentManager fm = getChildFragmentManager();
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }
    }

    public void onPlateNoInput(Fragment fragment, String plateNo) {
        mVehicle.setPlateNo(plateNo);
        VehicleItem item = mAdapter.getItem(ITEM_PLATE_NO);
        item.value = plateNo;

        mAdapter.notifyDataSetInvalidated();

        FragmentManager fm = getChildFragmentManager();
        fm.popBackStack(null, 0);
    }

    private void setEditable(boolean editable) {
        for (int i = ITEM_MAKER; i <= ITEM_ISG; i++) {
            VehicleItem item = mAdapter.getItem(i);
            item.disabled = !editable;
        }

        mAdapter.notifyDataSetInvalidated();
    }

    private boolean validate() {
        VehicleModel model = mVehicle.getModel();
        if (TextUtils.isEmpty(model.getMakerCode())) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_maker, Toast.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(model.getModelCode())) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_model, Toast.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(model.getEngineCode())) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_fuel, Toast.LENGTH_LONG).show();
            return false;
        }
        if (model.getDisplacement() == 0) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_displacement, Toast.LENGTH_LONG).show();
            return false;
        }
        if (mVehicle.getProdYear() == 0) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_years, Toast.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(mVehicle.getIsgCode())) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_pick_isg, Toast.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(mVehicle.getPlateNo())) {
            Toast.makeText(getActivity(), R.string.vehicle_setting_input_plate_no, Toast.LENGTH_LONG).show();
            return false;
        }

        // Activate it!
        mVehicle.setActiveCode(TwoState.Y.name());

        return true;
    }

    public void onVehicleRegistered(Vehicle newVehicle) {
        logger.debug("onVehicleRegistered(): vehicle=" + newVehicle);

        // Copy previous obd info
        Vehicle oldVehicle = mSettingsStore.getVehicle(newVehicle.getVehicleId());
        logger.debug("onVehicleRegistered(): oldVehicle=" + oldVehicle);
        if (oldVehicle != null) {
            newVehicle.setObdAddress(oldVehicle.getObdAddress());
            newVehicle.setObdConnectionMethod(oldVehicle.getObdConnectionMethod());
            newVehicle.setObdProtocol(oldVehicle.getObdProtocol());
        }

        if (mInactiveVehicleId != null) {
            // Remove inactive vehicle id
            mSettingsStore.removeVehicleId(mInactiveVehicleId);

            mInactiveVehicleId = null;
        }

        // Store vehicle
        mSettingsStore.storeVehicle(newVehicle);
        PushManagerHelper.updateVehicleTag(getActivity(), newVehicle);

        // Add vehicle id
        mSettingsStore.addVehicleId(newVehicle.getVehicleId());
        logger.info("Vehicle ids=" + mSettingsStore.getVehicleIds());

        // Change current vehicle id
        mSettingsStore.storeVehicleId(newVehicle.getVehicleId());

        // Keep vehicle
        mVehicle = newVehicle;

        // Pick OBD device
        FragmentManager fm = getChildFragmentManager();
        Fragment fragment = PickObdDeviceFragment.newInstance(mVehicle);
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, fragment, PickObdDeviceFragment.TAG)
                .addToBackStack(null).commitAllowingStateLoss();
    }

    @Override
    public void onObdDevicePicked(String vehicleId, String obdAddress, int engineDistance, String vin, int odometer) {
        logger.debug("onObdDevicePicked(): obdAddress=" + obdAddress + ", engineDistance=" + engineDistance
                + ", vin=" + vin + ", odometer=" + odometer);

        // Store obd address and engine distance
        mVehicle.setObdAddress(obdAddress);
        mVehicle.setEngineDistance(engineDistance);
        mVehicle.setVin(vin);
        mVehicle.setOdometer(odometer);

        // Update vehicle's obd address and engine distance
        SettingsStore settingsStore = SettingsStore.getInstance();
        settingsStore.storeVehicle(mVehicle);

        String accountId = settingsStore.getAccountId();

        Vehicle update = new Vehicle();
        update.setVehicleId(mVehicle.getVehicleId());
        update.setOdometer(mVehicle.getOdometer());
        update.setEngineDistance(mVehicle.getEngineDistance());
        update.setProdYear(mVehicle.getProdYear());
        update.setVin(mVehicle.getVin());
        update.setIsgCode(mVehicle.getIsgCode());

        update.setObdAddress(obdAddress);
        update.setObdProtocol(mVehicle.getObdProtocol());
        update.setElmVer(mVehicle.getElmVer());

        FragmentManager fm = getChildFragmentManager();
        DialogFragment fragment = UpdateVehicleDialogFragment.newInstance(accountId, update);
        fm.beginTransaction().add(fragment, UpdateVehicleDialogFragment.TAG)
                .commit();
    }

    @Override
    public void onUpdateOdometerSuccess(DialogFragment fragment, Vehicle vehicle) {
        logger.debug("onUpdateOdometerSuccess()");

        mCallbacks.onVehicleRegistered(mVehicle);
    }

    @Override
    public void onUpdateOdometerFailure(DialogFragment fragment, Vehicle vehicle) {
        logger.warn("onUpdateOdometerFailure()");

        mCallbacks.onVehicleRegistered(mVehicle);
    }


    private static final int ITEM_MAKER = 0;
    private static final int ITEM_TYPE = 1;
    private static final int ITEM_MODEL = 2;
    private static final int ITEM_FUEL = 3;
    private static final int ITEM_DISPLACEMENT = 4;
    private static final int ITEM_MODEL_YEAR = 5;
    private static final int ITEM_ISG = 6;
    private static final int ITEM_PLATE_NO = 7;
    private static final int ITEM_COUNT = 8;

    class ListAdapter extends ArrayAdapter<VehicleItem> {

        private final LayoutInflater mInflater;

        ListAdapter(Context context, ArrayList<VehicleItem> items) {
            super(context, 0, items);

            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.vehicle_info_list_item, null);
            }

            VehicleItem item = getItem(position);

            TextView nameText = (TextView) convertView.findViewById(R.id.name_text);
            nameText.setText(getItemNameRes(position));

            TextView valueText = (TextView) convertView.findViewById(R.id.value_text);
            if (TextUtils.isEmpty(item.value)) {
                valueText.setText(getEmptyValueRes(position));
            } else {
                valueText.setText(item.value);
            }
            valueText.setEnabled(!item.disabled);

            return convertView;
        }

        private int getItemNameRes(int position) {
            switch (position) {
                default:
                case ITEM_MAKER:
                    return R.string.vehicle_setting_maker;
                case ITEM_TYPE:
                    return R.string.vehicle_setting_type;
                case ITEM_MODEL:
                    return R.string.vehicle_setting_model;
                case ITEM_FUEL:
                    return R.string.vehicle_setting_fuel;
                case ITEM_DISPLACEMENT:
                    return R.string.vehicle_setting_displacement;
                case ITEM_MODEL_YEAR:
                    return R.string.vehicle_setting_years;
                case ITEM_ISG:
                    return R.string.vehicle_setting_isg;
                case ITEM_PLATE_NO:
                    return R.string.vehicle_setting_plate_no;
            }
        }

        private int getEmptyValueRes(int position) {
            switch (position) {
                case ITEM_PLATE_NO:
                    return R.string.vehicle_setting_please_input;
                default:
                    return R.string.vehicle_setting_please_select;
            }
        }

    }

    class VehicleItem {
        String value;
        boolean disabled;
    }

    public interface RegisterVehicleCallbacks {
        void onVehicleRegistered(Vehicle newVehicle);

        void onVehicleRegisterCancel();
    }

    public static class PickMakerFragment extends Fragment
            implements LoadVehicleMakerCallbacks, OnItemClickListener {

        public static final String TAG = "PickMakerFragment";
        final Logger log = Logger.getLogger(TAG);

        private ListView mList;
        private MakerListAdapter mAdapter;

        public static PickMakerFragment newInstance() {
            PickMakerFragment fragment = new PickMakerFragment();
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_maker);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_maker);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<VehicleModel> items = new ArrayList<VehicleModel>();
            mAdapter = new MakerListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleMakerDialogFragment.newInstance();
            fragment.show(fm, LoadVehicleMakerDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleMakerSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {
                mAdapter.addAll(modelList.getList());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleMakerFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VehicleModel model = mAdapter.getItem(position);
            log.trace("onItemClick(): markerCode=" + model.getMakerCode());

            ((RegisterVehicleFragment) getParentFragment()).onMakerSelected(this, model);
        }

        class MakerListAdapter extends ArrayAdapter<VehicleModel> {

            private final LayoutInflater mInflater;

            MakerListAdapter(Context context, List<VehicleModel> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                VehicleModel item = getItem(position);

                CheckedTextView makerNameText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                makerNameText.setText(item.getMakerName());

                return convertView;
            }

        }

    }

    public static class PickTypeFragment extends Fragment
            implements LoadVehicleTypeCallbacks, OnItemClickListener {

        public static final String TAG = "PickTypeFragment";
        final Logger log = Logger.getLogger(TAG);

        private String mMakerCode;
        private ListView mList;
        private TypeListAdapter mAdapter;

        public static PickTypeFragment newInstance(String makerCode) {
            PickTypeFragment fragment = new PickTypeFragment();
            Bundle args = new Bundle();
            args.putString("maker_code", makerCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMakerCode = args.getString("maker_code");
            } else {
                mMakerCode = savedInstanceState.getString("maker_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("maker_code", mMakerCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_grade);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_type);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<VehicleModel> items = new ArrayList<VehicleModel>();
            mAdapter = new TypeListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleTypeDialogFragment.newInstance(mMakerCode);
            fragment.show(fm, LoadVehicleTypeDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleTypeSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {
                mAdapter.addAll(modelList.getList());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleTypeFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VehicleModel model = mAdapter.getItem(position);
            log.debug("onItemClick(): modelCode=" + model.getTypeCode());

            ((RegisterVehicleFragment) getParentFragment()).onTypeSelected(this, model);
        }

        class TypeListAdapter extends ArrayAdapter<VehicleModel> {

            private final LayoutInflater mInflater;

            TypeListAdapter(Context context, List<VehicleModel> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                VehicleModel item = getItem(position);

                CheckedTextView TypeNameText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                TypeNameText.setText(item.getTypeName());

                return convertView;
            }

        }

    }

    public static class PickModelFragment extends Fragment
            implements LoadVehicleModelCallbacks, OnItemClickListener {

        public static final String TAG = "PickModelFragment";
        final Logger log = Logger.getLogger(TAG);

        private String mMakerCode;
        private String mTypeCode;
        private ListView mList;
        private ModelListAdapter mAdapter;

        public static PickModelFragment newInstance(String makerCode, String typeCode) {
            PickModelFragment fragment = new PickModelFragment();
            Bundle args = new Bundle();
            args.putString("maker_code", makerCode);
            args.putString("type_code", typeCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMakerCode = args.getString("maker_code");
                mTypeCode = args.getString("type_code");
            } else {
                mMakerCode = savedInstanceState.getString("maker_code");
                mTypeCode = savedInstanceState.getString("type_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("maker_code", mMakerCode);
            outState.putString("type_code", mTypeCode);
            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_model);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_model);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<VehicleModel> items = new ArrayList<VehicleModel>();
            mAdapter = new ModelListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleModelDialogFragment.newInstance(mMakerCode, mTypeCode);
            fragment.show(fm, LoadVehicleModelDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleModelSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {
                mAdapter.addAll(modelList.getList());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleModelFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VehicleModel model = mAdapter.getItem(position);
            log.trace("onItemClick(): modelCode=" + model.getModelCode());

            ((RegisterVehicleFragment) getParentFragment()).onModelSelected(this, model);
        }

        class ModelListAdapter extends ArrayAdapter<VehicleModel> {

            private final LayoutInflater mInflater;

            ModelListAdapter(Context context, List<VehicleModel> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                VehicleModel item = getItem(position);

                CheckedTextView modelNameText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                modelNameText.setText(item.getModelName());

                return convertView;
            }

        }

    }

    public static class PickFuelFragment extends Fragment
            implements LoadVehicleFuelCallbacks, OnItemClickListener {

        public static final String TAG = "PickFuelFragment";
        final Logger log = Logger.getLogger(TAG);

        private String mMakerCode;
        private String mModelCode;
        private ListView mList;
        private FuelListAdapter mAdapter;

        public static PickFuelFragment newInstance(String makerCode, String modelCode) {
            PickFuelFragment fragment = new PickFuelFragment();
            Bundle args = new Bundle();
            args.putString("maker_code", makerCode);
            args.putString("model_code", modelCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMakerCode = args.getString("maker_code");
                mModelCode = args.getString("model_code");
            } else {
                mMakerCode = savedInstanceState.getString("maker_code");
                mModelCode = savedInstanceState.getString("model_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("maker_code", mMakerCode);
            outState.putString("model_code", mModelCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_fuel);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_fuel);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<VehicleModel> items = new ArrayList<VehicleModel>();
            mAdapter = new FuelListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleFuelDialogFragment.newInstance(mMakerCode, mModelCode);
            fragment.show(fm, LoadVehicleFuelDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleFuelSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {
                mAdapter.addAll(modelList.getList());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleFuelFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VehicleModel model = mAdapter.getItem(position);
            log.trace("onItemClick(): modelCode=" + model.getModelCode());

            ((RegisterVehicleFragment) getParentFragment()).onFuelSelected(this, model);
        }

        class FuelListAdapter extends ArrayAdapter<VehicleModel> {

            private final LayoutInflater mInflater;

            FuelListAdapter(Context context, List<VehicleModel> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                VehicleModel item = getItem(position);

                CheckedTextView fuelNameText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                fuelNameText.setText(item.getEngineName());

                return convertView;
            }

        }
    }

    public static class PickDisplacementFragment extends Fragment
            implements LoadVehicleDisplacementCallbacks, OnItemClickListener {

        public static final String TAG = "PickDisplacementFragment";
        final Logger log = Logger.getLogger(TAG);

        private String mMakerCode;
        private String mModelCode;
        private String mFuelCode;
        private ListView mList;
        private DisplacementListAdapter mAdapter;

        public static PickDisplacementFragment newInstance(String makerCode, String modelCode, String fuelCode) {
            PickDisplacementFragment fragment = new PickDisplacementFragment();
            Bundle args = new Bundle();
            args.putString("maker_code", makerCode);
            args.putString("model_code", modelCode);
            args.putString("fuel_code", fuelCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMakerCode = args.getString("maker_code");
                mModelCode = args.getString("model_code");
                mFuelCode = args.getString("fuel_code");
            } else {
                mMakerCode = savedInstanceState.getString("maker_code");
                mModelCode = savedInstanceState.getString("model_code");
                mFuelCode = savedInstanceState.getString("fuel_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("maker_code", mMakerCode);
            outState.putString("model_code", mModelCode);
            outState.putString("fuel_code", mFuelCode);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_displacement);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_displacement);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<VehicleModel> items = new ArrayList<VehicleModel>();
            mAdapter = new DisplacementListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleDisplacementDialogFragment.newInstance(mMakerCode, mModelCode, mFuelCode);
            fragment.show(fm, LoadVehicleDisplacementDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleDisplacementSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {

                Logger.getLogger("register").debug("onLoadVehicleDisplacementSuccess#" + modelList.getList().get(0).toString());

                mAdapter.addAll(modelList.getList());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleDisplacementFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            VehicleModel model = mAdapter.getItem(position);
            log.trace("onItemClick(): displacement=" + model.getDisplacement());

            ((RegisterVehicleFragment) getParentFragment()).onDisplacementSelected(this, model);
        }

        class DisplacementListAdapter extends ArrayAdapter<VehicleModel> {

            private final LayoutInflater mInflater;
            private NumberFormat mDecimalFormatter = NumberFormat.getInstance();

            DisplacementListAdapter(Context context, List<VehicleModel> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                VehicleModel item = getItem(position);

                CheckedTextView displacementText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                displacementText.setText(mDecimalFormatter.format(item.getDisplacement()) + Unit.CC);

                return convertView;
            }

        }

    }

    public static class PickReleaseYearFragment extends Fragment
            implements LoadVehicleReleaseYearCallbacks, OnItemClickListener {

        public static final String TAG = "PickReleaseYearFragment";
        final Logger log = Logger.getLogger(TAG);

        private String mMakerCode;
        private String mModelCode;
        private String mFuelCode;
        private int mDisplacement;
        private ListView mList;
        private ReleaseYearListAdapter mAdapter;

        public static PickReleaseYearFragment newInstance(String makerCode, String modelCode, String fuelCode, int displacement) {
            PickReleaseYearFragment fragment = new PickReleaseYearFragment();
            Bundle args = new Bundle();
            args.putString("maker_code", makerCode);
            args.putString("model_code", modelCode);
            args.putString("fuel_code", fuelCode);
            args.putInt("eng_disp", displacement);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mMakerCode = args.getString("maker_code");
                mModelCode = args.getString("model_code");
                mFuelCode = args.getString("fuel_code");
                mDisplacement = args.getInt("eng_disp");
            } else {
                mMakerCode = savedInstanceState.getString("maker_code");
                mModelCode = savedInstanceState.getString("model_code");
                mFuelCode = savedInstanceState.getString("fuel_code");
                mDisplacement = savedInstanceState.getInt("eng_disp");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("maker_code", mMakerCode);
            outState.putString("model_code", mModelCode);
            outState.putString("fuel_code", mFuelCode);
            outState.putInt("eng_disp", mDisplacement);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_years);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_years);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<Integer> items = new ArrayList<>();
            mAdapter = new ReleaseYearListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getChildFragmentManager();
            DialogFragment fragment = LoadVehicleReleaseYearDialogFragment.newInstance(mMakerCode, mModelCode, mFuelCode, mDisplacement);
            fragment.show(fm, LoadVehicleReleaseYearDialogFragment.TAG);
        }

        @Override
        public void onLoadVehicleReleaseYearSuccess(VehicleModelList modelList) {
            if (modelList != null && modelList.getList() != null) {

                VehicleModel model = modelList.getList().get(0);
                int discontinued = model.getDiscontinuedYear() == 9999 ? Calendar.getInstance().get(Calendar.YEAR) : model.getDiscontinuedYear();
                for (int i = model.getReleaseYear(); i <= discontinued; i++) {
                    mAdapter.add(i);
                }

                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onLoadVehicleReleaseYearFailure() {
            FragmentManager fm = getParentFragment().getChildFragmentManager();
            fm.beginTransaction().remove(this).commit();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            int year = mAdapter.getItem(position);

            ((RegisterVehicleFragment) getParentFragment()).onReleaseYearSelected(this, mAdapter.getItem(position));
        }

        class ReleaseYearListAdapter extends ArrayAdapter<Integer> {

            private final LayoutInflater mInflater;

            ReleaseYearListAdapter(Context context, List<Integer> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                CheckedTextView ReleaseYearText = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                ReleaseYearText.setText(String.format("%d", getItem(position)));

                return convertView;
            }
        }
    }

    public static class PickIsgSupportFragment extends Fragment implements OnItemClickListener {

        public static final String TAG = "PickIsgSupportFragment";
        final Logger log = Logger.getLogger(TAG);

        private ListView mList;
        private IsgSupportListAdapter mAdapter;

        public static PickIsgSupportFragment newInstance() {
            PickIsgSupportFragment fragment = new PickIsgSupportFragment();
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_pick_vehicle_item, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_isg);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_pick_isg);

            mList = (ListView) view.findViewById(R.id.list);
            mList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            ArrayList<String> items = new ArrayList<>();
            items.add(getString(R.string.vehicle_setting_isg_support));
            items.add(getString(R.string.vehicle_setting_isg_not_support));

            mAdapter = new IsgSupportListAdapter(getActivity(), items);
            mList.setAdapter(mAdapter);

            mList.setOnItemClickListener(this);

        }

//        @Override
//        public void onActivityCreated(Bundle savedInstanceState) {
//            super.onActivityCreated(savedInstanceState);
//
//            FragmentManager fm = getChildFragmentManager();
//            DialogFragment fragment = LoadVehicleReleaseYearDialogFragment.newInstance(mMakerCode, mModelCode, mFuelCode, mDisplacement);
//            fragment.show(fm, LoadVehicleReleaseYearDialogFragment.TAG);
//        }

//        @Override
//        public void onLoadVehicleReleaseYearSuccess(VehicleModelList modelList) {
//            if (modelList != null && modelList.getList() != null) {
//
//                VehicleModel model = modelList.getList().get(0);
//                int discontinued = model.getDiscontinuedYear() == 9999 ? Calendar.getInstance().get(Calendar.YEAR) : model.getDiscontinuedYear();
//                for (int i = model.getReleaseYear(); i <= discontinued; i++) {
//                    mAdapter.add(i);
//                }
//
//                mAdapter.notifyDataSetChanged();
//            }
//        }

//        @Override
//        public void onLoadVehicleReleaseYearFailure() {
//            FragmentManager fm = getParentFragment().getChildFragmentManager();
//            fm.beginTransaction().remove(this).commit();
//        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            ((RegisterVehicleFragment) getParentFragment()).onIsgSupportSelected(this, mAdapter.getItem(position));
        }

        class IsgSupportListAdapter extends ArrayAdapter<String> {

            private final LayoutInflater mInflater;

            IsgSupportListAdapter(Context context, List<String> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_single_choice, null);
                }

                CheckedTextView tv = (CheckedTextView) convertView.findViewById(android.R.id.text1);
                tv.setText(getItem(position));

                return convertView;
            }
        }
    }

    public static class InputPlateNoFragment extends Fragment
            implements OnClickListener, OnEditorActionListener {

        public static final String TAG = "InputPlateNoFragment";

        private String mPlateNo;
        private EditText mPlateNoEdit;

        public static InputPlateNoFragment newInstance(String plateNo) {
            InputPlateNoFragment fragment = new InputPlateNoFragment();
            Bundle args = new Bundle();
            args.putString("plate_no", plateNo);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mPlateNo = args.getString("plate_no");
            } else {
                mPlateNo = savedInstanceState.getString("plate_no");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("plate_no", mPlateNo);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_input_plate_no, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ImageView titleImg = (ImageView) view.findViewById(R.id.title_img);
            titleImg.setImageResource(R.drawable.ic_vehicle_plate_no);

            TextView titleText = (TextView) view.findViewById(R.id.title);
            titleText.setText(R.string.vehicle_setting_input_plate_no);

            mPlateNoEdit = (EditText) view.findViewById(R.id.plate_no_edit);
            mPlateNoEdit.setText(mPlateNo);
            mPlateNoEdit.setOnEditorActionListener(this);

            view.findViewById(R.id.done_btn).setOnClickListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();

            mPlateNoEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mPlateNoEdit, InputMethodManager.SHOW_IMPLICIT);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.done_btn) {
                onDone();
            }
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            int id = v.getId();

            if (id == R.id.plate_no_edit) {
                int action = actionId & EditorInfo.IME_MASK_ACTION;
                if (action == EditorInfo.IME_ACTION_DONE) {
                    onDone();
                    return true;
                }
            }
            return false;
        }

        private void onDone() {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mPlateNoEdit.getWindowToken(), 0);

            mPlateNo = mPlateNoEdit.getText().toString().trim();
            ((RegisterVehicleFragment) getParentFragment()).onPlateNoInput(this, mPlateNo);
        }

    }

    public static class RegisterVehicleProcessDialogFragment extends DialogFragment implements AlertDialogCallbacks {

        public static final String TAG = "RegisterVehicleProcessDialogFragment";
        final Logger log = Logger.getLogger(TAG);

        private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
        private String mAccountId;
        private Vehicle mVehicle;
        private String mInactiveVehicleId;

        public static RegisterVehicleProcessDialogFragment newInstance(Vehicle vehicle, String deactiveVehicleId) {
            RegisterVehicleProcessDialogFragment fragment = new RegisterVehicleProcessDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("vehicle", vehicle);
            args.putString("inactive_vehicle_id", deactiveVehicleId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            SettingsStore settingsStore = SettingsStore.getInstance();
            mAccountId = settingsStore.getAccountId();
            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mVehicle = (Vehicle) args.getSerializable("vehicle");
                mInactiveVehicleId = args.getString("inactive_vehicle_id");
            } else {
                mVehicle = (Vehicle) savedInstanceState.getSerializable("vehicle");
                mInactiveVehicleId = savedInstanceState.getString("inactive_vehicle_id");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("vehicle", mVehicle);
            outState.putString("inactive_vehicle_id", mInactiveVehicleId);

            super.onSaveInstanceState(outState);
        }

        @Override
        public void onDestroy() {
            mRequestQueue.cancelAll(TAG);

            super.onDestroy();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new WaitForDialog(getActivity());
            setCancelable(false);
            return dialog;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            GetVehicleModelRequest request = new GetVehicleModelRequest(mVehicle.getModel(),
                    new ValidateModelListener());
            request.setTag(TAG);
            mRequestQueue.add(request);
        }

        @Override
        public void onDialogButtonClick(DialogFragment fragment, int which) {
            String tag = fragment.getTag();

            if ("validate-model-failure-dialog".equals(tag)) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    GetVehicleModelRequest request = new GetVehicleModelRequest(mVehicle.getModel(),
                            new ValidateModelListener());
                    request.setTag(TAG);
                    mRequestQueue.add(request);
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    dismiss();
                }
            } else if ("register-vehicle-failure-dialog".equals(tag)) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    RegisterVehicleRequest request = new RegisterVehicleRequest(
                            mRequestQueue,
                            mAccountId, mVehicle, mInactiveVehicleId,
                            new RegisterVehicleListener());
                    request.request(TAG);
                } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                    dismiss();
                }
            }
        }

        private class ValidateModelListener extends VolleyListener<VehicleModel> {

            @Override
            public void onResponse(VehicleModel model) {
                if (model != null) {
                    log.trace("ValidateModelListener::onResponse(): model=" + model);

                    mVehicle.setModel(model);

                    RegisterVehicleRequest request = new RegisterVehicleRequest(
                            mRequestQueue,
                            mAccountId, mVehicle, mInactiveVehicleId,
                            new RegisterVehicleListener());
                    request.request(TAG);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof ServerError) {
                    FragmentManager fm = getChildFragmentManager();
                    DialogFragment fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_server_error),
                            getString(R.string.dialog_message_server_error),
                            getString(R.string.btn_no),
                            getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "validate-model-failure-dialog")
                            .commitAllowingStateLoss();
                } else {
                    FragmentManager fm = getChildFragmentManager();
                    DialogFragment fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_network_error),
                            getString(R.string.dialog_message_network_error),
                            getString(R.string.btn_no),
                            getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "validate-model-failure-dialog")
                            .commitAllowingStateLoss();
                }
            }

        }

        private class RegisterVehicleListener extends VolleyListener<String> {

            @Override
            public void onResponse(String vehicleId) {
                if (vehicleId != null) {
                    mVehicle.setVehicleId(vehicleId);
                    log.debug("RegisterListener::onRequestSuccess(): vehicle=" + mVehicle);

                    ((RegisterVehicleFragment) getParentFragment()).onVehicleRegistered(mVehicle);
                    dismiss();
                } else {
                    FragmentManager fm = getChildFragmentManager();
                    AlertDialogFragment fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_server_error),
                            getString(R.string.dialog_message_server_error),
                            getString(R.string.btn_no),
                            getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "register-vehicle-failure-dialog")
                            .commitAllowingStateLoss();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                FragmentManager fm = getChildFragmentManager();
                AlertDialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "register-vehicle-failure-dialog")
                        .commitAllowingStateLoss();
            }

        }

    }

}

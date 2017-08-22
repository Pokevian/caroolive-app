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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleModel;
import com.pokevian.app.smartfleet.request.GetVehicleModelRequest;
import com.pokevian.app.smartfleet.request.RegisterVehicleRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.model.TwoState;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class PickVehicleFragment extends Fragment implements OnItemClickListener, View.OnClickListener {

    public static final String TAG = "PickVehicleFragment";
    final Logger logger = Logger.getLogger(TAG);

    private ListView mListView;
    private Button mPrevBtn;
    private Button mNextBtn;

    private VehicleListAdapter mVehicleAdapter;
    //    private BluetoothDevice mObdDevice;
//    private BroadcastReceiver mDiscoveryReceiver;
//    private BroadcastReceiver mBondStateReceiver;
//    private VehicleService mVehicleService;

    private ArrayList<Vehicle> mVehiceList;
    private Vehicle mVehicle;
    private String mInactiveVehicleId;
    private PickVehicleCallbacks mCallbacks;

//    private BluetoothAdapter mBtAdapter;
//    private boolean mIsDiscovery;

    public static PickVehicleFragment newInstance(ArrayList<Vehicle> list) {
        PickVehicleFragment fragment = new PickVehicleFragment();
        Bundle args = new Bundle();
        args.putSerializable("list", list);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (PickVehicleCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement SelectObdDeviceCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (PickVehicleCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement SelectObdDeviceCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        startAndBindVehicleService();

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mVehiceList = (ArrayList<Vehicle>) args.getSerializable("list");
//            mIsDiscovery = false;
        } else {
            mVehiceList = (ArrayList<Vehicle>) savedInstanceState.getSerializable("list");
//            mIsDiscovery = savedInstanceState.getBoolean("discovery");
        }

        if (mVehiceList != null) {
            for (Vehicle v : mVehiceList) {
                if (TwoState.Y.name().equals(v.getActiveCode())) {
                    mVehicle = v;
                    mInactiveVehicleId = mVehicle.getVehicleId();
                    break;
                }
            }
        }


//        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Has options menu ('discovery')
//        setHasOptionsMenu(true);

        mVehicleAdapter = new VehicleListAdapter(getActivity(), mVehiceList);
    }

//    @Override
//    public void onDestroy() {
////        unbindVehicleService();
//
//        super.onDestroy();
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("list", mVehiceList);
//        outState.putBoolean("discovery", mIsDiscovery);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_vehicle, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setAdapter(mVehicleAdapter);
        mListView.setOnItemClickListener(this);

        mPrevBtn = (Button) view.findViewById(R.id.prev_btn);
        mPrevBtn.setText(R.string.btn_cancel);
        mPrevBtn.setOnClickListener(this);

        mNextBtn = (Button) view.findViewById(R.id.next_btn);
        mNextBtn.setText(R.string.btn_add);
        mNextBtn.setOnClickListener(this);
    }

    public void onVehicleRegistered(Vehicle newVehicle) {
        logger.debug("onVehicleRegistered(): vehicle=" + newVehicle);

        SettingsStore mSettingsStore = SettingsStore.getInstance();
        // Copy previous obd info
        Vehicle oldVehicle = mSettingsStore.getVehicle(newVehicle.getVehicleId());
        logger.debug("onVehicleRegistered(): oldVehicle=" + oldVehicle);
        if (oldVehicle != null) {
//            newVehicle.setObdAddress(oldVehicle.getObdAddress());
//            newVehicle.setObdConnectionMethod(oldVehicle.getObdConnectionMethod());
//            newVehicle.setObdProtocol(oldVehicle.getObdProtocol());
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
//        FragmentManager fm = getChildFragmentManager();
//        Fragment fragment = PickObdDeviceFragment.newInstance(mVehicle);
//        fm.beginTransaction()
//                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
//                        R.anim.slide_in_left, R.anim.slide_out_right)
//                .add(R.id.container, fragment, PickObdDeviceFragment.TAG)
//                .addToBackStack(null).commitAllowingStateLoss();

        mCallbacks.onVehiclePicked(oldVehicle, newVehicle);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        mVehicleAdapter.setChecked(position);
//        mVehicleAdapter.notifyDataSetInvalidated();

        final Vehicle item = mVehicleAdapter.getItem(position);
        Logger.getLogger(TAG).debug("onItemClick#" + item.toString());
        if (TwoState.Y.name().equals(item.getActiveCode())) {
            getActivity().setResult(RESULT_OK);
            getActivity().finish();
        } else {
            try {
//                Logger.getLogger(TAG).debug(item.toString());
//                FragmentManager fm = getChildFragmentManager();
//                DialogFragment dialogFragment = SelectVehicleFragment.RegisterVehicleProcessDialogFragment
//                        .newInstance(item, mVehicle.getVehicleId());
//                dialogFragment.show(fm, SelectVehicleFragment.RegisterVehicleProcessDialogFragment.TAG);
                item.setActiveCode(TwoState.Y.name());

//                ((SelectVehicleFragment) getParentFragment()).showRegisterVehicleProcessDialog(item, mInactiveVehicleId);
                FragmentManager fm = getChildFragmentManager();
                DialogFragment dialogFragment = RegisterVehicleProcessDialogFragment
                        .newInstance(item, mInactiveVehicleId);
                dialogFragment.show(fm, RegisterVehicleProcessDialogFragment.TAG);


            } catch (NullPointerException e) {

            }
        }


    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.prev_btn) {
            getActivity().setResult(RESULT_OK);
            getActivity().finish();

        } else if (id == R.id.next_btn) {
            ((SelectVehicleFragment)getParentFragment()).popBackStack();
        }
    }

    public static class RegisterVehicleProcessDialogFragment extends DialogFragment implements AlertDialogFragment.AlertDialogCallbacks {

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
                    new RegisterVehicleProcessDialogFragment.ValidateModelListener());
            request.setTag(TAG);
            mRequestQueue.add(request);
        }

        @Override
        public void onDialogButtonClick(DialogFragment fragment, int which) {
            String tag = fragment.getTag();

            if ("validate-model-failure-dialog".equals(tag)) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    GetVehicleModelRequest request = new GetVehicleModelRequest(mVehicle.getModel(),
                            new RegisterVehicleProcessDialogFragment.ValidateModelListener());
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
                            new RegisterVehicleProcessDialogFragment.RegisterVehicleListener());
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
                    log.debug("ValidateModelListener::onResponse(): model=" + model);

                    mVehicle.setModel(model);

                    RegisterVehicleRequest request = new RegisterVehicleRequest(
                            mRequestQueue,
                            mAccountId, mVehicle, mInactiveVehicleId,
                            new RegisterVehicleProcessDialogFragment.RegisterVehicleListener());
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

                    ((PickVehicleFragment) getParentFragment()).onVehicleRegistered(mVehicle);
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



    private final class VehicleListAdapter extends ArrayAdapter<Vehicle> {

        private List<Vehicle> mItems;
        private final LayoutInflater mInflater;

        private VehicleListAdapter(Context context, List<Vehicle> items) {
            super(context, 0, items);
//            mObdDeviceAddress = obdDeviceAddress;
            mItems = items;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View view , ViewGroup parent) {
            if (view  == null) {
                view  = mInflater.inflate(R.layout.obd_device_list_item, null);
            }

            Vehicle item = getItem(position);
            ((TextView) view.findViewById(R.id.name)).setText(item.getPlateNo());
            ((TextView) view.findViewById(R.id.address)).setText(item.getModel().getModelName());
            ((RadioButton) view.findViewById(R.id.select)).setChecked(TwoState.Y.name().equals(item.getActiveCode()));


//            if (item.name == null) {
//                BluetoothDevice device = mBtAdapter.getRemoteDevice(item.address);
//                item.name = device.getName();
//            }
//
//            TextView nameText = (TextView) convertView.findViewById(R.id.name);
//            String name = item.name
//                    + (item.address.equals(mObdDeviceAddress) ? " (*)" : "");
//            nameText.setText(name);
//
//            TextView addressText = (TextView) convertView.findViewById(R.id.address);
//            addressText.setText(item.address);
//
//            RadioButton selectRadio = (RadioButton) convertView.findViewById(R.id.select);
//            selectRadio.setChecked(item.isChecked);

            return view ;
        }

//        @Override
//        public void add(DeviceItem newItem) {
//            // Check same device
//            int count = getCount();
//            for (int i = 0; i < count; i++) {
//                DeviceItem item = getItem(i);
//                if (newItem.device.getAddress().equals(item.device.getAddress())) {
//                    logger.debug(">>> same device=" + newItem.device);
//                    return;
//                }
//            }
//
//            super.add(newItem);
//        }

//        private void setChecked(int position) {
//            int count = getCount();
//            for (int p = 0; p < count; p++) {
//                if (p == position) {
//                    getItem(p).isChecked = true;
//                } else {
//                    getItem(p).isChecked = false;
//                }
//            }
//        }

    }

    public interface PickVehicleCallbacks {
        void onVehiclePicked(Vehicle old, Vehicle picked);
    }

}

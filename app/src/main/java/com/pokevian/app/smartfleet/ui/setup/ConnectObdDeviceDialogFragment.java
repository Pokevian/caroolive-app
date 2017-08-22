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
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.VehicleService.VehicleServiceBinder;
import com.pokevian.app.smartfleet.ui.setup.InputOdometerDialogFragment.InputOdometerCallbacks;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.DeviceInfo;
import com.pokevian.lib.obd2.defs.DeviceInfo.Protocol;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

import org.apache.log4j.Logger;

public class ConnectObdDeviceDialogFragment extends DialogFragment implements InputOdometerCallbacks {

    public static final String TAG = "ConnectObdDeviceDialogFragment";
    final Logger logger = Logger.getLogger(TAG);

    public static final int STEP_0 = 1;
    public static final int STEP_1 = 2;
    public static final int STEP_2 = 3;
    public static final int STEP_3 = 4;
    public static final int STEP_4 = 5;
    public static final int STEP_5 = 6;
    public static final int STEP_MAX = STEP_5;

    private Vehicle mVehicle;
    private VehicleService mVehicleService;
    private LocalBroadcastManager mBroadcastManager;
    private ConnectObdDeviceCallbacks mCallbacks;
    private boolean mIsDataReceived = false;

    public static ConnectObdDeviceDialogFragment newInstance(Vehicle vehicle) {
        ConnectObdDeviceDialogFragment fragment = new ConnectObdDeviceDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("vehicle", vehicle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (ConnectObdDeviceCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement ConnectVehicleCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (ConnectObdDeviceCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement ConnectVehicleCallbacks");
            }
        }

        // Ensure Bluetooth is enabled
        ensureBluetoothEnabled();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mVehicle = (Vehicle) args.getSerializable("vehicle");
            if (TextUtils.isEmpty(mVehicle.getVehicleId())) {
                throw new RuntimeException("Vehicle ID is not speicified!");
            }
        } else {
            mVehicle = (Vehicle) savedInstanceState.getSerializable("vehicle");
        }

        mVehicle.setObdProtocol(Protocol.AUTO.getCode());
        mVehicle.setObdConnectionMethod(0);

        mBroadcastManager = LocalBroadcastManager.getInstance(getActivity());

        registerVehicleReceiver();

        startAndBindVehicleService();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("vehicle", mVehicle);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        unregisterVehicleReceiver();

        unbindVehicleService();

        super.onDestroy();
    }

    private ProgressBar mProgressBar;
    private TextView mProgressMessage;
    private TextView mProgressStep;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_connect_obd_device, null);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        mProgressBar.setMax(STEP_MAX);
        mProgressMessage = (TextView) view.findViewById(R.id.progress_message);
        mProgressStep = (TextView) view.findViewById(R.id.progress_step);

        setCancelable(true);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.vehicle_setting_obd_connection)
                .setView(view)
                .create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mCallbacks.onObdDeviceNotConnected(mVehicle);

        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
        }
    }

    private void startAndBindVehicleService() {
        getActivity().startService(new Intent(getActivity(), VehicleService.class));
        getActivity().bindService(new Intent(getActivity(), VehicleService.class),
                mVehicleServiceConnection, 0);
    }

    private void unbindVehicleService() {
        try {
            getActivity().unbindService(mVehicleServiceConnection);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private ServiceConnection mVehicleServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            logger.info("Vehicle service connected...");

            mVehicleService = ((VehicleServiceBinder) binder).getService();
            mVehicleService.resetVehiclePersistData(mVehicle.getVehicleId());
            mVehicleService.disconnectVehicle();

            connect();
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void registerVehicleReceiver() {
        unregisterVehicleReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_INSUFFICIENT_PID);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
        mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
    }

    private void unregisterVehicleReceiver() {
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (isDetached() || isRemoving() || !isAdded()) return;

            final String action = intent.getAction();
            if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                ObdState state = (ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                Parcelable extra = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE_EXTRA);
                onObdStateChanged(state, extra);
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                onObdCannotConnect();
            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
                onObdDeviceNotSupported();
            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
                onObdProtocolNotSupported();
            } else if (VehicleDataBroadcaster.ACTION_OBD_INSUFFICIENT_PID.equals(action)) {
                onObdInsufficientPid();
            } else if (VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED.equals(action)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
            }
        }
    };

    private void onObdStateChanged(ObdState state, Parcelable extra) {
        logger.warn("onObdStateChanged(): state=" + state + ", extra=" + extra);

        if (state == ObdState.READY_TO_SCAN && extra != null) {
            DeviceInfo deviceInfo = (DeviceInfo) extra;
            mVehicle.setObdProtocol(deviceInfo.protocol.getCode());
            mVehicle.setObdConnectionMethod(deviceInfo.connectionMethod);
            mVehicle.setElmVer(deviceInfo.chipset);
        }

        if (state == ObdState.CONNECTING) {
            mProgressBar.setProgress(STEP_0);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step0);
            mProgressStep.setText(String.format("%d / %d", STEP_0, STEP_MAX));
        } else if (state == ObdState.CONNECTED) {
            mProgressBar.setProgress(STEP_1);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step1);
            mProgressStep.setText(String.format("%d / %d", STEP_1, STEP_MAX));
        } else if (state == ObdState.INITIALIZING) {
            mProgressBar.setProgress(STEP_2);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step2);
            mProgressStep.setText(String.format("%d / %d", STEP_2, STEP_MAX));
        } else if (state == ObdState.READY_TO_SCAN) {
            mProgressBar.setProgress(STEP_3);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step3);
            mProgressStep.setText(String.format("%d / %d", STEP_3, STEP_MAX));
        } else if (state == ObdState.SCANNING) {
            mProgressBar.setProgress(STEP_4);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step4);
            mProgressStep.setText(String.format("%d / %d", STEP_4, STEP_MAX));
        }
    }

    private void onObdCannotConnect() {
        mVehicleService.disconnectVehicle();

        DialogFragment fragment = new CannotConnectDialogFragment();
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction().add(fragment, CannotConnectDialogFragment.TAG)
                .commitAllowingStateLoss();

        // Enable bluetooth if disabled
        ensureBluetoothEnabled();
    }

    private void onConnectCanceled() {
        if (isDetached() || isRemoving() || !isAdded()) return;

        mCallbacks.onObdDeviceNotConnected(mVehicle);

        // dismiss self
        getFragmentManager().beginTransaction().remove(this)
                .commitAllowingStateLoss();
    }

    private void connect() {
        mVehicleService.connectVehicle(mVehicle);
        mIsDataReceived = false;
    }

    public static class CannotConnectDialogFragment extends DialogFragment {

        public static final String TAG = "cannot-connect-dialog";

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_message_cannot_connect_obd_device)
                    .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((ConnectObdDeviceDialogFragment) getParentFragment()).onConnectCanceled();
                        }
                    })
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((ConnectObdDeviceDialogFragment) getParentFragment()).connect();
                        }
                    })
                    .create();
        }

        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            ((ConnectObdDeviceDialogFragment) getParentFragment()).onConnectCanceled();
        }
    }

    private void onObdDeviceNotSupported() {
        mVehicleService.disconnectVehicle();

        DialogFragment fragment = new DeviceNotSupportedDialogFragment();
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction().add(fragment, DeviceNotSupportedDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    public void onDeviceNotSupported() {
        if (isDetached() || isRemoving() || !isAdded()) return;

        mCallbacks.onObdDeviceNotConnected(mVehicle);

        // dismiss self
        getFragmentManager().beginTransaction().remove(this)
                .commitAllowingStateLoss();
    }

    public static class DeviceNotSupportedDialogFragment extends DialogFragment {

        public static final String TAG = "devuce-not-supported-dialog";

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_message_cannot_connect_obd_device)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
                        }
                    })
                    .create();
        }

        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
        }
    }

    private void onObdProtocolNotSupported() {
        mVehicleService.disconnectVehicle();

        DialogFragment fragment = new ProtocolNotSupportedDialogFragment();
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction().add(fragment, ProtocolNotSupportedDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    public static class ProtocolNotSupportedDialogFragment extends DialogFragment {

        public static final String TAG = "protocol-not-supported-dialog";

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_message_not_supported_obd_protocol)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
                        }
                    })
                    .create();
        }

        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
        }

    }

    private void onObdInsufficientPid() {
        mVehicleService.disconnectVehicle();

        DialogFragment fragment = new InsufficientPidDialogFragment();
        FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction().add(fragment, InsufficientPidDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    public static class InsufficientPidDialogFragment extends DialogFragment {

        public static final String TAG = "insufficient-pid-dialog";

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.dialog_message_insufficient_pid)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
                        }
                    })
                    .create();
        }

        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            ((ConnectObdDeviceDialogFragment) getParentFragment()).onDeviceNotSupported();
        }

    }

    private void onObdDataReceived(ObdData obdData) {
        logger.debug("onObdDataReceived(): has RPM=" + obdData.isValid(KEY.SAE_RPM));

        if (!mIsDataReceived && obdData.isValid(KEY.SAE_RPM)) {
            mIsDataReceived = true;

            mProgressBar.setProgress(STEP_5);
            mProgressMessage.setText(R.string.vehicle_setting_obd_step5);
            mProgressStep.setText(String.format("%d / %d", STEP_5, STEP_MAX));

            // Read VIN if supported
            if (obdData.isValid(KEY.SAE_VIN)) {
                mVehicle.setVin(obdData.getString(KEY.SAE_VIN));
            }

            // Read engine distance if supported
            if (obdData.isValid(KEY.SAE_DIST)) {
                mVehicle.setEngineDistance(obdData.getInteger(KEY.SAE_DIST));
            }

            unregisterVehicleReceiver();

            FragmentManager fm = getChildFragmentManager();
            if (fm.findFragmentByTag(InputOdometerDialogFragment.TAG) == null) {
                Fragment fragment = InputOdometerDialogFragment.newInstance();
                fm.beginTransaction()
                        .add(fragment, InputOdometerDialogFragment.TAG)
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onOdometerInput(int odometer) {
        if (odometer > 0) {
            // dismiss self
            getFragmentManager().beginTransaction().remove(this)
                    .commitAllowingStateLoss();

            mVehicle.setOdometer(odometer);

            mCallbacks.onObdDeviceConnected(mVehicle);
        } else {
            Fragment fragment = InputOdometerDialogFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .add(fragment, InputOdometerDialogFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    private void ensureBluetoothEnabled() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            btAdapter.enable();
        }
    }

    public interface ConnectObdDeviceCallbacks {
        void onObdDeviceConnected(Vehicle vehicle);

        void onObdDeviceNotConnected(Vehicle vehicle);
    }

}

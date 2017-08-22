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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.VehicleService.VehicleServiceBinder;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.BluetoothControlFragment;
import com.pokevian.app.smartfleet.ui.common.BluetoothControlFragment.BluetoothControlCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.ui.setup.ConnectObdDeviceDialogFragment.ConnectObdDeviceCallbacks;

import org.apache.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

public class PickObdDeviceFragment extends Fragment implements OnItemClickListener, BluetoothControlCallbacks,
        ConnectObdDeviceCallbacks {

    public static final String TAG = "PickObdDeviceFragment";
    final Logger logger = Logger.getLogger(TAG);

    private ListView mDeviceList;
    private DeviceAdapter mDeviceAdapter;
    private BluetoothDevice mObdDevice;
    private BroadcastReceiver mDiscoveryReceiver;
    private BroadcastReceiver mBondStateReceiver;
    private VehicleService mVehicleService;

    private Vehicle mVehicle;
    private PickObdDeviceCallbacks mCallbacks;

    private BluetoothAdapter mBtAdapter;
    private boolean mIsDiscovery;

    public static PickObdDeviceFragment newInstance(Vehicle vehicle) {
        PickObdDeviceFragment fragment = new PickObdDeviceFragment();
        Bundle args = new Bundle();
        args.putSerializable("vehicle", vehicle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (PickObdDeviceCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement SelectObdDeviceCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (PickObdDeviceCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement SelectObdDeviceCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startAndBindVehicleService();

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mVehicle = (Vehicle) args.getSerializable("vehicle");
            mIsDiscovery = false;
        } else {
            mVehicle = (Vehicle) savedInstanceState.getSerializable("vehicle");
            mIsDiscovery = savedInstanceState.getBoolean("discovery");
        }

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Has options menu ('discovery')
        setHasOptionsMenu(true);

        mDeviceAdapter = new DeviceAdapter(getActivity(), mVehicle.getObdAddress());
    }

    @Override
    public void onDestroy() {
        unbindVehicleService();

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("vehicle", mVehicle);
        outState.putBoolean("discovery", mIsDiscovery);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pick_obd_device, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleText = (TextView) view.findViewById(R.id.title);
        titleText.setText(R.string.vehicle_setting_pick_obd_device);

        mDeviceList = (ListView) view.findViewById(R.id.obd_device_list);
        mDeviceList.setAdapter(mDeviceAdapter);
        mDeviceList.setOnItemClickListener(this);
        mDeviceList.setEmptyView(view.findViewById(R.id.empty_obd_device));

        FragmentManager fm = getChildFragmentManager();
        Fragment discoveryDialog = fm.findFragmentByTag(DiscoveryDialogFragment.TAG);
        if (discoveryDialog != null) {
            ((DialogFragment) discoveryDialog).dismiss();
            logger.debug("onViewCreated#dismiss");
        }

        // Add bluetooth control fragment (remove previous fragment if exist)
//        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(BluetoothControlFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = BluetoothControlFragment.newInstance();
        ft.add(fragment, BluetoothControlFragment.TAG);
        ft.commit();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pick_obd_device, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_discovery) {
            startDiscovory();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
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
            mVehicleService.disconnectVehicle();
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onBluetoothTurnedOn(boolean isEnabled) {
        logger.debug("onBluetoothTurnedOn()");

        // Load bonded devices
        loadBondedDevices();

        if (mDeviceAdapter.isEmpty()) {
            startDiscovory();
        }
    }

    private void loadBondedDevices() {
        mDeviceAdapter.clear();

        Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
        if (devices != null) {
            Iterator<BluetoothDevice> iter = devices.iterator();
            while (iter.hasNext()) {
                BluetoothDevice device = iter.next();
                mDeviceAdapter.add(new DeviceItem(device));
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mDeviceAdapter.setChecked(position);
        mDeviceAdapter.notifyDataSetInvalidated();

        final DeviceItem item = mDeviceAdapter.getItem(position);
        mObdDevice = item.device;

        DialogFragment fragment = ConnectDialogFragment.newInstance(item.name);
        fragment.show(getChildFragmentManager(), ConnectDialogFragment.TAG);
    }

    public static class ConnectDialogFragment extends DialogFragment {

        public static final String TAG = "connect-dialog";

        private String mDeviceName;

        public static ConnectDialogFragment newInstance(String deviceName) {
            ConnectDialogFragment fragment = new ConnectDialogFragment();
            Bundle args = new Bundle();
            args.putString("device_name", deviceName);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mDeviceName = args.getString("device_name");
            } else {
                mDeviceName = savedInstanceState.getString("device_name");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("device_name", mDeviceName);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.dialog_message_connect_obd_device, mDeviceName))
                    .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((PickObdDeviceFragment) getParentFragment()).onConnectCanceled();
                        }
                    })
                    .setPositiveButton(R.string.btn_connect, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((PickObdDeviceFragment) getParentFragment()).onConnectConfirmed();
                        }
                    })
                    .create();
        }
    }

    private void onConnectConfirmed() {
        int bondState = mObdDevice.getBondState();
        if (bondState == BluetoothDevice.BOND_NONE  && !isRockchipAVNDevice()) {
            createBond();
        } else {
            mVehicle.setObdAddress(mObdDevice.getAddress());

            // Let's connect
            DialogFragment fragment = ConnectObdDeviceDialogFragment.newInstance(mVehicle);
            fragment.show(getChildFragmentManager(), ConnectObdDeviceDialogFragment.TAG);
        }
    }

    private void onConnectCanceled() {
        mDeviceAdapter.setChecked(-1);
        mDeviceAdapter.notifyDataSetInvalidated();
        mObdDevice = null;
    }

    private void startDiscovory() {
        cancelDiscovery();

        if (mDiscoveryReceiver == null) {
            mDiscoveryReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                        logger.debug("@ACTION_DISCOVERY_STARTED");
                        showDiscoveryDialog();

                        mDeviceAdapter.clear();
                        mDeviceAdapter.notifyDataSetChanged();
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        logger.debug("@ACTION_DISCOVERY_FINISHED");
                        cancelDiscovery();
                        dismissDiscoveryDialog();
                        mDeviceAdapter.notifyDataSetChanged();
                    } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        logger.debug("@ACTION_FOUND: " + device);

                        if (isClassicDevice(device)) {
                            mDeviceAdapter.add(new DeviceItem(device));
                            mDeviceAdapter.notifyDataSetChanged();
                        }
                    }
                }

                @SuppressLint("NewApi")
                private boolean isClassicDevice(BluetoothDevice device) {
                    boolean result = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        result = (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC);
                    }
                    return result;
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);

            getActivity().registerReceiver(mDiscoveryReceiver, filter);
        }

        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    private void cancelDiscovery() {
        if (mDiscoveryReceiver != null) {
            if (getActivity() != null) {
                getActivity().unregisterReceiver(mDiscoveryReceiver);
            }
            mDiscoveryReceiver = null;
            mDeviceAdapter.notifyDataSetChanged();

            if (mBtAdapter.isDiscovering()) {
                mBtAdapter.cancelDiscovery();
            }

            logger.debug("cancelDiscovery");
        }
    }

    private void showDiscoveryDialog() {
        FragmentManager fm = getChildFragmentManager();
        if (fm.findFragmentByTag(DiscoveryDialogFragment.TAG) == null) {
            Fragment discoveryDialog = new DiscoveryDialogFragment();
            fm.beginTransaction().add(discoveryDialog, DiscoveryDialogFragment.TAG)
                    .commitAllowingStateLoss();
            logger.debug("showDiscoveryDialog");
            mIsDiscovery = true;
        }
    }

    private void dismissDiscoveryDialog() {
        logger.debug("dismissDiscoveryDialog");
        FragmentManager fm = getChildFragmentManager();
        Fragment discoveryDialog = fm.findFragmentByTag(DiscoveryDialogFragment.TAG);
        if (discoveryDialog != null) {
            fm.beginTransaction().remove(discoveryDialog)
                    .commitAllowingStateLoss();

            logger.debug("dismissDiscoveryDialog#end");
            mIsDiscovery = false;
        }
    }

    public static class DiscoveryDialogFragment extends DialogFragment {

        public static final String TAG = "discovery-dialog";

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new WaitForDialog(getActivity());
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Logger.getLogger(TAG).debug("onDismiss@DiscoveryDialogFragment");
            ((PickObdDeviceFragment) getParentFragment()).cancelDiscovery();
            super.onDismiss(dialog);
        }

        @Override
        public void onDestroyView() {
            Logger.getLogger(TAG).debug("onDestroyView");
            if (getDialog() != null ) {
                getDialog().setOnDismissListener(null);
            }
            super.onDestroyView();
        }
    }

    @SuppressLint("NewApi")
    private void createBond() {
        logger.debug(mObdDevice + " is NOT bonded -> create bond");

        registerBondStateReceiver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mObdDevice.createBond();
        } else {
            try {
                Method m = mObdDevice.getClass().getMethod("createBond", (Class[]) null);
                m.invoke(mObdDevice, (Object[]) null);
            } catch (Exception e) {
                logger.error("Cannot find createBond() method!");
            }
        }
    }

    private void registerBondStateReceiver() {
        if (mBondStateReceiver == null) {
            mBondStateReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    logger.debug("ACTION_BOND_STATE_CHANGED: address=" + device + ", bondState=" + bondState);

                    if (mObdDevice.getAddress().equals(device.getAddress())) {
                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            logger.debug(device + " is bonded");

                            mVehicle.setObdAddress(mObdDevice.getAddress());

                            unregisterBondStateReceiver();

                            // Let's connect
                            DialogFragment fragment = ConnectObdDeviceDialogFragment.newInstance(mVehicle);
                            fragment.show(getChildFragmentManager(), ConnectObdDeviceDialogFragment.TAG);
                        } else if (bondState == BluetoothDevice.BOND_NONE) {
                            logger.warn(device + " is NOT bonded!");

                            mObdDevice = null;

                            unregisterBondStateReceiver();
                        }
                    }
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            getActivity().registerReceiver(mBondStateReceiver, filter);
        }
    }

    private void unregisterBondStateReceiver() {
        if (mBondStateReceiver != null) {
            try {
                getActivity().unregisterReceiver(mBondStateReceiver);
            } catch (NullPointerException e) {

            }
            mBondStateReceiver = null;
        }
    }

    @Override
    public void onObdDeviceConnected(Vehicle vehicle) {
        logger.debug("onObdDeviceConnected(): vehicle=" + vehicle);

        mVehicle = vehicle;

        // Store vehicle (\w obd address)
        SettingsStore settingsStore = SettingsStore.getInstance();
        settingsStore.storeVehicle(vehicle);

        mCallbacks.onObdDevicePicked(vehicle.getVehicleId(), vehicle.getObdAddress(),
                vehicle.getEngineDistance(), vehicle.getVin(), vehicle.getOdometer());
    }

    @Override
    public void onObdDeviceNotConnected(Vehicle vehicle) {
        logger.debug("onObdDeviceNotConnected()");
    }

    private final class DeviceAdapter extends ArrayAdapter<DeviceItem> {

        private final String mObdDeviceAddress;
        private final LayoutInflater mInflater;

        private DeviceAdapter(Context context, String obdDeviceAddress) {
            super(context, 0);
            mObdDeviceAddress = obdDeviceAddress;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.obd_device_list_item, null);
            }

            DeviceItem item = getItem(position);

            if (item.name == null) {
                BluetoothDevice device = mBtAdapter.getRemoteDevice(item.address);
                item.name = device.getName();
            }

            TextView nameText = (TextView) convertView.findViewById(R.id.name);
            String name = item.name
                    + (item.address.equals(mObdDeviceAddress) ? " (*)" : "");
            nameText.setText(name);

            TextView addressText = (TextView) convertView.findViewById(R.id.address);
            addressText.setText(item.address);

            RadioButton selectRadio = (RadioButton) convertView.findViewById(R.id.select);
            selectRadio.setChecked(item.isChecked);

            return convertView;
        }

        @Override
        public void add(DeviceItem newItem) {
            // Check same device
            int count = getCount();
            for (int i = 0; i < count; i++) {
                DeviceItem item = getItem(i);
                if (newItem.device.getAddress().equals(item.device.getAddress())) {
                    logger.debug(">>> same device=" + newItem.device);
                    return;
                }
            }

            super.add(newItem);
        }

        private void setChecked(int position) {
            int count = getCount();
            for (int p = 0; p < count; p++) {
                if (p == position) {
                    getItem(p).isChecked = true;
                } else {
                    getItem(p).isChecked = false;
                }
            }
        }

    }

    private class DeviceItem {
        BluetoothDevice device;
        String name;
        String address;
        boolean isChecked;

        DeviceItem(BluetoothDevice device) {
            this.device = device;
            this.name = device.getName();
            this.address = device.getAddress();
        }
    }

    public interface PickObdDeviceCallbacks {
        void onObdDevicePicked(String vehicleId, String obdAddress, int engineDistance, String vin, int odometer);
    }

    private boolean isRockchipAVNDevice() {
        // rockchip avn device
        return (Build.MANUFACTURER.equals("rockchip") /*&& Build.MODEL.equals("rk30sdk")*/);
    }

}

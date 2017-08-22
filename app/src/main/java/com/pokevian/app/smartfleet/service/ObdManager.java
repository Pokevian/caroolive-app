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

package com.pokevian.app.smartfleet.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;
import android.os.Parcelable;

import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.service.ObdPostProcessor.OnTripResetCallback;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.data.PersistData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.engine.ObdEngine;
import com.pokevian.lib.obd2.engine.PostProcessor;
import com.pokevian.lib.obd2.listener.OnObdDataListener;
import com.pokevian.lib.obd2.listener.OnObdExtraDataListener;
import com.pokevian.lib.obd2.listener.OnObdStateListener;
import com.pokevian.lib.obd2.conf.ObdConfig;

import org.apache.log4j.Logger;

public class ObdManager implements OnObdStateListener, OnObdDataListener, OnObdExtraDataListener,
        OnLocationChangedListener, ObdEngine.OnObdErrorListener {

    static final String TAG = "ObdManager";
    final Logger logger = Logger.getLogger(TAG);
    private static final String CONFIG_VESRION = "1.0";

    private final Context mContext;
    private ObdEngine mEngine;
    private BluetoothDevice mDevice;
    private final LocationWrapper mLocationWrapper;
    private final ObdPostProcessor mProcessor;
    private final ObdManagerCallbacks mCallbacks;
    private boolean mNotifyData;
    private ObdState mObdState = ObdState.UNKNOWN;
    private int mVes = VehicleEngineStatus.UNKNOWN;

    private boolean mIsISG;
    private boolean mIsISGEngineStart;
    private float mRpm = -100f;
    private boolean mRunning;


    public ObdManager(Context context, ObdManagerCallbacks callbacks) {
        mContext = context.getApplicationContext();
        mCallbacks = callbacks;

        mLocationWrapper = new LocationWrapper(context);
        mLocationWrapper.setAccuracyFilterEnabled(true, Consts.LOCATION_MIN_ACCURACY);
        LocationWrapper.INTERVAL = Consts.LOCATION_INTERVAL;
        LocationWrapper.FASTEST_INTERVAL = Consts.LOCATION_FASTEST_INTERVAL;

        mProcessor = new ObdPostProcessor(context);

        HandlerThread thread = new HandlerThread("obd-engine");
        thread.start();
        mEngine = new ObdEngine(mContext, thread.getLooper());
    }

    public void exit(boolean join) {
        Logger.getLogger(TAG).debug("exit#join:" + join);
        if (join) {
            mEngine.releaseAndJoin();
        } else {
            mEngine.release();
        }
    }

    public void run(Vehicle vehicle, boolean autoScan, boolean allowBluetoothControl) {
        synchronized (this) {
            if (vehicle == null) {
                logger.error("run(): vehicle is NULL");
                return;
            }

            BluetoothDevice device = getBluetoothDevice(vehicle.getObdAddress());
            if (device == null) {
                logger.warn("# invalid obd device: address=" + vehicle.getObdAddress());
                return;
            }

            if (!mRunning) {
                mEngine.registerOnStateListener(this);
                mEngine.registerOnDataListener(this);
                mEngine.registerOnExtraDataListener(this);
                mEngine.registerOnErrorListener(this);

                ObdConfig config = mEngine.getConfig();
                config.carId = vehicle.getVehicleId();
                config.fuelType = vehicle.getFuelType();
                config.engineDisplacement = vehicle.getModel().getDisplacement();
                config.protocol = vehicle.getObdProtocol();
                config.connectionMethod = vehicle.getObdConnectionMethod();
                config.authEnabled = false;
                config.encryptEnabled = false;
                config.portConnectionEnabled = true;
                config.bluetoothControlAllowed = allowBluetoothControl;
                config.maxConnectionFailureCount = 1;
                config.hybrid = vehicle.isHybrid();

                mEngine.setConfig(config);

                mLocationWrapper.registerOnLocationChangedListener(this);

                mEngine.connect(device, autoScan);
                mDevice = device;

                mNotifyData = true;

                mRunning = true;

                mIsISG = TwoState.Y.name().equals(vehicle.getIsgCode());
                mIsISGEngineStart = false;

//                Logger.getLogger(TAG).debug(mIsISG + "#" + vehicle.toString());
            }
        }
    }

    public void run(Vehicle vehicle, boolean autoScan) {
        run(vehicle, autoScan, true);
    }

    private BluetoothDevice getBluetoothDevice(String address) {
        BluetoothDevice device = null;
        try {
            device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                logger.warn("run(): " + address + " is not bonded!");
            }
            return device;
        } catch (Exception e) {
            logger.error("run(): invalid bluetooth address=" + address);
            return null;
        }
    }

    public void stop() {
        Logger.getLogger(TAG).info("stop");
        synchronized (this) {
            if (mRunning) {
                mNotifyData = false;

                mLocationWrapper.unregisterOnLocationChangedListener(this);
                mLocationWrapper.cancelUpdates();

                mEngine.unregisterOnStateListener(this);
                mEngine.unregisterOnDataListener(this);
                mEngine.unregisterOnExtraDataListener(this);
                mEngine.disconnect();

                mRunning = false;
            }
        }
    }

    public void clearStoredDTC() {
        mEngine.clearStoredDTC();
    }

    public void resetIfNeeded(boolean isNew) {
        Logger.getLogger(TAG).info("resetIfNeeded#" + isNew);
        mNotifyData = false;

        mEngine.unregisterOnDataListener(this);

        if (isNew) {
            PersistData.clearLastData(mContext);

            mProcessor.resetTrip(new OnTripResetCallback() {
                public void onTripReset() {
                    if (mRunning) {
                        mEngine.registerOnDataListener(ObdManager.this);
                        mNotifyData = true;
                    }
                }
            });
        } else {
            mEngine.registerOnDataListener(ObdManager.this);
            mNotifyData = true;
        }
    }

    public void startDataBackup() {
        mProcessor.startDataBackup();
    }

    public void stopDataBackup() {
        mProcessor.stopDataBackup();
    }

    public ObdState getObdState() {
        return mObdState;
    }

    public int getVehicleEngineStatus() {
        return mVes;
    }

    public void pause() {
        mNotifyData = false;
    }

    public void resume() {
        mNotifyData = true;
    }

    @Override
    public void onObdStateChanged(ObdEngine engine, ObdState state) {
        mObdState = state;

        if (mNotifyData) {
            if (state == ObdState.READY_TO_SCAN || state == ObdState.SCANNING) {
                mLocationWrapper.requestUpdates();
            } else {
                mLocationWrapper.cancelUpdates();
            }

            if (state != ObdState.SCANNING) {
                mVes = VehicleEngineStatus.UNKNOWN;
            }

            Parcelable extra = null;
            if (state == ObdState.CONNECTED || state == ObdState.READY_TO_SCAN) {
                extra = mEngine.getDeviceInfo();
            }
            mCallbacks.onObdStateChanged(state, extra);
        }
    }

    @Override
    public void onVehicleEngineStatusChanged(ObdEngine engine, int status) {
        int ves = status;
        if (mIsISG) {
            if (mIsISGEngineStart) {
                if (VehicleEngineStatus.OFF == status && (mRpm >= 0 && mRpm <= 300)) {
                    mRpm = -100f;
                    ves = VehicleEngineStatus.READY;
                } else if(VehicleEngineStatus.UNKNOWN == status) {
                    ves = VehicleEngineStatus.OFF;
                }
            } else {
                mIsISGEngineStart = VehicleEngineStatus.ON == status;
            }
        }
        Logger.getLogger(TAG).info(VehicleEngineStatus.toString(status) + ">" + VehicleEngineStatus.toString(ves)
                + "@onVehicleEngineStatusChanged#isg:" + mIsISG);

        mVes = ves;
        mCallbacks.onObdVehicleEngineStatusChanged(ves);
    }

    @Override
    public void onObdDeviceNotSupported(ObdEngine engine, String reason) {
        mCallbacks.onObdDeviceNotSupported(mDevice);
    }

    @Override
    public void onObdProtocolNotSupported(ObdEngine engine, String reason) {
        mCallbacks.onObdProtocolNotSupported(mDevice);
    }

    @Override
    public void onInsufficientPid(ObdEngine engine) {
        mCallbacks.onObdInsufficientPid();
    }

    @Override
    public void onEngineDistanceSupported(ObdEngine engine, boolean isSupported, String reason) {
        mCallbacks.onObdEngineDistanceSupported(isSupported);
    }

    @Override
    public void onConnectionFailure(ObdEngine engine, int failureCount) {
        mCallbacks.onObdConnectionFailed(mDevice, failureCount);
    }

    @Override
    public void onCannotConnect(ObdEngine engine, BluetoothDevice device, boolean isBlocked) {
        mCallbacks.onObdCannotConnect(mDevice, isBlocked);
    }

    @Override
    public void onObdData(ObdEngine engine, ObdData data) {
        if (mIsISG) {
            data.put(KEY.CALC_VES, mVes);
        }

        int ves = data.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN);

        if (!VehicleEngineStatus.isOnDriving(ves)) {
            data.put(KEY.CALC_HARSH_ACCEL, false);
            data.put(KEY.CALC_HARSH_BRAKE, false);
            data.put(KEY.CALC_HARSH_START, false);
            data.put(KEY.CALC_HARSH_STOP, false);
            data.put(KEY.CALC_HARSH_LEFT_TURN, false);
            data.put(KEY.CALC_HARSH_RIGHT_TURN, false);
            data.put(KEY.CALC_HARSH_U_TURN, false);
            data.put(KEY.CALC_HARSH_RPM, false);
            data.put(KEY.CALC_IDLING, false);
        }

        if (mNotifyData) {
            mCallbacks.onObdDataReceived(data.clone());
        }
    }

    @Override
    public void onObdExtraData(ObdEngine engine, float rpm, int vss) {
        mRpm = rpm;
        // for fast RPM/VSS notification
        if (mNotifyData) {
            mCallbacks.onObdExtraDataReceived(rpm, vss);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Pass to post-processor
        mProcessor.onLocationChanged(location);
    }

    @Override
    public void onError(ObdEngine obdEngine, int what, int extra) {
        Logger.getLogger(TAG).debug("onError#" + what);
        switch (what) {
            case ObdEngine.OBD_ERROR_BUSINIT:
                mCallbacks.onObdBusInitError(extra);
                break;
            case ObdEngine.OBD_ERROR_NO_DATA:
                mCallbacks.onObdNoData();
                break;
        }
    }

    public interface ObdManagerCallbacks {
        public void onObdStateChanged(ObdState state, Parcelable extra);

        public void onObdVehicleEngineStatusChanged(int ves);

        public void onObdDataReceived(ObdData data);

        public void onObdExtraDataReceived(float rpm, int vss);

        public void onObdConnectionFailed(BluetoothDevice obdDevice, int failureCount);

        public void onObdCannotConnect(BluetoothDevice obdDevice, boolean isBlocked);

        public void onObdDeviceNotSupported(BluetoothDevice obdDevice);

        public void onObdProtocolNotSupported(BluetoothDevice obdDevice);

        public void onObdInsufficientPid();

        public void onObdEngineDistanceSupported(boolean isSupported);

        public void onObdBusInitError(int protocol);

        public void onObdNoData();
    }

}

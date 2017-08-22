/*
 * Copyright (c) 2015. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.service.floatinghead;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.FuelType;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener;

import org.apache.log4j.Logger;

/**
 * Created by dg.kim on 2015-02-26.
 */
public class FloatingHeadService extends BaseFloatingHeadService {

    public static final String EXTRA_INTENT = "extra-intent";
    public static final String EXTRA_FUEL_TYPE = "extra-fuel-type";

    private ServiceConnection mVehicleServiceConnection;
    private VehicleService mVehicleService;
    private LocalBroadcastManager mBroadcastManager;
    private BroadcastReceiver mVehicleReceiver;
    private FloatingHeadWindow mHeadWindow;
    private Handler mHandler = new Handler();
    private FloatingHeadCallbacks mHeadCallbacks;

    private Point mClickPosition;
    private Point mActivePosition;
    //    private boolean mExpanded;
//    private boolean mWaitExpandedCallback;
    private int mLastVes = VehicleEngineStatus.UNKNOWN;

    private String mIntent;
    private FuelType mFuelType;



    @Override
    public void onCreate() {
        super.onCreate();
        Logger.getLogger(TAG).trace("create floating head service");

        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        mFuelType = SettingsStore.getInstance().getVehicle().getFuelType();

        bindVehicleService();
        registerVehicleReceiver();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        unregisterVehicleReceiver();
        unbindVehicleService();
        destroyWindows();

        Logger.getLogger(TAG).warn("destroy floating head service");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.getLogger(TAG).info("# onStartCommand() startId=" + startId);
        if (startId == 1) {
            mIntent = intent.getStringExtra(EXTRA_INTENT);
        }
        init();

        return START_NOT_STICKY;
    }

    private void init() {
        if (mHeadWindow == null) {
            Logger.getLogger(TAG).trace("initialize... ");

            mHeadCallbacks = new FloatingHeadCallbacks();
            mHeadWindow = new FloatingHeadWindow(this, mHeadCallbacks);
            mHeadWindow.show();

            // move to last position
            mHeadWindow.moveTo(mHeadWindow.getLastPosition());
        }
    }

    private void destroyWindows() {
        if (mHeadWindow != null) {
            mHeadWindow.destroy();
            mHeadWindow = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FloatingHeadServiceBinder();
    }

    @Override
    protected void onOrientationChanged(int orientation) {
        try {
            // calculate click position
            mHeadWindow.onOrientationChanged(orientation, mClickPosition, mActivePosition);
        } catch (Exception e) {

        }
    }

    public class FloatingHeadServiceBinder extends Binder {
        public FloatingHeadService getService() {
            return FloatingHeadService.this;
        }
    }

    private void bindVehicleService() {
        if (mVehicleServiceConnection == null) {
            mVehicleServiceConnection = new VehicleServiceConnection();
            Intent service = new Intent(this, VehicleService.class);
            bindService(service, mVehicleServiceConnection, 0);
        }
    }

    private void unbindVehicleService() {
        if (mVehicleServiceConnection != null) {
            unbindService(mVehicleServiceConnection);
            mVehicleServiceConnection = null;
        }
    }

    private class VehicleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mVehicleService = ((VehicleService.VehicleServiceBinder) binder).getService();

            OnObdStateListener.ObdState obdState = mVehicleService.getObdState();
            onObdStateChanged(obdState);

            int ves = mVehicleService.getVehicleEngineStatus();
            onVesChanged(ves);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mVehicleService = null;
        }
    }

    private void registerVehicleReceiver() {
        if (mVehicleReceiver == null) {
            mVehicleReceiver = new VehicleReceiver();

            IntentFilter filter = new IntentFilter();
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
            filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED);
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
            filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);

            mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
        }
    }

    private void unregisterVehicleReceiver() {
        if (mVehicleReceiver != null) {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
            mVehicleReceiver = null;
        }
    }

    private class VehicleReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED)) {
                OnObdStateListener.ObdState state = (OnObdStateListener.ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                onObdStateChanged(state);
            } else if (action.equals(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED)) {
                int ves = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS, VehicleEngineStatus.OFF);
                onVesChanged(ves);
            } else if (action.equals(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
            } else if (VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED.equals(action)) {
                float rpm = intent.getFloatExtra(VehicleDataBroadcaster.EXTRA_RPM, 0);
                int vss = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VSS, 0);
                onObdExtraDataReceived(rpm, vss);
            }/*else if (action.equals(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED)) {
                reconnect();
            } else if (action.equals(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED)) {
                reconnect();
            } else if (action.equals(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT)) {
                reconnect();
            }*/

        }
    }

    private void onObdStateChanged(OnObdStateListener.ObdState obdState) {
        Logger.getLogger(TAG).debug("ObdState=" + obdState);

        if (obdState == OnObdStateListener.ObdState.SCANNING) {
            if (mVehicleService != null) {
                int ves = mVehicleService.getVehicleEngineStatus();
                onVesChanged(ves);
            }
            if (mHeadWindow != null) {
                mHeadWindow.onObdConnected();
            }
        } else {
            mLastVes = VehicleEngineStatus.UNKNOWN;
            if (mHeadWindow != null) {
                mHeadWindow.onObdDisconnected();
            }
        }
    }

    private void onVesChanged(int ves) {
        Logger.getLogger(TAG).debug("onVesChanged=" + VehicleEngineStatus.toString(ves));
        if (mHeadWindow != null && !VehicleEngineStatus.isOnDriving(ves)) {
            mHeadWindow.onEngineOff();
        }
    }

    private void onObdDataReceived(ObdData obdData) {
        if (mHeadWindow != null) {
//            Logger.getLogger(TAG).info("onObdDataReceived#" + VehicleEngineStatus.toString(obdData.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN)));
            if (VehicleEngineStatus.isOnDriving(obdData.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN))) {
                mHeadWindow.onObdDataChanged(obdData, calcEcoLevel(obdData));
            }
        }
    }

    private void onObdExtraDataReceived(float rpm, int  vss) {
        if (mHeadWindow != null) {
            mHeadWindow.onObdExtraDataReceived(rpm, vss);
        }
    }

    private class FloatingHeadCallbacks implements FloatingHeadWindow.Callbacks {
        @Override
        public void onClick() {
        }

        @Override
        public void onDoubleClick() {
            startDrivingActivity();
        }

        @Override
        public void onLongClock() {
            startDrivingActivity();
        }

        @Override
        public void onPositionChanged() {
        }

        @Override
        public void onSettled() {
        }
    }

    private void startDrivingActivity() {
        Intent activity = new Intent(mIntent);
        activity.setPackage(getPackageName());
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(activity);
    }

    private int calcEcoLevel(ObdData data) {
        int ecoLevel = 0;

        if (mFuelType == FuelType.GASOLINE || mFuelType == FuelType.LPG) {
            if (data.isValid(KEY.SAE_VSS) && data.isValid(KEY.SAE_LOAD_PCT)) {
                int vss = data.getInteger(KEY.SAE_VSS);
                if (vss > 0) {
                    float loadPct = data.getFloat(KEY.SAE_LOAD_PCT);
                    if (loadPct < 50) {
                        ecoLevel = 1;
                    } else if (50 <= loadPct && loadPct < 80) {
                        ecoLevel = 2;
                    } else if (80 <= loadPct && loadPct < 90) {
                        ecoLevel = 3;
                    } else if (90 <= loadPct) {
                        ecoLevel = 4;
                    }
                }
            }
        } else if (mFuelType == FuelType.DIESEL) {
            if (data.isValid(KEY.SAE_VSS) && data.isValid(KEY.SAE_MAP)) {
                int vss = data.getInteger(KEY.SAE_VSS);
                if (vss > 0) {
                    int map = data.getInteger(KEY.SAE_MAP);
                    int baro = data.getInteger(KEY.SAE_BARO, 100);
                    int mapBaroDiff = (map - baro);

                    if (mapBaroDiff < 50) {
                        ecoLevel = 1;
                    } else if (50 <= mapBaroDiff && mapBaroDiff < 80) {
                        ecoLevel = 2;
                    } else if (80 <= mapBaroDiff && mapBaroDiff < 100) {
                        //by jake 09.16 (90->100)
                        ecoLevel = 3;
                    } else if (100 <= mapBaroDiff) {
                        ecoLevel = 4;
                    }
                }
            }
        }

        return ecoLevel;
    }

}

package com.pokevian.app.smartfleet.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.receiver.BringToFrontReceiver;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.main.MainActivity;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;

import org.apache.log4j.Logger;

public class ScreenMonitorService extends Service {

    private static final String TAG = "screen-monitor";

    private final int mForegroundId = 100004;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();

//    private SettingsStore mSettingsStore;
    private ServiceConnection mVehicleServiceConnection;
    private VehicleService mVehicleService;
    private Vehicle mVehicle;
    private LocalBroadcastManager mBroadcastManager;

    private static final int LAUNCH_DELAY = 500; // 1.5 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.getLogger(TAG).debug("onCreate@service");

        SettingsStore mSettingsStore = SettingsStore.getInstance();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);
        if (mSettingsStore.isValidAccount() && mSettingsStore.isValidVehicle()) {
            mVehicle = mSettingsStore.getVehicle();
            registerVehicleReceiver();
            startAndBindVehicleService();

            acquireWakeLock();

            startForeground(mForegroundId, buildForegroundNotification(getString(R.string.general_setting_engine_on_detection_enabled)));

        } else {
            stopSelf();
        }


    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Logger.getLogger(TAG).debug("handleMessage@service");
            if (BluetoothAdapter.getDefaultAdapter().isEnabled() && mVehicleService != null) {
                mVehicleService.connectVehicle(mVehicle);
            } else {
                handler.sendEmptyMessageDelayed(0, Consts.AUTO_CONNECT_WAKEUP_DELAY);
            }

        }
    };

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        stopForeground(true);
        releaseWakeLock();

        unregisterVehicleReceiver();
        unbindVehicleService();

        Logger.getLogger(TAG).debug("onDestroy@service#");

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAndBindVehicleService() {
        Logger.getLogger(TAG).debug("startAndBindVehicleService@service#");
        Intent service = new Intent(this, VehicleService.class);
        startService(service);

        if (mVehicleServiceConnection == null) {
            mVehicleServiceConnection = new VehicleServiceConnection();
            bindService(service, mVehicleServiceConnection, 0);
        }
    }

    private void unbindVehicleService() {
        Logger.getLogger(TAG).debug("unbindVehicleService@service");
        if (mVehicleServiceConnection != null) {
            unbindService(mVehicleServiceConnection);
            mVehicleServiceConnection = null;
        }
    }

    private void unregisterVehicleReceiver() {
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            Logger.getLogger(TAG).error(e.getMessage());
        }
    }

    private class VehicleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Logger.getLogger(TAG).debug("onServiceConnected@service");
            mVehicleService = ((VehicleService.VehicleServiceBinder) binder).getService();

            handler.sendEmptyMessageDelayed(0, Consts.AUTO_CONNECT_WAKEUP_DELAY);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private void registerVehicleReceiver() {
        unregisterVehicleReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
//        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
//        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
//        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CONNECTION_FAILED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_INSUFFICIENT_PID);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR);

        mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Logger.getLogger(TAG).debug("onReceive@service" + action);
            if (VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR.equals(action)) {
                int protocol = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_OBD_PROTOCOL, -1);
                Logger.getLogger(TAG).info("BUSINIT_ERROR@service2#" + protocol);
                onEngineOff();
            } else if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                int ves = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS, VehicleEngineStatus.UNKNOWN);
                onVehicleEngineStatusChanged(ves);
            } /*else if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                ObdState state = (ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                onObdStateChanged(state);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED.equals(action)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
            }*/ else if (VehicleDataBroadcaster.ACTION_OBD_CONNECTION_FAILED.equals(action)) {
                Logger.getLogger(TAG).info("CONNECTION_FAILED@service2");
                onObdCannotConnect(false);
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
                Logger.getLogger(TAG).info("CANNOT_CONNECT@service2#" + isBlocked);
                onObdCannotConnect(isBlocked);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
                onObdCannotConnect(false);
            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
                onObdCannotConnect(false);
            } else if (VehicleDataBroadcaster.ACTION_OBD_INSUFFICIENT_PID.equals(action)) {
                onObdCannotConnect(false);
            }
        }
    };

    private Notification buildForegroundNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_noti)
                .setContentTitle(getString(R.string.noti_driving_content_title))
                .setContentText(contentText);

        Intent i = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);

        PendingIntent pi = PendingIntent.getBroadcast(this, mForegroundId, i, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pi);

        return builder.build();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void acquireWakeLock() {

        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

            if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
                Logger.getLogger(TAG).debug("acquireWakeLock@service#" + pm.isPowerSaveMode());
//                if (pm.isPowerSaveMode()) {
//                    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//                    mWakeLock.acquire();
//                }
            }
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            mWakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    private int mVes = VehicleEngineStatus.UNKNOWN;

    private void onVehicleEngineStatusChanged(int ves) {
        Logger.getLogger(TAG).debug("onVehicleEngineStatusChanged#" + VehicleEngineStatus.toString(ves) + "@service");

        if (mVes != ves) {
            if (VehicleEngineStatus.isOnDriving(ves)) {
                onEngineOn();
            } else if (VehicleEngineStatus.isOffDriving(ves)) {
                onEngineOff();
            }
            mVes = ves;
        }
    }

    private void onObdCannotConnect(boolean isBlocked) {
        Logger.getLogger(TAG).debug("# onObdCannotConnect(): isBlocked=" + isBlocked);

        mVehicleService.disconnectVehicle();

        if (isBlocked) {
            Logger.getLogger(TAG).warn("# bluetooth blocked!");

            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

            // wait off
            btAdapter.disable();
            while (btAdapter.getState() != BluetoothAdapter.STATE_OFF) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Logger.getLogger(TAG).warn("interrupted!");
                }
            }
            Logger.getLogger(TAG).debug("# bluetooth off");

            // wait on
            btAdapter.enable();
            Logger.getLogger(TAG).warn("BT disabled -> enable");
        }

//        stopSelf();
        handler.sendEmptyMessageDelayed(0, Consts.AUTO_CONNECT_WAKEUP_DELAY);
    }

    private void onEngineOn() {
        Logger.getLogger(TAG).info("# Engine ON!");

        mHandler.postDelayed(mLaunchRunnable, LAUNCH_DELAY);
    }

    private void onEngineOff() {
        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();

            handler.sendEmptyMessageDelayed(0, Consts.AUTO_CONNECT_WAKEUP_DELAY);
        }
    }

    private Runnable mLaunchRunnable = new Runnable() {
        public void run() {
            // Launch main service
            launch();

            // DO NOT DISCONNECT

            stopSelf();
        }
    };

    private void launch() {
        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        i.putExtra(MainActivity.EXTRA_ON_DRIVING_DETECTED, true);
        startActivity(i);
    }

}

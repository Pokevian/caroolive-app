package com.pokevian.app.smartfleet.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;

import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.main.AutoStartManager;
import com.pokevian.app.smartfleet.ui.main.MainActivity;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

import org.apache.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class AutoConnectService extends Service {

    private static final String TAG = "AutoConnectService";
//    public static final String EXTRA_COUNT = "extra-count";
//    public static final int CONTINUATION = -100000;
//
//    private static final int DIAGNOSTICS_TIMEOUT = 3000; // 3 seconds
    private static final int LAUNCH_DELAY = 1500; // 1.5 seconds

    private SettingsStore mSettingsStore;
    private ServiceConnection mVehicleServiceConnection;
    private VehicleService mVehicleService;
    private LocalBroadcastManager mBroadcastManager;
    private PowerManager.WakeLock mWakeLock;

    private final Handler mHandler = new Handler();
    private boolean mIsConnected;

//    private static final String ACTION_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED";
//    private static int mTryCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.getLogger(TAG).trace("onCreate@service");

        acquireWakeLock();

        mSettingsStore = SettingsStore.getInstance();
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter.isEnabled()) {
            if (AutoStartManager.isPowerSaveMode()) {
                if (isScreenOn()) {
                    registerVehicleReceiver();
                    startAndBindVehicleService();
                } else {
                    stopSelf();
                }
            } else {
                registerVehicleReceiver();
                startAndBindVehicleService();
            }
        } else {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        Logger.getLogger(TAG).debug("onDestroy@service#" + sIsAlarmSet.get());

        mHandler.removeCallbacksAndMessages(null);

        unregisterVehicleReceiver();
        unbindVehicleService();

        if (!mIsConnected) {
            setAlarmIfNeeded(Consts.AUTO_CONNECT_WAKEUP_DELAY);
//            Intent service = new Intent(this, VehicleService.class);
//            stopService(service);
        }

        releaseWakeLock();

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void acquireWakeLock() {

        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
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

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION_CODES.KITKAT_WATCH <= Build.VERSION.SDK_INT) {
            return pm.isInteractive();
        }

        return pm.isScreenOn();
    }



//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Logger.getLogger(TAG).trace("# onStartCommand(): startId=" + startId);
//        return START_STICKY;
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAndBindVehicleService() {
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

//    private void tryConnectVehicle() {
//        if (mVehicleService != null && mBluetoothTurnedOn) {
//            SettingsStore settingsStore = SettingsStore.getInstance();
//
//            if (settingsStore.isValidVehicle()) {
//                Vehicle vehicle = settingsStore.getVehicle();
//                logger.debug("tryConnectVehicle(): vehicle=" + vehicle);
//
//                // Connect to the vehicle
//                mVehicleService.connectVehicle(vehicle);
//
//                // Store my bluetooth device
//                BluetoothDevice myDevice = BluetoothAdapter.getDefaultAdapter()
//                        .getRemoteDevice(vehicle.getObdAddress());
//                BtConnectionStore btConnStore = BtConnectionStore.getInstance(this);
//                btConnStore.storeMyDevice(myDevice);
//            }
//        }
//    }

    private class VehicleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mVehicleService = ((VehicleService.VehicleServiceBinder) binder).getService();

//            if (mVehicleService.getObdState() == ObdState.SCANNING) {
//                Logger.getLogger(TAG).warn(">>>>> auto connect#illegal state");
//            }

            if (mSettingsStore.isValidAccount() && mSettingsStore.isValidVehicle()) {
                Vehicle vehicle = mSettingsStore.getVehicle();
//                mVehicleService.disconnectVehicle();
                mVehicleService.connectVehicle(vehicle);
            } else {
                stopSelf();
            }
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

    private void unregisterVehicleReceiver() {
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            Logger.getLogger(TAG).error(e.getMessage());
        }
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Logger.getLogger(TAG).debug("onReceive#" + action);
            if (VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR.equals(action)) {
                int protocol = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_OBD_PROTOCOL, -1);
                Logger.getLogger(TAG).info("BUSINIT_ERROR@service#" + protocol);
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
                Logger.getLogger(TAG).info("CONNECTION_FAILED@service");
                onObdCannotConnect(false);
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
                Logger.getLogger(TAG).info("CANNOT_CONNECT@service#" + isBlocked);
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

//    private void onObdStateChanged(ObdState state) {
//    }

//    private boolean mIsMilOn;
//    private boolean mReceiveMil;

//    private void onObdDataReceived(ObdData data) {
//        if (data.isValid(KEY.SAE_MIL)) {
//            if (mDiagnosticsDialog != null && !mReceiveMil) {
//                mReceiveMil = true;
//
//                mIsMilOn = data.getBoolean(KEY.SAE_MIL, false);
//                Logger.getLogger(TAG).info("receive MIL: " + mIsMilOn);
//                mHandler.postDelayed(mDiagnosticsRunnable, DIAGNOSTICS_TIMEOUT);
//            }
//        }
//    }

    private void onObdCannotConnect(boolean isBlocked) {
        Logger.getLogger(TAG).debug("# onObdCannotConnect(): isBlocked=" + isBlocked);
        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
        }

        if (isBlocked) {
            Logger.getLogger(TAG).warn("# bluetooth blocked!@service");
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
            Logger.getLogger(TAG).warn("BT disabled -> enable@service");
        }

        stopSelf();
    }

    private void onEngineOn() {
        Logger.getLogger(TAG).info("# Engine ON!");

//        if (mSettingsStore.isWaitUnitlEngineOff()) {
//            Logger.getLogger(TAG).warn("# wait until engine off...");
//            mVehicleService.disconnectVehicle();
//            mIsConnected = false;
//            stopSelf();
//            return;
//        }

        // disable sound notifier: should be enabled later
//        mVehicleService.setSoundNotifierEnabled(false);
//        showDiagnosticsDialog();



        mHandler.postDelayed(mLaunchRunnable, LAUNCH_DELAY);
//        mHandler.post(mLaunchRunnable);
    }

    private void onEngineOff() {
        Logger.getLogger(TAG).debug("onEngineOff@service");

        mHandler.removeCallbacks(mLaunchRunnable);

        mVehicleService.disconnectVehicle();
        mIsConnected = false;
        stopSelf();
    }

    private Runnable mLaunchRunnable = new Runnable() {
        public void run() {
            // Launch main service
            launch(false);

            // DO NOT DISCONNECT

            mIsConnected = true;
            stopSelf();
        }
    };

    private void launch(boolean runDiagnostics) {
        // cancel alarm
        cancelAlarm();

        /*mVehicleService.setSoundNotifierEnabled(true);

        // start main service
        Intent service = new Intent(this, FloatingHeadService.class);
        service.putExtra(FloatingHeadService.EXTRA_MIL, mIsMilOn);
        service.putExtra(FloatingHeadService.EXTRA_RUN_DIAGNOSTICS, runDiagnostics);
        startService(service);

        // play sound: start app
        service = new Intent(this, SoundEffectService.class);
        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.SOUND_PLAY);
        service.putExtra(SoundEffectService.EXTRA_SOUND_ID, SoundEffectService.SID_LAUNCH_APP);
        startService(service);*/

        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());
        i.putExtra(MainActivity.EXTRA_ON_DRIVING_DETECTED, true);
        startActivity(i);
    }

    /*private Runnable mNoticeMilRunnable = new Runnable() {
        @Override
        public void run() {
            dismissDiagnosticsDialog();
            showNoticeMilDialog();
        }
    };*/

//    private NoticeMilDialog mNoticeMilDialog;

    /*private void showNoticeMilDialog() {
        if (mNoticeMilDialog == null) {
            mNoticeMilDialog = new NoticeMilDialog(this);
            mNoticeMilDialog.setClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launch(true);

                    // DO NOT DISCONNECT

                    mIsConnected = true;
                    stopSelf();
                }
            });
            mNoticeMilDialog.setCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    launch(false);

                    // DO NOT DISCONNECT

                    mIsConnected = true;
                    stopSelf();
                }
            });
            mNoticeMilDialog.setDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mNoticeMilDialog = null;
                }
            });
            mNoticeMilDialog.show();
        }
    }

    private void dismissNoticeMilDialog() {
        if (mNoticeMilDialog != null) {
            mNoticeMilDialog.dismiss();
            mNoticeMilDialog = null;
        }
    }*/

    private void setAlarmIfNeeded(int delay) {
        if (sIsAlarmSet.get()) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            PendingIntent operation = getAlarmIntent(this);
            long triggerAt = SystemClock.elapsedRealtime() + delay;
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation);
        }
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        PendingIntent operation = getAlarmIntent(this);
        am.cancel(operation);
        operation.cancel();
    }

    // default value should be true
    private static final AtomicBoolean sIsAlarmSet = new AtomicBoolean(true);
    private static final Object sLock = new Object();

    public static void setAlarm(Context context, int delay) {
        Logger.getLogger(TAG).debug("setAlarm#" + isAlarmSet(context));
        if (isAlarmSet(context)) {
            return;
        }

        SettingsStore settings = SettingsStore.getInstance();
        if (settings.isValidAccount() && settings.isValidVehicle() && !settings.isBlackboxEnabled() && settings.isEngineOnDetectionEnabled()) {
            Logger.getLogger(TAG).debug("# set auto connect alarm: delay=" + delay + "ms");

            synchronized (sLock) {
                sIsAlarmSet.set(true);

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent operation = getAlarmIntent(context);
                long triggerAt = SystemClock.elapsedRealtime() + delay;
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation);
//                am.setAlarmClock(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, operation);
            }
        } else if (!settings.isBlackboxEnabled()) {
            Logger.getLogger(TAG).debug("# auto connect is NOT enabled!");
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static void setAlarm(AlarmManager am, int type, long triggerAtMillis, PendingIntent operation) {
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, operation);
//        am.setAlarmClock(AlarmManager.AlarmClockInfo );
//        am.setAlarmClock(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, operation);
    }

    public static void cancelAlarm(Context context) {
        Logger.getLogger(TAG).debug("# cancel auto connect alarm#" + isAlarmSet(context));
        if (!isAlarmSet(context)) {
            return;
        }

        synchronized (sLock) {
            sIsAlarmSet.set(false);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent operation = getAlarmIntent(context);
            am.cancel(operation);
            operation.cancel();

            Logger.getLogger(TAG).debug("# cancel auto connect alarm > " + isAlarmSet(context));

            // stop!
            Intent service = new Intent(context, AutoConnectService.class);
            context.stopService(service);
        }
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent receiver = new Intent(context, AutoConnectService.class);
        return PendingIntent.getService(context, R.id.req_wake_up_auto_connect_service,
                receiver, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static boolean isAlarmSet(Context context) {
        Intent receiver = new Intent(context, AutoConnectService.class);
        PendingIntent service = PendingIntent.getService(context, R.id.req_wake_up_auto_connect_service,
                receiver, PendingIntent.FLAG_NO_CREATE);

        return service != null;
    }

}

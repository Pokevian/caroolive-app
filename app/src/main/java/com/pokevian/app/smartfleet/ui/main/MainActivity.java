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

package com.pokevian.app.smartfleet.ui.main;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.receiver.BringToFrontReceiver;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.VehicleService.VehicleServiceBinder;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.BluetoothControlFragment;
import com.pokevian.app.smartfleet.ui.common.BluetoothControlFragment.BluetoothControlCallbacks;
import com.pokevian.app.smartfleet.ui.common.SignOutActivity;
import com.pokevian.app.smartfleet.ui.driving.DrivingActivity;
import com.pokevian.app.smartfleet.ui.intro.IntroActivity;
import com.pokevian.app.smartfleet.ui.main.NavigationDrawerFragment.NavigationDrawerCallbacks;
import com.pokevian.app.smartfleet.ui.main.ObdRecoveryDialog.ObdRecoveryDialogCallbacks;
import com.pokevian.app.smartfleet.ui.report.LogReportFragment;
import com.pokevian.app.smartfleet.ui.settings.BlackboxSettingActivity;
import com.pokevian.app.smartfleet.ui.settings.DisclaimerActivity;
import com.pokevian.app.smartfleet.ui.settings.GeneralSettingActivity;
import com.pokevian.app.smartfleet.ui.settings.VehicleSettingActivity;
import com.pokevian.app.smartfleet.ui.settings.VehiclesSettingActivity;
import com.pokevian.app.smartfleet.ui.setup.AccountInfoActivity;
import com.pokevian.app.smartfleet.ui.tripmonitor.TripMonitorActivity;
import com.pokevian.app.smartfleet.ui.tripmonitor.TripMonitorFragment;
import com.pokevian.app.smartfleet.ui.version.VersionInfoDialogFragment;
import com.pokevian.app.smartfleet.ui.video.VideoListActivity;
import com.pokevian.app.smartfleet.ui.web.EventActivity;
import com.pokevian.app.smartfleet.ui.web.WebActivity;
import com.pokevian.app.smartfleet.util.PackageUtils;
import com.pokevian.app.smartfleet.util.ShareUtils;
import com.pokevian.app.smartfleet.util.WebUtils;
import com.pokevian.lib.obd2.defs.DeviceInfo;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MainActivity extends BaseActivity implements
        NavigationDrawerCallbacks, BluetoothControlCallbacks, AlertDialogCallbacks,
        ObdRecoveryDialogCallbacks, DrivingRecordFragment.DrivingRecordUpdateCallback {

    static final String TAG = "MainActivityIntent";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_FROM_SETUP = "extra.FROM_SETUP";
    public static final String EXTRA_ON_DRIVING_DETECTED = "extra.on-driving-detected";

    private static final int REQUEST_INTRO = 0;
    private static final int REQUEST_TRIP_MONITOR = 1;
    private static final int REQUEST_SETTINGS = 2;
    private static final int REQUEST_VEHICLE_SETTING = 3;
    private static final int REQUEST_BLACKBOX_SETTING = 4;
    private static final int REQUEST_DRIVING = 5;
    private static final int REQUEST_SIGN_OUT = 6;
    private static final int REQUEST_VIDEO_LIST = 7;
    private static final int REQUEST_NOTICE = 8;
    private static final int REQUEST_EVENT = 9;
    private static final int REQUEST_TERMS = 10;
    private static final int REQUEST_USER_INFO = 11;
    private static final int REQUEST_MY_POINT = 12;
    private static final int REQUEST_CAR_STATUS_CHECK = 13;
    private static final int REQUEST_MOV = 14;
    private static final int REQUEST_DRIVING_PATTERN = 15;
    private static final int REQUEST_RANKING = 16;
    private static final int REQUEST_HOF = 17;

    private static final int MENU_ITEM_NOTICE = 0;
    private static final int MENU_ITEM_EVENT = 1;
    private static final int MENU_ITEM_MOV;
    private static final int MENU_ITEM_VEHICLE_SETTING;
    private static final int MENU_ITEM_BLACKBOX_SETTING;
    private static final int MENU_ITEM_GENERAL_SETTING;
    private static final int MENU_ITEM_STIPULATION;
    private static final int MENU_ITME_LOG_SEND;
    private static final int MENU_ITEM_VSERION_INFO = -1;
    private static final int MENU_ITEM_OBD_SUPPLIER = -1;
    private static final int MENU_ITEM_MY_POINT = -1;

    static {
        if (BuildConfig.INCLUDE_BLACKBOX) {
            MENU_ITEM_MOV = 2;
            MENU_ITEM_VEHICLE_SETTING = 3;
            MENU_ITEM_BLACKBOX_SETTING = 4;
            MENU_ITEM_GENERAL_SETTING = 5;
            MENU_ITEM_STIPULATION = 6;
            MENU_ITME_LOG_SEND = 7;
        } else {
            MENU_ITEM_MOV = -1;
            MENU_ITEM_VEHICLE_SETTING = 2;
            MENU_ITEM_BLACKBOX_SETTING = -1;
            MENU_ITEM_GENERAL_SETTING = 3;
            MENU_ITEM_STIPULATION = 4;
            MENU_ITME_LOG_SEND = 5;
        }
    }


    private View mContentView;
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private VehicleService mVehicleService;

    private LocalBroadcastManager mBroadcastManager;

    private boolean mBluetoothTurnedOn = false;
    private boolean mIsVehicleConnectionStopped = false;
    private int mLastVes = VehicleEngineStatus.UNKNOWN;
    private int mObdConnectionFailureCount = 0;

    private ObdRecoveryDialog mObdRecoveryDialog;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logger.trace("onCreate(): savedInstanceState=" + savedInstanceState);
//        AutoConnectService.cancelAlarm(getApplicationContext());
//        stopService(new Intent(this, ScreenMonitorService.class));
        AutoStartManager.stopAutoStartService(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mContentView = findViewById(android.R.id.content);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

// ian        startActivityForResult(new Intent(this, IntroActivity.class), REQUEST_INTRO);

        //
        //-------------------------------------------------------------------
        //

        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Register Bluetooth Connection Receiver
        registerBluetoothConnectionReceiver();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(MainFragment.TAG);
        if (fragment == null) {
            fragment = new MainFragment();
            fm.beginTransaction().add(fragment, MainFragment.TAG).commit();
        }

        // Start and bind Vehicle service
        startAndBindVehicleService();

        if (savedInstanceState == null) {
            // Ensure bluetooth is turned on
            fragment = BluetoothControlFragment.newInstance();
            fm.beginTransaction().add(fragment, BluetoothControlFragment.TAG)
                    .commit();

            // Show 'No vehicle registered' dialog if needed
            boolean fromSetup = getIntent().getBooleanExtra(EXTRA_FROM_SETUP, false);
            if (!fromSetup) {
                showNoVehicleRegisteredDialogIfNeeded();
            }

            String appLink = getIntent().getStringExtra("appLink");
            String msgTag = getIntent().getStringExtra("msgTag");
            String mode = getIntent().getStringExtra("mode");

            if (!TextUtils.isEmpty(appLink)) {
                if ("1".equals(appLink) /*|| "notice".equals(appLink)*/) {
                    startNoticeActivity();
                } else if ("2".equals(appLink) /*|| "event".equals(appLink)*/) {
                    startEventActivity();
                } else if ("10".equals(appLink) /*|| "trip".equals(appLink)*/) {
                    startTripListActivity();
                }
            }

        }

        // Register vehicle receiver, so start monitoring vehicle engine statues
        registerVehicleReceiver();
    }

    @Override
    protected void onDestroy() {
        logger.trace("onDestroy()");
//        mContentView.removeCallbacks(mReconnectRunnable);
//        disconnectVehicle();

        unregisterBluetoothConnectionReceiver();

        unregisterVehicleReceiver();

        unbindAndStopVehicleService();

        turnOffBluetoothIfNeeded();

        SettingsStore.commit();

        // Crash detect service
//        stopService(new Intent(getApplicationContext(), CrashDetectService.class));

//        AutoConnectService.setAlarm(getApplicationContext(), Consts.AUTO_CONNECT_WAKEUP_DELAY);
//        startService(new Intent(this, ScreenMonitorService.class));
        AutoStartManager.startAutoStartService(this);
        super.onDestroy();
    }

    private int mBackPressedCount;
    private Toast mBackPressToast;

    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
            return;
        }

        if (++mBackPressedCount < 2) {
            mBackPressToast = Toast.makeText(this, R.string.main_press_back_more, Toast.LENGTH_SHORT);
            mBackPressToast.show();
            findViewById(android.R.id.content).postDelayed(new Runnable() {
                public void run() {
                    mBackPressToast.cancel();
                    mBackPressedCount--;
                }
            }, 2000);
            return;
        }
        mBackPressToast.cancel();

        mContentView.removeCallbacks(mReconnectRunnable);
        disconnectVehicle();

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        logger.trace("onActivityResult(): requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (REQUEST_INTRO == requestCode) {
//            if (RESULT_OK == resultCode) {
//
//            } else {
//                finish();
//            }
        }
        if (REQUEST_USER_INFO == requestCode) {
            if (data != null && data.getBooleanExtra(AccountInfoActivity.EXTRA_GOTO_INTRO, false)) {
                startActivity(new Intent(this, IntroActivity.class));
                finish();
            } else {
                mNavigationDrawerFragment.updateAccountInfo();
                reconnectVehicleService();
            }
        } else if (requestCode == REQUEST_SETTINGS) {
            reconnectVehicleService();

//            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
//            if (fragment != null) {
//                ((MainMenuFragment) fragment).updateLaunchButton();
//            }

        } else if (REQUEST_VEHICLE_SETTING == requestCode) {
            reconnectVehicleService();

            mNavigationDrawerFragment.notifyDataSetChanged();
        } else if (REQUEST_BLACKBOX_SETTING == requestCode || REQUEST_TERMS == requestCode) {
            reconnectVehicleService();

//            Fragment fragment = getSupportFragmentManager().findFragmentByTag("main-fragment");
//            if (fragment != null) {
//                ((MainMenuFragment)fragment).updateProfile();
//            }

        } else if (requestCode == REQUEST_DRIVING) {
            logger.debug("onActivityResult(): REQUEST_DRIVING: data=" + data);

            if (resultCode == RESULT_OK) {
//                if (Consts.ALPHA) {
//                    sayGoodBye();
//                }

                if (data != null && data.getBooleanExtra(DrivingActivity.EXTRA_REQUEST_EXIT, false)) {
                    // Need to exit
                    finish();
                } else {
                    // Update vehicle engine status
                    mLastVes = data.getIntExtra(DrivingActivity.EXTRA_VES, VehicleEngineStatus.UNKNOWN);
                    invalidateOptionsMenu();

                    // Register vehicle receiver again
                    registerVehicleReceiver();
                }
            }
        } else if (requestCode == REQUEST_SIGN_OUT) {
            if (resultCode == RESULT_OK) {
                logger.debug("signed out!");

                Intent intent = new Intent(this, IntroActivity.class);
                startActivity(intent);

                finish();
            } else {
                logger.warn("failed to sign out!");

                Toast.makeText(this, R.string.main_failed_to_sign_out, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_VIDEO_LIST) {
            // Reset failure count
            mObdConnectionFailureCount = 0;

            // Register vehicle receiver again
            registerVehicleReceiver();
            // Try connect vehicle
            tryConnectVehicle();

            // Update action bar title
            mNavigationDrawerFragment.selectPreviousSegment();
        } else {
            // jj.ahn delegate result to child fragment
//            FragmentManager fm = getSupportFragmentManager();
//            Fragment fragment = fm.findFragmentById(R.id.container);
//            fragment.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_OK) {
                if (data != null && data.getBooleanExtra(DrivingActivity.EXTRA_REQUEST_EXIT, false)) {
                    // Need to exit
                    finish();
                }
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (NavigationDrawerFragment.DRAWER_MENU_USER_INFO == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, AccountInfoActivity.class);
            startActivityForResult(intent, REQUEST_USER_INFO);
        } else if (NavigationDrawerFragment.DRAWER_MENU_LOG_OUT == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, SignOutActivity.class);
            startActivityForResult(intent, REQUEST_SIGN_OUT);
        } else if (MENU_ITEM_NOTICE == position) {
            startNoticeActivity();
        } else if (MENU_ITEM_EVENT == position) {
            startEventActivity();
        } else if (MENU_ITEM_MOV == position) {
            startVideoListActivity();
        } else if (MENU_ITEM_VEHICLE_SETTING == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, VehiclesSettingActivity.class);
            startActivityForResult(intent, REQUEST_VEHICLE_SETTING);
        } else if (MENU_ITEM_BLACKBOX_SETTING == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, BlackboxSettingActivity.class);
            startActivityForResult(intent, REQUEST_BLACKBOX_SETTING);
        } else if (MENU_ITEM_GENERAL_SETTING == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, GeneralSettingActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS);
        } else if (MENU_ITEM_STIPULATION == position) {
            // Unregister vehicle receiver
            unregisterVehicleReceiver();
            // Disconnect vehicle
            disconnectVehicle();
            // Update options menu
            mLastVes = VehicleEngineStatus.UNKNOWN;
            invalidateOptionsMenu();

            Intent intent = new Intent(this, DisclaimerActivity.class);
            startActivityForResult(intent, REQUEST_TERMS);
        } else if (MENU_ITME_LOG_SEND == position) {
            FragmentManager fm = getSupportFragmentManager();
            String subject = getString(R.string.log_report, PackageUtils.loadLabel(this, getApplicationInfo()));
            Fragment fragment = LogReportFragment.newInstance(subject);
            fm.beginTransaction().add(fragment, LogReportFragment.TAG)
                    .commitAllowingStateLoss();
        } else if (MENU_ITEM_MY_POINT == position) {
            startMyPointActivity();
        } else if (MENU_ITEM_VSERION_INFO == position) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = VersionInfoDialogFragment.newInstance();
            fragment.show(fm, VersionInfoDialogFragment.TAG);
        } else if (MENU_ITEM_EVENT == position) {
            startEventActivity();
        } else if (MENU_ITEM_OBD_SUPPLIER == position) {
            WebUtils.launchWebLink(this, Consts.OBD_SUPPLIER_URL);
        } else if (NavigationDrawerFragment.DRAWER_MENU_GOOLE_PLUS_SELECTED == position) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/110517586842207212965"));
//                intent.setPackage("com.google.android.apps.plus");
//                startActivity(intent);
//            } catch  (Exception e){
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/110517586842207212965")));
//            }
            WebUtils.launchWebLink(this, Consts.SNS_GOOGLE_PLUS);

        } else if(NavigationDrawerFragment.DRAWER_MENU_FACEBOOK_SELECTED == position) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://groups/caroolive"));
//                startActivity(intent);
//            } catch(Exception e) {
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/caroolive")));
//            }
            WebUtils.launchWebLink(this, Consts.SNS_FACE_BOOK);
        } else if(NavigationDrawerFragment.DRAWER_MENU_KAKAO_TALK_SELECTED == position) {
            WebUtils.launchWebLink(this, Consts.SNS_KAKAO_TALK);
        }

        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
        }
    }

    private void init() {

        // Start and bind Vehicle service
        startAndBindVehicleService();

        String appLink = getIntent().getStringExtra("appLink");
        String msgTag = getIntent().getStringExtra("msgTag");
        String mode = getIntent().getStringExtra("mode");

        if (!TextUtils.isEmpty(appLink)) {
            if ("1".equals(appLink) /*|| "notice".equals(appLink)*/) {
                startNoticeActivity();
            } else if ("2".equals(appLink) /*|| "event".equals(appLink)*/) {
                startEventActivity();
            } else if ("10".equals(appLink) /*|| "trip".equals(appLink)*/) {
                startTripListActivity();
            }
        }

        registerVehicleReceiver();
    }

    private void disconnectVehicleService() {
        // Unregister vehicle receiver
        unregisterVehicleReceiver();
        // Disconnect vehicle
        disconnectVehicle();
    }

    private void reconnectVehicleService() {
        // Reset failure count
        mObdConnectionFailureCount = 0;

        // Register vehicle receiver again
        registerVehicleReceiver();
        // Try connect vehicle
        tryConnectVehicle();

        // Check vehicle engine status
        if (mVehicleService != null) {
            ObdState obdState = mVehicleService.getObdState();
            if (obdState == ObdState.SCANNING) {
                int ves = mVehicleService.getVehicleEngineStatus();
                onVehicleEngineStatusChanged(ves);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onVehicleEngineStatusChanged(mVehicleService.getVehicleEngineStatus());
                    }
                }, 100);
            }
        }
    }


    private void startAndBindVehicleService() {
        startService(new Intent(this, VehicleService.class));
        bindService(new Intent(this, VehicleService.class), mVehicleServiceConnection, 0);
    }

    private void unbindAndStopVehicleService() {
        try {
            unbindService(mVehicleServiceConnection);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            stopService(new Intent(this, VehicleService.class));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        mVehicleService = null;
    }

    private ServiceConnection mVehicleServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            logger.debug("Vehicle service connected");

            mVehicleService = ((VehicleServiceBinder) binder).getService();

            // Update vehicle engine status
            mLastVes = mVehicleService.getVehicleEngineStatus();

            onVehicleEngineStatusChanged(mVehicleService.getVehicleEngineStatus());

            // Try connect
            tryConnectVehicle();
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void registerVehicleReceiver() {
        unregisterVehicleReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
        mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
    }

    private void unregisterVehicleReceiver() {
        Logger.getLogger(TAG).trace("unregisterVehicleReceiver");
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (isFinishing()) return;

            final String action = intent.getAction();
            if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                ObdState obdState = (ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                Parcelable extra = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE_EXTRA);
                onObdStateChanged(obdState, extra);

            } else if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                onVehicleEngineStatusChanged(intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS,
                        VehicleEngineStatus.UNKNOWN));
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
                onObdCannotConnect(obdDevice, isBlocked);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdDeviceNotSupported(obdDevice);
            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdProtocolNotSupported(obdDevice);
            }
        }
    };


    private void onObdStateChanged(ObdState state, Parcelable extra) {
        if (ObdState.READY_TO_SCAN == state && extra != null) {
            DeviceInfo deviceInfo = (DeviceInfo) extra;
            Vehicle v = SettingsStore.getInstance().getVehicle();
            v.setObdProtocol(deviceInfo.protocol.getCode());
            v.setObdConnectionMethod(deviceInfo.connectionMethod);
            SettingsStore.getInstance().storeVehicle(v);
        }

        if (ObdState.SCANNING == state) {
            // Reset failure count
            mObdConnectionFailureCount = 0;
        }
    }


    private void onVehicleEngineStatusChanged(int ves) {
        logger.debug("onVehicleEngineStatusChanged(): ves=" + VehicleEngineStatus.toString(ves));
        if (VehicleEngineStatus.isOnDriving(ves)) {
            if (mNavigationDrawerFragment.isDrawerOpen()) {
                mNavigationDrawerFragment.closeDrawer();
            }

            Logger.getLogger(TAG).debug("onVehicleEngineStatusChanged#" + "bg:" + isBackground() + " ks:" + isKeyScreenOn() + "/" + isPaused());

            if (isPaused()) {
                if (isBackground() || isKeyScreenOn()) {
                    startDrivingActivity();
                } else if (!isScreenOn()) {
                    startDrivingActivity();
                }
            } else {
                FragmentManager fm = getSupportFragmentManager();
                DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(DrivingOnDialogFragment.TAG);
                if (fragment == null) {
                    fragment = DrivingOnDialogFragment.newInstance();
                    fm.beginTransaction().add(fragment, DrivingOnDialogFragment.TAG)
                            .commitAllowingStateLoss();
                }
            }
        } else if (ves == VehicleEngineStatus.OFF) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = (DialogFragment) fm.findFragmentByTag(DrivingOnDialogFragment.TAG);
            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
            }
        }

        mLastVes = ves;
        mIsVehicleConnectionStopped = false;

        invalidateOptionsMenu();
    }

    private void onObdCannotConnect(BluetoothDevice obdDevice, boolean isBlocked) {
        logger.debug("onObdCannotConnect(): isBlocked=" + isBlocked);

        mObdConnectionFailureCount++;
        logger.warn("onObdConnectionFailed(): " + mObdConnectionFailureCount + "/"
                + Consts.MAX_OBD_CONNECTION_FAILURE_COUNT);

        disconnectVehicle();

        if (mObdConnectionFailureCount < Consts.MAX_OBD_CONNECTION_FAILURE_COUNT) {
            if (mObdConnectionFailureCount < 2) {
                // Retry right now!
                mContentView.post(mReconnectRunnable);
            } else {
                // Retry after 5 seconds
                mContentView.postDelayed(mReconnectRunnable, 5000);
            }
        } else {
            logger.warn("onObdCannotConnect(): failed to connect obd more than "
                    + Consts.MAX_OBD_CONNECTION_FAILURE_COUNT + " times");

            // Show 'resume connect' menu
            mIsVehicleConnectionStopped = true;
            invalidateOptionsMenu();
        }

        if (isBlocked) {
            if (mObdRecoveryDialog == null) {
                mObdRecoveryDialog = new ObdRecoveryDialog(this);
                mObdRecoveryDialog.show();
            }
        }
    }

    private void onObdDeviceNotSupported(BluetoothDevice obdDevice) {
        logger.debug("onObdDeviceNotSupported()");
        onObdCannotConnect(obdDevice, false);
    }

    private void onObdProtocolNotSupported(BluetoothDevice obdDevice) {
        logger.debug("onObdProtocolNotSupported()");
        onObdCannotConnect(obdDevice, false);
    }

    private final Runnable mReconnectRunnable = new Runnable() {
        public void run() {
            if (!isFinishing()) {
                tryConnectVehicle();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);

            SettingsStore setting = SettingsStore.getInstance();
            if (setting.isValidVehicle()) {
                menu.findItem(R.id.action_driving_mode).setVisible(true);
            } else {
                menu.findItem(R.id.action_driving_mode).setVisible(false);
            }

            if (mLastVes == VehicleEngineStatus.ON) {
                menu.findItem(R.id.action_driving_mode).setVisible(true);
            } /*else {
                menu.findItem(R.id.action_driving_mode).setVisible(false);
            }*/
            if (mIsVehicleConnectionStopped) {
                menu.findItem(R.id.action_obd_reconnect).setVisible(true);
            } else {
                menu.findItem(R.id.action_obd_reconnect).setVisible(false);
            }

//            MenuItem menuItem = menu.findItem(R.id.action_share);
//            ShareActionProvider actionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
//            actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
//            actionProvider.setShareIntent(createShareIntent());

            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.app_name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        logger.trace("onOptionsItemSelected(): itemId=" + itemId);

       if (itemId == R.id.action_log_report) {
            FragmentManager fm = getSupportFragmentManager();
            String subject = getString(R.string.log_report, PackageUtils.loadLabel(this, getApplicationInfo()));
            Fragment fragment = LogReportFragment.newInstance(subject);
            fm.beginTransaction().add(fragment, LogReportFragment.TAG)
                    .commit();
            return true;
        } else if (itemId == R.id.action_driving_mode) {
            startDrivingActivity();
            return true;
        } else if (itemId == R.id.action_obd_reconnect) {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!btAdapter.isEnabled()) {
                btAdapter.enable();
            }

            tryConnectVehicle();

            // Hide 'obd reconnect' menu
            mIsVehicleConnectionStopped = false;
            invalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.action_share) {
           final View view = findViewById(R.id.container);
           view.post(new Runnable() {
               @Override
               public void run() {
                   try {
                       ShareUtils.doShare(MainActivity.this, view);
                   } catch (FileNotFoundException e) {
                       e.printStackTrace();
                   }
               }
           });
           return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void tryConnectVehicle() {
        if (mVehicleService != null && mBluetoothTurnedOn) {
            SettingsStore settingsStore = SettingsStore.getInstance();

            if (settingsStore.isValidVehicle()) {
                Vehicle vehicle = settingsStore.getVehicle();

                // Connect to the vehicle
                mVehicleService.connectVehicle(vehicle);

                // Store my bluetooth device
                BluetoothDevice myDevice = BluetoothAdapter.getDefaultAdapter()
                        .getRemoteDevice(vehicle.getObdAddress());
                BtConnectionStore btConnStore = BtConnectionStore.getInstance(this);
                btConnStore.storeMyDevice(myDevice);
            }
        }
    }

    private void showNoVehicleRegisteredDialogIfNeeded() {
        SettingsStore settingsStore = SettingsStore.getInstance();

        if (!settingsStore.isValidVehicle()) {
            if (mNavigationDrawerFragment.isDrawerOpen()) {
                mNavigationDrawerFragment.closeDrawer();
            }

            // Need to setup vehicle
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = (DialogFragment) fm.findFragmentByTag("no-vehicle-registered-dialog");
            if (fragment == null) {
                fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_need_register_vehicle),
                        getString(R.string.dialog_message_need_register_vehicle),
                        getString(R.string.btn_no), getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "no-vehicle-registered-dialog").commitAllowingStateLoss();
            }
        }
    }

    private void disconnectVehicle() {
        Logger.getLogger(TAG).trace("disconnectVehicle()");
        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
        }
    }

    private void startDrivingActivity() {
        // Unregister vehicle receiver
        unregisterVehicleReceiver();

        Intent bringToFront = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);
        sendBroadcast(bringToFront);

        Intent intent = new Intent(this, DrivingActivity.class);
        startActivityForResult(intent, REQUEST_DRIVING);

        overridePendingTransition(R.anim.slide_in_down, R.anim.fade_out);

        // Say hello
//        if (Consts.ALPHA) {
//            sayHello();
//        }
    }

    private void startTripListActivity() {
        Intent intent = new Intent(this, TripMonitorActivity.class);
        intent.putExtra(TripMonitorActivity.EXTRA_POSITION, TripMonitorFragment.MENU_TRIP);
        startActivityForResult(intent, REQUEST_TRIP_MONITOR);
    }

    protected void startVideoListActivity() {
        Intent intent = new Intent(this, VideoListActivity.class);
        startActivityForResult(intent, REQUEST_MOV);
    }

    protected void startNoticeActivity() {
        Intent intent = new Intent(this, TripMonitorActivity.class);
        intent.putExtra(TripMonitorActivity.EXTRA_POSITION, TripMonitorFragment.MENU_NOTICE);
        startActivityForResult(intent, REQUEST_NOTICE);
        if (SettingsStore.getInstance().hasNewNoti()) {
            SettingsStore.getInstance().storeNewNotiCount(0);
//            updateBadgeCount(SettingsStore.getInstance().getNewEventCount());
            updateBadgeCount(0);
        }
    }

    protected void startEventActivity() {
        startActivityForResult(new Intent(this, EventActivity.class), REQUEST_EVENT);
        if (SettingsStore.getInstance().hasNewEvent()) {
            SettingsStore.getInstance().storeNewEventCount(0);
            updateBadgeCount(SettingsStore.getInstance().getNewNotiCount());
        }
    }

    protected void startRankingActivity() {
//        Intent intent = new Intent(this, RankingActivity.class);
//        Intent intent = new Intent(this, RankingViewActivity.class);

        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.MEMBER_RANKING_URL);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.weekly_ranking));

        startActivityForResult(intent, REQUEST_RANKING);
    }

    protected void startMyPointActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.MY_POINT_URL);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.main_point));

        startActivityForResult(intent, REQUEST_MY_POINT);
    }

    protected void startCarStatusCheckActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.CAR_STATUS_CHECK_URL);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.main_status));

        startActivityForResult(intent, REQUEST_CAR_STATUS_CHECK);
    }

    protected void startDrivingPatternActivity() {
//        Intent intent = new Intent(this, DrivingPatternActivity.class);

        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.DRIVING_PATTERN_URL);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.drv_pattern));

        startActivityForResult(intent, REQUEST_DRIVING_PATTERN);
    }

    protected void startHofActivity() {
//        Intent intent = new Intent(this, HofActivity.class);

        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.HALL_OF_FAME);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.hof));

        startActivityForResult(intent, REQUEST_HOF);
    }

    protected void startDtcActivity() {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.EXTRA_URL, ServerUrl.DIAGNOSTICS_URL);
        intent.putExtra(WebActivity.EXTRA_TITLE, getString(R.string.diag));

        startActivityForResult(intent, REQUEST_HOF);
    }

    private void updateBadgeCount(int count) {
        Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
        badgeIntent.putExtra("badge_count", count);
        badgeIntent.putExtra("badge_count_package_name", getPackageName());
        badgeIntent.putExtra("badge_count_class_name", "com.pokevian.app.smartfleet.ui.intro.IntroActivity");

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            badgeIntent.setFlags(0x00000020);
        }
        sendBroadcast(badgeIntent);
    }

    private void sayHello() {
//        Calendar now = Calendar.getInstance();
//        int hour = now.get(Calendar.HOUR_OF_DAY);
//        if (5 <= hour && hour <= 9) {
//            Intent service = new Intent(this, SoundEffectService.class);
//            service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.TTS_SPEAK);
//            service.putExtra(SoundEffectService.EXTRA_TTS_TEXT, getString(R.string.tts_hello_driving_work));
//            startService(service);
//        } else {
//            Intent service = new Intent(this, SoundEffectService.class);
//            service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.TTS_SPEAK);
//            service.putExtra(SoundEffectService.EXTRA_TTS_TEXT, getString(R.string.tts_hello_driving_default));
//            startService(service);
//        }
    }

    private void registerBluetoothConnectionReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(mBluetoothConnectionReceiver, filter);
    }

    private void unregisterBluetoothConnectionReceiver() {
        try {
            unregisterReceiver(mBluetoothConnectionReceiver);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private BroadcastReceiver mBluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                logger.debug("@ACTION_ACL_CONNECTED: " + device);
                BtConnectionStore btConnStore = BtConnectionStore.getInstance(context);
                btConnStore.addConnectedBtDevice(device);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                logger.debug("@ACTION_ACL_DISCONNECTED: " + device);
                BtConnectionStore btConnStore = BtConnectionStore.getInstance(context);
                btConnStore.removeConnectedBtDevices(device);
            }
        }
    };

    private void turnOffBluetoothIfNeeded() {
        BtConnectionStore btConnStore = BtConnectionStore.getInstance(this);
        if (btConnStore.isBtEnabled()) {
            String myDevice = btConnStore.getMyBtDevice();
            Set<String> connectedDevices = btConnStore.getConnectedBtDevices();

            // Remove my device.
            while (connectedDevices.remove(myDevice)) ;

            // Turn off bluetooth if there is no connected devices.
            logger.info("Connected bluetooth devices=" + connectedDevices);
            if (connectedDevices.size() == 0) {
                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                btAdapter.disable();
            }
        }

        // Reset!
        btConnStore.reset();
    }

    public void onSectionAttached(int number) {
        logger.debug("onSectionAttached(): number=" + number);
    }

    @Override
    public void onBluetoothTurnedOn(boolean isEnabled) {
        logger.info("Bluetooth is turned on: isEnabled=" + isEnabled);

        // Store Bluetooth enabled state
        BtConnectionStore btConnStore = BtConnectionStore.getInstance(this);
        btConnStore.storeBtEnabled(isEnabled);

        mBluetoothTurnedOn = true;

        // Bluetooth is turned on, so try connect vehicle(OBD)
        tryConnectVehicle();
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("no-vehicle-registered-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                // Start vehicle setting activity
                findViewById(android.R.id.content).post(new Runnable() {
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, VehicleSettingActivity.class);
                        startActivityForResult(intent, REQUEST_VEHICLE_SETTING);
                    }
                });
                // Unregister vehicle receiver
                unregisterVehicleReceiver();
            }
        } else if (DrivingOnDialogFragment.TAG.equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                startDrivingActivity();
            }
        }
    }

    @Override
    public void onObdRecoveryBluetoothDisabled() {
        logger.debug("onObdRecoveryBluetoothDisabled()");

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        btAdapter.enable();
    }

    @Override
    public void onObdRecoveryDialogDismiss() {
        logger.debug("onObdRecoveryDialogDismiss()");

        mObdRecoveryDialog = null;
    }

    // FIXME: Do not start intro activity
    public void onSessionClosed() {
        logger.warn("session closed!");

        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);

        finish();
    }

    private void sayGoodBye() {
//        Calendar now = Calendar.getInstance();
//        int hour = now.get(Calendar.HOUR_OF_DAY);
//        if (17 <= hour && hour <= 21) {
//            Intent service = new Intent(this, SoundEffectService.class);
//            service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.TTS_SPEAK);
//            service.putExtra(SoundEffectService.EXTRA_TTS_TEXT, getString(R.string.tts_googbye_driving_home));
//            startService(service);
//        } else {
//            Intent service = new Intent(this, SoundEffectService.class);
//            service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.TTS_SPEAK);
//            service.putExtra(SoundEffectService.EXTRA_TTS_TEXT, getString(R.string.tts_goodbye_driving_default));
//            startService(service);
//        }
    }

    private void doShare() {
        File imageFile = getShareImageFile();
        logger.trace("createShareIntent#" + imageFile.getAbsolutePath() );
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            captureView(findViewById(R.id.container)).compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imageFile));

        startActivity(shareIntent);
    }

    private Intent createShareIntent() {
//        View view = findViewById(R.id.container);
//        view.buildDrawingCache();
//
//        Bitmap bitmap = view.getDrawingCache();

        File imageFile = getShareImageFile();
        logger.trace("createShareIntent#" + imageFile.getAbsolutePath() );
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            captureView(findViewById(R.id.container)).compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        Uri uri = Uri.fromFile(imageFile);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri.toString());
        return shareIntent;
    }

    private File getShareImageFile() {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (dir.mkdirs()) {

        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

        return new File(dir, simpleDateFormat.format(new Date()) + ".png");
    }

    private Bitmap captureView(View view) {
        view.buildDrawingCache();
       return view.getDrawingCache();
    }

    private boolean isBackground() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        try {
//            ComponentName topActivity = am.getRunningTasks(1).get(0).topActivity;
//            Logger.getLogger(TAG).trace("topActivity#" + topActivity.getClassName() + "@" + topActivity.getPackageName());
            return !am.getRunningTasks(1).get(0).topActivity.getPackageName().equals(getPackageName());
        } catch (Exception e) {
            Logger.getLogger(TAG).warn("isBackground#" + e.getMessage());
        }

        return false;
    }

    private boolean isKeyScreenOn() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        try {
            return km.inKeyguardRestrictedInputMode();
        } catch (Exception e) {
            Logger.getLogger(TAG).warn("isKeyScreenOn#" + e.getMessage());
        }

        return false;
    }

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm.isScreenOn();
    }

    protected int getBadgeIndexByLevel(int level) {
        if (25 < level) {
            return (level - 26) / 5 + 10;
        } else if (10 < level) {
            return (level - 11) / 3 + 5;
        }

        return (level - 1) / 2;
    }

    @Override
    public void onUpdate(final int level) {
        int index = getBadgeIndexByLevel(level);
        Fragment fragment =  getSupportFragmentManager().findFragmentById(R.id.fragment_weekly_record);
        if (fragment != null) {
            ((WeeklyRecordFragment) fragment).updateBadge(index);
        }

        if (mNavigationDrawerFragment != null) {
            mNavigationDrawerFragment.updateBadge(index);
        }
    }


    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

}

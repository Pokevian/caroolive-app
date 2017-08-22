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

package com.pokevian.app.smartfleet.ui.driving;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleData;
import com.pokevian.app.smartfleet.receiver.BringToFrontReceiver;
import com.pokevian.app.smartfleet.service.ImpactDetector;
import com.pokevian.app.smartfleet.service.SoundEffectService;
import com.pokevian.app.smartfleet.service.TripReportService;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.VehicleService.VehicleServiceBinder;
import com.pokevian.app.smartfleet.service.YoutubeUploadService.VideoInfo;
import com.pokevian.app.smartfleet.service.floatinghead.FloatingHeadService;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.diagnostic.DiagnosticActivity;
import com.pokevian.app.smartfleet.ui.driving.BlackboxControlFragment.BlackboxControlCallbacks;
import com.pokevian.app.smartfleet.ui.driving.DrivingService.DrivingServiceBinder;
import com.pokevian.app.smartfleet.ui.main.AutoStartManager;
import com.pokevian.app.smartfleet.util.PackageUtils;
import com.pokevian.app.smartfleet.util.ViewCompatUtils;
import com.pokevian.lib.blackbox.BlackboxConst;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.FuelType;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;


public final class DrivingActivity extends BaseActivity
        implements OnClickListener, AlertDialogCallbacks, BlackboxControlCallbacks {

    static final String TAG = "DrivingActivity";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_REQUEST_EXIT = "request_exit";
    public static final String EXTRA_VES = "vehicle_engine_status";

    private static final int REQUEST_YOUTUBE_AUTH = 1;

    private VehicleService mVehicleService;
    private DrivingService mDrivingService;

    private LocalBroadcastManager mBroadcastManager;

    private IndicatorFragment mIndicatorFragment;
    private ObdClusterFragment mObdFragment;
    private TripClusterFragment mTripFragment;
    private BottomFragment mBottomFragment;
    private DrivingDetailInfoFragment mDetailInfoFragment;

    private View mContentView;
    private ImageView mErsBtn;
    private ImageView mPreviewBtn;
    private ImageView mErsInPreviewBtn;

    private SettingsStore mSettingsStore;
    private BlackboxControlFragment mBlackboxControlFragment;
    private boolean mIsBlackboxEnabled;
    private boolean mIsBlackboxRunning;
    private boolean mIsBlackboxPaused;
    private boolean mIsErsEnabled;
    private boolean mIsDrivingPaused;
    private boolean mIsDetailInfoShown;
    private boolean mIsDetailInfoAnimating;
    private boolean mIsNewDriving;
    private ErsTarget mErsTarget = ErsTarget.NONE;
    private VideoInfo mErsVideoInfo;

    private TextView mPreviewDrivingDistance;
    private TextView mPreviewDrivingTime;
    private RelativeLayout mPreviewSpeedControl;
    private ImageView mPreviewEcoIndicator;
    private SpeedMeter mPreviewSpeedMeter;
    private ImageView mPreviewOverspeed;
    private AnimationDrawable mPreviewAnimDrawableOverspeed;

    private ImageView mPreviewSteadySpeedLamp;
    private ImageView mPreviewIdleLamp;
    private ImageView mPreviewHarshAccelLamp;
    private ImageView mPreviewHarshBrakeLamp;

    private TableLayout mPreviewLampControl;
    private ToggleButton mPreviewOsdBtn;


    private int mPreviewSpeedControlShown = -1;

    boolean mIsAlreadyPreviewOverspeedAnimating = false;

    private int mPreviewSpeedControlW = 0;
    private boolean mShowDetailInfo;

//    private  Drawable mGreenDrawable;
//    private  Drawable mYellowDrawable;
//    private Drawable mRedDrawable;

    private boolean mLaunchNavi;
//    private boolean mIsgSupport;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logger.trace("onCreate(): savedInstanceState=" + savedInstanceState);

        if (SettingsStore.getInstance().isBlackboxEnabled()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        AutoStartManager.stopAutoStartService(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);

        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        mContentView = findViewById(android.R.id.content);

        mSettingsStore = SettingsStore.getInstance();
        mIsBlackboxEnabled = mSettingsStore.isBlackboxEnabled();
        mIsErsEnabled = mSettingsStore.isErsEnabled();

        mFuelType = mSettingsStore.getVehicle().getFuelType();

        mErsBtn = (ImageView) findViewById(R.id.iv_btn_ers);
        mErsBtn.setOnClickListener(this);
        mPreviewBtn = (ImageView) findViewById(R.id.iv_btn_preview);
        mPreviewBtn.setEnabled(false);
        if (mIsBlackboxEnabled) {
            mPreviewBtn.setOnClickListener(this);
            mErsBtn.setEnabled(false);
        }

        findViewById(R.id.iv_btn_exit).setOnClickListener(this);
        findViewById(R.id.detail_info_btn).setOnClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        mIndicatorFragment = (IndicatorFragment) fm.findFragmentById(R.id.center_fragment);
        mObdFragment = (ObdClusterFragment) fm.findFragmentById(R.id.obd_fragment);
        mTripFragment = (TripClusterFragment) fm.findFragmentById(R.id.trip_fragment);
        mBottomFragment = (BottomFragment) fm.findFragmentById(R.id.bottom_fragment);
        mDetailInfoFragment = (DrivingDetailInfoFragment) fm.findFragmentById(R.id.detail_info_fragment);

        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Remove blackbox control fragment if retained!
        // blackbox control fragment will be added when driving service is connected.
        Fragment fragment = fm.findFragmentByTag(BlackboxControlFragment.TAG);
        if (fragment != null) {
            logger.warn("Remove BlackboxControlFragment...");
            fm.beginTransaction().remove(fragment).commit();
        }

        if (savedInstanceState == null) {
            mIsNewDriving = true;
            mLaunchNavi = true;
        } else {
            mIsNewDriving = false;
            mShowDiagnosticDialog =  savedInstanceState.getBoolean("show-diagnostic", true);
            mShowDetailInfo = savedInstanceState.getBoolean("is-detail-info-shown", false);
            mLastMil = savedInstanceState.getBoolean("last-mil", false);
        }

        // Set volume control
        setVolumeControlStream(Consts.AUDIO_TARGET_STREAM);
//        mGreenDrawable = getResources().getDrawable(R.drawable.color_deepgreen_thick);
//        mYellowDrawable= getResources().getDrawable(R.drawable.color_yellow_thick);
//        mRedDrawable = getResources().getDrawable(R.drawable.color_red_thick);

        startAndBindVehicleService();
        bindDrivingService();

    }

    private void launchNaviAppIfNeeded() {
        android.util.Log.w(TAG, "launchNaviAppIfNeeded#" + mLaunchNavi + ": " + mSettingsStore.isAutoLaunchNaviAppEnabled());
        boolean enabled = mSettingsStore.isAutoLaunchNaviAppEnabled();
        if (mLaunchNavi && enabled) {
            launchAppNavi(mSettingsStore.getQuickLaunchNaviApp());
        }
    }

    protected void launchAppNavi(String appId) {
        String packageName = PackageUtils.parsePackageName(appId);
        String className = PackageUtils.parseClassName(appId);
        if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
            Intent i = new Intent()
                    .setClassName(packageName, className)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                startActivity(i);
                android.util.Log.w(TAG, "startActivity#" + i);
            } catch (Exception e) {
                Logger.getLogger(TAG).error("launchAppNavi#" + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        logger.trace("onDestroy()");
        stopService(new Intent(getApplicationContext(), FloatingHeadService.class));

        unregisterVehicleReceiver();
        unbindDrivingService();
        unbindVehicleService();

//        AutoConnectService.setAlarm(getApplicationContext(), Consts.AUTO_CONNECT_WAKEUP_DELAY);
//        startService(new Intent(this, ScreenMonitorService.class));
        AutoStartManager.startAutoStartService(this);

        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("show-diagnostic", mShowDiagnosticDialog);
        outState.putBoolean("is-detail-info-shown", mIsDetailInfoShown);
        outState.putBoolean("last-mil", mLastMil);
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down);
    }

    @Override
    protected void onPause() {
        logger.trace("onPause#" + !isFinishing());
        if (!isFinishing()) {
            if (mVehicleService != null) {
                mVehicleService.startDataBackup();
            }

            if (mSettingsStore.isFloatingWindowEnabled() && !mIsShowDiagnosticActivty) {
                Intent service = new Intent(getApplicationContext(), FloatingHeadService.class);
                service.putExtra(FloatingHeadService.EXTRA_INTENT, "com.pokevian.intent.ACTION_LAUNCH_DRIVING");
                startService(service);
            }

            if (mDrivingService != null) {
                BlackboxPreview preview = mDrivingService.getBlackboxPreview();
                if (preview.getVisibility() != View.GONE) {
                    preview.setVisibility(View.INVISIBLE);
                }
            }
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsShowDiagnosticActivty = false;
        stopService(new Intent(getApplicationContext(), FloatingHeadService.class));

        if (mVehicleService != null) {
            mVehicleService.stopDataBackup();
        }

        if (mDrivingService != null) {
            BlackboxPreview preview = mDrivingService.getBlackboxPreview();
            if (preview.getVisibility() != View.GONE) {
                preview.setVisibility(View.VISIBLE);
            }
        }

        if (mShowDetailInfo) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    toggleDetailInfo();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mVehicleService != null && mDrivingService != null) {
            // Show exit dialog
            mDrivingService.showExitDialog();

            // Disable impact detector (will be enabled when dialog dismissed)
            mVehicleService.setImpactDetectorEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_YOUTUBE_AUTH) {
            logger.debug("onActivityResult(): REQUEST_YOUTUBE_AUTH=" + resultCode);
        }
    }

    private void startAndBindVehicleService() {
//        startService(new Intent(this, VehicleService.class));
        bindService(new Intent(this, VehicleService.class), mVehicleServiceConnection, 0);
    }

    private void unbindVehicleService() {
        try {
            unbindService(mVehicleServiceConnection);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        mVehicleService = null;
    }

    private ServiceConnection mVehicleServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mVehicleService = ((VehicleServiceBinder) binder).getService();

            onVehicleEngineStatusChanged(mVehicleService.getVehicleEngineStatus());

            String accountId = mSettingsStore.getAccountId();
            Vehicle vehicle = mSettingsStore.getVehicle();

            if (accountId != null && vehicle != null) {
                mVehicleService.startDriving(accountId, vehicle, mIsNewDriving);
                mVehicleService.connectVehicle(vehicle);

                registerVehicleReceiver();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void registerVehicleReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_NO_DATA);

        filter.addAction(DrivingService.ACTION_GOTO_EXIT);
        filter.addAction(DrivingService.ACTION_GOTO_HOME);
        filter.addAction(DrivingService.ACTION_GOTO_PAUSE);
        filter.addAction(DrivingService.ACTION_READY_TO_EXIT);
        filter.addAction(DrivingService.ACTION_ERS_TARGET_CHANGED);
        filter.addAction(DrivingService.ACTION_DIALOG_DISMISSED);

//        filter.addAction(DrivingService.ACTION_GOTO_DIAGNOSTIC);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CLEAR_STORED_DTC);
        filter.addAction(ImpactDetector.ACTION_IMPACT_DETECTED);

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
            if (isFinishing()) return;

            final String action = intent.getAction();
            if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                ObdState obdState = (ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                onObdStateChanged(obdState);
            } else if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                int ves = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS,
                        VehicleEngineStatus.UNKNOWN);
                onVehicleEngineStatusChanged(ves);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED.equals(action)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
            } else if (VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED.equals(action)) {
                float rpm = intent.getFloatExtra(VehicleDataBroadcaster.EXTRA_RPM, -1);
                int vss = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VSS, -1);
                onObdExtraDataReceived(rpm, vss);
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
                onObdCannotConnect(obdDevice, isBlocked);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdDeviceNotSupported(obdDevice);
            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdProtocolNotSupported(obdDevice);
            } else if (VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR.equals(action)) {
                Logger.getLogger(TAG).warn("BUSINIT_ERROR#" + intent.getIntExtra(VehicleDataBroadcaster.EXTRA_OBD_PROTOCOL, -1));
            } else if (VehicleDataBroadcaster.ACTION_OBD_NO_DATA.equals(action)) {
                Logger.getLogger(TAG).warn("No data!!");
            } else if (DrivingService.ACTION_GOTO_EXIT.equals(action)) {
                onGogoExit();
            } else if (DrivingService.ACTION_GOTO_HOME.equals(action)) {
                onGotoHome();
            } else if (DrivingService.ACTION_GOTO_PAUSE.equals(action)) {
                onGotoPause();
            } else if (DrivingService.ACTION_READY_TO_EXIT.equals(action)) {
                onReadyToExit();
            } else if (DrivingService.ACTION_ERS_TARGET_CHANGED.equals(action)) {
                ErsTarget target = (ErsTarget) intent.getSerializableExtra(DrivingService.EXTRA_ERS_TARGET);
                boolean userSelect = intent.getBooleanExtra(DrivingService.EXTRA_USER_SELECT, false);
                onErsTargetChanged(target, userSelect);
            } else if (DrivingService.ACTION_DIALOG_DISMISSED.equals(action)) {
                Log.i(TAG, "onReceive#" + action);
                int type = intent.getIntExtra(DrivingService.EXTRA_DIALOG_TYPE, -1);
                onDialogDismiss(type);
            } /*else if (DrivingService.ACTION_GOTO_DIAGNOSTIC.equals(action)) {
                Log.i(TAG, "onReceive#" + action);
//                mLaunchNavi = false;
                startDiagnosticActivity(mDtc, false);
            }*/ else if (VehicleDataBroadcaster.ACTION_OBD_CLEAR_STORED_DTC.equals(action)) {
                if (mVehicleService != null) {
                    mVehicleService.clearStoredDTC();
                }
            } else if (ImpactDetector.ACTION_IMPACT_DETECTED.equals(action)) {
                Logger.getLogger(TAG).debug("onReceive#" + action);
                if (mDrivingService != null && !mIsBlackboxEnabled) {
                    // Show ERS dialog
                    if (mIsErsEnabled) {
                        mDrivingService.showErsDialog(false);

                        // Sound effect!
                        Intent service = new Intent(DrivingActivity.this, SoundEffectService.class);
                        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.SOUND_PLAY);
                        service.putExtra(SoundEffectService.EXTRA_SOUND_ID, SoundEffectService.SID_IMPACT);
                        startService(service);
                    }
                }
            }
        }
    };

    private boolean mIsShowDiagnosticActivty;
    protected void startDiagnosticActivity(String dtc, boolean showDialog) {
        Log.w(TAG, "startDiagnosticActivity#" + dtc);
        mIsShowDiagnosticActivty = true;
        Intent i = new Intent(getApplicationContext(), DiagnosticActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("dtc", dtc);
        i.putExtra("show-dialog", showDialog);
        startActivity(i);
    }

    private void onObdStateChanged(ObdState obdState) {
        logger.debug("onObdStateChanged(): obdState=" + obdState);

        mIndicatorFragment.onObdStateChanged(obdState);
        mObdFragment.onObdStateChanged(obdState);
        mTripFragment.onObdStateChanged(obdState);

        if (obdState == ObdState.SCANNING) {
            if (mDrivingService != null) {
                mDrivingService.dismissObdRecoveryDialog();
            }
        }
    }

    private int mVes = VehicleEngineStatus.UNKNOWN;
    private void onVehicleEngineStatusChanged(int ves) {
        logger.debug("onVehicleEngineStatusChanged#" + VehicleEngineStatus.toString(ves) + " < " + VehicleEngineStatus.toString(mVes));

        if (mDrivingService != null) {
            mDrivingService.dismissObdRecoveryDialog();
        }

        if (VehicleEngineStatus.isOnDriving(ves) && !VehicleEngineStatus.isOnDriving(mVes)) {
            moveTaskToFront();

            if (mVehicleService != null && mDrivingService != null) {
                mDrivingService.dismissDrivingOffDialog();

                if (mIsDrivingPaused) {
                    mIsDrivingPaused = false;

                    mVehicleService.resumeDriving();
                    mDrivingService.resumeDriving();

                    // Resume blackbox if needed
                    // FIXME: Need delay for screen rotation
                    mContentView.postDelayed(new Runnable() {
                        public void run() {
                            if (!isFinishing() && !mIsBlackboxRunning && mIsBlackboxPaused) {
                                if (mBlackboxControlFragment != null) {
                                    mIsBlackboxPaused = false;
                                    mBlackboxControlFragment.prepareBlackbox();
                                    mBlackboxControlFragment.startBlackbox();
                                }
                            }
                        }
                    }, 1000);
                }
            }
        } else if (VehicleEngineStatus.isOffDriving(ves)) {
            moveTaskToFront();

            if (mVehicleService != null && mDrivingService != null) {
                if (!mDrivingService.isExitDialogShowing() && !mDrivingService.isDrivingOffDialogShowing()
                        && !mDrivingService.isWaitForExitDialogShowing()) {
                    boolean engineOffDetectionEnabled = mSettingsStore.isEngineOffDetectionEnabled();
                    if (engineOffDetectionEnabled) {
                        mDrivingService.showDrivingOffDialog();

                        // Disable impact detector (will be enabled when driving off dialog dismissed)
                        mVehicleService.setImpactDetectorEnabled(false);
                    }
                }
            }
        }

        mVes = ves;
    }

    private boolean mShowDiagnosticDialog = true;
    private boolean mLastMil = false;
    protected String mDtc;

    private void onObdDataReceived(ObdData data) {
        mIndicatorFragment.onObdDataReceived(data);
        mObdFragment.onObdDataReceived(data);
        mTripFragment.onObdDataReceived(data);
        mBottomFragment.onObdDataReceived(data);

        mDtc = getDTC(data);
        if (mShowDiagnosticDialog) {
            if (mDrivingService != null) {
                showDiagnosticProcess(mDtc);
                mShowDiagnosticDialog = false;
                mLastMil = isMilOn(data);
            }
        } else if (isMilOn(data) && !mLastMil) {
            mLastMil = true;
            showDiagnosticProcess(mDtc);
        }

        if (mIsDetailInfoShown) {
            mDetailInfoFragment.onObdDataReceived(data);
        }

        if (mDrivingService != null && /*mDrivingService.getBlackboxPreview() != null*/mIsBlackboxEnabled) {
            if (data.isValid(KEY.TRIP_DRIVING_DIST) && data.isValid(KEY.TRIP_DURATION)) {
                int distance = data.getFloat(KEY.TRIP_DRIVING_DIST, 0).intValue();
                int time = data.getFloat(KEY.TRIP_DURATION, 0).intValue();

                updateBlackboxPreviewDistanceTime(distance, time);
            }

            updateBlackboxPreviewLamps(data.getBoolean(KEY.CALC_STEADY_SPEED, false),
                    data.getBoolean(KEY.CALC_IDLING, false),
                    data.getBoolean(KEY.CALC_HARSH_ACCEL, false),
                    data.getBoolean(KEY.CALC_HARSH_BRAKE, false));

            if (data.isValid(KEY.CALC_OVERSPEED)) {
                animatePreviewOverspeedWarning(data.getBoolean(KEY.CALC_OVERSPEED, false));
            }

            updateBlackboxPreviewEcoLevel(calcEcoLevel(data));
        }
    }

    String getDTC(ObdData data) {
        return data.getBoolean(KEY.SAE_MIL, false) ? data.getString(KEY.SAE_DTC) : null;
    }

    boolean isMilOn(ObdData data) {
        return !TextUtils.isEmpty(getDTC(data));
    }

    private void showDiagnosticProcess(String dtc) {
        if (mDrivingService != null) {
            mDrivingService.showDiagnosticProcess(dtc);
        }
    }

    int mLastVss = -1;
    float mLastRpm = -1;
    private void onObdExtraDataReceived(float rpm, int vss) {
        mObdFragment.onObdExtraDataReceived(rpm, vss);
        mTripFragment.onObdExtraDataReceived(rpm, vss);

        if (mIsDetailInfoShown) {
            mDetailInfoFragment.onObdExtraDataReceived(rpm, vss);
        }

        mLastVss = vss ;
        mLastRpm = rpm;

        runOnUiThread(mUpdateVssRunnable);
    }

    private final Runnable mUpdateVssRunnable = new Runnable() {
        public void run() {

            if(mPreviewSpeedMeter != null) {
                mPreviewSpeedMeter.setValueText(String.valueOf(mLastVss));
            }

            if (mIsDetailInfoShown) {
                mDetailInfoFragment.onObdExtraDataReceived(mLastRpm, mLastVss);
            }

            if(mLastRpm > 0 && mLastVss > 0) {
                animatePreviewSpeedControl(View.VISIBLE);
            }else {
                animatePreviewSpeedControl(View.GONE);
            }

        }
    };

    private void onObdCannotConnect(BluetoothDevice obdDevice, boolean isBlocked) {
//        logger.debug("onObdCannotConnect(): isBlocked=" + isBlocked);

        mIndicatorFragment.onObdCannotConnect();
        mObdFragment.onObdCannotConnect();
        mTripFragment.onObdCannotConnect();
        mBottomFragment.onObdCannotConnect();

//        logger.debug("onObdCannotConnect#" + mVehicleService);
        // Retry!
        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
            Vehicle vehicle = mSettingsStore.getVehicle();
            if (vehicle != null && mVehicleService != null) {
                mVehicleService.connectVehicle(vehicle);
                logger.debug("connectVehicle#" + vehicle);
            }
        }

        if (isBlocked) {
            Logger.getLogger(TAG).info("device is blocked@onObdCannotConnect");
            if (mDrivingService != null) {
                mDrivingService.showObdRecoveryDialog();
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

    private void onGogoExit() {
        logger.debug("onGogoExit()");

        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_REQUEST_EXIT, true);
        setResult(RESULT_OK, result);

        waitForExit();
    }

    private void onGotoHome() {
        logger.debug("onGotoHome()");

        // Home button
        Intent result = new Intent();
        if (mVehicleService != null) {
            result.putExtra(EXTRA_VES, mVehicleService.getVehicleEngineStatus());
        }
        setResult(RESULT_OK, result);

        waitForExit();
    }

    private void onGotoPause() {
        logger.debug("onGotoPause()");

        // Pause button
        moveTaskToBack(true);

        if (!mIsDrivingPaused) {
            mIsDrivingPaused = true;

            if (mVehicleService != null) {
                mVehicleService.pauseDriving();
            }
            if (mDrivingService != null) {
                mDrivingService.pauseDriving();
            }

            // Pause blackbox
            if (mIsBlackboxRunning && !mIsBlackboxPaused) {
                if (mBlackboxControlFragment != null) {
                    mBlackboxControlFragment.stopBlackbox();
                    mIsBlackboxPaused = true;
                }
            }
        }
    }

    private void onReadyToExit() {
        logger.debug("onReadyToExit()");

        unregisterVehicleReceiver();

        // Finish at next thread time
        mContentView.post(new Runnable() {
            public void run() {
                finish();
            }
        });
    }

    private void onErsTargetChanged(ErsTarget target, boolean userSelected) {
        logger.debug("onErsTargetChanged(): mErsTarget=" + target);

        // Keep last ERS target
        mErsTarget = target;

        logger.debug("onErsTargetChanged(): userSelected=" + userSelected + "#" + mIsErsEnabled);
        // Process ERS (Call and SMS case)
        if (userSelected && mIsErsEnabled && (mErsTarget == ErsTarget.CALL || mErsTarget == ErsTarget.SMS)) {
            if (mVehicleService != null) {
                ErsProcessor ersProcessor = new ErsProcessor(this, mVehicleService, mErsTarget, mErsVideoInfo);
                ersProcessor.process();
            } else {
                logger.warn("Cannot process ERS because vehicle service is not connected!");
            }

            mErsVideoInfo = null;
        }
    }

    private void onDialogDismiss(int type) {
        logger.debug("onDialogDismiss(): type=" + type);

        switch (type) {
            case DrivingService.DIALOG_EXIT:
                if (mVehicleService != null) {
                    mVehicleService.setImpactDetectorEnabled(true);
                }
                break;
            case DrivingService.DIALOG_DRIVING_OFF:
                if (mVehicleService != null) {
                    mVehicleService.setImpactDetectorEnabled(true);
                }
                break;
            case DrivingService.DIALOG_WAIT_FOR_EXIT:
                if (mVehicleService != null) {
                    mVehicleService.setImpactDetectorEnabled(true);
                }
                break;
            case DrivingService.DIALOG_ERS:
                break;
            case DrivingService.DIALOG_OBD_RECOVERY:
                break;
            case DrivingService.DIALOG_OBD_DIAGNOSTIC:
                if (mVehicleService != null) {
                    mVehicleService.setImpactDetectorEnabled(true);
                }
//                launchNaviAppIfNeeded();
                break;
            default:
                break;
        }
    }

    private void moveTaskToFront() {
        if (isPaused()) {
            Intent bringToFront = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);
            sendBroadcast(bringToFront);
        }
    }

    private void bindDrivingService() {
        bindService(new Intent(this, DrivingService.class), mDrivingServiceConnection, BIND_AUTO_CREATE);
    }

    private void unbindDrivingService() {
        try {
            unbindService(mDrivingServiceConnection);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        mDrivingService = null;
    }

    private int mCameraExposureOffset;

    private ServiceConnection mDrivingServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mDrivingService = ((DrivingServiceBinder) binder).getService();

            launchNaviAppIfNeeded();

            if (mIsBlackboxEnabled) {
                BlackboxPreview preview = mDrivingService.getBlackboxPreview();
                mErsInPreviewBtn = (ImageView) preview.findViewById(R.id.iv_preview_btn_ers);
                if (mIsErsEnabled) {
                    mErsInPreviewBtn.setOnClickListener(DrivingActivity.this);
                }
                preview.findViewById(R.id.btn_car_monitor).setOnClickListener(DrivingActivity.this);
                preview.findViewById(R.id.iv_preview_btn_exit).setOnClickListener(DrivingActivity.this);

                mPreviewDrivingDistance = (TextView) preview.findViewById(R.id.preview_driving_distance);
                mPreviewDrivingDistance.setText(R.string.empty_int_value);
                ((TextView) preview.findViewById(R.id.preview_driving_distance_unit)).setText(mSettingsStore.getDistanceUnit().toString());

                mPreviewDrivingTime = (TextView) preview.findViewById(R.id.preview_driving_time);
                mPreviewDrivingTime.setText(R.string.empty_hmmss_value);

                mPreviewSpeedControl = (RelativeLayout) preview.findViewById(R.id.rl_preview_speed);
                mPreviewOverspeed = (ImageView) preview.findViewById(R.id.preview_overspeed);
                mPreviewEcoIndicator = (ImageView) preview.findViewById(R.id.preview_eco_indicator);

                mPreviewSpeedMeter = (SpeedMeter) preview.findViewById(R.id.preview_speedmeter);
                mPreviewSpeedMeter.setUnitText(mSettingsStore.getSpeedUnit().toString());
                mPreviewSpeedMeter.setValueText(getString(R.string.empty_int_value));

                mPreviewAnimDrawableOverspeed = (AnimationDrawable) getResources().getDrawable(R.drawable.ic_overspeed_blink);
                //mPreviewSpeedControl.setVisibility(View.GONE);
                mPreviewSpeedControl.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                mPreviewSpeedControlW = mPreviewSpeedControl.getMeasuredWidth();

                animatePreviewSpeedControl(View.GONE);

                mPreviewLampControl = (TableLayout) preview.findViewById(R.id.tl_preview_lamp);
                mPreviewSteadySpeedLamp = (ImageView) preview.findViewById(R.id.cb_preview_steady_speed);
                mPreviewSteadySpeedLamp.setEnabled(false);
                mPreviewIdleLamp = (ImageView) preview.findViewById(R.id.cb_preview_idling);
                mPreviewIdleLamp.setEnabled(false);
                mPreviewHarshAccelLamp = (ImageView) preview.findViewById(R.id.cb_preview_harsh_accel);
                mPreviewHarshAccelLamp.setEnabled(false);
                mPreviewHarshBrakeLamp = (ImageView) preview.findViewById(R.id.cb_preview_harsh_brake);
                mPreviewHarshBrakeLamp.setEnabled(false);

                mPreviewOsdBtn = ((ToggleButton) preview.findViewById(R.id.preview_osd));
                mPreviewOsdBtn.setOnClickListener(DrivingActivity.this);

                if (mSettingsStore.getBlackboxOsdEnabled()) {
                    mPreviewOsdBtn.setChecked(true);
                    mPreviewSpeedControl.setVisibility(View.VISIBLE);
                    mPreviewLampControl.setVisibility(View.VISIBLE);
                } else {
                    mPreviewOsdBtn.setChecked(false);
                    mPreviewSpeedControl.setVisibility(View.GONE);
                    mPreviewLampControl.setVisibility(View.GONE);
                }

                // Add blackbox control fragment here!
                // We need preview to start blackbox
                FragmentManager fm = getSupportFragmentManager();
                mBlackboxControlFragment = (BlackboxControlFragment) fm.findFragmentByTag(BlackboxControlFragment.TAG);
                if (mBlackboxControlFragment == null) {
                    mBlackboxControlFragment = BlackboxControlFragment.newInstance();
                    fm.beginTransaction()
                            .add(mBlackboxControlFragment, BlackboxControlFragment.TAG)
                            .commitAllowingStateLoss();
                }

                /*if (preview instanceof BlackboxPreview2) {
                    mBlackboxControlFragment.startBlackbox(((BlackboxPreview2) preview).getSurfaceTexture(), ((BlackboxPreview2) preview).width, ((BlackboxPreview2) preview).height);
                } else */{
                    mBlackboxControlFragment.prepareBlackbox();
                    mBlackboxControlFragment.startBlackbox();
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_btn_ers || id == R.id.iv_preview_btn_ers) {
//            if (mBlackboxControlFragment != null) {
//                mBlackboxControlFragment.onEmergency();
//            } else {
//                if (mVehicleService != null) {
//                    mVehicleService.onEmergency();
//                }
//            }

            if (mVehicleService != null) {
                mVehicleService.onEmergency();
            }

            // Show ERS dialog
            if (mIsErsEnabled) {
                mErsVideoInfo = new VideoInfo(mSettingsStore.getErsYoutubeAccountName(), mSettingsStore.getAccountId(),
                        mSettingsStore.getVehicleId());
                mErsVideoInfo.setBlackboxEventType(BlackboxConst.BlackboxEventType.USER.name());
                if (mVehicleService != null) {
                    VehicleData.VehicleLocation location = mVehicleService.getLastLocation();
                    mErsVideoInfo.setLocation(location);
                }

                if (mDrivingService != null) {
                    mDrivingService.showErsDialog(mIsBlackboxRunning);
                }
            }
        } else if (id == R.id.iv_btn_exit || id == R.id.iv_preview_btn_exit) {
            if (mVehicleService != null && mDrivingService != null) {
                mDrivingService.showExitDialog();

                // Disable impact detector (will be enabled when exit dialog dismissed)
                mVehicleService.setImpactDetectorEnabled(false);
            }
        } else if (id == R.id.iv_btn_preview) {
            if (mDrivingService != null) {
                BlackboxPreview preview = mDrivingService.getBlackboxPreview();
                preview.setVisibility(View.VISIBLE);
            }
        } else if (id == R.id.btn_car_monitor) {
            if (mDrivingService != null) {
                BlackboxPreview preview = mDrivingService.getBlackboxPreview();
                preview.setVisibility(View.GONE);
            }
        } else if (id == R.id.detail_info_btn) {
            toggleDetailInfo();
        } else if(id == R.id.preview_osd) {
            mSettingsStore.storeBlackboxOsdEnabled(mPreviewOsdBtn.isChecked());
            if (mPreviewOsdBtn.isChecked()) {
                mPreviewSpeedControl.setVisibility(View.VISIBLE);
                mPreviewLampControl.setVisibility(View.VISIBLE);
            } else {
                mPreviewSpeedControl.setVisibility(View.GONE);
                mPreviewLampControl.setVisibility(View.GONE);
            }
        } else if (R.id.iv_preview_auto_focus == id) {
            mBlackboxControlFragment.onClickFocusMode();
        }
    }

    private void toggleDetailInfo() {
        if (mIsDetailInfoAnimating) {
            return;
        }

        if (mIsDetailInfoShown && !mIsDetailInfoAnimating) {
            mIsDetailInfoShown = false;

            AnimatorSet animSet = new AnimatorSet();
            animSet.setInterpolator(new DecelerateInterpolator());
            animSet.setDuration(500);

            View leftView = mObdFragment.getView();
            ObjectAnimator leftAnim = ObjectAnimator.ofFloat(leftView, "translationX", 0);

            View rightView = mTripFragment.getView();
            ObjectAnimator rightAnim = ObjectAnimator.ofFloat(rightView, "translationX", 0);

            View centerView1 = mIndicatorFragment.getView().findViewById(R.id.indicator_pane);
            ObjectAnimator centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 1);
//            View centerView2 = mIndicatorFragment.getView().findViewById(R.id.trip_pane);
//            ObjectAnimator centerAnim2 = ObjectAnimator.ofFloat(centerView2, "alpha", 1);

            View detailInfoView = mDetailInfoFragment.getView();
            ObjectAnimator detailInfoAnim = ObjectAnimator.ofFloat(detailInfoView, "alpha", 0);

            animSet.playTogether(leftAnim, rightAnim, centerAnim1, /*centerAnim2,*/ detailInfoAnim);
            animSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationRepeat(Animator animation) {}
                public void onAnimationCancel(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    mIsDetailInfoAnimating = false;

                    FragmentManager fm = getSupportFragmentManager();
                    fm.beginTransaction().hide(mDetailInfoFragment).commitAllowingStateLoss();

                    ((CheckBox)findViewById(R.id.detail_info_btn)).setChecked(false);

                }

                @Override
                public void onAnimationStart(Animator animation) {
                    mIsDetailInfoAnimating = true;

                }
            });
            animSet.start();

        } else if (!mIsDetailInfoShown && !mIsDetailInfoAnimating) {
            mIsDetailInfoShown = true;

            AnimatorSet animSet = new AnimatorSet();
            animSet.setInterpolator(new DecelerateInterpolator());
            animSet.setDuration(500);

            View leftView = mObdFragment.getView();
            ObjectAnimator leftAnim = ObjectAnimator.ofFloat(leftView, "translationX", -leftView.getWidth());

            View rightView = mTripFragment.getView();
            ObjectAnimator rightAnim = ObjectAnimator.ofFloat(rightView, "translationX", rightView.getWidth());

            Logger.getLogger(TAG).trace("toggleDetailInfo#" + leftView.getWidth());

            View centerView1 = mIndicatorFragment.getView().findViewById(R.id.indicator_pane);
            ObjectAnimator centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 0);
//            View centerView2 = mIndicatorFragment.getView().findViewById(R.id.trip_pane);
//            ObjectAnimator centerAnim2 = ObjectAnimator.ofFloat(centerView2, "alpha", 0);

            if (isPortrait()) {
                centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 1);
//                centerAnim2 = ObjectAnimator.ofFloat(centerView2, "alpha", 1);
            }

            View detailInfoView = mDetailInfoFragment.getView();
            ObjectAnimator detailInfoAnim = ObjectAnimator.ofFloat(detailInfoView, "alpha", 1);

            animSet.playTogether(leftAnim, rightAnim, centerAnim1, /*centerAnim2,*/ detailInfoAnim);
            animSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                    mIsDetailInfoAnimating = true;

                    FragmentManager fm = getSupportFragmentManager();
                    fm.beginTransaction().show(mDetailInfoFragment).commitAllowingStateLoss();
                }

                public void onAnimationEnd(Animator animation) {
                    mIsDetailInfoAnimating = false;

                    ((CheckBox)findViewById(R.id.detail_info_btn)).setChecked(true);
                }

                public void onAnimationRepeat(Animator animation) {}
                public void onAnimationCancel(Animator animation) {}
            });
            animSet.start();
        }
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();
        if (ClosePreviewDialogFragment.TAG.equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                if (mDrivingService != null) {
                    BlackboxPreview preview = mDrivingService.getBlackboxPreview();
                    preview.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onBlackboxPreview() {
        // Show preview here!
        if (mDrivingService != null) {
			BlackboxPreview preview = mDrivingService.getBlackboxPreview();

			// Scale preview
//			View zoomPanel = preview.findViewById(R.id.zoom_panel);
//			scale(zoomPanel, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

			// Don't show preview now
//			preview.setVisibility(View.VISIBLE);
		}
    }

    @Override
    public void onBlackboxStarted() {
        logger.debug("onBlackboxStarted()");

        mIsBlackboxRunning = true;

//        mIndicatorFragment.onRecordStarted();

        mPreviewBtn.setEnabled(true);
        mErsBtn.setEnabled(true);

        FragmentManager fm = getSupportFragmentManager();

        // Show preview closing dialog
        if (mDrivingService != null) {
            BlackboxPreview preview = mDrivingService.getBlackboxPreview();
            if (preview.getVisibility() == View.VISIBLE) {
                ClosePreviewDialogFragment fragment = ClosePreviewDialogFragment.newInstance();
                if (mDrivingService != null) {
                    fragment.setSystemDialog(mDrivingService.createAlertDialog());
                }
                fm.beginTransaction().add(fragment, ClosePreviewDialogFragment.TAG)
                        .commitAllowingStateLoss();
            }
        }

        mDrivingService.getBlackboxPreview().findViewById(R.id.iv_preview_auto_focus).setOnClickListener(DrivingActivity.this);
        final SeekBar exposureBar = (SeekBar) mDrivingService.getBlackboxPreview().findViewById(R.id.preview_exposure);

        /*if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            if (mDrivingService.getBlackboxPreview() instanceof BlackboxPreview2) {
                final BlackboxEngine engine = mVehicleService.getBlackboxEngine();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        int max = engine.getMaxExposureCompensation();
                        int min = engine.getMinExposureCompensation();
                        Log.i(TAG, "exposure@" + min + ": " + max);
                        if (max != 0 || min != 0) {
                            int range = max - min;

                            mCameraExposureOffset = range / 2;
                            int exposure = mSettingsStore.getBlackboxExposureExtra();
                            int progress = mCameraExposureOffset + exposure;

                            engine.setCameraExposure(exposure);

                            Log.d(TAG, String.format("cameraReady::exposureBar@%d[%d#%d]" , progress, range, exposure));

                            exposureBar.setMax(range);
                            exposureBar.setProgress(progress);
                        } else {
//                            disableExposureBar();
                        }

                        exposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                try {
                                    int exposure = progress - mCameraExposureOffset;
                                    engine.setCameraExposure(exposure);
                                    mSettingsStore.storeBlackboxExposureExtra(exposure);
                                } catch (IllegalArgumentException e) {
                                    Log.w(TAG, "failed to increase camera exposure");
                                }
                            }
                        });

                        *//*try {
                            mSupporetedFocusModes = new ArrayList<String>();
                            List<Integer> modes = mBlackboxEngine.getControlAfAvailableModes();
                            for (Integer mode : modes) {
                                String s = CameraHelper.getFocusModeByControlAfMode(mode);
                                if (s != null) {
                                    mSupporetedFocusModes.add(s);
                                }
                            }

                            setFocusMode();
                            if (mSupporetedFocusModes.size() > 1) {
                                mBlackboxPreview.findViewById(R.id.iv_preview_auto_focus).setOnClickListener(this);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "" + e.getMessage(), e);
                        }*//*

                        mBlackboxControlFragment.prepare();

                        ((BlackboxPreview2) mDrivingService.getBlackboxPreview()).configureTransform();

                    }
                });

                return;
            }
        }*/

        Camera camera = null;
        try {
            camera = mBlackboxControlFragment.getCamera().instance;
        } catch (NullPointerException e) {
            Logger.getLogger(TAG).warn("" + e.getMessage(), e);
            return;
        }

        Camera.Parameters params = camera.getParameters();
        if (params != null) {
            int max = params.getMaxExposureCompensation();
            int min = params.getMinExposureCompensation();
            Log.i(TAG, "exposure@" + min + ": " + max);
            if (max != 0 || min != 0) {
                int range = max - min;

                mCameraExposureOffset = range / 2;
                int exposure = mSettingsStore.getBlackboxExposureExtra();
                int progress = mCameraExposureOffset + exposure;

                Log.i(TAG, String.format("cameraReady::exposureBar@%d[%d#%d]", progress, range, exposure));

                params.setExposureCompensation(exposure);
                camera.setParameters(params);

                exposureBar.setMax(range);
                exposureBar.setProgress(progress);
            } else {
                disableExposureBar();
            }

            exposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    try {
                        int exposure = progress - mCameraExposureOffset;

                        Camera camera = mBlackboxControlFragment.getCamera().instance;
                        Camera.Parameters params = camera.getParameters();
                        params.setExposureCompensation(exposure);
                        camera.setParameters(params);
                        mSettingsStore.storeBlackboxExposureExtra(exposure);
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "failed to increase camera exposure");
                    } catch (RuntimeException e) {
                        Log.e(TAG, "failed to increase camera exposure", e);
                    }
                }
            });
        }
    }

    @Override
    public void onBlackboxStopped() {
        logger.debug("onBlackboxStopped()");

        mPreviewBtn.setEnabled(false);
        mErsBtn.setEnabled(false);

//        mIndicatorFragment.onRecordStopped();

        if (mDrivingService != null) {
            mDrivingService.setReadyToExit(true);
        }

        mIsBlackboxRunning = false;
    }

    @Override
    public void onBlackboxError(int errorCode) {
        logger.debug("onBlackboxError(): errorCode=" + errorCode);

        mPreviewBtn.setEnabled(false);

        String msg = getString(R.string.driving_blackbox_error, errorCode);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBlackboxEventBegin(BlackboxConst.BlackboxEventType eventType) {
        if (isFinishing()) return;

        // Sound effect!
        Intent service = new Intent(this, SoundEffectService.class);
        service.putExtra(SoundEffectService.EXTRA_CMD, SoundEffectService.SOUND_PLAY);
        service.putExtra(SoundEffectService.EXTRA_SOUND_ID, SoundEffectService.SID_IMPACT);
        startService(service);

        // Disable impact detector (will be enabled when event finished)
        if (mVehicleService != null) {
            mVehicleService.setImpactDetectorEnabled(false);
        }

        // Disable ERS buttons
        mErsBtn.setEnabled(false);
        if (mErsInPreviewBtn != null) {
            mErsInPreviewBtn.setEnabled(false);
        }

        // Show ERS dialog if sensor event
        if (eventType == BlackboxConst.BlackboxEventType.SENSOR) {
            if (mIsErsEnabled) {
                mErsVideoInfo = new VideoInfo(mSettingsStore.getErsYoutubeAccountName(),
                        mSettingsStore.getAccountId(), mSettingsStore.getVehicleId());
                mErsVideoInfo.setOccurTime(Calendar.getInstance().getTime());
                mErsVideoInfo.setBlackboxEventType(BlackboxConst.BlackboxEventType.SENSOR.name());
                if (mVehicleService != null) {
                    VehicleData.VehicleLocation location = mVehicleService.getLastLocation();
                    mErsVideoInfo.setLocation(location);
                }

                if (mDrivingService != null) {
                    mDrivingService.showErsDialog(mIsBlackboxRunning);
                }
            }
        }
    }

    @Override
    public void onBlackboxEventEnd(File videoFile, Date beginTime, Date endTime) {
        if (isFinishing()) return;

        // Enable impact detector
        if (mVehicleService != null) {
            mVehicleService.setImpactDetectorEnabled(true);
        }

        // Enable ERS buttons
        mErsBtn.setEnabled(true);
        if (mErsInPreviewBtn != null) {
            mErsInPreviewBtn.setEnabled(true);
        }

        // Dismiss ERS dialog even if user not select any target
        if (mDrivingService != null) {
            mDrivingService.dismissErsDialog();
        }

        // Process ERS (Youtube case)
        if (mIsErsEnabled && mErsTarget == ErsTarget.YOUTUBE) {
            if (mErsVideoInfo != null) {
                mErsVideoInfo.setVideoFile(videoFile);
                mErsVideoInfo.setBeginTime(beginTime);
                mErsVideoInfo.setEndTime(endTime);
                mErsVideoInfo.setPrivacy(mSettingsStore.getErsYoutubePrivacy());

                if (mVehicleService != null) {
                    ErsProcessor ersProcessor = new ErsProcessor(this, mVehicleService, mErsTarget, mErsVideoInfo);
                    ersProcessor.process();
                } else {
                    logger.warn("Cannot process ERS because vehicle service is not connected!");
                }

                mErsVideoInfo = null;
            }
        }
    }

    private void disableExposureBar() {
        if (mDrivingService.getBlackboxPreview() != null) {
            Log.w(TAG, ">>> exposurebar@disable");
            SeekBar exposureBar = (SeekBar) mDrivingService.getBlackboxPreview().findViewById(R.id.preview_exposure);

            Drawable progressDrawable = getResources().getDrawable(R.drawable.seekbar_exposure_disable);
            progressDrawable.setBounds (exposureBar.getProgressDrawable ().getBounds());
            exposureBar.setProgressDrawable(progressDrawable);

            exposureBar.setThumb(getResources().getDrawable(R.drawable.seekbar_thumb_exposure_disable));

            ImageView brightness =  (ImageView)mDrivingService.getBlackboxPreview().findViewById(R.id.ic_brightness);
            brightness.setEnabled(false);

            exposureBar.setEnabled(false);
        }
    }


    private void waitForExit() {
        if (mVehicleService != null && mDrivingService != null) {
            mDrivingService.showWaitForExitDialog();
            mDrivingService.setReadyToExit(!mIsBlackboxRunning);

            BlackboxPreview preview = mDrivingService.getBlackboxPreview();
            preview.setVisibility(View.GONE);

            mDrivingService.stopDriving();
            VehicleData.VehicleTrip trip = mVehicleService.stopDriving();

            // start trip report service
            if (trip != null) {
                if (Consts.ALPHA) {
                    Intent service = new Intent(getApplicationContext(), TripReportService.class);
                    service.putExtra(TripReportService.EXTRA_TRIP, trip);
                    startService(service);
                }
            }
        }

        if (mBlackboxControlFragment != null) {
            mBlackboxControlFragment.stopBlackbox();
        }
    }


    // Called by IndicatorFragment or DrivingDetailInfoFragment
    public void clearStoredDTC() {
        if (mVehicleService != null /*&& mIndicatorFragment.isMilOn()*/) {
            mVehicleService.clearStoredDTC();

            Toast.makeText(this, R.string.driving_clear_stored_dtc, Toast.LENGTH_LONG).show();
        }
    }


    private void animatePreviewSpeedControl(int visible) {
        if(visible == View.VISIBLE) {
            if(mPreviewSpeedControl != null && mPreviewSpeedControlShown != View.VISIBLE) {
                View v = (View) mPreviewSpeedControl;
                //mPreviewSpeedControl.setVisibility(View.VISIBLE);
                ObjectAnimator leftAnim = ObjectAnimator.ofFloat(v, "translationX", 0);
                leftAnim.setDuration(500);
//				leftAnim.addListener(new AnimatorListener() {
//					public void onAnimationRepeat(Animator animation) {}
//					public void onAnimationCancel(Animator animation) {}
//
//					@Override
//					public void onAnimationEnd(Animator animation) {
//						mIsSpeedControlAnimating = false;
//					}
//
//					@Override
//					public void onAnimationStart(Animator animation) {
//						mIsSpeedControlAnimating = true;
//
//					}
//				});
                leftAnim.start();
                mPreviewSpeedControlShown = View.VISIBLE;
            }
        } else if(visible == View.GONE) {
            if(mPreviewSpeedControl != null && mPreviewSpeedControlShown != View.GONE) {
                View v = (View) mPreviewSpeedControl;
                ObjectAnimator leftAnim = ObjectAnimator.ofFloat(v, "translationX", -mPreviewSpeedControlW);
                leftAnim.setDuration(500);
                leftAnim.addListener(new Animator.AnimatorListener() {
                    public void onAnimationRepeat(Animator animation) {}
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
//						mIsSpeedControlAnimating = false;
//                        animatePreviewAdsControl(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
//						mIsSpeedControlAnimating = true;
                    }
                });
                leftAnim.start();
                mPreviewSpeedControlShown = View.GONE;
            }
        }
    }


    private void updateBlackboxPreviewDistanceTime(final float metaDistance, final int time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (mPreviewDrivingDistance != null) {
                    mPreviewDrivingDistance.setText(String.valueOf(metaDistance));
                }

                if (mPreviewDrivingTime != null) {
                    mPreviewDrivingTime.setText(DateUtils.formatElapsedTime(time));
                }
            }
        });

    }

    private void updateBlackboxPreviewEcoLevel(int level) {
        Drawable drawable  = null;

        if (1 == level) {
           drawable = getImageDrawable(R.drawable.color_deepgreen_thick);
        } else if (2 == level || 3 == level) {
            drawable = getImageDrawable(R.drawable.color_yellow_thick);
        } else if (4 == level || 5 == level) {
            drawable = getImageDrawable(R.drawable.color_red_thick);
        }

        setPreviewEcoIndicatorImageDrawable(drawable);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Drawable getImageDrawable(int id) {
        Drawable drawable  = null;
        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            drawable = getDrawable(id);
        } else {
            drawable = getResources().getDrawable(id);
        }

        return drawable;
    }

    private void setPreviewEcoIndicatorImageDrawable(Drawable drawable) {
        if (mPreviewEcoIndicator != null) {
            mPreviewEcoIndicator.setImageDrawable(drawable);
        }
    }

    private void updateBlackboxPreviewLamps(final boolean isSteadySpeed, final boolean isIdling, final boolean isHarshAccel, final boolean isHarshDecel) {
        if (mPreviewSteadySpeedLamp != null) {
            mPreviewSteadySpeedLamp.setEnabled(isSteadySpeed);
        }
        if (mPreviewIdleLamp != null) {
            mPreviewIdleLamp.setEnabled(isIdling);
        }

        setHarshAccelLamp(isHarshAccel);
        setHarshBrakeLamp(isHarshDecel);
    }


    private void setHarshAccelLamp(boolean enabled) {
        if (enabled) {
            if (mPreviewHarshAccelLamp.isEnabled()) {
                mPreviewHarshAccelLamp.removeCallbacks(HarshAccelLampOffRunnable);
            }
            mPreviewHarshAccelLamp.setEnabled(true);
            mPreviewHarshAccelLamp.postDelayed(HarshAccelLampOffRunnable, 5000);
//            if (mTripFragment != null) {
//                mTripFragment.setHarshAccelLamp(true);
//            }

            if (mPreviewHarshBrakeLamp.isEnabled()) {
                mPreviewHarshBrakeLamp.removeCallbacks(HarshBrakeLampOffRunnable);
                mPreviewHarshBrakeLamp.post(HarshBrakeLampOffRunnable);
            }
        }
    }


    private Runnable HarshAccelLampOffRunnable = new Runnable() {

        @Override
        public void run() {
            mPreviewHarshAccelLamp.setEnabled(false);
//            if (mTripFragment != null) {
//                mTripFragment.setHarshAccelLamp(false);
//            }
        }
    };

    private void setHarshBrakeLamp(boolean enabled) {
        if (enabled) {
            if (mPreviewHarshBrakeLamp.isEnabled()) {
                mPreviewHarshBrakeLamp.removeCallbacks(HarshBrakeLampOffRunnable);
            }
            mPreviewHarshBrakeLamp.setEnabled(true);
            mPreviewHarshBrakeLamp.postDelayed(HarshBrakeLampOffRunnable, 5000);
//            if (mTripFragment != null) {
//                mTripFragment.setHarshBrakeLamp(true);
//            }

            if (mPreviewHarshAccelLamp.isEnabled()) {
                mPreviewHarshAccelLamp.removeCallbacks(HarshAccelLampOffRunnable);
                mPreviewHarshAccelLamp.post(HarshAccelLampOffRunnable);
            }
        }
    }

    private Runnable HarshBrakeLampOffRunnable = new Runnable() {

        @Override
        public void run() {
            mPreviewHarshBrakeLamp.setEnabled(false);
//            if (mTripFragment != null) {
//                mTripFragment.setHarshBrakeLamp(false);
//            }
        }
    };

    private FuelType mFuelType;

    protected int calcEcoLevel(ObdData data) {
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

    private void animatePreviewOverspeedWarning(boolean isAnimate) {
        if (isAnimate) {
            if(!mIsAlreadyPreviewOverspeedAnimating) {
                mPreviewOverspeed.setVisibility(View.VISIBLE);
                ViewCompatUtils.setBackground(mPreviewOverspeed, mPreviewAnimDrawableOverspeed);
                mPreviewAnimDrawableOverspeed.start();
                mIsAlreadyPreviewOverspeedAnimating = true;
            }
        } else {
            if(mIsAlreadyPreviewOverspeedAnimating) {
                mIsAlreadyPreviewOverspeedAnimating = false;
                mPreviewAnimDrawableOverspeed.stop();
            }
            mPreviewOverspeed.setVisibility(View.GONE);
        }
    }

    private void setAlpha(int alpha) {
        if (isLandscape()) {

        }
    }

    private boolean isLandscape() {
        return getWindowManager().getDefaultDisplay().getWidth() > getWindowManager().getDefaultDisplay().getHeight();
    }

    private boolean isPortrait() {
        return getWindowManager().getDefaultDisplay().getWidth() < getWindowManager().getDefaultDisplay().getHeight();
    }

    private int getScreenOrientation() {
        return getWindowManager().getDefaultDisplay().getRotation();

    }

    protected boolean isEngineOn() {
        return VehicleEngineStatus.isOnDriving(mVes);
    }


    private void startFloatingHeadIfNeeded() {
        if (mSettingsStore.isFloatingWindowEnabled() && !mIsShowDiagnosticActivty) {
            Intent service = new Intent(getApplicationContext(), FloatingHeadService.class);
            service.putExtra(FloatingHeadService.EXTRA_INTENT, "com.pokevian.intent.ACTION_LAUNCH_DRIVING");
            startService(service);
//                mDrivingService.startFloatingHead("com.pokevian.intent.ACTION_LAUNCH_DRIVING");
        }
    }

}

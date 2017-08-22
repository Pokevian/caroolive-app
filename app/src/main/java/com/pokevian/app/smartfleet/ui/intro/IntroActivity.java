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

package com.pokevian.app.smartfleet.ui.intro;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.model.StorageType;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleList;
import com.pokevian.app.smartfleet.service.SoundEffectService;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.driving.DrivingActivity;
import com.pokevian.app.smartfleet.ui.intro.AutoSignInFragment.AutoSignInCallbacks;
import com.pokevian.app.smartfleet.ui.intro.IntroduceFragment.IntroduceCallbacks;
import com.pokevian.app.smartfleet.ui.main.AutoStartManager;
import com.pokevian.app.smartfleet.ui.main.MainActivity;
import com.pokevian.app.smartfleet.ui.settings.BlackboxSettingActivity;
import com.pokevian.app.smartfleet.ui.setup.LoadVehicleDialogFragment;
import com.pokevian.app.smartfleet.ui.setup.SetupActivity;
import com.pokevian.app.smartfleet.ui.setup.UpdateVehicleFragmentNew;
import com.pokevian.app.smartfleet.util.FacebookUtils;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.caroo.common.model.TwoState;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;

public class IntroActivity extends BaseActivity
        implements IntroduceCallbacks, AutoSignInCallbacks, AlertDialogCallbacks, LoadVehicleDialogFragment.LoadVehicleDialogCallbacks,
        UpdateVehicleFragmentNew.UpdateVehicleCallbacks {

    static final String TAG = "IntroActivity";
    final Logger logger = Logger.getLogger(TAG);

    private static final int REQUEST_SETUP = 1;
    private static final int REQUEST_BLACKBOX_SETTING = 2;

    private boolean mSignedIn;
    private boolean mDrivingOnDetect;
    private String mTag;
    private String mAppLink;
    private String mMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

//        DisplayMetrics metrics = new DisplayMetrics();
//        WindowManager mgr = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
//        mgr.getDefaultDisplay().getMetrics(metrics);

//        Logger.getLogger("density").debug(">>>>> " + metrics.density);
//        Logger.getLogger("density").debug(">>>>> " + metrics.densityDpi);

        // Possible work around for market launches. See http://code.google.com/p/android/issues/detail?id=2373
        // for more details. Essentially, the market launches the main activity on top of other activities.
        // we never want this to happen. Instead, we check if we are the root and if not, we finish.
        if (!isTaskRoot()) {
            final Intent intent = getIntent();
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
                logger.warn("The Activity is not the root. Finishing!");
                finish();
                return;
            }
        }

        AutoStartManager.stopAutoStartService(this);

        // imsi
        mDrivingOnDetect = getIntent().getBooleanExtra(MainActivity.EXTRA_ON_DRIVING_DETECTED, false);
        mTag = getIntent().getStringExtra("msgTag");
        mMode = getIntent().getStringExtra("mode");
        mAppLink = getIntent().getStringExtra("appLink");

        Logger.getLogger("fingerpush").trace("" + mTag + "-" + mMode + "-" + mAppLink);

        PushManagerHelper.checkPush(this, mTag, mMode);

        setContentView(R.layout.activity_intro);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = null;

        SettingsStore settingsStore = SettingsStore.getInstance();
        if (!settingsStore.isValidAccount()) {
            logger.trace("Account is not setup");

            if (settingsStore.isDisclaimerAgreed()) {
                onNeedToSetup();
            } else {
                fragment = IntroduceFragment.newInstance();
            }
        } else {
            logger.trace("Request sign-in");

            AuthTarget authTarget = settingsStore.getAuthTarget();
            String loginId = settingsStore.getLoginId();

            fragment = AutoSignInFragment.newInstance(authTarget, loginId);
        }

        if (fragment != null) {
            fm.beginTransaction().replace(R.id.container, fragment)
                    .commit();
        }


        // Start sound effect service for preparing
        startService(new Intent(this, SoundEffectService.class));

        if (BuildConfig.DEBUG) {
            FacebookUtils.printKeyHash(this);
        }

        logger.info("server url=" + ServerUrl.SERVICE_SERVER_BASE_URL);
    }

    @Override
    protected void onDestroy() {
        if (mVehicleServiceConnection != null) {
            unbindService(mVehicleServiceConnection);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETUP) {
            if (SettingsStore.getInstance().isValidAccount()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_FROM_SETUP, true);
                startActivity(intent);
                setResult(RESULT_OK);
            }
            finish();
        } else if (requestCode == REQUEST_BLACKBOX_SETTING) {
            if (mSignedIn) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_FROM_SETUP, true);
                startActivity(intent);
                setResult(RESULT_OK);
            }

            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onNeedToSetup() {
        logger.debug("onNeedToSetup()");

        Intent intent = new Intent(this, SetupActivity.class);
        Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
        ActivityCompat.startActivityForResult(this, intent, REQUEST_SETUP, animOptions);
    }

    @Override
    public void onAutoSignedIn(String accountId, String loginId, String imageUrl) {
        logger.debug("onAutoSignedIn(): accountId=" + accountId + ", loginId=" + loginId
                + ", imageUrl=" + imageUrl);
        mSignedIn = true;

        SettingsStore settingsStore = SettingsStore.getInstance();

        // updateLaunchButton account image url
        settingsStore.storeAccountImageUrl(imageUrl);

        if (settingsStore.isValidVehicle() && TextUtils.isEmpty(settingsStore.getVehicle().getModel().getEngineCode())) {
            LoadVehicleDialogFragment.newInstance().show(getSupportFragmentManager(), LoadVehicleDialogFragment.TAG);
        }

        if (!settingsStore.isUpdatedObdAddress() && settingsStore.isValidVehicle()) {
            try {
                String ver = settingsStore.getAccount().getAppVer();
                Logger.getLogger(TAG).debug("onAutoSignedIn#" + ver);
                if (ver.contains("-SNAPSHOT")) {
                    ver = ver.substring(0, ver.indexOf("-"));
                }
                String[] codes = ver.split("\\.");
                if (Integer.parseInt(codes[0]) < 2 && Integer.parseInt(codes[1]) < 6) {
                    UpdateVehicleFragmentNew.newInstance(accountId, settingsStore.getVehicle())
                            .show(getSupportFragmentManager(), UpdateVehicleFragmentNew.TAG);
                } else {
                    settingsStore.storeUpdatedObdAddress(true);
                }
            } catch (NullPointerException e) {
            } catch (NumberFormatException e) {
            }
        }

        if (!isFinishing()) {
            if (checkStorage()) {
                Intent intent = null;
                if (mDrivingOnDetect) {
                    intent = new Intent(this, DrivingActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                    intent.putExtra("appLink", mAppLink);
                    intent.putExtra("msgTag", mTag);
                    intent.putExtra("mode", mMode);
                }
//                Intent intent = new Intent(this, MainActivity.class);
//                intent.putExtra(MainActivity.EXTRA_ON_DRIVING_DETECTED, mDrivingOnDetect);
                startActivity(intent);

                finish();
            }
        }
    }

    private ServiceConnection mVehicleServiceConnection;
    @Override
    public void onAutoSignInFailed(String loginId) {
        logger.warn("onAutoSignInFailed(): loginId=" + loginId);
        mSignedIn = false;

        if (!isFinishing()) {
            if (isVehicleServiceRunning()) {
                mVehicleServiceConnection = new VehicleServiceConnection();
                bindService(new Intent(IntroActivity.this, VehicleService.class), mVehicleServiceConnection, 0);
            } else {
//                AutoConnectService.setAlarm(getApplicationContext(), Consts.AUTO_CONNECT_WAKEUP_DELAY);
                AutoStartManager.startAutoStartService(IntroActivity.this);
                finish();
            }
        }
    }

    private boolean isVehicleServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (VehicleService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private class VehicleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            VehicleService service = ((VehicleService.VehicleServiceBinder) binder).getService();
            if (service != null) {
                service.disconnectVehicle();
            }

            try {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mVehicleServiceConnection != null) {
                            unbindService(mVehicleServiceConnection);
                            mVehicleServiceConnection = null;
                        }

                        stopService(new Intent(IntroActivity.this, VehicleService.class));

//                        AutoConnectService.setAlarm(getApplicationContext(), Consts.AUTO_CONNECT_WAKEUP_DELAY);
//                        startService(new Intent(IntroActivity.this, ScreenMonitorService.class));
                        AutoStartManager.startAutoStartService(IntroActivity.this);

                        finish();
                    }
                }, 500);

            } catch (Exception e) {
                logger.error(e.getMessage());
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private boolean checkStorage() {
        SettingsStore settingsStore = SettingsStore.getInstance();
        if (settingsStore.isBlackboxEnabled()) {
            StorageType storageType = settingsStore.getBlackboxStorageType();

            File[] dirs = StorageUtils.getExternalFilesDirs(this, null);
            if (dirs == null) {
                Toast.makeText(this, R.string.intro_blackbox_storage_error, Toast.LENGTH_LONG).show();
                return false;
            } else if (dirs.length <= storageType.ordinal()) {
                logger.warn("Target storage is not mounted: " + storageType);
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_blackbox_storage_warning),
                        getString(R.string.dialog_message_blackbox_storage_warning),
                        getString(R.string.btn_ok));
                fragment.show(getSupportFragmentManager(), "blackbox-storage-warning-dialog");
                return false;
            }
        }

        return true;
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("blackbox-storage-warning-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                Intent intent = new Intent(this, BlackboxSettingActivity.class);
                startActivityForResult(intent, REQUEST_BLACKBOX_SETTING);
            }
        }
    }


    @Override
    public void onLoadVehicleSuccess(VehicleList list) {
        logger.debug("onLoadVehicleSuccess(): list=" + list);

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

        if (vehicle != null && SettingsStore.getInstance().isValidVehicle()) {
            Vehicle v =  SettingsStore.getInstance().getVehicle();
            v.setModel(vehicle.getModel());
            SettingsStore.getInstance().storeVehicle(v);

            if (vehicle.getObdAddress() == null) {

            }
        }
    }

    @Override
    public void onLoadVehicleFailure() {

    }


    @Override
    public void onUpdateVehicleSuccess(DialogFragment fragment, Vehicle vehicle) {
        Logger.getLogger(TAG).debug("onUpdateVehicleSuccess#" + vehicle.toString());
        SettingsStore.getInstance().storeUpdatedObdAddress(true);
    }

    @Override
    public void onUpdateVehiclerFailure(DialogFragment fragment, Vehicle vehicle) {
        Logger.getLogger(TAG).debug("onUpdateVehiclerFailure#" + vehicle.toString());
    }
}

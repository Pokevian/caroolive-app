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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleList;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.settings.BlackboxSettingActivity;
import com.pokevian.app.smartfleet.ui.settings.GeneralSettingActivity;
import com.pokevian.app.smartfleet.ui.settings.VehicleSettingActivity;
import com.pokevian.app.smartfleet.ui.setup.BlackboxInitDialogFragment.BlackboxInitiCallbacks;
import com.pokevian.app.smartfleet.ui.setup.DisclaimerFragment.DisclaimerCallbacks;
import com.pokevian.app.smartfleet.ui.setup.RegisterAccountFragment.RegisterAccountCallbacks;
import com.pokevian.app.smartfleet.ui.setup.SignInFragment.SignInCallbacks;
import com.pokevian.caroo.common.model.TwoState;

import org.apache.log4j.Logger;

import java.util.ArrayList;

public class SetupActivity extends BaseActivity
        implements DisclaimerCallbacks, SignInCallbacks, RegisterAccountCallbacks,
        AlertDialogCallbacks, BlackboxInitiCallbacks , LoadVehicleDialogFragment.LoadVehicleDialogCallbacks {

    static final String TAG = "SetupActivity";
    final Logger logger = Logger.getLogger(TAG);

    private static final int REQUEST_WELCOME = 1;
    private static final int REQUEST_SETUP_VEHICLE = 2;
    private static final int REQUEST_SETUP_BLACKBOX = 3;
    private static final int REQUEST_GENERAL_SETTING = 4;

    private SettingsStore mSettingsStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup);

        mSettingsStore = SettingsStore.getInstance();

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            if (mSettingsStore.isDisclaimerAgreed()) {
                Fragment fragment = SignInFragment.newInstance();
                fm.beginTransaction().replace(R.id.container, fragment, SignInFragment.TAG)
                        .commit();
            } else {
                Fragment fragment = DisclaimerFragment.newInstance();
                fm.beginTransaction().replace(R.id.container, fragment, DisclaimerFragment.TAG)
                        .commit();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        logger.debug("onActivityResult(): requestCode=" + requestCode);

        if (requestCode == REQUEST_WELCOME) {
            if (data != null && data.getBooleanExtra(WelcomeActivity.EXTRA_NEED_TO_REGISTER_VEHICLE, false)) {
                Intent intent = new Intent(this, VehicleSettingActivity.class);
                Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                ActivityCompat.startActivityForResult(this, intent, REQUEST_SETUP_VEHICLE, animOptions);
            } else if (BuildConfig.INCLUDE_BLACKBOX){
                FragmentManager fm = getSupportFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_enable_blackbox),
                        getString(R.string.dialog_message_enable_blackbox),
                        getString(R.string.btn_no), getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "enable-blackbox-dialog").commitAllowingStateLoss();
            }
        } else if (requestCode == REQUEST_SETUP_VEHICLE) {
            if (BuildConfig.INCLUDE_BLACKBOX) {
                FragmentManager fm = getSupportFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_enable_blackbox),
                        getString(R.string.dialog_message_enable_blackbox),
                        getString(R.string.btn_no), getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "enable-blackbox-dialog").commitAllowingStateLoss();
            } else {
                Intent intent = new Intent(this, GeneralSettingActivity.class);
                Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                ActivityCompat.startActivityForResult(this, intent, REQUEST_GENERAL_SETTING, animOptions);
            }
        } else if (requestCode == REQUEST_SETUP_BLACKBOX) {
            Intent intent = new Intent(this, GeneralSettingActivity.class);
            Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                    R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
            ActivityCompat.startActivityForResult(this, intent, REQUEST_GENERAL_SETTING, animOptions);
        } else if (requestCode == REQUEST_GENERAL_SETTING) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = AlertDialogFragment.newInstance(
                    getString(R.string.dialog_title_setup_finished),
                    getString(R.string.dialog_message_setup_finished),
                    null, getString(R.string.btn_ok), null);
            fragment.setCancelable(false);
            fm.beginTransaction().add(fragment, "setup-finished-dialog").commitAllowingStateLoss();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();

        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDisclaimerAgreed() {
        logger.debug("onDisclaimerAgreed()");

        PushManagerHelper.setDevice(this);

        SettingsStore settingStore = SettingsStore.getInstance();
        settingStore.storeDisclaimerAgreed(true);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = SignInFragment.newInstance();
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.container, fragment, SignInFragment.TAG)
                .commit();
    }

    @Override
    public void onAccountSignedIn(AuthTarget authTarget, String accountId, String loginId, String name, String imageUrl) {
        logger.debug("onAccountSignedIn(): authTarget=" + authTarget + ", accountId=" + accountId
                + ", loginId=" + loginId);

        if (mSettingsStore.getLoginId() != null && !loginId.equals(mSettingsStore.getLoginId())) {
            logger.warn("different loginId!: previous id=" + mSettingsStore.getLoginId());

            // clear previous vehicle
            mSettingsStore.storeVehicleId(null);
        }

        mSettingsStore.storeAuthTarget(authTarget);
        mSettingsStore.storeAccountId(accountId);
        mSettingsStore.storeLoginId(loginId);
        mSettingsStore.storeAccountName(name);
        mSettingsStore.storeAccountImageUrl(imageUrl);

        if (!mSettingsStore.isValidVehicle()) {
            FragmentManager fm = getSupportFragmentManager();
            LoadVehicleDialogFragment.newInstance().show(fm, LoadVehicleDialogFragment.TAG);
        }

    }

    @Override
    public void onAccountDeactivated(AuthTarget authTarget, String accountId, String loginId) {
        logger.debug("onAccountDeactivated(): authTarget=" + authTarget
                + ", accountId=" + accountId + ", loginId=" + loginId);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = RegisterAccountFragment.newInstance(authTarget, accountId, loginId);
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, fragment, RegisterAccountFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAccountNotRegistered(AuthTarget authTarget, String loginId) {
        logger.debug("onAccountNotRegistered(): authTarget=" + authTarget
                + ", loginId=" + loginId);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = RegisterAccountFragment.newInstance(authTarget, null, loginId);
        fm.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.container, fragment, RegisterAccountFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNewAccountRegistered(Account newAccount) {
        logger.debug("onNewAccountRegistered(): newAccount=" + newAccount);

        // Store account information
        mSettingsStore.storeAuthTarget(newAccount.getAuthTarget());
        mSettingsStore.storeLoginId(newAccount.getLoginId());
        mSettingsStore.storeAccountName(newAccount.getNickName());
        mSettingsStore.storeAccountId(newAccount.getAccountId());

        mSettingsStore.storeAccount(newAccount);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fm.findFragmentByTag(RegisterAccountFragment.TAG));
        ft.commitAllowingStateLoss();

        Intent intent = new Intent(this, WelcomeActivity.class);
        Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
        ActivityCompat.startActivityForResult(this, intent, REQUEST_WELCOME, animOptions);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("no-vehicle-registered-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                Intent intent = new Intent(this, VehicleSettingActivity.class);
                Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                ActivityCompat.startActivityForResult(this, intent, REQUEST_SETUP_VEHICLE, animOptions);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                if (BuildConfig.INCLUDE_BLACKBOX) {
                    FragmentManager fm = getSupportFragmentManager();
                    fragment = AlertDialogFragment.newInstance(
                            getString(R.string.dialog_title_enable_blackbox),
                            getString(R.string.dialog_message_enable_blackbox),
                            getString(R.string.btn_no), getString(R.string.btn_yes));
                    fm.beginTransaction().add(fragment, "enable-blackbox-dialog").commitAllowingStateLoss();
                } else {
                    Intent intent = new Intent(this, GeneralSettingActivity.class);
                    Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                            R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                    ActivityCompat.startActivityForResult(this, intent, REQUEST_GENERAL_SETTING, animOptions);
                }
            }
        } else if ("enable-blackbox-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                // Enable blackbox
                SettingsStore settingStore = SettingsStore.getInstance();
                settingStore.storeBlackboxEnabled(true);

                FragmentManager fm = getSupportFragmentManager();
                fragment = BlackboxInitDialogFragment.newInstance();
                fm.beginTransaction().add(fragment, BlackboxInitDialogFragment.TAG).commit();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                Intent intent = new Intent(this, GeneralSettingActivity.class);
                Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                ActivityCompat.startActivityForResult(this, intent, REQUEST_GENERAL_SETTING, animOptions);
            }
        } else if ("setup-finished-dialog".equals(tag)) {
            finish();
        }
    }

    @Override
    public void onBlackboxInitialized() {
        logger.debug("onBlackboxInitialized()");

        // Setup blackbox
        Intent intent = new Intent(this, BlackboxSettingActivity.class);
        Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
        ActivityCompat.startActivityForResult(this, intent, REQUEST_SETUP_BLACKBOX, animOptions);
    }

    public void onSetupFinished() {
        logger.debug("onSetupFinished()");

        finish();
    }

    @Override
    public void onLoadVehicleSuccess(VehicleList list) {
        if (list != null) {
            ArrayList<Vehicle> vehicles = list.getList();
            if (vehicles != null) {
                // Pick first active vehicle (should be only one active vehicle for now)
                for (Vehicle v : vehicles) {
                    if (TwoState.Y.name().equals(v.getActiveCode())) {
                        Logger.getLogger(TAG).debug("vehicle#" + v.getModel().getModelName() + "@" + v.toString());
                        if (v.getObdAddress() != null && !"0".equals(v.getObdAddress())) {
                            mSettingsStore.storeUpdatedObdAddress(true);
                        }

                        // Store vehicle
                        mSettingsStore.storeVehicle(v);

                        // Add vehicle id
                        mSettingsStore.addVehicleId(v.getVehicleId());

                        // Change current vehicle id
                        mSettingsStore.storeVehicleId(v.getVehicleId());
                        break;
                    }

                }
            }
        }

        if (!mSettingsStore.isValidVehicle()) {
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment fragment = AlertDialogFragment.newInstance(
                    getString(R.string.dialog_title_need_register_vehicle),
                    getString(R.string.dialog_message_need_register_vehicle),
                    getString(R.string.btn_no), getString(R.string.btn_yes));
            fm.beginTransaction().add(fragment, "no-vehicle-registered-dialog").commitAllowingStateLoss();
        } else {
            if (BuildConfig.INCLUDE_BLACKBOX) {
                FragmentManager fm = getSupportFragmentManager();
                Fragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_enable_blackbox),
                        getString(R.string.dialog_message_enable_blackbox),
                        getString(R.string.btn_no), getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "enable-blackbox-dialog").commitAllowingStateLoss();
            } else {
                Intent intent = new Intent(this, GeneralSettingActivity.class);
                Bundle animOptions = ActivityOptionsCompat.makeCustomAnimation(this,
                        R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
                ActivityCompat.startActivityForResult(this, intent, REQUEST_GENERAL_SETTING, animOptions);
            }
        }
    }

    @Override
    public void onLoadVehicleFailure() {

    }
}

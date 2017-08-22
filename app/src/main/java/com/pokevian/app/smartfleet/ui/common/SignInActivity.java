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

package com.pokevian.app.smartfleet.ui.common;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.FacebookAuthFragment.FacebookAuthCallbacks;
import com.pokevian.app.smartfleet.ui.common.GoogleAuthFragment.GoogleAuthCallbacks;
import com.pokevian.app.smartfleet.ui.common.ServerSignInFragment.ServerSignInCallbacks;

import org.apache.log4j.Logger;

public class SignInActivity extends BaseActivity implements GoogleAuthCallbacks, FacebookAuthCallbacks,
        ServerSignInCallbacks, AlertDialogCallbacks {

    static final String TAG = "SignInActivity";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_AUTH_TARGET = "extra.AUTH_TARGET";
    public static final String EXTRA_LOGIN_ID = "extra.LOGIN_ID";
    public static final String EXTRA_ACCOUNT_ID = "extra.ACCOUNT_ID";
    public static final String EXTRA_ACCOUNT_NAME = "extra.ACCOUNT_NAME";
    public static final String EXTRA_ACCOUNT_IMAGE_URL = "extra.ACCOUNT_IMAGE_URL";
    public static final String EXTRA_AUTHENTICATED = "extra.AUTHENTICATED";
    public static final String EXTRA_DEACTIVATED = "extra.DEACTIVATED";
    public static final String EXTRA_SIGNED_IN = "extra.SIGNED_IN";

    private AuthTarget mAuthTarget;
    private String mLoginId;

    private Intent mResult = new Intent();
    private int mLoginTryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent data = getIntent();
            mAuthTarget = (AuthTarget) data.getSerializableExtra(EXTRA_AUTH_TARGET);
            mLoginId = data.getStringExtra(EXTRA_LOGIN_ID);
        } else {
            mAuthTarget = (AuthTarget) savedInstanceState.getSerializable(EXTRA_AUTH_TARGET);
            mLoginId = savedInstanceState.getString(EXTRA_LOGIN_ID);
        }

        mResult.putExtra(EXTRA_AUTH_TARGET, mAuthTarget);
        mResult.putExtra(EXTRA_LOGIN_ID, mLoginId);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        // Remove retained fragment
        Fragment fragment = fm.findFragmentByTag(GoogleAuthFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = fm.findFragmentByTag(FacebookAuthFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }

        // And add new fragment!
        if (mAuthTarget == AuthTarget.GOOGLE) {
            logger.debug("Request google auth");
            fragment = GoogleAuthFragment.newInstance(mLoginId);
            ft.add(fragment, GoogleAuthFragment.TAG);
        } else if (mAuthTarget == AuthTarget.FACEBOOK) {
            logger.debug("Request facebook auth");
            fragment = FacebookAuthFragment.newInstance(mLoginId);
            ft.add(fragment, FacebookAuthFragment.TAG);
        } else {
            finish();
        }

        ft.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_AUTH_TARGET, mAuthTarget);
        outState.putString(EXTRA_LOGIN_ID, mLoginId);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        logger.debug("onActivityResult(): requestCode=" + requestCode);

        if (requestCode == GoogleAuthFragment.REQUEST_GOOGLE_AUTH) {
            // Pass REQUEST_GOOGLE_AUTH through
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentByTag(GoogleAuthFragment.TAG);
            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onGoogleAuthSuccess(GoogleAuthFragment fragment, String loginId, String imageUrl) {
        logger.debug("onGoogleAuthSuccess(): loginId=" + loginId);

        mResult.putExtra(EXTRA_AUTHENTICATED, true);
        mResult.putExtra(EXTRA_LOGIN_ID, loginId);
        mResult.putExtra(EXTRA_ACCOUNT_IMAGE_URL, imageUrl);
        mLoginId = loginId;

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.add(ServerSignInFragment.newInstance(loginId), ServerSignInFragment.TAG);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onGoogleAuthFailure(GoogleAuthFragment fragment) {
        logger.warn("onGoogleAuthFailure()");

        mResult.putExtra(EXTRA_AUTHENTICATED, false);

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.add(AlertDialogFragment.newInstance(mLoginId,
                    getString(R.string.dialog_message_cannot_sign_in_google),
                    getString(R.string.btn_no), getString(R.string.btn_yes))
                    , "google-auth-failure-dialog");
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onFacebookAuthSuccess(FacebookAuthFragment fragment, String loginId, String imageUrl) {
        logger.debug("onFacebookAuthSuccess(): loginId=" + loginId);

        mResult.putExtra(EXTRA_AUTHENTICATED, true);
        mResult.putExtra(EXTRA_LOGIN_ID, loginId);
        mResult.putExtra(EXTRA_ACCOUNT_IMAGE_URL, imageUrl);
        mLoginId = loginId;

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.add(ServerSignInFragment.newInstance(loginId), ServerSignInFragment.TAG);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onFacebookAuthFailure(FacebookAuthFragment fragment) {
        logger.warn("onFacebookAuthFailure()");

        mResult.putExtra(EXTRA_AUTHENTICATED, false);

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.add(AlertDialogFragment.newInstance(mLoginId,
                    getString(R.string.dialog_message_cannot_sign_in_facebook),
                    getString(R.string.btn_no), getString(R.string.btn_yes))
                    , "facebook-auth-failure-dialog");
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onSignInSuccess(ServerSignInFragment fragment, Account account) {
        logger.debug("onSignInSuccess(): account=" + account);

        if (!PushManagerHelper.setDeviceCompat(this, account)) {
            PushManagerHelper.setIdentity(this, account.getAccountId());
            PushManagerHelper.updateMemberTag(this, account);

        }

        SettingsStore.getInstance().storeAccount(account);

        mResult.putExtra(EXTRA_SIGNED_IN, true);
        mResult.putExtra(EXTRA_ACCOUNT_ID, account.getAccountId());
        mResult.putExtra(EXTRA_ACCOUNT_NAME, account.getNickName());

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commitAllowingStateLoss();

            setResult(RESULT_OK, mResult);
            finish();
        }
    }

    @Override
    public void onSignInDeactivated(ServerSignInFragment fragment, Account account) {
        logger.debug("onSignInDeactivated(): account=" + account);

        mResult.putExtra(EXTRA_DEACTIVATED, true);
        mResult.putExtra(EXTRA_ACCOUNT_ID, account.getAccountId());

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commitAllowingStateLoss();

            setResult(RESULT_OK, mResult);
            finish();
        }
    }

    @Override
    public void onSignInNotRegistered(ServerSignInFragment fragment, String loginId) {
        logger.warn("onSignInNotRegistered(): loginId=" + loginId);

        mResult.putExtra(EXTRA_SIGNED_IN, false);

        if (!isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.commitAllowingStateLoss();

            setResult(RESULT_OK, mResult);
            finish();
        }
    }

    @Override
    public void onSignInFailure(ServerSignInFragment fragment, final String loginId) {

        logger.warn("onSignInFailure#loginId=" + loginId + ": " + mLoginTryCount);
        if (isFinishing()) return;

        if (++mLoginTryCount < 10) {
            CountDownDialog dialog = new CountDownDialog(SignInActivity.this, loginId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (DialogInterface.BUTTON_POSITIVE == which) {
                        FragmentManager fm = getSupportFragmentManager();
                        fm.beginTransaction().add(ServerSignInFragment.newInstance(mLoginId),
                                ServerSignInFragment.TAG).commitAllowingStateLoss();
                    } else if (DialogInterface.BUTTON_NEGATIVE == which) {
                        dialog.dismiss();
                        finish();
                    }
                }
            });
            dialog.show();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.remove(fragment);
            ft.add(AlertDialogFragment.newInstance(loginId,
                    getString(R.string.dialog_message_cannot_sign_in),
                    getString(R.string.btn_no), getString(R.string.btn_yes))
                    , "server-sign-in-failure-dialog");
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("google-auth-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction().add(GoogleAuthFragment.newInstance(mLoginId),
                        GoogleAuthFragment.TAG).commit();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            }
        } else if ("facebook-auth-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction().add(FacebookAuthFragment.newInstance(mLoginId),
                        FacebookAuthFragment.TAG).commit();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            }
        } else if ("server-sign-in-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                FragmentManager fm = getSupportFragmentManager();
                fm.beginTransaction().add(ServerSignInFragment.newInstance(mLoginId),
                        ServerSignInFragment.TAG).commit();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finish();
            }
        }
    }

}

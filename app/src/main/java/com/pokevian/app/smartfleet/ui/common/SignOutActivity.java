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

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.webkit.CookieManager;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.request.SignOutRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.FacebookAuthFragment.FacebookAuthCallbacks;
import com.pokevian.app.smartfleet.ui.common.GoogleAuthFragment.GoogleAuthCallbacks;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

public class SignOutActivity extends BaseActivity implements GoogleAuthCallbacks, FacebookAuthCallbacks {

    static final String TAG = "SignOutActivity";
    final Logger logger = Logger.getLogger(TAG);

    private AuthTarget mAuthTarget;
    private String mLoginId;
    private SettingsStore mSettingsStore;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueueForCookie();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsStore = SettingsStore.getInstance();
        mAuthTarget = mSettingsStore.getAuthTarget();
        mLoginId = mSettingsStore.getLoginId();

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = null;

        if (savedInstanceState == null) {
            fragment = SignOutProgressDialogFrament.newInstance();
            ft.add(fragment, SignOutProgressDialogFrament.TAG);
        }

        // Remove retained fragment if needed
        fragment = fm.findFragmentByTag(GoogleAuthFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = fm.findFragmentByTag(FacebookAuthFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }

        ft.commit();

        SignOutRequest request = new SignOutRequest(new SignOutListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Prevent back key
    }

    @Override
    public void onGoogleAuthSuccess(GoogleAuthFragment fragment, String loginId, String imageUrl) {
        onSignedOut();
    }

    @Override
    public void onGoogleAuthFailure(GoogleAuthFragment fragment) {
        finish();
    }

    @Override
    public void onFacebookAuthSuccess(FacebookAuthFragment fragment, String loginId, String imageUrl) {
        onSignedOut();
    }

    @Override
    public void onFacebookAuthFailure(FacebookAuthFragment fragment) {
        finish();
    }

    private void onSignedOut() {
        // Clear account
        mSettingsStore.storeAccountId(null);

        mSettingsStore.storeAccount(null);

        // Clear cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        setResult(RESULT_OK);
        finish();
    }

    class SignOutListener extends VolleyListener<Boolean> {

        @Override
        public void onResponse(Boolean result) {
            logger.debug("SignOutListener::onResponse(): result=" + result);

            // Add fragment here!
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            if (mAuthTarget == AuthTarget.GOOGLE) {
                Fragment fragment = GoogleAuthFragment.newInstance(mLoginId, true);
                ft.add(fragment, GoogleAuthFragment.TAG);
            } else if (mAuthTarget == AuthTarget.FACEBOOK) {
                Fragment fragment = FacebookAuthFragment.newInstance(mLoginId, true);
                ft.add(fragment, FacebookAuthFragment.TAG);
            }

            ft.commitAllowingStateLoss();
        }

        @Override
        public void onErrorResponse(VolleyError arg0) {
            finish();
        }

    }

    public static class SignOutProgressDialogFrament extends DialogFragment {

        public static final String TAG = "sign-out-progress-dialog";

        public static SignOutProgressDialogFrament newInstance() {
            SignOutProgressDialogFrament fragment = new SignOutProgressDialogFrament();
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new WaitForDialog(getActivity());
            dialog.setCancelable(false);
            return dialog;
        }

    }

}

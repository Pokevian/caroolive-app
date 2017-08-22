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
import com.pokevian.app.smartfleet.request.WithdrawalRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

public class WithdrawalActivity extends BaseActivity {

    static final String TAG = "WithdrawalActivity";
    final Logger logger = Logger.getLogger(TAG);

    private AuthTarget mAuthTarget;
    private String mLoginId;
    private SettingsStore mSettingsStore;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueueForCookie();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsStore = SettingsStore.getInstance();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(WithdrawProgressDialogFragment.TAG);
        if (fragment != null) {
            WithdrawProgressDialogFragment.newInstance().show(fm, WithdrawProgressDialogFragment.TAG);
        }

        WithdrawalRequest request = new WithdrawalRequest(mSettingsStore.getAccountId(), new VolleyListener<Boolean>() {
            @Override
            public void onResponse(Boolean response) {
                onWithdrawal();
            }

            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }

    private void onWithdrawal() {
        // Clear account
        mSettingsStore.storeAccountId(null);
        mSettingsStore.storeLoginId(null);
        mSettingsStore.storeAccount(null);

        mSettingsStore.storeDisclaimerAgreed(false);

        // Clear cookie
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        setResult(RESULT_OK);
        finish();
    }

    public static class WithdrawProgressDialogFragment extends DialogFragment {

        public static final String TAG = "WITHDRAW-PROGRESS";

        public static WithdrawProgressDialogFragment newInstance() {
            WithdrawProgressDialogFragment fragment = new WithdrawProgressDialogFragment();
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

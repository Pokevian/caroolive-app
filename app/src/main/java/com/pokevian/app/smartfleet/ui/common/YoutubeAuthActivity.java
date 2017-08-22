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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.api.services.youtube.YouTubeScopes;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseActivity;

import org.apache.log4j.Logger;

public class YoutubeAuthActivity extends BaseActivity implements ConnectionCallbacks, OnConnectionFailedListener {

    static final String TAG = "YoutubeAuthActivity";
    final Logger logger = Logger.getLogger(TAG);

    public static final String EXTRA_ACCOUNT_NAME = "extra.ACCOUNT_NAME";

    public static final int REQUEST_YOUTUBE_AUTH = 1001;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = newGoogleApiClient();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            // Clear default account!
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);

            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_YOUTUBE_AUTH) {
            logger.debug("onActivityResult(): REQUEST_YOUTUBE_AUTH=" + resultCode);
            if (resultCode == Activity.RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                finish(null);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        logger.warn("onConnectionFailed(): hasResolution=" + result.hasResolution());

        if (result.hasResolution()) {
            try {
                startIntentSenderForResult(result.getResolution().getIntentSender(),
                        REQUEST_YOUTUBE_AUTH, null, 0, 0, 0);
            } catch (SendIntentException e) {
                logger.warn("Sign in intent could not be sent: error=" + e.getMessage());
                mGoogleApiClient.connect();
            }
        } else {
            DialogFragment fragment = GooglePlayServiceErrorDialogFragment.newInstance(result.getErrorCode());
            fragment.show(getSupportFragmentManager(), GooglePlayServiceErrorDialogFragment.TAG);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        String accountName = Plus.AccountApi.getAccountName(mGoogleApiClient);
        logger.debug("onConnected(): accountName=" + accountName);

        finish(accountName);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        logger.debug("onConnectionSuspended()");

        mGoogleApiClient.connect();
    }

    private void finish(String accountName) {
        if (accountName != null) {
            Intent result = new Intent();
            result.putExtra(EXTRA_ACCOUNT_NAME, accountName);
            setResult(RESULT_OK, result);
        }
        finish();
    }

    private GoogleApiClient newGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(YouTubeScopes.YOUTUBE_UPLOAD))
                .build();
    }

    public void onGoogleAuthFailure() {
        finish(null);
    }

    public static class GooglePlayServiceErrorDialogFragment extends DialogFragment {

        public static final String TAG = "google-play-service-error-dialog";

        private int mErrorCode;

        public static GooglePlayServiceErrorDialogFragment newInstance(int errorCode) {
            GooglePlayServiceErrorDialogFragment fragment = new GooglePlayServiceErrorDialogFragment();
            Bundle args = new Bundle();
            args.putInt("error_code", errorCode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mErrorCode = args.getInt("error_code");
            } else {
                mErrorCode = savedInstanceState.getInt("error_code");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt("error_code", mErrorCode);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            if (GooglePlayServicesUtil.isUserRecoverableError(mErrorCode)) {
                return GooglePlayServicesUtil.getErrorDialog(mErrorCode, getActivity(),
                        REQUEST_YOUTUBE_AUTH, new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                ((YoutubeAuthActivity) getActivity()).onGoogleAuthFailure();
                            }
                        });
            } else {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.sign_in_google_play_services_error)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((YoutubeAuthActivity) getActivity()).onGoogleAuthFailure();
                            }
                        })
                        .create();
            }
        }

    }
}

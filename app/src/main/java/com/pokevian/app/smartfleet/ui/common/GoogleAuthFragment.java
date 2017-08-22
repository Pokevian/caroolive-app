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
import android.support.v4.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.pokevian.app.smartfleet.R;

import org.apache.log4j.Logger;

public class GoogleAuthFragment extends Fragment implements ConnectionCallbacks, OnConnectionFailedListener {

    public static final String TAG = "GoogleAuthFragment";
    final Logger logger = Logger.getLogger(TAG);

    public static final int REQUEST_GOOGLE_AUTH = 1000;

    private String mLoginId;
    private boolean mRequestSignOut;
    private GoogleApiClient mGoogleApiClient;
    private GoogleAuthCallbacks mCallbacks;

    public static GoogleAuthFragment newInstance(String loginid) {
        return newInstance(loginid, false);
    }

    public static GoogleAuthFragment newInstance(String loginId, boolean requestSignOut) {
        GoogleAuthFragment fragment = new GoogleAuthFragment();
        Bundle args = new Bundle();
        args.putString("login_id", loginId);
        args.putBoolean("request_sign_out", requestSignOut);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (GoogleAuthCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement GoogleSignInCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (GoogleAuthCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement GoogleSignInCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mLoginId = args.getString("login_id");
            mRequestSignOut = args.getBoolean("request_sign_out");
        } else {
            mLoginId = savedInstanceState.getString("login_id");
            mRequestSignOut = savedInstanceState.getBoolean("request_sign_out");
        }

        mGoogleApiClient = newGoogleApiClient(mLoginId);
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            // Clear default account!
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);

            if (mRequestSignOut) {
                logger.debug("revokeAccessAndDisconnect()");
                Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
            }

            mGoogleApiClient.disconnect();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("login_id", mLoginId);
        outState.putBoolean("request_sign_out", mRequestSignOut);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        logger.debug("onActivityResult(): requestCode=" + requestCode);

        if (requestCode == REQUEST_GOOGLE_AUTH) {
            if (resultCode == Activity.RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                mCallbacks.onGoogleAuthFailure(this);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        logger.warn("onConnectionFailed(): hasResolution=" + result.hasResolution());

        if (isDetached() || isRemoving() || !isAdded()) return;

        if (result.hasResolution()) {
            try {
                getActivity().startIntentSenderForResult(result.getResolution().getIntentSender(),
                        REQUEST_GOOGLE_AUTH, null, 0, 0, 0);
            } catch (SendIntentException e) {
                logger.warn("Sign in intent could not be sent: " + e.getMessage());
                mGoogleApiClient.connect();
            }
        } else {
            DialogFragment fragment = GooglePlayServiceErrorDialogFragment.newInstance(result.getErrorCode());
            fragment.show(getChildFragmentManager(), GooglePlayServiceErrorDialogFragment.TAG);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        logger.debug("onConnected()");

        if (isDetached() || isRemoving() || !isAdded()) return;

        String loginId = Plus.AccountApi.getAccountName(mGoogleApiClient);
        logger.debug("google loginId=" + loginId);

        String imageUrl = null;
        Person person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
        if (person != null) {
            Person.Image image = person.getImage();
            if (image != null) {
                imageUrl = image.getUrl();
                logger.debug("google image url=" + imageUrl);
            }
        }

        mCallbacks.onGoogleAuthSuccess(this, loginId, imageUrl);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        logger.debug("onConnectionSuspended()");

        if (isDetached() || isRemoving() || !isAdded()) return;

        mGoogleApiClient.connect();
    }

    public void reset() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
        }
    }

    private GoogleApiClient newGoogleApiClient(String accountName) {
        return new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .setAccountName(accountName)
                .build();
    }

    public void onGoogleAuthFailure() {
        mCallbacks.onGoogleAuthFailure(this);
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
                        REQUEST_GOOGLE_AUTH, new DialogInterface.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                ((GoogleAuthFragment) getParentFragment()).onGoogleAuthFailure();
                            }
                        });
            } else {
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.sign_in_google_play_services_error)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((GoogleAuthFragment) getParentFragment()).onGoogleAuthFailure();
                            }
                        })
                        .create();
            }
        }

    }

    public interface GoogleAuthCallbacks {
        void onGoogleAuthSuccess(GoogleAuthFragment fragment, String loginId, String imageUrl);

        void onGoogleAuthFailure(GoogleAuthFragment fragment);
    }

}

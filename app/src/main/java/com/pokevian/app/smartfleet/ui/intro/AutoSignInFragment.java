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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.ui.common.SignInActivity;

public class AutoSignInFragment extends Fragment {

    private static final int REQUEST_SIGN_IN = 1;
    private static final int MIN_PROCESS_TIME = 3000;

    private AuthTarget mAuthTarget;
    private String mLoginId;
    private AutoSignInCallbacks mCallbacks;

    private ProgressBar mProgressBar;
    private long mProcessTime;

    public static AutoSignInFragment newInstance(AuthTarget authTarget, String loginId) {
        AutoSignInFragment fragment = new AutoSignInFragment();
        Bundle args = new Bundle();
        args.putSerializable("auth_target", authTarget);
        args.putString("login_id", loginId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (AutoSignInCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement SignInCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (AutoSignInCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement SignInCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mAuthTarget = (AuthTarget) args.getSerializable("auth_target");
            mLoginId = args.getString("login_id");
        } else {
            mAuthTarget = (AuthTarget) savedInstanceState.getSerializable("auth_target");
            mLoginId = savedInstanceState.getString("login_id");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("auth_target", mAuthTarget);
        outState.putString("login_id", mLoginId);

        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auto_sign_in, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mProgressBar = (ProgressBar) view.findViewById(R.id.sign_in_progress);
        try {
            PackageInfo pi= getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version)).setText(pi.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = new Intent(getActivity(), SignInActivity.class);
        intent.putExtra(SignInActivity.EXTRA_AUTH_TARGET, mAuthTarget);
        intent.putExtra(SignInActivity.EXTRA_LOGIN_ID, mLoginId);
        startActivityForResult(intent, REQUEST_SIGN_IN);

        mProcessTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK
                    && data.getBooleanExtra(SignInActivity.EXTRA_SIGNED_IN, false)) {
                final String accountId = data.getStringExtra(SignInActivity.EXTRA_ACCOUNT_ID);
                final String imageUrl = data.getStringExtra(SignInActivity.EXTRA_ACCOUNT_IMAGE_URL);

                long diff = SystemClock.elapsedRealtime() - mProcessTime;
                if (diff >= MIN_PROCESS_TIME) {
                    mCallbacks.onAutoSignedIn(accountId, mLoginId, imageUrl);
                } else {
                    mProgressBar.postDelayed(new Runnable() {
                        public void run() {
                            mCallbacks.onAutoSignedIn(accountId, mLoginId, imageUrl);
                        }
                    }, MIN_PROCESS_TIME - diff);
                }
            } else {
                mCallbacks.onAutoSignInFailed(mLoginId);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface AutoSignInCallbacks {
        void onAutoSignedIn(String accountId, String loginId, String imageUrl);
        void onAutoSignInFailed(String loginId);
    }

}

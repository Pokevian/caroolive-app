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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.ui.common.SignInActivity;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;

public class SignInFragment extends Fragment {

    public static final String TAG = "SignInFragment";

    private static final int REQUST_SIGN_IN = 1;

    private SignInCallbacks mCallbacks;

    public static SignInFragment newInstance() {
        SignInFragment fragment = new SignInFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (SignInCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement SignInCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (SignInCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement SignInCallbacks");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.google_auth_btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = SignInProgressDialogFrament.newInstance();
                fragment.show(fm, SignInProgressDialogFrament.TAG);

                Intent intent = new Intent(getActivity(), SignInActivity.class);
                intent.putExtra(SignInActivity.EXTRA_AUTH_TARGET, AuthTarget.GOOGLE);
                startActivityForResult(intent, REQUST_SIGN_IN);
            }
        });

        view.findViewById(R.id.facebook_auth_btn).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = SignInProgressDialogFrament.newInstance();
                fragment.show(fm, SignInProgressDialogFrament.TAG);

                Intent intent = new Intent(getActivity(), SignInActivity.class);
                intent.putExtra(SignInActivity.EXTRA_AUTH_TARGET, AuthTarget.FACEBOOK);
                startActivityForResult(intent, REQUST_SIGN_IN);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUST_SIGN_IN) {
            FragmentManager fm = getChildFragmentManager();
            Fragment fragment = fm.findFragmentByTag(SignInProgressDialogFrament.TAG);
            fm.beginTransaction().remove(fragment).commitAllowingStateLoss();

            if (resultCode == Activity.RESULT_OK) {
                AuthTarget authTarget = (AuthTarget) data.getSerializableExtra(SignInActivity.EXTRA_AUTH_TARGET);
                String accountId = data.getStringExtra(SignInActivity.EXTRA_ACCOUNT_ID);
                String loginId = data.getStringExtra(SignInActivity.EXTRA_LOGIN_ID);
                String name = data.getStringExtra(SignInActivity.EXTRA_ACCOUNT_NAME);
                String imageUrl = data.getStringExtra(SignInActivity.EXTRA_ACCOUNT_IMAGE_URL);
                boolean authenticated = data.getBooleanExtra(SignInActivity.EXTRA_AUTHENTICATED, false);

                if (authenticated) {
                    boolean signedIn = data.getBooleanExtra(SignInActivity.EXTRA_SIGNED_IN, false);

                    if (signedIn) {
                        mCallbacks.onAccountSignedIn(authTarget, accountId, loginId, name, imageUrl);
                    } else {
                        boolean deactivated = data.getBooleanExtra(SignInActivity.EXTRA_DEACTIVATED, false);

                        if (deactivated) {
                            mCallbacks.onAccountDeactivated(authTarget, accountId, loginId);
                        } else {
                            mCallbacks.onAccountNotRegistered(authTarget, loginId);
                        }
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface SignInCallbacks {
        void onAccountSignedIn(AuthTarget authTarget, String accountId, String loginId, String name, String imageUrl);

        void onAccountDeactivated(AuthTarget authTarget, String accountId, String loginId);

        void onAccountNotRegistered(AuthTarget authTarget, String loginId);
    }

    public static class SignInProgressDialogFrament extends DialogFragment {

        public static final String TAG = "sign-in-progress-dialog";

        public static SignInProgressDialogFrament newInstance() {
            SignInProgressDialogFrament fragment = new SignInProgressDialogFrament();
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new WaitForDialog(getActivity());
            setCancelable(false);
            return dialog;
        }

    }

}

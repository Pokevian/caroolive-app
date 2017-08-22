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
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.request.SignInRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.model.TwoState;

import org.apache.log4j.Logger;

public class ServerSignInFragment extends Fragment {

    public static final String TAG = "ServerSignInFragment";
    final Logger logger = Logger.getLogger(TAG);

    private String mLoginId;
    private ServerSignInCallbacks mCallbacks;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();

    public static ServerSignInFragment newInstance(String loginId) {
        ServerSignInFragment fragment = new ServerSignInFragment();
        Bundle args = new Bundle();
        args.putString("login_id", loginId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (ServerSignInCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement ServerSignInCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (ServerSignInCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement ServerSignInCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mLoginId = args.getString("login_id");
        } else {
            mLoginId = savedInstanceState.getString("login_id");
        }
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("login_id", mLoginId);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SignInRequest request = new SignInRequest(mLoginId, new SignInListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private class SignInListener extends VolleyListener<Account> {

        @Override
        public void onResponse(Account account) {
            if (account != null) {
                if (TwoState.Y.name().equals(account.getActiveCode())) {
                    mCallbacks.onSignInSuccess(ServerSignInFragment.this, account);
                } else {
                    mCallbacks.onSignInDeactivated(ServerSignInFragment.this, account);
                }
            } else {
                mCallbacks.onSignInNotRegistered(ServerSignInFragment.this, mLoginId);
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Logger.getLogger(TAG).warn(error.getMessage());
            mCallbacks.onSignInFailure(ServerSignInFragment.this, mLoginId);
        }

    }

    public interface ServerSignInCallbacks {
        void onSignInSuccess(ServerSignInFragment fragment, Account account);

        void onSignInDeactivated(ServerSignInFragment fragment, Account account);

        void onSignInNotRegistered(ServerSignInFragment fragment, String loginId);

        void onSignInFailure(ServerSignInFragment fragment, String loginId);
    }

}

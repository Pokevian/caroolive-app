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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Arrays;

public class FacebookAuthFragment extends Fragment {

    public static final String TAG = "FacebookAuthFragment";
    final Logger logger = Logger.getLogger(TAG);

    private String mLoginId;
    private boolean mRequestSignOut;
    private FacebookAuthCallbacks mCallbacks;

//    private Session.StatusCallback mStatusCallback = new SessionStatusCallback();

    private CallbackManager mCallbackManager;

    public static FacebookAuthFragment newInstance(String loginId) {
        return newInstance(loginId, false);
    }

    public static FacebookAuthFragment newInstance(String loginId, boolean requestSignOut) {
        FacebookAuthFragment fragment = new FacebookAuthFragment();
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
            mCallbacks = (FacebookAuthCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement FacebookAuthCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (FacebookAuthCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement FacebookAuthCallbacks");
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

        logger.debug("onCreate#" + mLoginId);

        FacebookSdk.sdkInitialize(getActivity(), new FacebookSdk.InitializeCallback() {

            @Override
            public void onInitialized() {
                if (AccessToken.getCurrentAccessToken() == null) {
                    logger.debug("not logged in");
                    LoginManager.getInstance().logInWithReadPermissions(FacebookAuthFragment.this, Arrays.asList("public_profile", "email"));
                } else {
                    Profile profile = Profile.getCurrentProfile();
                    mCallbacks.onFacebookAuthSuccess(FacebookAuthFragment.this, mLoginId, null);
                    logger.debug("logged-in#" + profile.toString());
                }
            }
        });

        FacebookSdk.sdkInitialize(getActivity());
        mCallbackManager = CallbackManager.Factory.create();
//        if (AccessToken.getCurrentAccessToken() == null) {
//            logger.debug("not loggined");
//            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
//        }

//        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request;
                request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
//                        if (response.getError() != null) {
//                            logger.debug(response.toString());
//                        } else {
//
//                        }
                        logger.debug(response.toString());
                        String imagUrl = "https://graph.facebook.com/" + object.optString("id") + "/picture";
                        mCallbacks.onFacebookAuthSuccess(FacebookAuthFragment.this, object.optString("email"), imagUrl);
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                logger.debug("onCancel");
                mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
            }

            @Override
            public void onError(FacebookException error) {
                logger.debug("onError");
                mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
            }
        });


//        Session session = Session.getActiveSession();
//        if (session == null) {
//            if (savedInstanceState != null) {
//                session = Session.restoreSession(getActivity(), null, mStatusCallback, savedInstanceState);
//            }
//            if (session == null) {
//                session = new Session(getActivity());
//            }
//            Session.setActiveSession(session);
//            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
//                logger.debug("CREATED_TOKEN_LOADED");
//            }
//        }

//        if (!session.isOpened() && !session.isClosed()) {
//            session.openForRead(new Session.OpenRequest(this)
//                    .setPermissions("email")
//                    .setCallback(mStatusCallback));
//        } else {
//            Session.openActiveSession(getActivity(), this, true, mStatusCallback);
//        }

//        if (session.isOpened()) {
//            Logger.getLogger(TAG).debug("close session and clear token");
//            session.closeAndClearTokenInformation();
//        }
//        Logger.getLogger(TAG).debug("open session");
//        String[] permissions = new String[]{"email"};
//        Session.openActiveSession(getActivity(), true, Arrays.asList(permissions), mStatusCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRequestSignOut) {
//            Session session = Session.getActiveSession();
//            if (session != null && !session.isClosed()) {
//                logger.debug("closeAndClearTokenInformation()");
//                session.closeAndClearTokenInformation();
//                Session.setActiveSession(null);
//            }
            LoginManager.getInstance().logOut();
        }
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        Session.getActiveSession().addCallback(mStatusCallback);
//    }

//    @Override
//    public void onStop() {
//        super.onStop();
//        Session.getActiveSession().removeCallback(mStatusCallback);
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        logger.debug("requestCode#" + requestCode);
        logger.debug("resultCode#" + resultCode);
        logger.debug("data#" + data.toString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("login_id", mLoginId);

        super.onSaveInstanceState(outState);

//        Session session = Session.getActiveSession();
//        Session.saveSession(session, outState);
    }

    /*private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            Logger.getLogger(TAG).debug("call(): state=" + state + ", exception=" + exception);
            if (session.isOpened()) {
                requestMe(session);
            } else if (state == SessionState.CLOSED_LOGIN_FAILED) {
//                finish();
                Toast.makeText(getActivity(), R.string.sign_up_cannot_connect_server,
                        Toast.LENGTH_LONG).show();

                mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
            }
        }
    }*/

    /*private void requestMe(Session session) {
//        if (!isVisible()) {
//            return;
//        }

        Logger.getLogger(TAG).debug("request me");
//        Request request = Request.newMeRequest(session, new MyMeRequestCallback());
//        request.executeAsync();

        Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                if (user != null) {
//                    String firstName = user.getFirstName();
//                    String lastName = user.getLastName();
//                    String id = user.getId();
//                    String email = user.getProperty("email").toString();

                    Logger.getLogger(TAG).debug("facebook id=" + user.getId());
                    Logger.getLogger(TAG).debug("facebook email=" + user.getProperty("email"));
                    Logger.getLogger(TAG).debug("facebook email2=" + user.asMap().get("email"));
                    Logger.getLogger(TAG).debug("facebook email3=" + response.getGraphObject().getProperty("email"));
                }
            }
        });
    }*/

    /*private class MyMeRequestCallback implements Request.GraphUserCallback {
        @Override
        public void onCompleted(GraphUser user, Response response) {
            Logger.getLogger(TAG).debug("user: " + user);
            if (user != null) {
                String id = "f" + user.getId();
                String email = (String) user.getProperty("email");
                Logger.getLogger(TAG).debug("facebook id=" + id);
                Logger.getLogger(TAG).debug("facebook email=" + email);
                Logger.getLogger(TAG).debug("facebook email2=" + user.asMap().get("email"));
                Logger.getLogger(TAG).debug("facebook email3=" + response.getGraphObject().getProperty("email"));

//                Intent data = new Intent();
//                data.putExtra(EXTRA_ID, id);
//                data.putExtra(EXTRA_EMAIL, email);
//                setResult(RESULT_OK, data);
//                finish();

                String imageUrl = "https://graph.facebook.com/"
                        + user.getId() + "/picture";
                logger.debug("facebook image url=" + imageUrl);

                mCallbacks.onFacebookAuthSuccess(FacebookAuthFragment.this, email, imageUrl);
            } else {
                Toast.makeText(getActivity(), R.string.sign_up_cannot_connect_server,
                        Toast.LENGTH_LONG).show();

                mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
            }
        }
    }*/

//    private class SessionStatusCallback implements Session.StatusCallback {
//        @Override
//        public void call(Session session, SessionState state, Exception exception) {
//            logger.debug("call(): state=" + state + ": " + exception);
//
//            if (exception != null) {
//                if (!(exception instanceof FacebookOperationCanceledException)) {
//                    Toast.makeText(getActivity(), R.string.sign_up_cannot_connect_server,
//                            Toast.LENGTH_LONG).show();
//                }
//
//                mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
//            } else if (session.isOpened()) {
//                Request request = Request.newMeRequest(session, new GraphUserCallback() {
//                    public void onCompleted(GraphUser user, Response response) {
//                        if (user != null) {
//                            logger.debug(user.getId() + "#" + user.getProperty("email"));
//                        }
////                        if (user != null && user.getProperty("email") != null) {
//                        if (user != null && user.getId() != null) {
////                            String loginId = (String) user.getProperty("email");
//                            String loginId = (String) user.getId();
//                            loginId = "pokevian.dev@gmail.com";
//                            logger.debug("facebook loginId=" + loginId);
//
//                            String imageUrl = "https://graph.facebook.com/"
//                                    + user.getId() + "/picture";
//                            logger.debug("facebook image url=" + imageUrl);
//
//                            mCallbacks.onFacebookAuthSuccess(FacebookAuthFragment.this,
//                                    loginId, imageUrl);
//                        } else {
//                            logger.error("" + response.getRawResponse() + "@onComplete");
//                            Toast.makeText(getActivity(), R.string.sign_up_cannot_connect_server,
//                                    Toast.LENGTH_LONG).show();
//
//                            mCallbacks.onFacebookAuthFailure(FacebookAuthFragment.this);
//                        }
//                    }
//                });
//                request.executeAsync();
//            }
//        }
//    }

    public interface FacebookAuthCallbacks {
        void onFacebookAuthSuccess(FacebookAuthFragment fragment, String loginId, String imageUrl);

        void onFacebookAuthFailure(FacebookAuthFragment fragment);
    }

}

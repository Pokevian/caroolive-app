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
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEngineType;
import com.pokevian.lib.blackbox.BlackboxProfileInstance;
import com.pokevian.lib.blackbox.BlackboxProfileInstance.VideoResolution;

import org.apache.log4j.Logger;

import java.util.List;

public class BlackboxInitDialogFragment extends DialogFragment {

    public static final String TAG = "BlackboxInitDialogFragment";
    final Logger logger = Logger.getLogger(TAG);

    private BlackboxInitTask mInitTask;
    private BlackboxInitiCallbacks mCallbacks;

    public static BlackboxInitDialogFragment newInstance() {
        BlackboxInitDialogFragment fragment = new BlackboxInitDialogFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (BlackboxInitiCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement BlackboxInitiCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (BlackboxInitiCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement BlackboxInitiCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInitTask = new BlackboxInitTask(getActivity());
        mInitTask.execute();
    }

    @Override
    public void onDestroy() {
        if (mInitTask != null) {
            mInitTask.cancel(true);
        }
        super.onDestroy();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getString(R.string.dialog_message_blackbox_init));
        setCancelable(false);
        return dialog;
    }

    private final class BlackboxInitTask extends AsyncTask<Void, Void, Void> {

        private Context mContext;
        private SettingsStore mSettingsStore;
        private BlackboxProfileInstance mProfile;

        private BlackboxInitTask(Context context) {
            mContext = context;
            mSettingsStore = SettingsStore.getInstance();
        }

        protected Void doInBackground(Void... params) {
            logger.debug("[+] blackbox init task");
            mProfile = BlackboxProfileInstance.getInstance(mContext);
            checkEngineType();
            checkVideoResolution();
            logger.debug("[-] blackbox init task");
            return null;
        }

        protected void onPostExecute(Void result) {
            if (isCancelled()) return;

            dismissAllowingStateLoss();

            mCallbacks.onBlackboxInitialized();
        }

        private void checkEngineType() {
            BlackboxEngineType currEngineType = mSettingsStore.getBlackboxEngineType();
            List<BlackboxEngineType> engineTypes = mProfile.getSupportedEngineList();
            logger.info("Supported engine types=" + engineTypes);
            if (currEngineType == BlackboxEngineType.MEDIA_CODEC
                    && !engineTypes.contains(BlackboxEngineType.MEDIA_CODEC)) {
                logger.warn("Blackbox engine type is changed to MEDIA_RECORDER");
                mSettingsStore.storeBlackboxEngineType(BlackboxEngineType.MEDIA_RECORDER);
            }
        }

        private void checkVideoResolution() {
            VideoResolution currResolution = mSettingsStore.getBlackboxVideoResolution();
            logger.debug("Current video resolutions=" + currResolution);
            BlackboxEngineType currEngineType = mSettingsStore.getBlackboxEngineType();
            List<VideoResolution> resolutions = mProfile.getSupportedVideoSize(currEngineType);
            logger.info("Supported video resolutions=" + resolutions);
            boolean matched = false;
            VideoResolution firstValidResolution = null;
            for (VideoResolution resolution : resolutions) {
                if (!matched
                        && resolution.width == currResolution.width
                        && resolution.height == currResolution.height) {
                    currResolution.isValid = resolution.isValid;
                    matched = true;
                }
                if (firstValidResolution == null && resolution.isValid) {
                    firstValidResolution = resolution;
                }
            }

            logger.info("video resolution matched: " + matched);

            if (!matched) {
                if (firstValidResolution == null) {
                    firstValidResolution = resolutions.get(0);
                }
                currResolution = firstValidResolution;
                logger.warn("Blackbox video resolution is changed to " + currResolution);
            }

            mSettingsStore.storeBlackboxVideoResolution(currResolution);
        }
    }

    public interface BlackboxInitiCallbacks {
        void onBlackboxInitialized();
    }

}

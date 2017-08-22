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

package com.pokevian.app.smartfleet.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import org.apache.log4j.Logger;

public class DrivingOnDialogFragment extends DialogFragment {

    public static final String TAG = "DrivingOnDialogFragment";

    private static final int TARGET_TIMEOUT = 3000; // 5seconds
    private static final int COUNTDOWN_INTERVAL = 100;

    private CountDownTask mCountDownTask;
    private AlertDialogCallbacks mCallbacks;

    public static DrivingOnDialogFragment newInstance() {
        DrivingOnDialogFragment fragment = new DrivingOnDialogFragment();
        return fragment;
    }

    /*@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }*/

    /*@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (AlertDialogCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (AlertDialogCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_title_driving_on)
                .setMessage(getString(R.string.dialog_message_driving_on, TARGET_TIMEOUT / 1000))
                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(DrivingOnDialogFragment.this, which);
                    }
                })
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(DrivingOnDialogFragment.this, which);
                    }
                });
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        mCountDownTask = new CountDownTask();
        mCountDownTask.execute();
    }

    @Override
    public void onStop() {
        if (mCountDownTask != null) {
            mCountDownTask.cancel(true);
            mCountDownTask = null;
        }

        super.onStop();
    }

    /*@Override
    public void onResume() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        super.onResume();
    }*/

    /*@Override
    public void onPause() {
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onPause();
    }*/

//    @Override
//    public void onActivityCreated(Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mCallbacks.onDialogButtonClick(DrivingOnDialogFragment.this, DialogInterface.BUTTON_NEGATIVE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mCountDownTask != null) {
            mCountDownTask.cancel(true);
            mCountDownTask = null;
        }

        super.onDismiss(dialog);
    }

    private class CountDownTask extends AsyncTask<Void, Long, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            long now = SystemClock.elapsedRealtime();
            long target = now + TARGET_TIMEOUT;
            int interval = COUNTDOWN_INTERVAL;

            while (now < target && !isCancelled()) {
                now += interval;
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }
                publishProgress(target - now);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {
                dialog.setMessage(getString(R.string.dialog_message_driving_on,
                        1 + (values[0] / 1000)));
            }
//            Logger.getLogger(TAG).trace("onProgressUpdate#" + values[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                AlertDialog dialog = (AlertDialog) getDialog();
                if (dialog != null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                }
            }
        }

    }

}

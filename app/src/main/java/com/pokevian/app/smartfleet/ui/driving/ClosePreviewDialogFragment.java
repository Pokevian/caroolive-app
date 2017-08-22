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

package com.pokevian.app.smartfleet.ui.driving;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;

public class ClosePreviewDialogFragment extends DialogFragment {

    public static final String TAG = "ClosePreviewDialogFragment";

    private static final int TARGET_TIMEOUT = 10000; // 10 seconds
    private static final int COUNTDOWN_INTERVAL = 100;

    private AlertDialog mSystemDialog;
    private CountDownTask mCountDownTask;
    private AlertDialogCallbacks mCallbacks;

    private DialogInterface.OnDismissListener mOnDismissListener;

    public static ClosePreviewDialogFragment newInstance() {
        ClosePreviewDialogFragment fragment = new ClosePreviewDialogFragment();
        return fragment;
    }

    public void setSystemDialog(AlertDialog dialog) {
        mSystemDialog = dialog;
    }

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
        AlertDialog dialog = mSystemDialog;

        // Create normal alert dialog if not system dialog
        if (dialog == null) {
            dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_System).create();
        }

        dialog.setMessage(getString(R.string.dialog_message_close_preview, TARGET_TIMEOUT / 1000));
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(ClosePreviewDialogFragment.this, which);
                    }
                });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.btn_preview_close),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(ClosePreviewDialogFragment.this, which);
                    }
                });

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
        attrs.alpha = 0.5f; // 50% opaque
        dialog.getWindow().setAttributes(attrs);

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

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mCallbacks.onDialogButtonClick(ClosePreviewDialogFragment.this, DialogInterface.BUTTON_NEGATIVE);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mCountDownTask != null) {
            mCountDownTask.cancel(true);
            mCountDownTask = null;
        }

        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss(dialog);
        }

        super.onDismiss(dialog);
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
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
                dialog.setMessage(getString(R.string.dialog_message_close_preview,
                        1 + (values[0] / 1000)));
            }
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

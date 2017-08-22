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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.PackageUtils;

public class AutoLaunchNaviDialogFragment extends DialogFragment {

    public static final String TAG = "AutoLaunchNaviDialogFragment";

    private static final int TARGET_TIMEOUT = 5000; // 5seconds
    private static final int COUNTDOWN_INTERVAL = 100;

    private String mNaviAppId;
    private CountDownTask mCountDownTask;

    public static AutoLaunchNaviDialogFragment newInstance() {
        AutoLaunchNaviDialogFragment fragment = new AutoLaunchNaviDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNaviAppId = SettingsStore.getInstance().getQuickLaunchNaviApp();
        if (TextUtils.isEmpty(mNaviAppId)) {
            dismiss();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Drawable icon = PackageUtils.loadIcon(getActivity(), mNaviAppId);
        String title = PackageUtils.loadLabel(getActivity(), mNaviAppId);

        return new AlertDialog.Builder(getActivity())
                .setIcon(icon)
                .setTitle(title)
                .setMessage(getString(R.string.dialog_message_quick_launch_navi, TARGET_TIMEOUT / 1000))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        launchApp(mNaviAppId);
                    }
                })
                .create();
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mCountDownTask != null) {
            mCountDownTask.cancel(true);
            mCountDownTask = null;
        }

        super.onDismiss(dialog);
    }

    private void launchApp(String appId) {
        String packageName = PackageUtils.parsePackageName(appId);
        String className = PackageUtils.parseClassName(appId);
        if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)) {
            Intent lauchIntent = new Intent()
                    .setClassName(packageName, className)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            try {
                startActivity(lauchIntent);
            } catch (Exception e) {
            }
        }
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
                dialog.setMessage(getString(R.string.dialog_message_quick_launch_navi,
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

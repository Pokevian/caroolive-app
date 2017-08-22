package com.pokevian.app.smartfleet.ui.common;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;

import com.pokevian.app.smartfleet.R;

/**
 * Created by ian on 2016-12-19.
 */

public class CountDownDialog {

    private static final int TARGET_TIMEOUT = 30000; // 5 seconds
    private static final int COUNTDOWN_INTERVAL = 100;

    private final Context mContext;
    private final String mTitle;
    private AlertDialog mDialog;
    private CountDownTask mCountDownTask;
    private final DialogInterface.OnClickListener mListener;

    public CountDownDialog(Context context, String title, DialogInterface.OnClickListener listener) {
        mContext = context;
        mTitle = title;
        mListener = listener;

        initDialog();
    }

    public void show() {
        mDialog.show();

        // Start count down
        mCountDownTask = new CountDownTask();
        mCountDownTask.execute();
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public AlertDialog createAlertDialog() {

//        return new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_System).create();
        AlertDialog dialog = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_System).create();
        toSystemWindow(dialog.getWindow());
        return dialog;
    }

    private void toSystemWindow(Window window) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        window.setAttributes(lp);
    }

    private void initDialog() {
        mDialog = createAlertDialog();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);

//        mDialog.setTitle(mContext.getString(R.string.dialog_title_driving_exit));
//        mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_exit, TARGET_TIMEOUT / 1000));
        mDialog.setTitle(mTitle);
        mDialog.setMessage(mContext.getString(R.string.dialog_message_cannot_sign_in2, TARGET_TIMEOUT / 1000));

        mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                mListener
               /* new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }*/);
//            mDialog.setButton(DialogInterface.BUTTON_NEUTRAL, mContext.getString(R.string.btn_home),
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            onExitDialogHome();
//                        }
//                    });
        mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_login),
                mListener
                /*new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
//                        onExitDialogExit();
                    }
                }*/);

        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mCountDownTask.cancel(true);
//                onExitDialogDismissed();
            }
        });
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
            mDialog.setMessage(mContext.getString(R.string.dialog_message_cannot_sign_in2,
                    1 + (values[0] / 1000)));
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            }
        }
    }

}

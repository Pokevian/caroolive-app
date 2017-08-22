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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.receiver.BringToFrontReceiver;
import com.pokevian.app.smartfleet.service.floatinghead.FloatingHeadService;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.diagnostic.DiagnosticActivity;
import com.pokevian.app.smartfleet.ui.diagnostic.DiagnosticProcess;
import com.pokevian.app.smartfleet.util.TelephonyUtils;

import org.apache.log4j.Logger;

public class DrivingService extends Service {

    static final String TAG = "DrivingService";
    final Logger logger = Logger.getLogger(TAG);

    public static final String ACTION_GOTO_EXIT
            = "com.pokevian.app.smartfleet.action.GOTO_EXIT";

    public static final String ACTION_GOTO_HOME
            = "com.pokevian.app.smartfleet.action.GOTO_HOME";

    public static final String ACTION_GOTO_PAUSE
            = "com.pokevian.app.smartfleet.action.GOTO_PAUSE";

    public static final String ACTION_READY_TO_EXIT
            = "com.pokevian.app.smartfleet.action.READY_TO_EXIT";

    public static final String ACTION_ERS_TARGET_CHANGED
            = "com.pokevian.app.smartfleet.action.ERS_TARGET_CHANGED";
    public static final String EXTRA_ERS_TARGET
            = "com.pokevian.app.smartfleet.extra.ERS_TARGET";
    public static final String EXTRA_USER_SELECT
            = "com.pokevian.app.smartfleet.extra.USER_SELECT";

    public static final String ACTION_DIALOG_DISMISSED
            = "com.pokevian.app.smartfleet.action.DIALOG_DISMISSED";
    public static final String EXTRA_DIALOG_TYPE
            = "com.pokevian.app.smartfleet.extra.DIALOG_TYPE";

//    public static final String ACTION_GOTO_DIAGNOSTIC = "com.pokevian.app.smartfleet.action.GOTO_DIAGNOSTIC";
    public static final String ACTION_SHOW_DIALOG_DIAGNOSTIC = "com.pokevian.app.smartfleet.action.SHOW_DIALOG_DIAGNOSTIC";

    public static final int DIALOG_EXIT = 1;
    public static final int DIALOG_DRIVING_OFF = 2;
    public static final int DIALOG_WAIT_FOR_EXIT = 3;
    public static final int DIALOG_ERS = 4;
    public static final int DIALOG_OBD_RECOVERY = 5;
    public static final int DIALOG_OBD_DIAGNOSTIC =  6;

    private final int mForegroundId = 1000;

    private BlackboxPreview mBlackboxPreview;

    private LocalBroadcastManager mBroadcastManager;

    @Override
    public void onCreate() {
        logger.debug("onCreate()");

        super.onCreate();

        startForeground(mForegroundId, buildForegroundNotification(
                getString(R.string.noti_driving_content_on_driving)));

        registerBluetoothStateReceiver();

        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        if (mBlackboxPreview == null) {
//            SettingsStore store = SettingsStore.getInstance();
//            if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT
//                    && BlackboxConst.BlackboxEngineType.MEDIA_CODEC == store.getBlackboxEngineType()  && false) {
//                mBlackboxPreview = new BlackboxPreview2(this, R.layout.blackbox_preview2);
//            } else {
//                mBlackboxPreview = new BlackboxPreview(this, R.layout.blackbox_preview);
//            }
            mBlackboxPreview = new BlackboxPreview(this, R.layout.blackbox_preview);

            mBlackboxPreview.show();
        }
    }

    @Override
    public void onDestroy() {
        logger.debug("onDestroy()");

        unregisterBluetoothStateReceiver();

        if (mBlackboxPreview != null) {
            mBlackboxPreview.dismiss();
        }

//        stopFloatingHead();

        dismissExitDialog();
        dismissDrivingOffDialog();
        dismissWaitForExitDialog();
        dismissErsDialog();
        dismissObdRecoveryDialog();
        dismissDiagnosticProcess();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if this service's process is killed while it is started (after returning from onStartCommand),
        // and there are no new start intents to deliver to it, then take the service out of the started state
        // and don't recreate until a future explicit call to Context.startService(Intent).
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new DrivingServiceBinder();
    }

    public void pauseDriving() {
        startForeground(mForegroundId, buildForegroundNotification(
                getString(R.string.noti_driving_content_off_driving)));
    }

    public void resumeDriving() {
        startForeground(mForegroundId, buildForegroundNotification(
                getString(R.string.noti_driving_content_on_driving)));
    }

    public void stopDriving() {
        stopForeground(true);
    }

    private Notification buildForegroundNotification(String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_noti)
                .setContentTitle(getString(R.string.noti_driving_content_title))
                .setContentText(contentText);

        Intent i = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);

        PendingIntent pi = PendingIntent.getBroadcast(this, mForegroundId, i, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(pi);

        return builder.build();
    }

    private void registerBluetoothStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
    }

    private void unregisterBluetoothStateReceiver() {
        unregisterReceiver(mBluetoothReceiver);
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                Logger.getLogger(TAG).info("ACTION_STATE_CHANGED#" + state + "@bluetooth@service") ;
                if (state == BluetoothAdapter.STATE_OFF) {
                    logger.info("TurnOn!!@bluetooth@service");
                    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                    btAdapter.enable();
                }
            }
        }
    };

    public BlackboxPreview getBlackboxPreview() {
        return mBlackboxPreview;
    }

    public AlertDialog createAlertDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_System)
                .create();
        toSystemWindow(dialog.getWindow());

        return dialog;
    }

    public ProgressDialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this, R.style.AppTheme_Dialog_System);
        toSystemWindow(dialog.getWindow());
        return dialog;
    }

    private void toSystemWindow(Window window) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        window.setAttributes(lp);
    }

    public class DrivingServiceBinder extends Binder {
        public DrivingService getService() {
            return DrivingService.this;
        }
    }

    private ExitDialog mExitDialog;

    public void showExitDialog() {
        if (mExitDialog == null) {
            mExitDialog = new ExitDialog(this);
            mExitDialog.show();
        }
    }

    public void dismissExitDialog() {
        if (mExitDialog != null) {
            mExitDialog.dismiss();
        }
    }

    public boolean isExitDialogShowing() {
        if (mExitDialog != null) {
            return mExitDialog.isShowing();
        } else {
            return false;
        }
    }

    public void onExitDialogExit() {
        Intent data = new Intent(ACTION_GOTO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onExitDialogHome() {
        Intent data = new Intent(ACTION_GOTO_HOME);
        mBroadcastManager.sendBroadcast(data);
    }

    private void onExitDialogDismissed() {
        mExitDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    private void onShowDiagnosticDialog() {
        Intent data = new Intent(ACTION_SHOW_DIALOG_DIAGNOSTIC);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ExitDialog {

        private static final int TARGET_TIMEOUT = 3000; // 5 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private final Context mContext;
        private AlertDialog mDialog;
        private CountDownTask mCountDownTask;

        public ExitDialog(Context context) {
            mContext = context;

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

        private void initDialog() {
            mDialog = createAlertDialog();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setTitle(mContext.getString(R.string.dialog_title_driving_exit));
            mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_exit, TARGET_TIMEOUT / 1000));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
//            mDialog.setButton(DialogInterface.BUTTON_NEUTRAL, mContext.getString(R.string.btn_home),
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            onExitDialogHome();
//                        }
//                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_exit),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onExitDialogExit();
                        }
                    });

            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    mCountDownTask.cancel(true);
                    onExitDialogDismissed();
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
                mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_exit,
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

    private DrivingOffDialog mDrivingOffDialog;

    public void showDrivingOffDialog() {
        if (mDrivingOffDialog == null) {
            mDrivingOffDialog = new DrivingOffDialog(this);
            mDrivingOffDialog.show();
        }
    }

    public void dismissDrivingOffDialog() {
        if (mDrivingOffDialog != null) {
            mDrivingOffDialog.dismiss();
        }
    }

    public boolean isDrivingOffDialogShowing() {
        if (mDrivingOffDialog != null) {
            return mDrivingOffDialog.isShowing();
        } else {
            return false;
        }
    }

    public void onDrivingOffDialogExit() {
        Intent data = new Intent(ACTION_GOTO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onDrivingOffDialogPause() {
        Intent data = new Intent(ACTION_GOTO_PAUSE);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onDrivingOffDialogDismissed() {
        mDrivingOffDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_DRIVING_OFF);
        mBroadcastManager.sendBroadcast(data);
    }

    public class DrivingOffDialog {

        private static final int TARGET_TIMEOUT = 3000; // 5 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private final Context mContext;
        private AlertDialog mDialog;
        private CountDownTask mCountDownTask;

        public DrivingOffDialog(Context context) {
            mContext = context;

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

        private void initDialog() {
            mDialog = createAlertDialog();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setTitle(mContext.getString(R.string.dialog_title_driving_off));
            mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_off, TARGET_TIMEOUT / 1000));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
//            mDialog.setButton(DialogInterface.BUTTON_NEUTRAL, mContext.getString(R.string.btn_pause),
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            onDrivingOffDialogPause();
//                        }
//                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_exit),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            onDrivingOffDialogExit();
                        }
                    });

            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    mCountDownTask.cancel(true);
                    onDrivingOffDialogDismissed();
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
                mDialog.setMessage(mContext.getString(R.string.dialog_message_driving_off,
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

    private WaitForExitDialog mWaitForExitDialog;

    public void showWaitForExitDialog() {
        if (mWaitForExitDialog == null) {
            mWaitForExitDialog = new WaitForExitDialog(this);
            mWaitForExitDialog.show();
        }
    }

    public void dismissWaitForExitDialog() {
        if (mWaitForExitDialog != null) {
            mWaitForExitDialog.dismiss();
        }
    }

    public boolean isWaitForExitDialogShowing() {
        return (mWaitForExitDialog != null) && mWaitForExitDialog.isShowing();
    }

    public void setReadyToExit(boolean isReady) {
        if (mWaitForExitDialog != null) {
            mWaitForExitDialog.setReadyToExit(isReady);
        }
    }

    public void onWaitForDialogReadyToExit() {
        Intent data = new Intent(ACTION_READY_TO_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public void onWaitForExitDialogDismissed() {
        mWaitForExitDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_WAIT_FOR_EXIT);
        mBroadcastManager.sendBroadcast(data);
    }

    public class WaitForExitDialog {

        private final Context mContext;
        private ProgressDialog mDialog;
        private CountDownTask mCountDownTask;
        private boolean mReadyToExit = true;

        public WaitForExitDialog(Context context) {
            mContext = context;

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

        public void setReadyToExit(boolean isReady) {
            mReadyToExit = isReady;
        }

        private void initDialog() {
            mDialog = createProgressDialog();

            mDialog.setMessage(mContext.getString(R.string.dialog_message_wait_for));
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setCancelable(false);

            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    mCountDownTask.cancel(true);
                    onWaitForExitDialogDismissed();
                }
            });
        }

        private class CountDownTask extends AsyncTask<Void, Long, Void> {

            @Override
            protected Void doInBackground(Void... args) {
                int sleepCount = 0;
                while (!mReadyToExit) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (++sleepCount >= 50) {
                        break;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isCancelled()) {
                    mCountDownTask.cancel(true);
                    mDialog.dismiss();

                    onWaitForDialogReadyToExit();
                }
            }

        }

    }

    private ErsDialog mErsDialog;

    public void showErsDialog(boolean isBlackboxRunning) {
        if (mErsDialog == null) {
            mErsDialog = new ErsDialog(this, isBlackboxRunning);
            mErsDialog.show();
        }
    }

    public void dismissErsDialog() {
        if (mErsDialog != null) {
            mErsDialog.dismiss();
        }
    }

    public boolean isErsDialogShowing() {
        return (mErsDialog != null) && mErsDialog.isShowing();
    }

    private void onErsTargetChanged(ErsTarget target, boolean userSelect) {
        Intent data = new Intent(ACTION_ERS_TARGET_CHANGED);
        data.putExtra(EXTRA_ERS_TARGET, target);
        data.putExtra(EXTRA_USER_SELECT, userSelect);
        mBroadcastManager.sendBroadcast(data);
    }

    private void onErsDialogDismissed() {
        mErsDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_ERS);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ErsDialog implements OnClickListener {

        private static final int TARGET_TIMEOUT = 10000; // 10 seconds
        private static final int COUNTDOWN_INTERVAL = 100;

        private Button mCallBtn;
        private Button mSmsBtn;
        private Button mYoutubeBtn;
        private View mCancelBtn;
        private TextView mMessageText;

        private final Context mContext;
        private final boolean mIsBlackboxRunning;
        private ErsTarget mTarget;
        private AlertDialog mDialog;
        private View mContentView;
        private CountDownTask mCountDownTask;
        private SettingsStore mSettingsStore;

        public ErsDialog(Context context, boolean isBlackboxRunning) {
            mContext = context;

            mSettingsStore = SettingsStore.getInstance();

            mTarget = mSettingsStore.getErsTarget();
            mIsBlackboxRunning = isBlackboxRunning;

            // Blackbox is not running but default ERS target is YOUTUBE
            if (!mIsBlackboxRunning && (mTarget == ErsTarget.YOUTUBE)) {
                mTarget = ErsTarget.NONE;
            }

            initDialog();
        }

        public void show() {
            mDialog.show();

            // setContentView() method should be invoked after showing
            mDialog.setContentView(mContentView);

            // Start count down
            mCountDownTask = new CountDownTask();
            mCountDownTask.execute();

            // Callback default target
            onErsTargetChanged(mTarget, false);
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_Transparent)
                    .create();
            toSystemWindow(mDialog.getWindow());

            initContentView();

            mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    // Cancel timer
                    mCountDownTask.cancel(true);

                    mTarget = ErsTarget.NONE;

                    // Callback modified target
                    onErsTargetChanged(mTarget, true);
                }
            });
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    // Cancel timer
                    mCountDownTask.cancel(true);

                    onErsDialogDismissed();
                }
            });

            // Select default target button
            Button targetBtn = getDefaultTargetButton(mTarget);
            if (targetBtn != null) {
                targetBtn.setSelected(true);
            }
//            targetBtn.setSelected(true);
        }

        private void initContentView() {
            mContentView = LayoutInflater.from(mContext).inflate(R.layout.dialog_ers, null);

            int simState = TelephonyUtils.getSimState(mContext);
            int callState = TelephonyUtils.getCallState(mContext);

            String callPhoneNumber = mSettingsStore.getErsCallContactPhoneNumber();
            String smsPhoneNumber = mSettingsStore.getErsSmsContactPhoneNumber();
            String smsMessage = mSettingsStore.getErsSmsMessage();

            // Call
            mCallBtn = (Button) mContentView.findViewById(R.id.ers_call_btn);
            if (simState == TelephonyManager.SIM_STATE_READY
                    && callState == TelephonyManager.CALL_STATE_IDLE
                    && !TextUtils.isEmpty(callPhoneNumber)) {
                mCallBtn.setEnabled(true);
                mCallBtn.setOnClickListener(this);
            }

            // SMS
            mSmsBtn = (Button) mContentView.findViewById(R.id.ers_sms_btn);
            if (simState == TelephonyManager.SIM_STATE_READY
                    && callState == TelephonyManager.CALL_STATE_IDLE
                    && !TextUtils.isEmpty(smsPhoneNumber) && !TextUtils.isEmpty(smsMessage)) {
                mSmsBtn.setEnabled(true);
                mSmsBtn.setOnClickListener(this);
            }

            // Youtube
            mYoutubeBtn = (Button) mContentView.findViewById(R.id.ers_youtube_btn);
            if (mIsBlackboxRunning
                    && !TextUtils.isEmpty(mSettingsStore.getErsYoutubeAccountName())) {
                mYoutubeBtn.setEnabled(true);
                mYoutubeBtn.setOnClickListener(this);
            }

            // Cancel
            mCancelBtn = mContentView.findViewById(R.id.ers_cancel_btn);
            mCancelBtn.setOnClickListener(this);

            // Message
            mMessageText = (TextView) mContentView.findViewById(R.id.ers_message_text);
            mMessageText.setText(mContext.getString(R.string.dialog_mesage_ers, TARGET_TIMEOUT / 1000));
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            // Cancel timer first
            mCountDownTask.cancel(true);

            if (id == R.id.ers_call_btn) {
                mTarget = ErsTarget.CALL;
            } else if (id == R.id.ers_sms_btn) {
                mTarget = ErsTarget.SMS;
            } else if (id == R.id.ers_cancel_btn) {
                mTarget = ErsTarget.NONE;
            }

            // Callback modified target
            onErsTargetChanged(mTarget, true);

            mDialog.dismiss();
        }

        private Button getDefaultTargetButton(ErsTarget target) {
            switch (target) {
                case CALL:
                    return mCallBtn;
                case SMS:
                    return mSmsBtn;
                case YOUTUBE:
                    return mYoutubeBtn;
                default:
                    return null;
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
                mMessageText.setText(mContext.getString(R.string.dialog_mesage_ers,
                        1 + (values[0] / 1000)));
            }

            @Override
            protected void onPostExecute(Void result) {
                if (!isCancelled()) {
                    Button targetBtn = getDefaultTargetButton(mTarget);
                    if (targetBtn != null) {
                        targetBtn.performClick();
                    }

                    mDialog.dismiss();
                }
            }

        }

    }

    private ObdRecoveryDialog mObdRecoveryDialog;

    public void showObdRecoveryDialog() {
        if (mObdRecoveryDialog == null) {
            mObdRecoveryDialog = new ObdRecoveryDialog(this);
            mObdRecoveryDialog.show();
        }
    }

    public void dismissObdRecoveryDialog() {
        if (mObdRecoveryDialog != null) {
            mObdRecoveryDialog.dismiss();
        }
    }

    public void onObdRecoveryDialogDismissed() {
        mObdRecoveryDialog = null;

        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_OBD_RECOVERY);
        mBroadcastManager.sendBroadcast(data);
    }

    public class ObdRecoveryDialog {

        private final Context mContext;
        private AlertDialog mDialog;

        public ObdRecoveryDialog(Context context) {
            mContext = context;

            initDialog();
        }

        public void show() {
            mDialog.show();
        }

        public void dismiss() {
            mDialog.dismiss();
        }

        public boolean isShowing() {
            return mDialog.isShowing();
        }

        private void initDialog() {
            mDialog = createAlertDialog();

            mDialog.setMessage(mContext.getString(R.string.dialog_message_obd_recovery));
            mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            logger.info("User disable bluetooth!@service");

                            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                            btAdapter.disable();
                        }
                    });

            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    onObdRecoveryDialogDismissed();
                }
            });
        }

    }

    private DiagnosticProcess mDiagnosticProcess;

    public void showDiagnosticProcess(final String dtc) {
        dismissDiagnosticProcess();

        mDiagnosticProcess = DiagnosticProcess.newInstance(this, dtc,
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        onDiagnosticProcessDismiss();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getApplicationContext(), DiagnosticActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("dtc", dtc);
                        i.putExtra("show-dialog", false);
                        startActivity(i);
                    }
                });
        toSystemWindow(mDiagnosticProcess.getDialog().getWindow());
        mDiagnosticProcess.show();
        onShowDiagnosticDialog();
    }

    public void dismissDiagnosticProcess() {
        if (mDiagnosticProcess != null) {
            mDiagnosticProcess.dismiss();
            mDiagnosticProcess = null;
        }
    }

    private void onDiagnosticProcessDismiss() {
        Intent data = new Intent(ACTION_DIALOG_DISMISSED);
        data.putExtra(EXTRA_DIALOG_TYPE, DIALOG_OBD_DIAGNOSTIC);
        mBroadcastManager.sendBroadcast(data);
    }

    public boolean isDiagnosticDialogDialogShowing() {
        return (mDiagnosticProcess != null) && mDiagnosticProcess.isShowing();
    }

//    public void startFloatingHead(String intent) {
//        Intent service = new Intent(getApplicationContext(), FloatingHeadService.class);
//        service.putExtra(FloatingHeadService.EXTRA_INTENT, intent);
//        startService(service);
//    }
//
//    public void stopFloatingHead() {
//        stopService(new Intent(getApplicationContext(), FloatingHeadService.class));
//    }
}

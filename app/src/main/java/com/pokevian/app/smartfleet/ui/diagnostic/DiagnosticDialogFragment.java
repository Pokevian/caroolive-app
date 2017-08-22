package com.pokevian.app.smartfleet.ui.diagnostic;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-09-08.
 */
public class DiagnosticDialogFragment extends DialogFragment implements View.OnClickListener {
    public static final String TAG = "DIAGNOSTIC-DIALOG";

    private static final int TARGET_TIMEOUT = 2000; // 10 seconds
    private static final int COUNTDOWN_INTERVAL = 100;
    private static final int FRAME_INTERVAL = 84;

    private DtcParser mDtcParser;
    private ProgressBar mProgressBar;
    private TextView mMessageText;

    private View mView;
    private AnimationTask mAnimationTask;
    private CountDownTask mCountDownTask;

    public static DiagnosticDialogFragment newInstance(String dtc) {
        DiagnosticDialogFragment fragment = new DiagnosticDialogFragment();
        Bundle args = new Bundle();
        args.putString("DTC", dtc);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        startAnimation();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String dtc = getArguments().getString("DTC", null);
        mDtcParser = new DtcParser(dtc);

        mView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_diagnostic, null);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress);
        mProgressBar.setProgress(0);
        // Message
        mMessageText = (TextView) mView.findViewById(R.id.text);
        mMessageText.setText(getString(R.string.dialog_diagnostic_msg));
        mView.findViewById(R.id.btn_cancel).setOnClickListener(this);

        Dialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Transparent)
                .setView(mView)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // Cancel timer
                        if (mCountDownTask != null) {
                            mCountDownTask.cancel(true);
                            mCountDownTask = null;
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Logger.getLogger(TAG).trace("onDissmiss@DiagnosticDialog");
                        // Cancel timer
                        if (mCountDownTask != null) {
                            mCountDownTask.cancel(true);
                            mCountDownTask = null;
                        }

                        Fragment fragment = getParentFragment();
                        if (fragment != null && fragment instanceof DiagnosticDialogFragment) {
                            ((DiagnosticFragment) fragment).update();
                        }
                    }
                })
                .setCancelable(false)
                .create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.btn_cancel == id) {
            if (mCountDownTask != null) {
                mCountDownTask.cancel(true);
                mCountDownTask = null;
            }
            dismiss();
        }
    }

    private void startAnimation() {
        if (mAnimationTask != null) {
            mAnimationTask.cancel(true);
            mAnimationTask = null;
        }
        mAnimationTask = new AnimationTask();
        mAnimationTask.execute();

        new Thread(mProgressRunnable).start();
    }

    private void stopAnimation() {
        if (mAnimationTask != null) {
            mAnimationTask.cancel(true);
            mAnimationTask = null;
        }
    }

    private void updateDrawable(int index) {
        TypedArray scan = getResources().obtainTypedArray(R.array.ani_scan);
        final int id = scan.getResourceId(index, -1);
        mView.findViewById(R.id.ani_scan).setBackgroundResource(id);
    }

    private void setComplete() {
        setWarningView();
        mView.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
        mView.findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);
        updateResult();
    }

    private void updateResult() {
        TextView title = (TextView) mView.findViewById(R.id.title);

        if (mDtcParser.getDtcGroupCount() > 0) {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_warning, 0, 0, 0);
            title.setText(R.string.dialog_diagnostic_title_warning);
        } else {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ok, 0, 0, 0);
            title.setText(R.string.dialog_diagnostic_title_ok);
        }
        // Start count down
        Logger.getLogger(TAG).trace("updateResult@DiagnosticDialog# start count down");
        mCountDownTask = new CountDownTask();
        mCountDownTask.execute();

        mView.findViewById(R.id.ani_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.getLogger(TAG).trace("dismiss@DiagnosticDialog");
//                mView.removeCallbacks(mProgressRunnable);
            if (mCountDownTask != null) {
                mCountDownTask.cancel(true);
                mCountDownTask = null;
            }
                mAnimationTask.cancel(true);
                dismiss();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setWarningView() {
        View view = mView.findViewById(R.id.ani_scan);
        view.getBackground().setCallback(null);

        LayerDrawable layerDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.layer_diagnostic_popup);
        for (int i = DtcParser.DTC_GROUP_01; i <= DtcParser.DTC_GROUP_10; i++) {
            if (mDtcParser.isValidGroup(i)) {
                Logger.getLogger(TAG).trace("valid-group#" + i);
                setLayerByGroupId(layerDrawable, i);
            }
        }

        if (Build.VERSION_CODES.JELLY_BEAN > Build.VERSION.SDK_INT) {
            view.setBackgroundDrawable(layerDrawable);
        } else {
            view.setBackground(layerDrawable);
        }

    }

    private void setLayerByGroupId(LayerDrawable layerDrawable, int groupId) {
        switch (groupId) {
            case DtcParser.DTC_GROUP_01:
                layerDrawable.setDrawableByLayerId(R.id.item_exhaust, getResources().getDrawable(R.drawable.popup_parts6_r));
                break;
            case DtcParser.DTC_GROUP_02:
            case DtcParser.DTC_GROUP_03:
            case DtcParser.DTC_GROUP_04:
            case DtcParser.DTC_GROUP_07:
                layerDrawable.setDrawableByLayerId(R.id.item_engine, getResources().getDrawable(R.drawable.popup_parts5_r));
                break;
            case DtcParser.DTC_GROUP_05:
                layerDrawable.setDrawableByLayerId(R.id.item_electronic_circuit, getResources().getDrawable(R.drawable.popup_parts3_r));
                break;
            case DtcParser.DTC_GROUP_06:
                layerDrawable.setDrawableByLayerId(R.id.item_transmission, getResources().getDrawable(R.drawable.popup_parts2_r));
                break;
            case DtcParser.DTC_GROUP_08:
                layerDrawable.setDrawableByLayerId(R.id.item_electronic_device, getResources().getDrawable(R.drawable.popup_parts7_r));
                break;
            case DtcParser.DTC_GROUP_09:
                layerDrawable.setDrawableByLayerId(R.id.item_chassis, getResources().getDrawable(R.drawable.popup_parts1_r));
                break;
            case DtcParser.DTC_GROUP_10:
                layerDrawable.setDrawableByLayerId(R.id.item_network, getResources().getDrawable(R.drawable.popup_parts4_r));
                break;
        }
    }

    private final Runnable mProgressRunnable = new Runnable() {

        @Override
        public void run() {
            mProgressBar.setProgress(0);
            try {
                Thread.sleep(FRAME_INTERVAL);
                while (mProgressBar.getProgress() <= 100) {
                    mProgressBar.incrementProgressBy(1);
                    mProgressBar.setProgress(mProgressBar.getProgress());
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
            }
        }

    };

    private class AnimationTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            for (int i = 0; i < 12; i++) {
                publishProgress(i);
                try {
                    Thread.sleep(FRAME_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                }
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateDrawable(values[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mProgressBar.setProgress(100);
                setComplete();
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
//            Logger.getLogger(TAG).trace("onProgressUpdate@CountDownTask#" + values[0]);
            mMessageText.setText(getString(R.string.dialog_message));
        }

        @Override
        protected void onPostExecute(Void result) {
            Logger.getLogger(TAG).trace("onPostExecute@CountDownTask");
            if (!isCancelled()) {
                getDialog().dismiss();
//                dismissAllowingStateLoss();
                Logger.getLogger(TAG).trace("dismiss#onPostExecute@CountDownTask");
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            Logger.getLogger(TAG).warn("onCancelled@CountDownTask");
            super.onCancelled(aVoid);
        }
    }
}

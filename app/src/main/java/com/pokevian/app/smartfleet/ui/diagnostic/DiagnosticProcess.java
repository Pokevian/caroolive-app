package com.pokevian.app.smartfleet.ui.diagnostic;

import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.driving.DrivingService;
import com.pokevian.app.smartfleet.util.ViewCompatUtils;

/**
 * Created by ian on 2016-09-08.
 */
public class DiagnosticProcess implements View.OnClickListener {

    private static final int TARGET_TIMEOUT = 2000; // 10 seconds
    private static final int COUNTDOWN_INTERVAL = 100;
    private static final int FRAME_INTERVAL = 84;

    private TextView mMessageText;

    private final Context mContext;
    private Dialog mDialog;
    private View mView;
    private AnimationTask mAnimationTask;
    private CountDownTask mCountDownTask;

    private ProgressBar mProgressBar;
    private DtcParser mDtcParser;

    private Boolean mAutoDismiss;

    private DialogInterface.OnDismissListener mOnDismissListener;
    private View.OnClickListener mOnClickListener;

    public static DiagnosticProcess newInstance(Context context, String dtc, DialogInterface.OnDismissListener listener) {
        return new DiagnosticProcess(context, dtc, listener, null, true);
    }

    public static DiagnosticProcess newInstance(Context context, String dtc,
                                                DialogInterface.OnDismissListener onDismissListener,
                                                View.OnClickListener onClickListener) {
        return new DiagnosticProcess(context, dtc, onDismissListener, onClickListener, false);
    }

    public DiagnosticProcess(Context context, String dtc,
                             DialogInterface.OnDismissListener onDismissListener,
                             View.OnClickListener onClickListener,
                             boolean autoDismiss) {
        mContext = context;
        mDtcParser = new DtcParser(dtc);
        mOnDismissListener = onDismissListener;
        mOnClickListener = onClickListener;
        mAutoDismiss = autoDismiss;

        initDialog();
    }

    public void show() {
        mDialog.show();

        mAnimationTask = new AnimationTask();
        mAnimationTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    public boolean isShowing() {
        return mDialog.isShowing();
    }

    public Dialog getDialog() {
        return mDialog;
    }

    private void initDialog() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.dialog_diagnostic, null);

        mProgressBar = (ProgressBar) mView.findViewById(R.id.progress);
        mProgressBar.setProgress(0);

        // Message
        mMessageText = (TextView) mView.findViewById(R.id.text);
        mMessageText.setText(mContext.getString(R.string.dialog_diagnostic_msg));

        mView.findViewById(R.id.btn_cancel).setOnClickListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_Transparent)
                .setView(mView)
                .setCancelable(false);
        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                cancelCountDown();
            }
        });
        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                cancelCountDown();

                if (mOnDismissListener != null) {
                    mOnDismissListener.onDismiss(dialog);
                }
            }
        });
    }

    private void   updateDrawable(int index) {
        TypedArray scan = obtainTypedArray(R.array.ani_scan);
        final int id = scan.getResourceId(index, -1);
        mView.findViewById(R.id.ani_scan).setBackgroundResource(id);
    }

    private void onComplete() {
        if (mAutoDismiss) {
            dismiss();

        } else {
            mView.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
            mView.findViewById(R.id.btn_cancel).setVisibility(View.VISIBLE);

            updateLayerDrawable();
            updateResult();
        }
    }

    private void updateResult() {
        TextView title = (TextView) mView.findViewById(R.id.title);

        if (mDtcParser.getDtcGroupCount() > 0) {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ico_warning, 0, 0, 0);
            title.setText(R.string.dialog_diagnostic_title_warning);

            mMessageText.setText(R.string.dialog_diagnostic_warning_text);

            mView.findViewById(R.id.ani_scan).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent data = new Intent(DrivingService.ACTION_GOTO_DIAGNOSTIC);
//                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(data);

                    if (mOnClickListener != null) {
                        mOnClickListener.onClick(v);
                    }

                    dismiss();
                }
            });
        } else {
            title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ok, 0, 0, 0);
            title.setText(R.string.dialog_diagnostic_title_ok);

            startCountDown();
        }
    }

    private void updateLayerDrawable() {
        LayerDrawable drawable = (LayerDrawable) getDrawable(R.drawable.layer_diagnostic_popup);

        for (int i = DtcParser.DTC_GROUP_01; i <= DtcParser.DTC_GROUP_10; i++) {
            if (mDtcParser.isValidGroup(i)) {
                setLayerByGroupId(drawable, i);
            }
        }

        View view = mView.findViewById(R.id.ani_scan);
        view.getBackground().setCallback(null);

        ViewCompatUtils.setBackground(view, drawable);
    }

    private void setLayerByGroupId(LayerDrawable layerDrawable, int groupId) {
        switch (groupId) {
            case DtcParser.DTC_GROUP_01:
                layerDrawable.setDrawableByLayerId(R.id.item_exhaust, getDrawable(R.drawable.popup_parts6_r));
                break;
            case DtcParser.DTC_GROUP_02:
            case DtcParser.DTC_GROUP_03:
            case DtcParser.DTC_GROUP_04:
            case DtcParser.DTC_GROUP_07:
                layerDrawable.setDrawableByLayerId(R.id.item_engine, getDrawable(R.drawable.popup_parts5_r));
                break;
            case DtcParser.DTC_GROUP_05:
                layerDrawable.setDrawableByLayerId(R.id.item_electronic_circuit, getDrawable(R.drawable.popup_parts3_r));
                break;
            case DtcParser.DTC_GROUP_06:
                layerDrawable.setDrawableByLayerId(R.id.item_transmission, getDrawable(R.drawable.popup_parts2_r));
                break;
            case DtcParser.DTC_GROUP_08:
                layerDrawable.setDrawableByLayerId(R.id.item_electronic_device, getDrawable(R.drawable.popup_parts7_r));
                break;
            case DtcParser.DTC_GROUP_09:
                layerDrawable.setDrawableByLayerId(R.id.item_chassis, getDrawable(R.drawable.popup_parts1_r));
                break;
            case DtcParser.DTC_GROUP_10:
                layerDrawable.setDrawableByLayerId(R.id.item_network, getDrawable(R.drawable.popup_parts4_r));
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.btn_cancel == id) {
            if (mCountDownTask != null) {
                mCountDownTask.cancel(true);
            }
            dismiss();
        }
    }


    private Drawable getDrawable(int id) {
        return mContext.getResources().getDrawable(id);
    }

    private TypedArray obtainTypedArray(int id) {
        return mContext.getResources().obtainTypedArray(id);
    }

    private class AnimationTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            try {
                for (int i = 0; i < 12; i++) {
                    publishProgress(i);
                    Thread.sleep(FRAME_INTERVAL);
                }
            } catch (InterruptedException e) {
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            updateDrawable(values[0]);
            mProgressBar.setProgress((FRAME_INTERVAL * values[0])  * 100 / (FRAME_INTERVAL * 11));
        }

        @Override
        protected void onPreExecute() {
            mProgressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(Void result) {
            mProgressBar.setProgress(100);
            if (!isCancelled()) {
                onComplete();
            }
        }
    }

    private void startCountDown() {
        mCountDownTask = new CountDownTask();
        mCountDownTask.execute();
    }

    private void cancelCountDown() {
        if (mCountDownTask != null) {
            mCountDownTask.cancel(true);
            mCountDownTask = null;
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
            mMessageText.setText(mContext.getString(R.string.dialog_message));
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mDialog.dismiss();
            }
        }

    }

}
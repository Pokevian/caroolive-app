package com.pokevian.app.smartfleet.ui.diagnostic;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.ui.driving.DrivingService;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener;

import org.apache.log4j.Logger;


/**
 * Created by ian on 2016-03-25.
 */
public class DiagnosticFragment extends Fragment implements View.OnClickListener/*, DialogFragmentInterface.OnClickListener*/ {
    public static final String TAG = "diagnostic-fragment";

    private DtcParser mDtcParser;
    private DiagnosticProcess mDiagnosticProcess;
    private LayerDrawable mVehicleLayerDrawable;

    private String mDtc;
    private boolean mShowDiagnosticProcess;

    private VehicleEngineStatus mVes;
    private int mVss;

    public static DiagnosticFragment newInstance(String dtc, boolean showDialog) {
        DiagnosticFragment fragment = new DiagnosticFragment();

        Bundle args = new Bundle();
        args.putString("dtc", dtc);
        args.putBoolean("show-dialog", showDialog);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mVehicleLayerDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.layer_diagnostic);

        if (getArguments() != null) {
            mShowDiagnosticProcess = getArguments().getBoolean("show-dialog");
            mDtc = getArguments().getString("dtc");
            mDtcParser = new DtcParser(mDtc);
        }

        registerVehicleReceiver();

        Logger.getLogger(TAG).trace("onCreate#" + getArguments());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dignostic, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDiagnosticProcess();
            }
        });

        view.findViewById(R.id.btn_reset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVss > 0) {
                    DrivingWarningFragment.newInstance().show(getFragmentManager(), DrivingWarningFragment.TAG);
                } else {
                    DtcResetWarningFragment.newInstance().show(getFragmentManager(), DtcResetWarningFragment.TAG);
                }
            }
        });

        if (mShowDiagnosticProcess) {
            mShowDiagnosticProcess = false;
            showDiagnosticProcess();
        } else {
            update();
        }
    }

    @Override
    public void onDestroyView() {
        dismissDiagnosticProcess();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        unregisterVehicleReceiver();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int groupId = -1;

        switch (v.getId()) {
            case R.id.btn_dtc01:
                groupId = DtcParser.DTC_GROUP_01;
                break;
            case R.id.btn_dtc02:
                groupId = DtcParser.DTC_GROUP_02;
                break;
            case R.id.btn_dtc03:
                groupId = DtcParser.DTC_GROUP_03;
                break;
            case R.id.btn_dtc04:
                groupId = DtcParser.DTC_GROUP_04;
                break;
            case R.id.btn_dtc05:
                groupId = DtcParser.DTC_GROUP_05;
                break;
            case R.id.btn_dtc06:
                groupId = DtcParser.DTC_GROUP_06;
                break;
            case R.id.btn_dtc07:
                groupId = DtcParser.DTC_GROUP_07;
                break;
            case R.id.btn_dtc08:
                groupId = DtcParser.DTC_GROUP_08;
                break;
            case R.id.btn_dtc09:
                groupId = DtcParser.DTC_GROUP_09;
                break;
            case R.id.btn_dtc10:
                groupId = DtcParser.DTC_GROUP_10;
                break;
        }

        DiagnosticDetailFragment.newInstance(mDtcParser.bindDtcGroup(groupId), groupId).show(getFragmentManager(), DiagnosticDetailFragment.TAG);

    }

    private void showDiagnosticProcess() {
        dismissDiagnosticProcess();

        mDiagnosticProcess = DiagnosticProcess.newInstance(getActivity(), mDtcParser.getDtc(), new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                update();
            }
        });

        mDiagnosticProcess.show();
    }

    private void dismissDiagnosticProcess() {
        if (mDiagnosticProcess != null) {
            mDiagnosticProcess.dismiss();
            mDiagnosticProcess = null;
        }
    }

    protected void update() {
        init();
        DtcParser dtcParser = mDtcParser;

        for (int i = DtcParser.DTC_GROUP_01; i <= DtcParser.DTC_GROUP_10; i++) {
            View v = getViewByIndex(i);
            if (dtcParser.isValidGroup(i)) {
                v.setOnClickListener(this);
                v.setEnabled(true);
                setFaultLayerByGroupId(i);
            }
        }

        ((ImageView) getView().findViewById(R.id.layer_diagnostic)).setImageDrawable(mVehicleLayerDrawable);
        getView().findViewById(R.id.btn_reset).setVisibility(dtcParser.getDtcGroupCount() > 0 ? View.VISIBLE : View.INVISIBLE);

        if (dtcParser.getDtcGroupCount() > 0) {
            ((TextView) getView().findViewById(R.id.message)).setText(R.string.diagnostic_message_warning);
        } else {
            ((TextView) getView().findViewById(R.id.message)).setText(R.string.diagnostic_message_good);
        }
    }

    private View getViewByIndex(int index) {
        switch (index) {
            case DtcParser.DTC_GROUP_01:
                return getView().findViewById(R.id.btn_dtc01);
            case DtcParser.DTC_GROUP_02:
                return getView().findViewById(R.id.btn_dtc02);
            case DtcParser.DTC_GROUP_03:
                return getView().findViewById(R.id.btn_dtc03);
            case DtcParser.DTC_GROUP_04:
                return getView().findViewById(R.id.btn_dtc04);
            case DtcParser.DTC_GROUP_05:
                return getView().findViewById(R.id.btn_dtc05);
            case DtcParser.DTC_GROUP_06:
                return getView().findViewById(R.id.btn_dtc06);
            case DtcParser.DTC_GROUP_07:
                return getView().findViewById(R.id.btn_dtc07);
            case DtcParser.DTC_GROUP_08:
                return getView().findViewById(R.id.btn_dtc08);
            case DtcParser.DTC_GROUP_09:
                return getView().findViewById(R.id.btn_dtc09);
            case DtcParser.DTC_GROUP_10:
                return getView().findViewById(R.id.btn_dtc10);
        }

        return null;
    }

    private void init() {
        initLayer();

        for (int i = DtcParser.DTC_GROUP_01; i <= DtcParser.DTC_GROUP_10; i++) {
            View v = getViewByIndex(i);
            v.setEnabled(false);
        }
        getView().findViewById(R.id.btn_reset).setVisibility(View.GONE);
        ((TextView) getView().findViewById(R.id.message)).setText(null);
    }

    private void initLayer() {
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_chassis, getResources().getDrawable(R.drawable.parts1_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_transmission, getResources().getDrawable(R.drawable.parts2_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_electronic_circuit, getResources().getDrawable(R.drawable.parts3_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_network, getResources().getDrawable(R.drawable.parts4_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_engine, getResources().getDrawable(R.drawable.parts5_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_exhaust, getResources().getDrawable(R.drawable.parts6_b));
        mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_electronic_device, getResources().getDrawable(R.drawable.parts7_b));

        ((ImageView) getView().findViewById(R.id.layer_diagnostic)).setImageDrawable(mVehicleLayerDrawable);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setFaultLayerByGroupId(int groupId) {
        Logger.getLogger(TAG).trace("setFaultLayerByGroupId#" + groupId);

        switch (groupId) {
            case DtcParser.DTC_GROUP_01:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_exhaust, getResources().getDrawable(R.drawable.parts6_r));
                break;
            case DtcParser.DTC_GROUP_02:
            case DtcParser.DTC_GROUP_03:
            case DtcParser.DTC_GROUP_04:
            case DtcParser.DTC_GROUP_07:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_engine, getResources().getDrawable(R.drawable.parts5_r));
                break;
            case DtcParser.DTC_GROUP_05:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_electronic_circuit, getResources().getDrawable(R.drawable.parts3_r));
                break;
            case DtcParser.DTC_GROUP_06:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_transmission, getResources().getDrawable(R.drawable.parts2_r));
                break;
            case DtcParser.DTC_GROUP_08:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_electronic_device, getResources().getDrawable(R.drawable.parts7_r));
                break;
            case DtcParser.DTC_GROUP_09:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_chassis, getResources().getDrawable(R.drawable.parts1_r));
                break;
            case DtcParser.DTC_GROUP_10:
                mVehicleLayerDrawable.setDrawableByLayerId(R.id.item_network, getResources().getDrawable(R.drawable.parts4_r));
                break;
        }
    }

    private void registerVehicleReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);

        filter.addAction(DrivingService.ACTION_GOTO_EXIT);
        filter.addAction(DrivingService.ACTION_GOTO_HOME);
        filter.addAction(DrivingService.ACTION_GOTO_PAUSE);
        filter.addAction(DrivingService.ACTION_READY_TO_EXIT);
//        filter.addAction(DrivingService.ACTION_ERS_TARGET_CHANGED);
        filter.addAction(DrivingService.ACTION_DIALOG_DISMISSED);

        filter.addAction(DrivingService.ACTION_SHOW_DIALOG_DIAGNOSTIC);
//        filter.addAction(DrivingService.ACTION_GOTO_DIAGNOSTIC);
//        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CLEAR_STORED_DTC);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mVehicleReceiver, filter);
    }

    private void unregisterVehicleReceiver() {
        try {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
        }
    }

    private void onObdExtraDataReceived(float rpm, int  vss) {
        mVss = vss;
    }

    private void onObdDataReceived(ObdData data) {
        String dtc = getDTC(data);
        String last = mDtcParser.getDtc();

        if (dtc == null) {
            if (last != null) {
                mDtcParser = new DtcParser(dtc);
            }
        } else if (!dtc.equals(last)) {
            mDtcParser = new DtcParser(dtc);
        }
    }

    private void onVehicleEngineStatusChanged(int ves) {
        if (VehicleEngineStatus.OFF == ves) {
            dismissDiagnosticProcess();
        }

        getView().findViewById(R.id.btn_refresh).setVisibility(VehicleEngineStatus.ON == ves ? View.VISIBLE : View.GONE);
        getView().findViewById(R.id.btn_reset).setVisibility(VehicleEngineStatus.ON == ves ? View.VISIBLE : View.INVISIBLE);
    }

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!isVisible()) return;

            final String action = intent.getAction();
            if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                OnObdStateListener.ObdState obdState = (OnObdStateListener.ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
//                onObdStateChanged(obdState);
            } else if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                int ves = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS,
                        VehicleEngineStatus.UNKNOWN);
                onVehicleEngineStatusChanged(ves);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED.equals(action)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
            } else if (VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED.equals(action)) {
                float rpm = intent.getFloatExtra(VehicleDataBroadcaster.EXTRA_RPM, 0);
                int vss = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VSS, 0);
                onObdExtraDataReceived(rpm, vss);
            } else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
//                onObdCannotConnect(obdDevice, isBlocked);
//            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
//                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
//                onObdDeviceNotSupported(obdDevice);
//            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
//                BluetoothDevice obdDevice = (BluetoothDevice) intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
//                onObdProtocolNotSupported(obdDevice);
            } else if (DrivingService.ACTION_GOTO_EXIT.equals(action)) {
                dismissDiagnosticProcess();
                getActivity().finish();
            } else if (DrivingService.ACTION_GOTO_HOME.equals(action)) {
//                onGotoHome();
            } else if (DrivingService.ACTION_GOTO_PAUSE.equals(action)) {
//                onGotoPause();
            } else if (DrivingService.ACTION_READY_TO_EXIT.equals(action)) {
//                onReadyToExit();
            } else if (DrivingService.ACTION_SHOW_DIALOG_DIAGNOSTIC.equals(action)) {
                dismissDiagnosticProcess();
                getActivity().finish();
            }
        }
    };

    private String getDTC(ObdData data) {
        return data.getBoolean(KEY.SAE_MIL, false) ? data.getString(KEY.SAE_DTC) : null;
    }

    public static final class DiagnosticDetailFragment extends DialogFragment {
        public static final String TAG = "diagnostic-detail";

        public static DiagnosticDetailFragment newInstance(String dtc, int groupId) {
            DiagnosticDetailFragment fragment = new DiagnosticDetailFragment();

            Bundle args = new Bundle();
            args.putString("detail-dtc", dtc);
            args.putInt("detail-id", groupId);
            fragment.setArguments(args);

            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_diagnostic_detail, null);
            setTitleText((TextView) v.findViewById(R.id.title), getArguments().getInt("detail-id"));
            setDtcText((TextView) v.findViewById(R.id.dtc), getArguments().getString("detail-dtc"));
            setMessageText((TextView) v.findViewById(R.id.message), getArguments().getInt("detail-id"));
            v.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Transparent)
                    .setView(v)
                    .create();

            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }

        private void setTitleText(TextView titleText, int id) {

            switch (id) {
                case DtcParser.DTC_GROUP_01:
                    titleText.setText(R.string.dtc_group01_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs01, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_02:
                    titleText.setText(R.string.dtc_group02_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs02, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_03:
                    titleText.setText(R.string.dtc_group03_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs03, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_04:
                    titleText.setText(R.string.dtc_group04_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs04, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_05:
                    titleText.setText(R.string.dtc_group05_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs05, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_06:
                    titleText.setText(R.string.dtc_group06_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs06, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_07:
                    titleText.setText(R.string.dtc_group07_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs07, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_08:
                    titleText.setText(R.string.dtc_group08_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs08, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_09:
                    titleText.setText(R.string.dtc_group09_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs09, 0, 0, 0);
                    break;
                case DtcParser.DTC_GROUP_10:
                    titleText.setText(R.string.dtc_group10_title);
                    titleText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_dtcs10, 0, 0, 0);
                    break;
            }
        }

        private void setDtcText(TextView dtcText, String dtc) {
            dtcText.setText(dtc);
        }

        private void setMessageText(TextView messageText, int id) {
            switch (id) {
                case DtcParser.DTC_GROUP_01:
                    messageText.setText(R.string.dtc_group01_text);
                    break;
                case DtcParser.DTC_GROUP_02:
                    messageText.setText(R.string.dtc_group02_text);
                    break;
                case DtcParser.DTC_GROUP_03:
                    messageText.setText(R.string.dtc_group03_text);
                    break;
                case DtcParser.DTC_GROUP_04:
                    messageText.setText(R.string.dtc_group04_text);
                    break;
                case DtcParser.DTC_GROUP_05:
                    messageText.setText(R.string.dtc_group05_text);
                    break;
                case DtcParser.DTC_GROUP_06:
                    messageText.setText(R.string.dtc_group06_text);
                    break;
                case DtcParser.DTC_GROUP_07:
                    messageText.setText(R.string.dtc_group07_text);
                    break;
                case DtcParser.DTC_GROUP_08:
                    messageText.setText(R.string.dtc_group08_text);
                    break;
                case DtcParser.DTC_GROUP_09:
                    messageText.setText(R.string.dtc_group09_text);
                    break;
                case DtcParser.DTC_GROUP_10:
                    messageText.setText(R.string.dtc_group10_text);
                    break;
            }
        }
    }

    public static class DtcResetWarningFragment extends DialogFragment {
        public static final String TAG = "reset-warning";

        private DialogFragmentInterface.OnClickListener mOnClickListener;

        public static DtcResetWarningFragment newInstance() {
            return new DtcResetWarningFragment();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

            try {
                mOnClickListener = (DialogFragmentInterface.OnClickListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement DialogFragmentInterface");
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_dtc_reset, null);
            view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            view.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnClickListener.onClick(DtcResetWarningFragment.this, DialogInterface.BUTTON_POSITIVE);
                    dismiss();
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Transparent)
                    .setCancelable(false)
                    .setView(view)
                    .create();

            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }
    }

    public static class DrivingWarningFragment extends DialogFragment {
        public static final String TAG = "driving-warning";

        public static DrivingWarningFragment newInstance() {
            return new DrivingWarningFragment();
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_dtc_reset, null);
            view.findViewById(R.id.btn_cancel).setVisibility(View.INVISIBLE);
            ((TextView) view.findViewById(R.id.message)).setText(getText(R.string.dialog_diagnostic_driving_warning));
            ((TextView) view.findViewById(R.id.btn_ok)).setText(getText(R.string.btn_cancel));
            view.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });

            AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Transparent)
                    .setCancelable(false)
                    .setView(view)
                    .create();

            dialog.setCanceledOnTouchOutside(false);

            return dialog;
        }
    }
}

interface DialogFragmentInterface {
    interface OnClickListener {
        public void onClick(DialogFragment fragment, int which);
    }
}

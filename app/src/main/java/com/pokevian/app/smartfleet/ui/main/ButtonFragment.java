package com.pokevian.app.smartfleet.ui.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pokevian.app.fingerpush.PushNotificationService;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.driving.DrivingActivity;
import com.pokevian.app.smartfleet.ui.tripmonitor.TripMonitorActivity;
import com.pokevian.app.smartfleet.ui.video.VideoListActivity;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-06-16.
 */
public class ButtonFragment extends Fragment implements View.OnClickListener {
    public static final String TAG = "ButtonFragment";

    private static final int REQUEST_RECORD = 1;
    private static final int REQUEST_STATSTICS = 2;
    private static final int REQUEST_MOV = 3;

    private View mView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerNotificationReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkNotificatioin();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_button, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_record).setOnClickListener(this);
        view.findViewById(R.id.btn_statstics).setOnClickListener(this);
//        view.findViewById(R.id.btn_mov).setOnClickListener(this);
//        view.findViewById(R.id.btn_event).setOnClickListener(this);
//        view.findViewById(R.id.btn_notice).setOnClickListener(this);
        view.findViewById(R.id.btn_point).setOnClickListener(this);
        view.findViewById(R.id.btn_status).setOnClickListener(this);
        view.findViewById(R.id.btn_dtc).setOnClickListener(this);

        mView = view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        Logger.getLogger(TAG).debug("onActivityResult(): requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == Activity.RESULT_OK) {
            if (data != null && data.getBooleanExtra(DrivingActivity.EXTRA_REQUEST_EXIT, false)) {
                // Need to exit
                getActivity().finish();
            }
        }

    }

    @Override
    public void onDestroy() {
        unregisterNotificationReceiver();

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.btn_record == id) {
            Intent intent = new Intent(getActivity(), TripMonitorActivity.class);
            intent.putExtra(TripMonitorActivity.EXTRA_POSITION, 0);
            startActivityForResult(intent, REQUEST_RECORD);
        } else if (R.id.btn_statstics == id) {
            Intent intent = new Intent(getActivity(), TripMonitorActivity.class);
            intent.putExtra(TripMonitorActivity.EXTRA_POSITION, 1);
            startActivityForResult(intent, REQUEST_STATSTICS);
        } /*else if (R.id.btn_mov == id) {
            Intent intent = new Intent(getActivity(), VideoListActivity.class);
            startActivityForResult(intent, REQUEST_MOV);
        }*/ /*else if (R.id.btn_event == id) {
            ((MainActivity)getActivity()).startEventActivity();
        }*/ /*else if (R.id.btn_notice == id) {
            ((MainActivity)getActivity()).startNoticeActivity();
        }*/ else if (R.id.btn_point == id) {
            ((MainActivity)getActivity()).startMyPointActivity();
        } else if (R.id.btn_status == id) {
            ((MainActivity)getActivity()).startCarStatusCheckActivity();
        } else if (R.id.btn_dtc == id) {
            ((MainActivity)getActivity()).startDtcActivity();
        }
    }

    private void checkNotificatioin() {
//        mView.findViewById(R.id.btn_event).setSelected(SettingsStore.getInstance().hasNewEvent());
//        mView.findViewById(R.id.btn_notice).setSelected(SettingsStore.getInstance().hasNewNoti());
    }

    private void registerNotificationReceiver() {
        unregisterNotificationReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(PushNotificationService.ACTION_NOTIFY);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mNotificationReceiver, filter);
    }

    private void unregisterNotificationReceiver() {
        try {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mNotificationReceiver);
        } catch (Exception e) {
            Logger.getLogger(TAG).error(e.getMessage());
        }
    }

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!isVisible()) return;

            final String action = intent.getAction();
            if (PushNotificationService.ACTION_NOTIFY == action) {
                checkNotificatioin();
            }
        }
    };
}

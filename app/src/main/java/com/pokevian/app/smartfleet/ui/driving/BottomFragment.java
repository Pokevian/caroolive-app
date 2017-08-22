package com.pokevian.app.smartfleet.ui.driving;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;


public class BottomFragment extends Fragment {
	
	private DrivingActivity mDrivingActivity;
	protected View mBtnDiagonostic;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_driving_bottom, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mDrivingActivity = (DrivingActivity)getActivity();
		View navi = view.findViewById(R.id.btn_quick_launch_navi);
		if (TextUtils.isEmpty(SettingsStore.getInstance().getQuickLaunchNaviApp())) {
			navi.setEnabled(false);
		} else {
			navi.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mDrivingActivity.launchAppNavi(SettingsStore.getInstance().getQuickLaunchNaviApp());
				}
			});
		}

		mBtnDiagonostic = view.findViewById(R.id.btn_diagnostic);
		mBtnDiagonostic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mDrivingActivity.startDiagnosticActivity(mDrivingActivity.mDtc, true);
			}
		});
		mBtnDiagonostic.setEnabled(false);
	}

	public void onObdDataReceived(ObdData data) {
		if (isVisible()) {
			int ves = data.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN);
			mBtnDiagonostic.setEnabled(VehicleEngineStatus.isOnDriving(ves));

		}
	}

	public void onObdCannotConnect() {
		if (isVisible()) {
			mBtnDiagonostic.setEnabled(false);
		}
	}


}

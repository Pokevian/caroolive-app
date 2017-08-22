package com.pokevian.app.fingerpush;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

public class GCMAlertService extends Service {
	private final String CHECKED_ACTIVITY = "PushAlertActivity";

	private PowerManager pm;
	private String notiMessage = "";
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		
		if(intent != null && intent.getExtras() != null) {
			Bundle b = intent.getExtras();
			if (b != null) {
				if (b.getString("msg") != null) {
					notiMessage = b.getString("msg");
				}
			} else {
				stopSelf();
			}
			Log.e("", "=== start alert service");
			if(!isScreenOn()) {
				initScreenOff();
			} else {
				if(CheckedTopActivity()) {
					initScreenOff();
				}
			}
		}
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub		
		super.onCreate();
	}
	
	private boolean isScreenOn() {
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		return pm.isScreenOn();
	}

	private void initScreenOff() {
		Intent i = new Intent(getApplicationContext(), com.pokevian.app.fingerpush.PushAlertActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra("msg", notiMessage);
		startActivity(i);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	private boolean CheckedTopActivity() {
		ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> info;
		info = activityManager.getRunningTasks(1);
		for(Iterator iterator = info.iterator(); iterator.hasNext();) {
			RunningTaskInfo runningTaskInfo = (RunningTaskInfo) iterator.next();
			if(!runningTaskInfo.topActivity.getClassName().contains(CHECKED_ACTIVITY)) {
				return false;
			}
		}
		return true;
	}
}

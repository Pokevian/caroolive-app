package com.pokevian.app.fingerpush;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigPictureStyle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.VehicleData;
import com.pokevian.app.smartfleet.service.TripReportService;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.web.EventActivity;

import org.apache.log4j.Logger;

import java.net.URLDecoder;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import kr.co.fingerpush.android.FingerPushIntentService;
import kr.co.fingerpush.android.GCMConstants;
import kr.co.fingerpush.android.GCMFingerPushManager;
import kr.co.fingerpush.android.NetworkUtility.NetworkBitmapListener;

public class GCMIntentService extends FingerPushIntentService {

	private static final String TAG = "GCMIntentService";
	final Logger logger = Logger.getLogger(TAG);

	private GCMFingerPushManager mManager;
	
	private String mMode = "";
	private String mMessage = "";
	private String sound = "";
	private String badge = "";
	private String img = "";
	private String imageURL = "";
	private String mAppLink = "";
	private String mWebLink = "";
	private String mMsgTag = "";
	private String custom1 = "";
	private String custom2 = "";
	private String custom3 = "";
	
	@Override
	protected void onError(Context context, String errorid) {
		logger.error("onError#" + errorid);
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.android.gcm.GCMBaseIntentService#onMessage(android.content.Context, android.content.Intent)
	 * 
	 * data.mMessage : 메세지 내용
	 * data.sound : 메세지 수신음
	 * data.badge : 메세지 뱃지 수
	 * data.imgUrl : 이미지 경로
	 * data.mMsgTag : 메세지 번호
	 * data.custom1 : 커스텀 필드 키(홈페이지에서 입력한 키를 입력)
	 * data.custom2 : 커스텀 필드 키(홈페이지에서 입력한 키를 입력)
	 * data.custom3 : 커스텀 필드 키(홈페이지에서 입력한 키를 입력)
	 */
	
	@Override
	protected void onMessage(Context context, Intent intent) {

		super.onMessage(context, intent, new onMessageListener() {

			@Override
			public void onMessage(final Context context, Intent intent) {
				Bundle bundle = intent.getExtras();

				Iterator<String> iterator = bundle.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					String value = bundle.get(key).toString();
					logger.trace("onMessage#" + key + "=" + value);
				}

				if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE")) {
					try {
						int badgeCount = 0;

						String code = intent.getStringExtra("data.code");
						mMessage = intent.getStringExtra("data.message");
						sound = intent.getStringExtra("data.sound");
						badge = intent.getStringExtra("data.badge");
						img = intent.getStringExtra("data.img");
						imageURL = intent.getStringExtra("data.imgUrl");
						mWebLink = intent.getStringExtra("data.weblink");
						mAppLink = intent.getStringExtra("data.appLink");
						mMsgTag = intent.getStringExtra("data.msgTag");
						custom1 = intent.getStringExtra("data.custom1");
						custom2 = intent.getStringExtra("data.custom2");
						custom3 = intent.getStringExtra("data.custom3");

						// 문자열 처리가 필요한 경우가 있으므로, GCMFingerPushManager class에 있는 getText
						// 메소드를 통해서 메세지 제목과 메세지 내용을 가져온다.
						mManager = GCMFingerPushManager.getInstance(GCMIntentService.this);
						if (mMessage != null && !mMessage.trim().equals("")) {
							mMessage = mManager.getText(URLDecoder.decode(mMessage, "UTF-8"));
						}

						mMode = mManager.getReceiveCode(intent.getStringExtra("data.code")).optString("PT");

						if (badge != null && !badge.trim().equals("")) {
							try {
								badgeCount = Integer.parseInt(badge);
							} catch (Exception e) {
								badgeCount = 0;
							}
						}

//						SettingsStore setting = SettingsStore.getInstance();
//						setting.storeNewEventCount(setting.getNewEventCount() + 1);
//
//						Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
//						badgeIntent.putExtra("badge_count", setting.getNewEventCount());
//						badgeIntent.putExtra("badge_count_package_name", context.getPackageName());
//						badgeIntent.putExtra("badge_count_class_name", "com.pokevian.app.smartfleet.ui.intro.IntroActivity");
//
//						if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
//							badgeIntent.setFlags(0x00000020);
//						}
//
//						context.sendBroadcast(badgeIntent);

						// 뱃지 처리
//						if (badgeCount >= 0) {
//							Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
//							badgeIntent.putExtra("badge_count", badgeCount);
//							// 메인 메뉴에 나타나는 어플의 패키지 명
//							badgeIntent.putExtra("badge_count_package_name", context.getPackageName());
//							// 메인메뉴에 나타나는 어플의 클래스 명
//							badgeIntent.putExtra("badge_count_class_name", "com.pokevian.app.smartfleet.ui.intro.IntroActivity");
//
//							if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
//								intent.setFlags(0x00000020);
//							}
//
//							context.sendBroadcast(badgeIntent);
//						}


						Intent service = new Intent(getApplicationContext(), PushNotificationService.class);
						service.putExtra(PushNotificationService.EXTRA_TAG, mMsgTag);
						service.putExtra(PushNotificationService.EXTRA_MODE, mMode);
						service.putExtra(PushNotificationService.EXTRA_CONTENT, mMessage);
						service.putExtra(PushNotificationService.EXTRA_APP_LINK, mAppLink);
						service.putExtra(PushNotificationService.EXTRA_WEB_LINK, mWebLink);
						startService(service);

						SettingsStore setting = SettingsStore.getInstance();
						if (!TextUtils.isEmpty(mAppLink)) {
//							notifyNotification(buildDefaultNotification());
							if ("1".equals(mAppLink) /*|| "noti".equals(mAppLink)*/) {
								setting.storeNewNotiCount(setting.getNewNotiCount() + 1);
							} else if ("2".equals(mAppLink)) {
								setting.storeNewEventCount(setting.getNewEventCount() + 1);
							}

							Intent badgeIntent = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
							badgeIntent.putExtra("badge_count", setting.getNewEventCount() + setting.getNewNotiCount());
							badgeIntent.putExtra("badge_count_package_name", context.getPackageName());
							badgeIntent.putExtra("badge_count_class_name", "com.pokevian.app.smartfleet.ui.intro.IntroActivity");

							if(Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
								badgeIntent.setFlags(0x00000020);
							}
							Logger.getLogger(TAG).trace("badge count#" + setting.getNewEventCount() + setting.getNewNotiCount());

							context.sendBroadcast(badgeIntent);

						} else if (!TextUtils.isEmpty(mWebLink)) {
//							notifyNotification(buildWebLinkNotification());
						} else if (mManager.existImageURL(img)) {

							mManager.getAttatchedImageURL(imageURL, new NetworkBitmapListener() {

								@Override
								public void onError(String code, String errorMessage) {
									// TODO Auto-generated method stub
//									setNotification(context, mMessage, null, mMsgTag);
								}

								@Override
								public void onComplete(String code, String resultMessage, Bitmap bitmap) {
									// TODO Auto-generated method stub
//									setNotification(context, GCMIntentService.this.mMessage, bitmap, mMsgTag);
								}

								@Override
								public void onCancel() {
								}
							});

						} else {
//							setNotification(context, mMessage, null, mMsgTag);
						}

						// 얼럿창 생성
//						showAlertService(context, mMessage);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}


			}


		});
	}

	// ian
	private String getLauncherClassName(Context context) {

		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setPackage(getPackageName());

		List<ResolveInfo> resolveInfoList = getPackageManager().queryIntentActivities(intent, 0);
		if (resolveInfoList != null && resolveInfoList.size() > 0) {
			return resolveInfoList.get(0).activityInfo.name;
		}

		return null;
	}

	@Override
	protected void onRegistered(Context context, String regid) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onRegistered ::: regid:" + regid);
		GCMConstants.setProjectToken(getApplicationContext(), regid);
	}

	@Override
	protected void onUnregistered(Context context, String regid) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUnregistered ::: regid:" + regid);
	}

	private NotificationCompat.Builder getNotificationBuilder() {
		String contentTitle = getResources().getString(R.string.app_name);
		String contentText = mMessage;

		String[] content = mMessage.split("#");
		if (content.length >1) {
			contentTitle = content[0];
			contentText = content[1];
		}

		return new NotificationCompat.Builder(this)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setSmallIcon(R.drawable.ic_stat_notify_msg)
//				.setTicker(ticker)
				.setContentTitle(contentTitle)
				.setContentText(contentText)
				.setAutoCancel(true);
	}

	private Notification buildDefaultNotification() {

		 /*
		Intent intent = new Intent(this, TripReportActivity.class);
        intent.putExtra(TripReportActivity.EXTRA_TRIP, trip);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        */

		PackageManager pm = getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(getPackageName());
		intent.putExtra("appLink", mAppLink);
		intent.putExtra("msgTag", mMsgTag);
		intent.putExtra("mode", mMode);

		PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = getNotificationBuilder();
		builder.setContentIntent(pi);

		return builder.build();
	}

	private Notification buildWebLinkNotification() {

		Intent intent = new Intent(Intent.ACTION_VIEW)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
				.setData(Uri.parse(mWebLink));

		PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//		PendingIntent pi = PendingIntent.getService(context, 1, intent, PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder builder = getNotificationBuilder();
		builder.setContentIntent(pi);

		return builder.build();
	}

	private void notifyNotification(Notification notification) {

		int id = Integer.MAX_VALUE;
		try {
			id = Integer.parseInt(mMsgTag);
		} catch (NumberFormatException e) {
			logger.warn(e.getMessage());
		}

		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, notification);
	}

	private void setNotification(Context context, String message, Bitmap bitmap, String message_id) {
//		RecentTaskInfo recentTaskInfo = getRecentTaskInfo(getPackageName());

//		Logger.getLogger(TAG).trace(">>> " +  recentTaskInfo.baseIntent.toString());

//		Intent intent = new Intent(context, IntroActivity.class);
//		Intent intent = new Intent(context, MainActivity.class);
		Intent intent = new Intent(context, EventActivity.class);
//		if(recentTaskInfo != null) {
//			if(!recentTaskInfo.baseIntent.toString().contains("IntroActivity")) {
//				intent = recentTaskInfo.baseIntent;
//			}
//		} else {
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		}
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.setAction("com.pokevian.intent.action.view.notice");
		intent.putExtra("mMessage-id", message_id);
		intent.putExtra("appLink", mAppLink);

		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
		mBuilder.setSmallIcon(R.drawable.ic_noti);
		String[] content = message.split("#");
		if (content.length > 1) {
			mBuilder.setContentTitle(content[0]);
			mBuilder.setContentText(content[1]);
		} else {
			mBuilder.setContentTitle(context.getResources().getString(R.string.app_name));
			mBuilder.setContentText(message);
		}

//		mBuilder.setContentTitle(context.getResources().getString(R.string.event_noti_title));
//		mBuilder.setContentText(mMessage);

		if (bitmap != null) {
			BigPictureStyle notification = new NotificationCompat.BigPictureStyle();
			notification.setBigContentTitle(context.getResources().getString(R.string.app_name));
			notification.bigPicture(bitmap);
			notification.setSummaryText(message);
			mBuilder.setContentText("손으로 당겨주세요.");
	    	mBuilder.setStyle(notification);
	    	
		} else if ("LNGT".equals(mMode)) {
			NotificationCompat.BigTextStyle notification = new NotificationCompat.BigTextStyle();
			notification.setBigContentTitle(context.getResources().getString(R.string.app_name));
			notification.bigText(message);
			mBuilder.setContentText("손으로 당겨주세요.");
			mBuilder.setStyle(notification);
		}

		mBuilder.setContentIntent(pi);
		mBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
	
		Notification noti = mBuilder.build();
		noti.flags |= Notification.FLAG_AUTO_CANCEL;

		int id = Integer.MAX_VALUE;
		try {
			id = Integer.parseInt(message_id);
		} catch (NumberFormatException e) {
			logger.warn(e.getMessage());
		}
		
		// Send the notification.
				((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, noti);
	}
	
	private void showAlertService(Context context, String message){

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return;
		}

		Bundle b = new Bundle();
		b.putString("msg", message);
		
		Intent i = new Intent(context, GCMAlertService.class);
		i.putExtras(b);
		
		PendingIntent pi = PendingIntent.getService(context, 1, i, PendingIntent.FLAG_ONE_SHOT);
		try {
			pi.send();
		} catch (CanceledException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private RecentTaskInfo getRecentTaskInfo(final String packageName) {
	    final ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    try {
	        // 10 : # of tasks you want to take a look at
	        final List<RecentTaskInfo> infoList = manager.getRecentTasks(10, 0); 
	        for (RecentTaskInfo info : infoList) {
	            if (info.baseIntent.getComponent().getPackageName().equals(packageName)) {
	                return info;
	            }
	        }
	    } catch (NullPointerException e) {}
	    return null;
	 }
}

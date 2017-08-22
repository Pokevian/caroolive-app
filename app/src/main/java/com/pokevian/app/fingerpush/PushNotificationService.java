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

package com.pokevian.app.fingerpush;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.web.WebLinkActivity;

public class PushNotificationService extends Service {

    public static final String EXTRA_CONTENT = "extra.CONTENT";
    public static final String EXTRA_TAG = "extra.TAG";
    public static final String EXTRA_MODE = "extra.MODE";
    public static final String EXTRA_APP_LINK = "extra.APP_INK";
    public static final String EXTRA_WEB_LINK = "extra.WEB_LINK";

    public static final String ACTION_NOTIFY = "com.pokevian.app.smartfleet.action.NOTI";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String content = intent.getStringExtra(EXTRA_CONTENT);
            String tag = intent.getStringExtra(EXTRA_TAG);
            String type = intent.getStringExtra(EXTRA_MODE);
            String appLink = intent.getStringExtra(EXTRA_APP_LINK);
            String webLink = intent.getStringExtra(EXTRA_WEB_LINK);

            notify(content, tag, type, appLink, webLink);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void notify(String content, String tag, String type, String appLink, String webLink) {
        if (!TextUtils.isEmpty(appLink)) {
            notifyNotification(buildNotification(content, tag, type, appLink), tag);
        } else if (!TextUtils.isEmpty(webLink)) {
            notifyNotification(buildWebLinkNotification(content, tag, type, webLink), tag);
        } else {
            notifyNotification(buildNotification(content, tag, type, null), tag);
        }
    }

    private Notification buildNotification(String content, String tag, String type, String appLink) {

        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
//        if (!TextUtils.isEmpty(appLink)) {
//            intent.putExtra("appLink", appLink);
//            intent.putExtra("msgTag", tag);
//            intent.putExtra("mode", type);
//        }
        intent.putExtra("appLink", appLink);
        intent.putExtra("msgTag", tag);
        intent.putExtra("mode", type);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = getNotificationBuilder(content);
        builder.setContentIntent(pi);

        return builder.build();
    }

    private Notification buildWebLinkNotification(String content, String tag, String type, String link) {

//        Intent intent = new Intent(Intent.ACTION_VIEW)
//                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
//                .setData(Uri.parse(uriString));

        Intent intent = new Intent(this, WebLinkActivity.class);
        intent.putExtra("webLink", link);
        intent.putExtra("msgTag", tag);
        intent.putExtra("mode", type);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = getNotificationBuilder(content);
        builder.setContentIntent(pi);

        return builder.build();
    }

    private NotificationCompat.Builder getNotificationBuilder(String content) {
        String title = getResources().getString(R.string.app_name);
        String text = content;

        String[] contents = content.split("#");
        if (contents.length >1) {
            title = contents[0];
            text = contents[1];
        }

        return new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notify_msg)
				.setTicker(getResources().getText(R.string.notify_notification))
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
    }


    private void notifyNotification(Notification notification, String tag) {
        int id = Integer.MAX_VALUE;
        try {
            id = Integer.parseInt(tag);
        } catch (NumberFormatException e) {
        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, notification);

        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_NOTIFY));

    }

}

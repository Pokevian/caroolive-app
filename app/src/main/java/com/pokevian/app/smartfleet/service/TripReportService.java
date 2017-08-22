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

package com.pokevian.app.smartfleet.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleTrip;
import com.pokevian.app.smartfleet.setting.SettingsStore;

import java.text.NumberFormat;

public class TripReportService extends Service {

    public static final String EXTRA_TRIP = "extra.TRIP";

    private NotificationManager mNm;

    @Override
    public void onCreate() {
        super.onCreate();

        mNm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            VehicleTrip trip = (VehicleTrip) intent.getSerializableExtra(EXTRA_TRIP);

            notify(trip);

            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void notify(VehicleTrip trip) {
        String ticker = getString(R.string.noti_trip_report_driving_stopped);
        String contentTitle = getString(R.string.noti_trip_report_title);

        SettingsStore settingsStore = SettingsStore.getInstance();
        NumberFormat distanceFormatter = NumberFormat.getInstance();
        StringBuilder contentText = new StringBuilder()
                .append(getString(R.string.noti_trip_report_summary,
                        DateUtils.formatElapsedTime((int) trip.getDrivingTime()),
                        distanceFormatter.format((int) trip.getDrivingDistance()),
                        settingsStore.getDistanceUnit().toString()));

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_notify_msg)
                .setTicker(ticker)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true);

        /*Intent intent = new Intent(this, TripReportActivity.class);
        intent.putExtra(TripReportActivity.EXTRA_TRIP, trip);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);*/
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        intent.putExtra("appLink", "10");

        int requestCode = (int) trip.getBeginTime();

        PendingIntent pi = PendingIntent.getActivity(this,
                requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(pi);

        Notification n = nb.build();
        mNm.notify(requestCode, n);
    }

}

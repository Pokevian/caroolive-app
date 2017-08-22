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

package com.pokevian.app.smartfleet;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.android.volley.VolleyLog;
import com.google.api.client.util.SslUtils;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.report.CrashReportActivity;
import com.pokevian.app.smartfleet.util.LogUtils;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class SmartFleetApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure log
        LogUtils.configLog(this);

        // Init settings store
        SettingsStore.init(this);

        // Init Volley
        VolleySingleton.init(this);

        // Trust all SSL context!
        trustAllSSLContext();

        // handle exception
        handleException();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void trustAllSSLContext() {
        try {
            SSLContext sslContext = SslUtils.trustAllSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (GeneralSecurityException e) {
            Log.e("ssl", "failed to set ssl context!");
        }
    }

    private void handleException() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable e) {
                Logger logger = Logger.getLogger("stack-trace");
                logger.error("uncaught exception", e);

                Intent activity = new Intent(getApplicationContext(), CrashReportActivity.class);
                activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                activity.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                activity.putExtra(CrashReportActivity.EXTRA_EXCEPTION, e);

                PendingIntent operation = PendingIntent.getActivity(getApplicationContext(),
                        R.id.req_crash_report_activity, activity, 0);
                try {
                    operation.send();
                } catch (PendingIntent.CanceledException e1) {
                    logger.error("failed to start crash report activity");
                }

                // kill myself
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

    static {
        VolleyLog.DEBUG = false;
    }

}

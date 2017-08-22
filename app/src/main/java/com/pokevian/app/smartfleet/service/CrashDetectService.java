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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.pokevian.app.smartfleet.ui.report.CrashReportActivity;

import org.apache.log4j.Logger;

import java.lang.Thread.UncaughtExceptionHandler;

public class CrashDetectService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ExceptionHandler implements UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable e) {
            Logger logger = Logger.getLogger("stack-trace");
            logger.error("uncaught exception", e);

            Intent intent = new Intent(CrashDetectService.this, CrashReportActivity.class);
            intent.putExtra(CrashReportActivity.EXTRA_EXCEPTION, e);

            PendingIntent operation = PendingIntent.getActivity(CrashDetectService.this,
                    1, intent, 0);
            try {
                operation.send();
            } catch (CanceledException e1) {
                e1.printStackTrace();
            }

            // kill myself
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}

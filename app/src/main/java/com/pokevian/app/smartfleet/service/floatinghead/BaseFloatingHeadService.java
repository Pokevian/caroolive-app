/*
 * Copyright (c) 2015. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.service.floatinghead;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.IBinder;

import org.apache.log4j.Logger;

/**
 * Created by dg.kim on 2015-02-26.
 */
public abstract class BaseFloatingHeadService extends Service {

    protected static final String TAG = "FloatingHeadService";

    private int mCurrOrientation;

    @Override
    public void onCreate() {
        super.onCreate();

        Configuration configuration = getResources().getConfiguration();
        mCurrOrientation = configuration.orientation;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mCurrOrientation != newConfig.orientation) {
            Logger.getLogger(TAG).info("orientation changed: " + mCurrOrientation
                    + " to " + newConfig.orientation);

            onOrientationChanged(newConfig.orientation);

            mCurrOrientation = newConfig.orientation;
        }
    }

    protected abstract void onOrientationChanged(int orientation);

    protected static class LocalSavedState {
        protected final SharedPreferences mPrefs;

        protected LocalSavedState(Context context) {
            mPrefs = context.getSharedPreferences("floating_head.prefs", Context.MODE_PRIVATE);
        }
    }

}

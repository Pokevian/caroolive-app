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

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.pokevian.app.smartfleet.util.OrientationUtils;

import org.apache.log4j.Logger;

@SuppressWarnings("unused")
public class AlignMonitor implements SensorEventListener {

    static final String TAG = "AlignMonitor";
    final Logger logger = Logger.getLogger(TAG);

    private static final float G = 9.8f;
    private static final float MARGIN = 0.3f;

    private static final float MIN_G = G - MARGIN;
    private static final float MAX_G = G + MARGIN;
    private static final long MIN_INTERVAL = 1000000000; // 1 second

    private final Context mContext;
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private final AlignMonitorCallbacks mCallbacks;
    private long mPrevTimestamp;
    private boolean mIsRunning;

    public AlignMonitor(Context context, AlignMonitorCallbacks callbacks) {
        mContext = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mCallbacks = callbacks;
    }

    public void run() {
        if (!mIsRunning) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            mPrevTimestamp = System.nanoTime();
            mIsRunning = true;
        }
    }

    public void stop() {
        if (mIsRunning) {
            mSensorManager.unregisterListener(this);
            mIsRunning = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timeDiff = event.timestamp - mPrevTimestamp;
        if (timeDiff < MIN_INTERVAL) return;
        mPrevTimestamp = event.timestamp;

        final float gx = event.values[0];
        final float gy = event.values[1];
        final float gz = event.values[2];
        logger.debug("onSensorChanged(): Gx=" + gx + ", Gy=" + gy + ", Gz=" + gz);

        if (checkAligned(gx, gy, gz)) {
            mCallbacks.onAligned();
        } else {
            mCallbacks.onNotAligned();
        }
    }

    private boolean checkAligned(float gx, float gy, float gz) {
        final float absGx = Math.abs(gx);
        final float absGy = Math.abs(gy);

        int orientation = OrientationUtils.getDeviceOrientation(mContext);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return (MIN_G <= absGy && absGy <= MAX_G);
        } else {
            return (MIN_G <= absGx && absGx <= MAX_G);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface AlignMonitorCallbacks {
        void onAligned();

        void onNotAligned();
    }

}

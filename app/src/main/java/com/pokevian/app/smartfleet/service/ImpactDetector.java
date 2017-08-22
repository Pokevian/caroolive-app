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
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;

import com.pokevian.app.smartfleet.util.OrientationUtils;

import org.apache.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ImpactDetector implements SensorEventListener {

    static final String TAG = "ImpactDetector";
    final Logger logger = Logger.getLogger(TAG);

    public static final String ACTION_IMPACT_DETECTED = "com.pokevian.app.smartfleet.action.IMPACT_DETECTED";

    private static final float G = 9.81f;
    private static final float THRESHOLD_VERY_LOW = 2 * G * 0.20f;	//  3.924
    private static final float THRESHOLD_LOW = 2 * G * 0.40f;		//  7.848
    private static final float THRESHOLD_NORMAL = 2 * G * 0.60f;	//  11.772
    private static final float THRESHOLD_HIGH = 2 * G * 0.80f;		// 15.696
    private static final float THRESHOLD_VERY_HIGH = 2 * G * 1.0f;	// 19.620

    private final Context mContext;
    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private final ImpactDetectorCallbacks mCallbacks;
    private ImpactSensitivity mSensitivity = ImpactSensitivity.NORMAL;
    private AtomicBoolean mDetected = new AtomicBoolean(false);
    private boolean mIsRunning;
    private boolean mEnabled;
    private int mThreshold;
    private int mOrientation;

    // Constants for the guard time of each detection
    private final int mGuardTime = 1000; // 1 second

    // Constants for the low-pass filters
    private float mTimeConstant = 0.18f;
    private float mAlpha = 0.9f;
    private float mDt = 0;

    // Timestamps for the low-pass filters
    private float mTimestamp = System.nanoTime();
    private float mTimestampOld = -1;

    // Gravity and linear accelerations components for the Wikipedia low-pass filter
    private float[] mGravity = new float[]{0, 0, 0};
    private float[] mLinearAcceleration = new float[]{0, 0, 0};

    private int mCount = 0;

    public static enum ImpactSensitivity {
        VERY_HIGH, HIGH, NORMAL, LOW, VERY_LOW, OFF;

        public static int getThreshold(ImpactSensitivity sensitivity) {
            switch (sensitivity) {
                case VERY_HIGH:
                    return 5;
                case HIGH:
                    return 8; //6;
                default:
                case NORMAL:
                    return  12; //8;
                case LOW:
                    return 15; //10;
                case VERY_LOW:
                    return 20; //11;
                case OFF:
                    return Integer.MAX_VALUE;
            }
        }
    }

    public ImpactDetector(Context context, ImpactDetectorCallbacks callbacks) {
        mContext = context.getApplicationContext();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mCallbacks = callbacks;
    }

    public void setSensitivity(ImpactSensitivity sensitivity) {
        mSensitivity = sensitivity;
    }

    public void run() {
        if (mSensitivity == ImpactSensitivity.OFF) {
            return;
        }

        if (!mIsRunning) {
            mTimestampOld = System.nanoTime();

            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            mThreshold = ImpactSensitivity.getThreshold(mSensitivity);
            mOrientation = OrientationUtils.getDeviceOrientation(mContext);
            mIsRunning = true;
        }
    }

    public void stop() {
        if (mIsRunning) {
            mSensorManager.unregisterListener(this);
            mIsRunning = false;
        }
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] linearAcceleration = calculate(event.values);

        final float x = linearAcceleration[0];
        final float y = linearAcceleration[1];
        final float z = linearAcceleration[2];

        logger.trace(String.format("x=%.3f, y=%.3f, z=%.3f, th=%d, ori=%d",
                x, y, z, mThreshold, mOrientation));

        if (mEnabled && !mDetected.get() && checkThreshold(x, y, z)) {
            Intent intent = new Intent(ACTION_IMPACT_DETECTED);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

            mCallbacks.onImpact();

            mDetected.set(true);

            Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    mDetected.set(false);
                }
            }, mGuardTime);
        }
    }

    private float[] calculate(float[] values) {
        mTimestamp = System.nanoTime();

        // Find the sample period (between updates). Convert from nanoseconds to seconds
        mDt = 1 / (mCount / ((mTimestamp - mTimestampOld) / 1000000000.0f));

        mCount++;

        mAlpha = mTimeConstant / (mTimeConstant + mDt);

        if (mCount > 5) {
            mGravity[0] = mAlpha * mGravity[0] + (1 - mAlpha) * values[0];
            mGravity[1] = mAlpha * mGravity[1] + (1 - mAlpha) * values[1];
            mGravity[2] = mAlpha * mGravity[2] + (1 - mAlpha) * values[2];

            if (mCount > 10) {
                mLinearAcceleration[0] = values[0] - mGravity[0];
                mLinearAcceleration[1] = values[1] - mGravity[1];
                mLinearAcceleration[2] = values[2] - mGravity[2];
            }
        }

        return mLinearAcceleration;
    }

    private boolean checkThreshold(float x, float y, float z) {
        final float absX = Math.abs(x);
        final float absY = Math.abs(y);
        final float absZ = Math.abs(z);

        if (mOrientation == Configuration.ORIENTATION_PORTRAIT) {
            // e.g) Phone
            return ((mThreshold * 1.0f < absX) || (mThreshold < absY) || (mThreshold < absZ));
        } else {
            // e.g) Tablet
            return ((mThreshold < absX) || (mThreshold * 1.0f < absY) || (mThreshold < absZ));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface ImpactDetectorCallbacks {
        void onImpact();
    }

}

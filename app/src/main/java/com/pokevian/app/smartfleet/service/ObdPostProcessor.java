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
import android.location.Location;

import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.data.PersistData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.engine.PostProcessor.CustomProcessor;

import org.apache.log4j.Logger;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObdPostProcessor extends CustomProcessor {

    static final String TAG = "ObdPostProcessor";
    final Logger logger = Logger.getLogger(TAG);

    private final Context mContext;

    private int mOverspeedThreshold = Consts.DEFAULT_ECO_OVERSPEED_THRESHOLD;
    private OnTripResetCallback mTripResetCallback;

    private final Location mLastLocation = new Location("");
    private final Object mDataLock = new Object();

    private final ArrayDeque<BearingItem> mBearingQueue = new ArrayDeque<>();

    private AtomicBoolean mShouldResetTripData = new AtomicBoolean(false);
    private AtomicBoolean mDataBackupEnabled = new AtomicBoolean(false);

    public interface OnTripResetCallback {
        void onTripReset();
    }

    public ObdPostProcessor(Context context) {
        mContext = context.getApplicationContext();

//        int calcMask = CALC_LT_IDLING | CALC_HARSH_ACCEL | CALC_HARSH_BRAKE
//                | CALC_ECO_SPEED | CALC_OVERSPEED | CALC_LT_OVERSPEED
//                | CALC_WARNING | CALC_GEAR_SHIFT_N | CALC_USER
//                | CALC_HIGH_RPM;
//        setCalcMask(calcMask);
//
//        int tripMask = TRIP_USER;
//        setTripMask(tripMask);
    }

    public void resetTrip(OnTripResetCallback callback) {
        mTripResetCallback = callback;

        mShouldResetTripData.set(true);

        // clear persist data
        PersistData.clearLastData(mContext);
    }

    public void startDataBackup() {
        mDataBackupEnabled.set(true);
    }

    public void stopDataBackup() {
        mDataBackupEnabled.set(false);
    }

    public void onLocationChanged(Location location) {
        synchronized (mDataLock) {
            // Filter out condition
            boolean filterOut = mLastLocation.getLatitude() == location.getLatitude()
                    && mLastLocation.getLongitude() == location.getLongitude()
                    && mLastLocation.getAccuracy() == location.getAccuracy();

            if (!filterOut) {
                mLastLocation.set(location);
            }
        }
    }

    public void setOverspeedThreshold(int overspeed) {
        mOverspeedThreshold = overspeed;
    }

    @Override
    public void begin(ObdData data) {
        if (mShouldResetTripData.getAndSet(false)) {
            // reset trip related data (TRIP_FIRST ~ TRIP_LAST)
            for (int key = KEY.TRIP_FIRST; key <= KEY.TRIP_LAST; key++) {
                data.remove(key);
            }
            logger.info("reset trip related data");

            // clear persist data
            PersistData.clearLastData(mContext);

            // clear bearing queue
            mBearingQueue.clear();

            if (mTripResetCallback != null) {
                mTripResetCallback.onTripReset();
                mTripResetCallback = null;
            }
        }

        // Merge location
        synchronized (mDataLock) {
            merge(data, mLastLocation);
        }
    }

    private void merge(ObdData data, Location location) {
        if (!location.getProvider().isEmpty() && data != null) {
            data.put(KEY.LOC_TIME, location.getTime());
            data.put(KEY.LOC_LATITUDE, location.getLatitude());
            data.put(KEY.LOC_LONGITUDE, location.getLongitude());
            if (location.hasAltitude()) {
                data.put(KEY.LOC_ALTITUDE, location.getAltitude());
            } else {
                data.remove(KEY.LOC_ALTITUDE);
            }
            if (location.hasSpeed()) {
                data.put(KEY.LOC_SPEED, location.getSpeed());
            } else {
                data.remove(KEY.LOC_SPEED);
            }
            if (location.hasBearing()) {
                data.put(KEY.LOC_BEARING, location.getBearing());
            } else {
                data.remove(KEY.LOC_BEARING);
            }
            if (location.hasAccuracy()) {
                data.put(KEY.LOC_ACCURACY, location.getAccuracy());
            } else {
                data.remove(KEY.LOC_ACCURACY);
            }
        }
    }

    @Override
    public void end(ObdData data) {
        if (mDataBackupEnabled.get() && !mShouldResetTripData.get()) {
            PersistData.saveLastData(mContext, data);
        }
    }

    static class BearingItem {
        long time;
        float bearing;

        BearingItem(long time, float bearing) {
            this.time = time;
            this.bearing = bearing;
        }
    }

}

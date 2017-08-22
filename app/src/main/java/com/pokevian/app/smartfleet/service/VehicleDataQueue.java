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

import com.pokevian.app.smartfleet.model.VehicleData;

import org.apache.log4j.Logger;

import java.util.ArrayDeque;

public class VehicleDataQueue extends ArrayDeque<VehicleData> {
    private static final long serialVersionUID = 1L;

    static final String TAG = "VehicleDataQueue";
    final Logger logger = Logger.getLogger(TAG);

    private static final int MIN_TIME_DIFF = 0; // 0: keep all data
    private static final int MAX_VEHICLE_DATA = 10;

    private String mAccountId;
    private String mVehicleId;

    public void setup(String accountId, String vehicleId) {
        mAccountId = accountId;
        mVehicleId = vehicleId;

        clear();
    }

    public String getAccountId() {
        return mAccountId;
    }

    public String getVehicleId() {
        return mVehicleId;
    }

    public void enqueue(VehicleData data) {
        if (needToEnqueue(data)) {
            addLast(data);
        }
    }

    private boolean needToEnqueue(VehicleData data) {
        VehicleData lastData = peekLast();
        if (lastData != null && lastData.getLocation() != null) {
            if (data.getLocation() != null && data.getEvent() == null && data.getTrip() == null
                    && (data.getLocation().getTime() - lastData.getLocation().getTime()) < MIN_TIME_DIFF) {
                //logger.debug("needToEnqueue(): ignore data");
                return false;
            }
        }
        return true;
    }

    public boolean needToFlush() {
        return size() >= MAX_VEHICLE_DATA;
    }

}

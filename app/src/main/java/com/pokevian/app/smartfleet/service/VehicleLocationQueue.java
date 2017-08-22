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

import com.pokevian.app.smartfleet.model.VehicleData.VehicleLocation;

import org.apache.log4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;

public class VehicleLocationQueue extends ArrayDeque<VehicleLocation> {
    private static final long serialVersionUID = 1L;

    static final String TAG = "VehicleLocationQueue";
    final Logger logger = Logger.getLogger(TAG);

    private static final int MAX_TIME_DIFF = 30000; // 30 seconds

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

    public void enqueue(VehicleLocation location) {
        addLast(location);

        // Remove locations in the past 30 seconds
        final long lastTime = location.getTime();
        while (!isEmpty()) {
            final long firstTime = peekFirst().getTime();
            if ((lastTime - firstTime) > MAX_TIME_DIFF) {
                //logger.debug("enqueue(): remove a past location");
                pollFirst();
            } else {
                break;
            }
        }
        //logger.debug("enqueue(): queue size=" + size());
    }

    public ArrayList<VehicleLocation> retrieve(long fromTime, long toTime) {
        ArrayList<VehicleLocation> locations = new ArrayList<VehicleLocation>();

        Iterator<VehicleLocation> iter = iterator();
        while (iter.hasNext()) {
            VehicleLocation location = iter.next();
            if (fromTime <= location.getTime() && location.getTime() <= toTime) {
                locations.add(location);
            }
        }

        return locations;
    }

}

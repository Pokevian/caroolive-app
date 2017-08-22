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
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleData;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleEvent;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleLocation;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleTrip;
import com.pokevian.app.smartfleet.util.PackageUtils;
import com.pokevian.app.smartfleet.util.TelephonyUtils;
import com.pokevian.app.smartfleet.volley.GsonRequest.GsonDateTypeAdapter;
import com.pokevian.caroo.common.model.ThreeState;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.model.code.EventTpCd;
import com.pokevian.caroo.common.smart.model.SmartEvent;
import com.pokevian.caroo.common.smart.model.SmartMessage;
import com.pokevian.caroo.common.smart.model.SmartMessageList;
import com.pokevian.caroo.common.smart.model.SmartRecord;
import com.pokevian.caroo.common.smart.model.SmartTrip;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class TripManager {

    protected static long totalSendSize = 0;
    static final String TAG = "trip-manager";

    private final Context mContext;
    private final Gson mGson;
    private final VehicleDataQueue mVehicleDataQueue;
    private final VehicleLocationQueue mVehicleLocationQueue;
    private VehicleTrip mTrip;

    private final int TARGET_TRIP_UPLOAD_COUNT = 60; // about 60 seconds
    private int mDataCount;

    private boolean mPendingEmergencyEvent;

    public TripManager(Context context) {
        mContext = context;
        mGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
                .create();
        mVehicleDataQueue = new VehicleDataQueue();
        mVehicleLocationQueue = new VehicleLocationQueue();

        totalSendSize = 0;
    }

    public boolean openTrip(String accountId, Vehicle vehicle) {
        if (accountId == null || vehicle == null || vehicle.getVehicleId() == null) {
            Logger.getLogger(TAG).error("openTrip(): invalid parameter: accountId=" + accountId + ", vehicle=" + vehicle);
            return false;
        }

        String vehicleId = vehicle.getVehicleId();

        mTrip = new VehicleTrip(accountId, vehicleId,TelephonyUtils.getNetworkOperatorName(mContext));
        String name = PackageUtils.getVersionName(mContext);
        if (BuildConfig.DEBUG && name.contains("-SNAPSHOT")) {
            name = name.substring(0, name.indexOf("-"));
        }
        mTrip.setAppVer(name);
        mTrip.setDevOs("ANDROID");
        mTrip.setDevOsVer(Build.VERSION.RELEASE);
        mTrip.setState(SmartTrip.TripState.opened);
        mDataCount = 0;

        synchronized (mVehicleDataQueue) {
            mVehicleDataQueue.setup(accountId, vehicleId);
        }
        synchronized (mVehicleLocationQueue) {
            mVehicleLocationQueue.setup(accountId, vehicleId);
        }

        mPendingEmergencyEvent = false;

        Logger.getLogger(TAG).info("trip opened");

        return false;
    }

    public VehicleTrip closeTrip() {
        uploadVehicleData(true);

        VehicleTrip tripBk = mTrip;

        // Upload trip to server
        if (mTrip != null) {
            mTrip.setState(SmartTrip.TripState.closed);
            if (isMeaningfulTrip()) {
                uploadTrip();
            } else {
                tripBk = null;
                Logger.getLogger(TAG).debug("Not meaningful trip -> drop: " + mTrip);
            }
            Logger.getLogger(TAG).info("trip state: " + mTrip.getState());

            mTrip = null;

            Logger.getLogger(TAG).info("trip closed");
        }

        return tripBk;
    }

    public VehicleLocation getLastLocation() {
        return mVehicleLocationQueue.peekLast();
    }

    public String getLocationMetaJson(long fromTime, long toTime) {
        synchronized (mVehicleLocationQueue) {
            ArrayList<VehicleLocation> locations = mVehicleLocationQueue.retrieve(fromTime, toTime);
            ArrayList<SmartRecord> smList = toServerBean(locations);
            return mGson.toJson(smList);
        }
    }

    public void onVehicleEngineStatusChanged(int ves) {
    }

    public void onObdDataReceived(ObdData data) {
        if (data.getBoolean(KEY.DATA_DUPL, false)) {
            // ignore duplicated data
            return;
        }

        if (mTrip != null) {
            int ves = data.getInteger(KEY.CALC_VES, VehicleEngineStatus.UNKNOWN);
            if (VehicleEngineStatus.isOnDriving(ves)) {
                updateTrip(data);

                if (isMeaningfulTrip() && (mDataCount++ % TARGET_TRIP_UPLOAD_COUNT) == 0) {
                    Logger.getLogger(TAG).info("trip state: " + mTrip.getState());

                    uploadTrip();

                    SmartTrip.TripState state = mTrip.getState();
                    if (state == SmartTrip.TripState.opened) {
                        mTrip.setState(SmartTrip.TripState.updating);
                    }
                }
            }
            if (enqueueVehicleData(data)) {
                uploadVehicleData(false);
            }
        } else {
            Logger.getLogger(TAG).trace("trip is not opened yet!");
        }
    }

    private void updateTrip(ObdData obdData) {
        if (mTrip != null) {
            mTrip.setBeginTime(obdData.getLong(KEY.TRIP_START_TIME, System.currentTimeMillis()));
            mTrip.setEndTime(obdData.getLong(KEY.TRIP_END_TIME, System.currentTimeMillis()));
            mTrip.setDrivingTime(obdData.getFloat(KEY.TRIP_DRIVING_TIME, 0));
            mTrip.setDrivingDistance(obdData.getFloat(KEY.TRIP_DRIVING_DIST, 0));

            mTrip.setHarshAccelCount(obdData.getInteger(KEY.TRIP_HARSH_ACCEL_COUNT, 0));
            mTrip.setHarshBrakeCount(obdData.getInteger(KEY.TRIP_HARSH_BRAKE_COUNT, 0));
            mTrip.setHarshRpmCount(obdData.getInteger(KEY.TRIP_HARSH_RPM_COUNT, 0));
            mTrip.setMaxVss(obdData.getFloat(KEY.TRIP_MAX_VSS, 0));
            mTrip.setAvgVssNi(obdData.getFloat(KEY.TRIP_AVG_VSS_NI, 0));
            mTrip.setAvgVssWi(obdData.getFloat(KEY.TRIP_AVG_VSS_WI, 0));
            mTrip.setMaxRpm(obdData.getFloat(KEY.TRIP_MAX_RPM, 0));
            mTrip.setAvgRpm(obdData.getFloat(KEY.TRIP_AVG_RPM, 0));
            mTrip.setFuelEconomy(obdData.getFloat(KEY.TRIP_FUEL_ECONOMY, 0));
            mTrip.setFuelConsumption(obdData.getFloat(KEY.TRIP_FUEL_CONSUMPTION, 0));
            mTrip.setCo2Emission(obdData.getFloat(KEY.TRIP_CO2, 0));

            mTrip.setSteadySpeedTime(obdData.getFloat(KEY.TRIP_STEADY_SPEED_TIME, 0));
            mTrip.setHighRpmTime(obdData.getFloat(KEY.TRIP_HIGH_RPM_TIME, 0));
            mTrip.setFuelCutTime(obdData.getFloat(KEY.TRIP_FUEL_CUT_TIME, 0));
            mTrip.setWarmUpTime(obdData.getFloat(KEY.TRIP_WARM_UP_TIME, 0));

            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_0, obdData.getFloat(KEY.TRIP_IDLING_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_1, obdData.getFloat(KEY.TRIP_SPEED_ZONE1_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_2, obdData.getFloat(KEY.TRIP_SPEED_ZONE2_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_3, obdData.getFloat(KEY.TRIP_SPEED_ZONE3_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_4, obdData.getFloat(KEY.TRIP_SPEED_ZONE4_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_5, obdData.getFloat(KEY.TRIP_SPEED_ZONE5_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_6, obdData.getFloat(KEY.TRIP_SPEED_ZONE6_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_7, obdData.getFloat(KEY.TRIP_SPEED_ZONE7_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_8, obdData.getFloat(KEY.TRIP_SPEED_ZONE8_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_9, obdData.getFloat(KEY.TRIP_SPEED_ZONE9_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_10, obdData.getFloat(KEY.TRIP_SPEED_ZONE10_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_11, obdData.getFloat(KEY.TRIP_SPEED_ZONE11_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_12, obdData.getFloat(KEY.TRIP_SPEED_ZONE12_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_13, obdData.getFloat(KEY.TRIP_SPEED_ZONE13_TIME, 0));
            mTrip.setSpeedZoneTime(VehicleTrip.SPEED_ZONE_14, obdData.getFloat(KEY.TRIP_SPEED_ZONE14_TIME, 0));

            mTrip.setEngineDistance(obdData.getInteger(KEY.SAE_DIST, 0));

            mTrip.setFuelEconomyA(obdData.getFloat(KEY.TRIP_FUEL_ECONOMY_A, 0));
            mTrip.setFuelEconomyB(obdData.getFloat(KEY.TRIP_FUEL_ECONOMY_B, 0));
            mTrip.setFuelEconomyC(obdData.getFloat(KEY.TRIP_FUEL_ECONOMY_C, 0));

            Logger.getLogger(TAG).trace("updateTrip");
        }
    }

    private void uploadTrip() {
        Intent service = new Intent(mContext, DataUploadService.class);
        SmartMessageList sml = toServerBean(mTrip);
        String data = mGson.toJson(sml);

        service.putExtra(DataUploadService.EXTRA_DATA, data);
        mContext.startService(service);

        Logger.getLogger(TAG).debug("uploadTrip#" + data);
    }

    private boolean isMeaningfulTrip() {
        if (mTrip != null) {
            // Check driving distance
            float distance = mTrip.getDrivingDistance();
            if (distance >= 1/*km*/) {
                return true;
            }
        }
        return false;
    }

    private boolean enqueueVehicleData(ObdData obdData) {
        if (mTrip != null) {
            VehicleData data = new VehicleData();
            data.setTime(System.currentTimeMillis());

            // Location
            VehicleLocation location = buildLocation(obdData);
            data.setLocation(location);

            // Event
            VehicleEvent event = buildEvent(obdData);
            //logger.info("# event=" + event);
            data.setEvent(event);

            // Enqueue location
            synchronized (mVehicleLocationQueue) {
                mVehicleLocationQueue.enqueue(location);
            }

            // Enqueue vehicle data
            synchronized (mVehicleDataQueue) {
                mVehicleDataQueue.enqueue(data);
            }

            return true;
        } else {
            Logger.getLogger(TAG).debug("enqueueVehicleData(): invalid location");
            return false;
        }
    }

    private VehicleLocation buildLocation(ObdData obdData) {
        VehicleLocation location = new VehicleLocation();
        location.setTime(obdData.getLong(KEY.CALC_TIME, System.currentTimeMillis()));

        if (obdData.isValid(KEY.LOC_LATITUDE) && obdData.isValid(KEY.LOC_LONGITUDE)) {
            location.setLatitude(obdData.getDouble(KEY.LOC_LATITUDE, 0));
            location.setLongitude(obdData.getDouble(KEY.LOC_LONGITUDE, 0));
            location.setAccuracy(obdData.getFloat(KEY.LOC_ACCURACY, -1));
        }

        if (obdData.isValid(KEY.SAE_RPM)) {
            location.setRpm(obdData.getFloat(KEY.SAE_RPM));
        }
        if (obdData.isValid(KEY.SAE_VSS)) {
            location.setVss(obdData.getInteger(KEY.SAE_VSS));
        }
        if (obdData.isValid(KEY.SAE_MAP)) {
            location.setMap(obdData.getInteger(KEY.SAE_MAP));
        }
        if (obdData.isValid(KEY.SAE_IAT)) {
            location.setIat(obdData.getInteger(KEY.SAE_IAT));
        }
        if (obdData.isValid(KEY.SAE_MAF)) {
            location.setMaf(obdData.getFloat(KEY.SAE_MAF));
        }
        if (obdData.isValid(KEY.SAE_CER)) {
            location.setCer(obdData.getFloat(KEY.SAE_CER));
        }
        if (obdData.isValid(KEY.SAE_FSS_1)) {
            location.setFss1(obdData.getInteger(KEY.SAE_FSS_1));
        }
        if (obdData.isValid(KEY.SAE_LOAD_PCT)) {
            location.setLoadPct(obdData.getFloat(KEY.SAE_LOAD_PCT));
        }
        if (obdData.isValid(KEY.SAE_LOAD_ABS)) {
            location.setLoadAbs(obdData.getFloat(KEY.SAE_LOAD_ABS));
        }
        if (obdData.isValid(KEY.SAE_TP)) {
            location.setTp(obdData.getFloat(KEY.SAE_TP));
        }
        if (obdData.isValid(KEY.SAE_TP_REL)) {
            location.setTpRel(obdData.getFloat(KEY.SAE_TP_REL));
        }
        if (obdData.isValid(KEY.SAE_ACCEL_D)) {
            location.setAccelD(obdData.getFloat(KEY.SAE_ACCEL_D));
        }
        if (obdData.isValid(KEY.SAE_MIL)) {
            location.setMil(obdData.getBoolean(KEY.SAE_MIL) ? TwoState.Y : TwoState.N);
        }
        if (obdData.isValid(KEY.SAE_ECT)) {
            location.setEct(obdData.getInteger(KEY.SAE_ECT));
        }
        if (obdData.isValid(KEY.SAE_DIST)) {
            location.setDist(obdData.getInteger(KEY.SAE_DIST));
        }
        if (obdData.isValid(KEY.CALC_AUX_BAT)) {
            location.setAuxBat(obdData.getFloat(KEY.CALC_AUX_BAT));
        }
        if (obdData.isValid(KEY.SAE_FLI)) {
            location.setFli(obdData.getFloat(KEY.SAE_FLI));
        }

        if (obdData.isValid(KEY.SAE_LFT_B1)) {
            location.setLftB1(obdData.getFloat(KEY.SAE_LFT_B1));
        }
        if (obdData.isValid(KEY.SAE_TA)) {
            location.setTa(obdData.getFloat(KEY.SAE_TA));
        }
        if (obdData.isValid(KEY.SAE_O2S_PRESENT)) {
            location.setO2sPresent(obdData.getInteger(KEY.SAE_O2S_PRESENT));
        }
        if (obdData.isValid(KEY.SAE_O2S2_V_B1)) {
            location.setO2s2VB1(obdData.getFloat(KEY.SAE_O2S2_V_B1));
        }
        if (obdData.isValid(KEY.SAE_O2S2_FT_B1)) {
            location.setO2s2FtB1(obdData.getFloat(KEY.SAE_O2S2_FT_B1));
        }
        if (obdData.isValid(KEY.SAE_OBD_STD)) {
            location.setObdStd(obdData.getInteger(KEY.SAE_OBD_STD));
        }
        if (obdData.isValid(KEY.SAE_RUNTIME)) {
            location.setRuntime(obdData.getInteger(KEY.SAE_RUNTIME));
        }
        if (obdData.isValid(KEY.SAE_DIST_MIL)) {
            location.setDistMil(obdData.getInteger(KEY.SAE_DIST_MIL));
        }
        if (obdData.isValid(KEY.SAE_FRP_D)) {
            location.setFrpD(obdData.getInteger(KEY.SAE_FRP_D));
        }
        if (obdData.isValid(KEY.SAE_BARO)) {
            location.setBaro(obdData.getInteger(KEY.SAE_BARO));
        }
        if (obdData.isValid(KEY.SAE_AAT)) {
            location.setAat(obdData.getInteger(KEY.SAE_AAT));
        }
        if (obdData.isValid(KEY.SAE_FUEL_TYPE)) {
            location.setFuelType(obdData.getInteger(KEY.SAE_FUEL_TYPE));
        }

        if (obdData.isValid(KEY.SUPPORT_PID_00)) {
            location.setSupportPid00(obdData.getInteger(KEY.SUPPORT_PID_00));
        }
        if (obdData.isValid(KEY.SUPPORT_PID_20)) {
            location.setSupportPid20(obdData.getInteger(KEY.SUPPORT_PID_20));
        }
        if (obdData.isValid(KEY.SUPPORT_PID_40)) {
            location.setSupportPid40(obdData.getInteger(KEY.SUPPORT_PID_40));
        }
        if (obdData.isValid(KEY.SAE_FP)) {
            location.setFp(obdData.getInteger(KEY.SAE_FP));
        }
        if (obdData.isValid(KEY.SAE_O2S1_V_B1)) {
            location.setO2s1VB1(obdData.getFloat(KEY.SAE_O2S1_V_B1));
        }
        if (obdData.isValid(KEY.SAE_O2S1_FT_B1)) {
            location.setO2s1FtB1(obdData.getFloat(KEY.SAE_O2S1_FT_B1));
        }
        if (obdData.isValid(KEY.SAE_O2S1_WR_LAMBDA_ER)) {
            location.setO2s1WrLambdaEr(obdData.getInteger(KEY.SAE_O2S1_WR_LAMBDA_ER));
        }
        if (obdData.isValid(KEY.SAE_O2S1_WR_LAMBDA_V)) {
            location.setO2s1WrLambdaV(obdData.getInteger(KEY.SAE_O2S1_WR_LAMBDA_V));
        }
        if (obdData.isValid(KEY.SAE_EOT)) {
            location.setEot(obdData.getInteger(KEY.SAE_EOT));
        }

        long time = obdData.getLong(KEY.CALC_DIFF_TIME);
        location.setDiffTime((int) time);
        location.setDiffVss(Float.valueOf(obdData.getInteger(KEY.CALC_DIFF_VSS)));

        return location;
    }

    private VehicleEvent buildEvent(ObdData obdData) {
        VehicleEvent event = new VehicleEvent();

        if (obdData.isValid(KEY.WARN_OVER_AUX_BAT)) {
            if (obdData.getBoolean(KEY.WARN_OVER_AUX_BAT)) {
                event.setOverAuxBatteryLevel(ThreeState.Y);
            } else {
                event.setOverAuxBatteryLevel(ThreeState.N);
            }
        }

        if (obdData.isValid(KEY.WARN_UNDER_AUX_BAT)) {
            if (obdData.getBoolean(KEY.WARN_UNDER_AUX_BAT)) {
                event.setUnderAuxBatteryLevel(ThreeState.Y);
            } else {
                event.setUnderAuxBatteryLevel(ThreeState.N);
            }
        }

        if (obdData.isValid(KEY.WARN_OVERHEAT)) {
            if (obdData.getBoolean(KEY.WARN_OVERHEAT)) {
                event.setOverHeated(ThreeState.Y);
            } else {
                event.setOverHeated(ThreeState.N);
            }
        }

//        if (obdData.isValid(KEY.WARN_MIL_ON)) {
//            if (obdData.getBoolean(KEY.WARN_MIL_ON)) {
//                event.setMilOn(ThreeState.Y);
//                event.setDtc(obdData.getString(KEY.SAE_DTC, null));
//            } else {
//                event.setMilOn(ThreeState.N);
//                event.setDtc(null);
//            }
//        }
        if (obdData.getBoolean(KEY.WARN_MIL_ON, false)) {
            event.setMilOn(ThreeState.Y);
            event.setDtc(obdData.getString(KEY.WARN_DTC));
        } else {
            event.setMilOn(ThreeState.N);
            event.setDtc(null);
        }

        // harsh accel event
        if (obdData.getBoolean(KEY.CALC_HARSH_ACCEL, false)
                && !obdData.getBoolean(KEY.CALC_HARSH_ACCEL_CONTINUE, false)) {
            event.addEvent(EventTpCd.harshAccel);
        }

        // harsh brake event
        if (obdData.getBoolean(KEY.CALC_HARSH_BRAKE, false)
                && !obdData.getBoolean(KEY.CALC_HARSH_BRAKE_CONTINUE, false)) {
            event.addEvent(EventTpCd.harshDecel);
        }

        // collision event, more then 1 dtc detected
        // only 1 time in a trip
        if (!mTrip.getCollision() && obdData.getInteger(KEY.SAE_DTC_CNT, 0) > 1) {
            mTrip.setCollision(true);
            event.addEvent(EventTpCd.collision);
        }

        // emergency event
        if (mPendingEmergencyEvent) {
            mPendingEmergencyEvent = false;
            event.addEvent(EventTpCd.emergency);
        }

        return event;
    }

    private void uploadVehicleData(boolean flush) {
        synchronized (mVehicleDataQueue) {
            if (flush || mVehicleDataQueue.needToFlush()) {
                SmartMessageList sml = toServerBean(mVehicleDataQueue);
                mVehicleDataQueue.clear();
                if (sml.getMsgList().size() > 0) {
                    String data = mGson.toJson(sml);

                    Intent service = new Intent(mContext, DataUploadService.class);
                    service.putExtra(DataUploadService.EXTRA_DATA, data);
                    mContext.startService(service);

//                    logger.debug("upload vehicle data...");
                }
            }
        }
    }

    private SmartMessageList toServerBean(VehicleTrip trip) {
        SmartMessageList sml = new SmartMessageList();
        sml.setMemberNo(trip.getAccountId());
        sml.setCarNo(trip.getVehicleId());

        ArrayList<SmartMessage> smList = new ArrayList<>();
        sml.setMsgList(smList);

        SmartMessage sm = new SmartMessage();
        smList.add(sm);

        SmartTrip st = trip.toServerBean();
        sm.setTrip(st);

        return sml;
    }

    private SmartMessageList toServerBean(VehicleDataQueue queue) {
        SmartMessageList sml = new SmartMessageList();
        sml.setMemberNo(queue.getAccountId());
        sml.setCarNo(queue.getVehicleId());

        ArrayList<SmartMessage> smList = new ArrayList<>();
        sml.setMsgList(smList);

        Iterator<VehicleData> iter = queue.iterator();
        while (iter.hasNext()) {
            VehicleData data = iter.next();

            SmartMessage sm = new SmartMessage();
            smList.add(sm);

            // Location
            VehicleLocation location = data.getLocation();
            if (location != null) {
                SmartRecord sl = location.toServerBean();
                sm.setLocation(sl);
            }

            // Event
            VehicleEvent event = data.getEvent();
            if (event != null) {
                SmartEvent se = event.toServerBean();
                sm.setEvent(se);
            }
        }

        return sml;
    }

    private ArrayList<SmartRecord> toServerBean(ArrayList<VehicleLocation> locations) {
        ArrayList<SmartRecord> slList = new ArrayList<>();
        for (VehicleLocation location : locations) {
            SmartRecord sl = new SmartRecord();
            sl.setReportDate(new Date(location.getTime()));
            sl.setLatitude(String.valueOf(location.getLatitude()));
            sl.setLongitude(String.valueOf(location.getLongitude()));
            sl.setVss(location.getVss());
            slList.add(sl);
        }
        return slList;
    }

    public void onEmergencyEvent() {
        if (!mPendingEmergencyEvent) {
            mPendingEmergencyEvent = true;
        }
    }

}

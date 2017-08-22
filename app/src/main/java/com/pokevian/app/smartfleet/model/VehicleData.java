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

package com.pokevian.app.smartfleet.model;

import com.pokevian.caroo.common.model.ThreeState;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.model.code.EventTpCd;
import com.pokevian.caroo.common.smart.model.SmartEvent;
import com.pokevian.caroo.common.smart.model.SmartRecord;
import com.pokevian.caroo.common.smart.model.SmartTrip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class VehicleData implements Serializable {
    private static final long serialVersionUID = 1L;

    private long time;
    private VehicleLocation location;
    private VehicleEvent event;
    private VehicleTrip trip;

    public VehicleData() {
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public VehicleLocation getLocation() {
        return location;
    }

    public void setLocation(VehicleLocation location) {
        this.location = location;
    }

    public VehicleEvent getEvent() {
        return event;
    }

    public void setEvent(VehicleEvent event) {
        this.event = event;
    }

    public VehicleTrip getTrip() {
        return trip;
    }

    public void setTrip(VehicleTrip trip) {
        this.trip = trip;
    }

    public static class VehicleLocation implements Serializable {
        private static final long serialVersionUID = 1L;

        private long time;
        private Double latitude;
        private Double longitude;
        private Float accuracy;
        private Float rpm;        // Engine Speed
        private Integer vss;    // Vehicle Speed
        private Integer map;    // Intake manifold absolute pressure
        private Integer iat;    // Intake air temperature
        private Float maf;        // Mass air flow rate
        private Float cer;        // Command equivalence ratio
        private Integer fss1;    // Fuel system status
        private Float sftB1;    // Short term fuel % trim—Bank 1
        private Float loadPct;    // Calculated engine load value
        private Float loadAbs;    // Absolute load value
        private Float tp;        // Throttle position
        private Float tpRel;    // Relative throttle position
        private Float accelD;    // Accelerator pedal position D
        private TwoState mil;    // Malfunction Indicator Lamp
        private Integer ect;    // Engine coolant temperature
        private Integer dist;    // Distance traveled since codes cleared
        private Float auxBat;    // Aux. battery level
        private Float fli;        // Fuel level input

        private Float xAxis;                                                 //x축
        private Float yAxis;                                                 //y축
        private Float zAxis;                                                 //z축

        //new
        private Float lftB1;                                               //Long term fuel % trim—Bank 1
        private Float ta;                                                  //Timing Advance (° relative to #1 cylinder)
        private Integer o2sPresent;                                        //Oxygen sensors present
        private Float o2s2VB1;                                             //Bank 1, Sensor 2: Oxygen sensor voltage (V)
        private Float o2s2FtB1;                                            //Bank 1, Sensor 2: Short term fuel trim (%)
        private Integer obdStd;                                            //OBD Standard
        private Integer runtime;                                           //Run time since engine start. (sec) 실제 Trip 타임으로 간주할 수 있으나, 미지원 차량도 있고, Trip 타임을 주로 애플리케이션에서 별도 관리함
        private Integer distMil;                                           //Distance traveled with malfunction indicator lamp (MIL) on (km)
        private Integer frpD;                                              //Fuel Rail Pressure (diesel, or gasoline direct inject) (kPa) Gasoline/LPG 엔진에서 미사용
        private Integer baro;                                              //Barometric pressure (kPa)
        private Integer aat;                                               //Ambient Air Temperature  (°C)
        private Integer fuelType;                                          //Fuel Type.

        //new
        private Integer supportPid00;                                      //지원 PID
        private Integer fp;                                                //Fuel pressure (kPa (guage))
        private Float o2s1VB1;                                             //Bank 1, Sensor 1: Oxygen sensor voltage (V)
        private Float o2s1FtB1;                                            //Bank 1, Sensor 1: Short term fuel trim (%)
        private Integer supportPid20;                                      //지원 PID
        private Integer o2s1WrLambdaEr;                                    //O2S1 wide range lambda : Equivalence Ratio
        private Integer o2s1WrLambdaV;                                     //O2S1 wide range lambda : Voltage (V)
        private Integer supportPid40;                                      //지원 PID

        private Integer eot;                                               //Engine Oil Temperature
        private String validPids;                                          //really supported pids of pid groups

        private Float diffVss;                                             //VSS difference between current and previous
        private int diffTime;                                            //Time difference between current and previous

        public VehicleLocation() {
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public Float getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Float accuracy) {
            this.accuracy = accuracy;
        }

        public Float getRpm() {
            return rpm;
        }

        public void setRpm(Float rpm) {
            this.rpm = rpm;
        }

        public Integer getVss() {
            return vss;
        }

        public void setVss(Integer vss) {
            this.vss = vss;
        }

        public Integer getMap() {
            return map;
        }

        public void setMap(Integer map) {
            this.map = map;
        }

        public Integer getIat() {
            return iat;
        }

        public void setIat(Integer iat) {
            this.iat = iat;
        }

        public Float getMaf() {
            return maf;
        }

        public void setMaf(Float maf) {
            this.maf = maf;
        }

        public Float getCer() {
            return cer;
        }

        public void setCer(Float cer) {
            this.cer = cer;
        }

        public Integer getFss1() {
            return fss1;
        }

        public void setFss1(Integer fss1) {
            this.fss1 = fss1;
        }

        public Float getSftB1() {
            return sftB1;
        }

        public void setSftB1(Float sftB1) {
            this.sftB1 = sftB1;
        }

        public Float getLoadPct() {
            return loadPct;
        }

        public void setLoadPct(Float loadPct) {
            this.loadPct = loadPct;
        }

        public Float getLoadAbs() {
            return loadAbs;
        }

        public void setLoadAbs(Float loadAbs) {
            this.loadAbs = loadAbs;
        }

        public Float getTp() {
            return tp;
        }

        public void setTp(Float tp) {
            this.tp = tp;
        }

        public Float getTpRel() {
            return tpRel;
        }

        public void setTpRel(Float tpRel) {
            this.tpRel = tpRel;
        }

        public Float getAccelD() {
            return accelD;
        }

        public void setAccelD(Float accelD) {
            this.accelD = accelD;
        }

        public TwoState getMil() {
            return mil;
        }

        public void setMil(TwoState mil) {
            this.mil = mil;
        }

        public Integer getEct() {
            return ect;
        }

        public void setEct(Integer ect) {
            this.ect = ect;
        }

        public Integer getDist() {
            return dist;
        }

        public void setDist(Integer dist) {
            this.dist = dist;
        }

        public Float getAuxBat() {
            return auxBat;
        }

        public void setAuxBat(Float auxBat) {
            this.auxBat = auxBat;
        }

        public Float getFli() {
            return fli;
        }

        public void setFli(Float fli) {
            this.fli = fli;
        }

        public Float getxAxis() {
            return xAxis;
        }

        public void setxAxis(Float xAxis) {
            this.xAxis = xAxis;
        }

        public Float getyAxis() {
            return yAxis;
        }

        public void setyAxis(Float yAxis) {
            this.yAxis = yAxis;
        }

        public Float getzAxis() {
            return zAxis;
        }

        public void setzAxis(Float zAxis) {
            this.zAxis = zAxis;
        }

        public Float getLftB1() {
            return lftB1;
        }

        public void setLftB1(Float lftB1) {
            this.lftB1 = lftB1;
        }

        public Float getTa() {
            return ta;
        }

        public void setTa(Float ta) {
            this.ta = ta;
        }

        public Integer getO2sPresent() {
            return o2sPresent;
        }

        public void setO2sPresent(Integer o2sPresent) {
            this.o2sPresent = o2sPresent;
        }

        public Float getO2s2VB1() {
            return o2s2VB1;
        }

        public void setO2s2VB1(Float o2s2vb1) {
            o2s2VB1 = o2s2vb1;
        }

        public Float getO2s2FtB1() {
            return o2s2FtB1;
        }

        public void setO2s2FtB1(Float o2s2FtB1) {
            this.o2s2FtB1 = o2s2FtB1;
        }

        public Integer getObdStd() {
            return obdStd;
        }

        public void setObdStd(Integer obdStd) {
            this.obdStd = obdStd;
        }

        public Integer getRuntime() {
            return runtime;
        }

        public void setRuntime(Integer runtime) {
            this.runtime = runtime;
        }

        public Integer getDistMil() {
            return distMil;
        }

        public void setDistMil(Integer distMil) {
            this.distMil = distMil;
        }

        public Integer getFrpD() {
            return frpD;
        }

        public void setFrpD(Integer frpD) {
            this.frpD = frpD;
        }

        public Integer getBaro() {
            return baro;
        }

        public void setBaro(Integer baro) {
            this.baro = baro;
        }

        public Integer getAat() {
            return aat;
        }

        public void setAat(Integer aat) {
            this.aat = aat;
        }

        public Integer getFuelType() {
            return fuelType;
        }

        public void setFuelType(Integer fuelType) {
            this.fuelType = fuelType;
        }

        public Integer getSupportPid00() {
            return supportPid00;
        }

        public void setSupportPid00(Integer supportPid00) {
            this.supportPid00 = supportPid00;
        }

        public Integer getFp() {
            return fp;
        }

        public void setFp(Integer fp) {
            this.fp = fp;
        }

        public Float getO2s1VB1() {
            return o2s1VB1;
        }

        public void setO2s1VB1(Float o2s1vb1) {
            o2s1VB1 = o2s1vb1;
        }

        public Float getO2s1FtB1() {
            return o2s1FtB1;
        }

        public void setO2s1FtB1(Float o2s1FtB1) {
            this.o2s1FtB1 = o2s1FtB1;
        }

        public Integer getSupportPid20() {
            return supportPid20;
        }

        public void setSupportPid20(Integer supportPid20) {
            this.supportPid20 = supportPid20;
        }

        public Integer getO2s1WrLambdaEr() {
            return o2s1WrLambdaEr;
        }

        public void setO2s1WrLambdaEr(Integer o2s1WrLambdaEr) {
            this.o2s1WrLambdaEr = o2s1WrLambdaEr;
        }

        public Integer getO2s1WrLambdaV() {
            return o2s1WrLambdaV;
        }

        public void setO2s1WrLambdaV(Integer o2s1WrLambdaV) {
            this.o2s1WrLambdaV = o2s1WrLambdaV;
        }

        public Integer getSupportPid40() {
            return supportPid40;
        }

        public void setSupportPid40(Integer supportPid40) {
            this.supportPid40 = supportPid40;
        }

        public Integer getEot() {
            return eot;
        }

        public void setEot(Integer eot) {
            this.eot = eot;
        }

        public String getValidPids() {
            return validPids;
        }

        public void setValidPids(String validPids) {
            this.validPids = validPids;
        }

        public Float getDiffVss() {
            return diffVss;
        }

        public void setDiffVss(Float diffVss) {
            this.diffVss = diffVss;
        }

        public int getDiffTime() {
            return diffTime;
        }

        public void setDiffTime(int diffTime) {
            this.diffTime = diffTime;
        }

        @Override
        public String toString() {
            return "VehicleLocation [latitude=" + latitude + ", longitude="
                    + longitude + ", rpm=" + rpm + ", vss=" + vss + "]";
        }

        public SmartRecord toServerBean() {
            SmartRecord sl = new SmartRecord();
            sl.setReportDate(new Date(time));
            if (latitude != null && longitude != null) {
                sl.setLatitude(String.valueOf(latitude));
                sl.setLongitude(String.valueOf(longitude));
            }
            sl.setRpm(rpm);
            sl.setVss(vss);
            sl.setMap(map);
            sl.setIat(iat);
            sl.setMaf(maf);
            sl.setCer(cer);
            sl.setFss1(fss1);
            sl.setSftB1(sftB1);
            sl.setLoadPct(loadPct);
            sl.setLoadAbs(loadAbs);
            sl.setTp(tp);
            sl.setTpRel(tpRel);
            sl.setAccelD(accelD);
            sl.setMil(mil);
            sl.setEct(ect);
            sl.setDist(dist);
            sl.setAuxBat(auxBat);
            sl.setFli(fli);

            sl.setxAxis(xAxis);
            sl.setyAxis(yAxis);
            sl.setzAxis(zAxis);

            sl.setLftB1(lftB1);
            sl.setTa(ta);
            sl.setO2sPresent(o2sPresent);
            sl.setO2s2VB1(o2s2VB1);
            sl.setO2s2FtB1(o2s2FtB1);
            sl.setObdStd(obdStd);
            sl.setRuntime(runtime);
            sl.setDistMil(distMil);
            sl.setFrpD(frpD);
            sl.setBaro(baro);
            sl.setAat(aat);
            sl.setFuelType(fuelType);

            sl.setSupportPid00(supportPid00);
            sl.setSupportPid20(supportPid20);
            sl.setSupportPid40(supportPid40);
            sl.setFp(fp);
            sl.setO2s1VB1(o2s1VB1);
            sl.setO2s1FtB1(o2s1FtB1);
            sl.setO2s1WrLambdaEr(o2s1WrLambdaEr);
            sl.setO2s1WrLambdaV(o2s1WrLambdaV);

            sl.setEot(eot);
            sl.setValidPids(validPids);

            sl.setDiffVss(diffVss);
            sl.setDiffTime(diffTime);

            return sl;
        }

    }

    public static class VehicleEvent implements Serializable {
        private static final long serialVersionUID = 1L;

        private ThreeState milOn = ThreeState.U;
        private ThreeState overAuxBatteryLevel = ThreeState.U;
        private ThreeState underAuxBatteryLevel = ThreeState.U;
        private ThreeState lowFuelLevel = ThreeState.U;
        private ThreeState overHeated = ThreeState.U;
        private String dtc;
        private ArrayList<EventTpCd> events = new ArrayList<>(0);

        public VehicleEvent() {
        }

        public ThreeState getMilOn() {
            return milOn;
        }

        public void setMilOn(ThreeState milOn) {
            this.milOn = milOn;
        }

        public ThreeState getOverAuxBatteryLevel() {
            return overAuxBatteryLevel;
        }

        public void setOverAuxBatteryLevel(ThreeState overAuxBatteryLevel) {
            this.overAuxBatteryLevel = overAuxBatteryLevel;
        }

        public ThreeState getUnderAuxBatteryLevel() {
            return underAuxBatteryLevel;
        }

        public void setUnderAuxBatteryLevel(ThreeState underAuxBatteryLevel) {
            this.underAuxBatteryLevel = underAuxBatteryLevel;
        }

        public ThreeState getLowFuelLevel() {
            return lowFuelLevel;
        }

        public void setLowFuelLevel(ThreeState lowFuelLevel) {
            this.lowFuelLevel = lowFuelLevel;
        }

        public ThreeState getOverHeated() {
            return overHeated;
        }

        public void setOverHeated(ThreeState overHeated) {
            this.overHeated = overHeated;
        }

        public String getDtc() {
            return dtc;
        }

        public void setDtc(String dtc) {
            this.dtc = dtc;
        }

        public void addEvent(EventTpCd code) {
            events.add(code);
        }

        public void set(VehicleEvent other) {
            this.milOn = other.milOn;
            this.overAuxBatteryLevel = other.overAuxBatteryLevel;
            this.underAuxBatteryLevel = other.underAuxBatteryLevel;
            this.lowFuelLevel = other.lowFuelLevel;
            this.overHeated = other.overHeated;
            this.dtc = other.dtc;
            this.events.clear();
            this.events.addAll(other.events);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || !(obj instanceof VehicleEvent)) return false;
            return (this.hashCode() == obj.hashCode());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (milOn == null ? 0 : milOn.hashCode());
            result = prime * result + (overAuxBatteryLevel == null ? 0 : overAuxBatteryLevel.hashCode());
            result = prime * result + (underAuxBatteryLevel == null ? 0 : underAuxBatteryLevel.hashCode());
            result = prime * result + (lowFuelLevel == null ? 0 : lowFuelLevel.hashCode());
            result = prime * result + (overHeated == null ? 0 : overHeated.hashCode());
            result = prime * result + (dtc == null ? 0 : dtc.hashCode());
            result = prime * result + (events == null ? 0 : events.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "VehicleEvent [milOn=" + milOn + ", overAuxBatteryLevel="
                    + overAuxBatteryLevel + ", underAuxBatteryLevel="
                    + underAuxBatteryLevel + ", lowFuelLevel=" + lowFuelLevel
                    + ", overHeated=" + overHeated + ", dtc=" + dtc
                    + ", events=" + events + "]";
        }

        public SmartEvent toServerBean() {
            SmartEvent se = new SmartEvent();
            se.setMilWarn(milOn);
            se.setDtc(dtc);
            se.setAuxBatteryWarn(overAuxBatteryLevel); // FIXME: set each over- and under- event
            se.setLowFuelWarn(lowFuelLevel);
            se.setCoolantTempWarn(overHeated);
            //se.setAccident(accident);
            if (events.size() > 0) {
                se.setEventTpCdList(events);
            }
            return se;
        }
    }

    public static class VehicleTrip implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * IDLE
         */
        public static final int SPEED_ZONE_0 = 0;
        /**
         * 0 ~ 10km/h
         */
        public static final int SPEED_ZONE_1 = 1;
        /**
         * 10 ~ 20km/h
         */
        public static final int SPEED_ZONE_2 = 2;
        /**
         * 20 ~ 30km/h
         */
        public static final int SPEED_ZONE_3 = 3;
        /**
         * 30 ~ 40km/h
         */
        public static final int SPEED_ZONE_4 = 4;
        /**
         * 40 ~ 50km/h
         */
        public static final int SPEED_ZONE_5 = 5;
        /**
         * 50 ~ 60km/h
         */
        public static final int SPEED_ZONE_6 = 6;
        /**
         * 60 ~ 70km/h
         */
        public static final int SPEED_ZONE_7 = 7;
        /**
         * 70 ~ 80km/h
         */
        public static final int SPEED_ZONE_8 = 8;
        /**
         * 80 ~ 90km/h
         */
        public static final int SPEED_ZONE_9 = 9;
        /**
         * 90 ~ 100km/h
         */
        public static final int SPEED_ZONE_10 = 10;
        /**
         * 100 ~ 110km/h
         */
        public static final int SPEED_ZONE_11 = 11;
        /**
         * 110 ~ 120km/h
         */
        public static final int SPEED_ZONE_12 = 12;
        /**
         * 120 ~ 130km/h
         */
        public static final int SPEED_ZONE_13 = 13;
        /**
         * 130km/h over
         */
        public static final int SPEED_ZONE_14 = 14;
        /**
         * Number of speed zone
         */
        public static final int SPEED_ZONE_NUM = 15;

        private SmartTrip.TripState state = SmartTrip.TripState.opened;
        private String accountId;
        private String vehicleId;
        private String networkOperator;
        private long beginTime;
        private long endTime;
        private float drivingTime;
        private float drivingDistance;
        private int harshAccelCount;
        private int harshBrakeCount;
        private int harshStartCount;
        private int harshStopCount;
        private int harshRpmCount;
        private float maxVss;
        private float avgVssNi;
        private float avgVssWi;
        private float maxRpm;
        private float avgRpm;
        private float fuelEconomy;
        private float fuelConsumption;
        private float co2Emission;
        private float steadySpeedTime;
        private float highRpmTime;
        private float fuelCutTime;
        private float warmUpTime;
        private float[] speedZoneTimes = new float[SPEED_ZONE_NUM];
        private int engineDistance;
        private boolean collision;

        private float fuelEconomyA;
        private float fuelEconomyB;
        private float fuelEconomyC;

        private String appVer;
        private String devOs;
        private String devOsVer;

        public VehicleTrip(String accountId, String vehicleId, String networkOperator) {
            this.accountId = accountId;
            this.vehicleId = vehicleId;
            this.networkOperator = networkOperator;
        }

        public SmartTrip.TripState getState() {
            return state;
        }

        public void setState(SmartTrip.TripState state) {
            this.state = state;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getVehicleId() {
            return vehicleId;
        }

        public void setVehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
        }

        public String getNetworkOperator() {
            return networkOperator;
        }

        public void setNetworkOperator(String networkOperator) {
            this.networkOperator = networkOperator;
        }

        public long getBeginTime() {
            return beginTime;
        }

        public void setBeginTime(long beginTime) {
            this.beginTime = beginTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public float getDrivingTime() {
            return drivingTime;
        }

        public void setDrivingTime(float drivingTime) {
            this.drivingTime = drivingTime;
        }

        public float getDrivingDistance() {
            return drivingDistance;
        }

        public void setDrivingDistance(float drivingDistance) {
            this.drivingDistance = drivingDistance;
        }

        public int getHarshAccelCount() {
            return harshAccelCount;
        }

        public void setHarshAccelCount(int harshAccelCount) {
            this.harshAccelCount = harshAccelCount;
        }

        public int getHarshBrakeCount() {
            return harshBrakeCount;
        }

        public void setHarshBrakeCount(int harshBrakeCount) {
            this.harshBrakeCount = harshBrakeCount;
        }

        public int getHarshStartCount() {
            return harshStartCount;
        }

        public void setHarshStartCount(int harshStartCount) {
            this.harshStartCount = harshStartCount;
        }

        public int getHarshStopCount() {
            return harshStopCount;
        }

        public void setHarshStopCount(int harshStopCount) {
            this.harshStopCount = harshStopCount;
        }

        public int getHarshRpmCount() {
            return harshRpmCount;
        }

        public void setHarshRpmCount(int harshRpmCount) {
            this.harshRpmCount = harshRpmCount;
        }

        public float getMaxVss() {
            return maxVss;
        }

        public void setMaxVss(float maxVss) {
            this.maxVss = maxVss;
        }

        public float getAvgVssNi() {
            return avgVssNi;
        }

        public void setAvgVssNi(float avgVssNi) {
            this.avgVssNi = avgVssNi;
        }

        public float getAvgVssWi() {
            return avgVssWi;
        }

        public void setAvgVssWi(float avgVssWi) {
            this.avgVssWi = avgVssWi;
        }

        public float getMaxRpm() {
            return maxRpm;
        }

        public void setMaxRpm(float maxRpm) {
            this.maxRpm = maxRpm;
        }

        public float getAvgRpm() {
            return avgRpm;
        }

        public void setAvgRpm(float avgRpm) {
            this.avgRpm = avgRpm;
        }

        public float getFuelEconomy() {
            return fuelEconomy;
        }

        public void setFuelEconomy(float fuelEconomy) {
            if (Float.isInfinite(fuelEconomy) || Float.isNaN(fuelEconomy)) {
                fuelEconomy = 0;
            }
            this.fuelEconomy = fuelEconomy;
        }

        public float getFuelConsumption() {
            return fuelConsumption;
        }

        public void setFuelConsumption(float fuelConsumption) {
            this.fuelConsumption = fuelConsumption;
        }

        public float getCo2Emission() {
            return co2Emission;
        }

        public void setCo2Emission(float co2Emission) {
            this.co2Emission = co2Emission;
        }

        public float getSteadySpeedTime() {
            return steadySpeedTime;
        }

        public void setSteadySpeedTime(float steadySpeedTime) {
            this.steadySpeedTime = steadySpeedTime;
        }

        public float getHighRpmTime() {
            return highRpmTime;
        }

        public void setHighRpmTime(float highRpmTime) {
            this.highRpmTime = highRpmTime;
        }

        public float getFuelCutTime() {
            return fuelCutTime;
        }

        public void setFuelCutTime(float fuelCutTime) {
            this.fuelCutTime = fuelCutTime;
        }

        public float getWarmUpTime() {
            return warmUpTime;
        }

        public void setWarmUpTime(float warmUpTime) {
            this.warmUpTime = warmUpTime;
        }

        public float[] getSpeedZoneTimes() {
            return speedZoneTimes;
        }

        public float getSpeedZoneTime(int zone) {
            return speedZoneTimes[zone];
        }

        public void setSpeedZoneTime(int zone, float time) {
            speedZoneTimes[zone] = time;
        }

        public int getEngineDistance() {
            return engineDistance;
        }

        public void setEngineDistance(int engineDistance) {
            this.engineDistance = engineDistance;
        }

        public boolean getCollision() {
            return collision;
        }

        public void setCollision(boolean collision) {
            this.collision = collision;
        }

        public float getFuelEconomyA() {
            return fuelEconomyA;
        }

        public void setFuelEconomyA(float fuelEconomy) {
            if (Float.isInfinite(fuelEconomy) || Float.isNaN(fuelEconomy)) {
                fuelEconomy = 0;
            }
            this.fuelEconomyA = fuelEconomy;
        }

        public float getFuelEconomyB() {
            return fuelEconomyB;
        }

        public void setFuelEconomyB(float fuelEconomy) {
            if (Float.isInfinite(fuelEconomy) || Float.isNaN(fuelEconomy)) {
                fuelEconomy = 0;
            }
            this.fuelEconomyB = fuelEconomy;
        }

        public float getFuelEconomyC() {
            return fuelEconomyC;
        }

        public void setFuelEconomyC(float fuelEconomy) {
            if (Float.isInfinite(fuelEconomy) || Float.isNaN(fuelEconomy)) {
                fuelEconomy = 0;
            }
            this.fuelEconomyC = fuelEconomy;
        }

        public String getAppVer() {
            return appVer;
        }

        public void setAppVer(String appVer) {
            this.appVer = appVer;
        }

        public String getDevOs() {
            return devOs;
        }

        public void setDevOs(String devOs) {
            this.devOs = devOs;
        }

        public String getDevOsVer() {
            return devOsVer;
        }

        public void setDevOsVer(String devOsVer) {
            this.devOsVer = devOsVer;
        }

        @Override
        public String toString() {
            return "VehicleTrip [accountId=" + accountId + ", vehicleId="
                    + vehicleId + ", beginTime=" + beginTime + ", endTime="
                    + endTime + ", drivingTime=" + drivingTime
                    + ", drivingDistance=" + drivingDistance + "]";
        }

        public SmartTrip toServerBean() {
            SmartTrip st = new SmartTrip();
            st.setState(state);
            st.setFromDate(new Date(beginTime));
            st.setToDate(new Date(endTime));
            st.setDrivingTime((int) drivingTime);
            st.setDrivingDistance(drivingDistance);
            st.setMaxSpeed((int) maxVss);
            st.setAvgSpeed((int) avgVssNi);
            st.setMaxEngineRpm((int) maxRpm);
            st.setHarshAccelCount(harshAccelCount);
            st.setHarshDecelCount(harshBrakeCount);
            st.setHarshStartCount(harshStartCount);
            st.setHarshStopCount(harshStopCount);
            st.setFuelEconomy(fuelEconomy);
            st.setFuelConsumption(fuelConsumption);
            st.setFuelCut(fuelCutTime);
            st.setTimeInWarmUp(warmUpTime);
            st.setTimeInAccel(highRpmTime);
            st.setTimeInIdle(speedZoneTimes[VehicleTrip.SPEED_ZONE_0]);
            st.setTimeIn0To10(speedZoneTimes[VehicleTrip.SPEED_ZONE_1]);
            st.setTimeIn10To20(speedZoneTimes[VehicleTrip.SPEED_ZONE_2]);
            st.setTimeIn20To30(speedZoneTimes[VehicleTrip.SPEED_ZONE_3]);
            st.setTimeIn30To40(speedZoneTimes[VehicleTrip.SPEED_ZONE_4]);
            st.setTimeIn40To50(speedZoneTimes[VehicleTrip.SPEED_ZONE_5]);
            st.setTimeIn50To60(speedZoneTimes[VehicleTrip.SPEED_ZONE_6]);
            st.setTimeIn60To70(speedZoneTimes[VehicleTrip.SPEED_ZONE_7]);
            st.setTimeIn70To80(speedZoneTimes[VehicleTrip.SPEED_ZONE_8]);
            st.setTimeIn80To90(speedZoneTimes[VehicleTrip.SPEED_ZONE_9]);
            st.setTimeIn90To100(speedZoneTimes[VehicleTrip.SPEED_ZONE_10]);
            st.setTimeIn100To110(speedZoneTimes[VehicleTrip.SPEED_ZONE_11]);
            st.setTimeIn110Over(speedZoneTimes[VehicleTrip.SPEED_ZONE_12]
                    + speedZoneTimes[VehicleTrip.SPEED_ZONE_13]
                    + speedZoneTimes[VehicleTrip.SPEED_ZONE_14]);
            st.setMobileCarrier(networkOperator);
            st.setEngineDistance(engineDistance);

            st.setFuelEconomyA(fuelEconomyA);
            st.setFuelEconomyB(fuelEconomyB);
            st.setFuelEconomyC(fuelEconomyC);

            st.setAppVer(appVer);
            st.setDevOs(devOs);
            st.setDevOsVer(devOsVer);

            return st;
        }

    }

}

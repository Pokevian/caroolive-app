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

import android.util.Log;

import org.apache.log4j.Logger;

import java.io.Serializable;


public class BlackboxMetadata implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    static final Logger LOGGER = Logger.getLogger("BlackboxMetadata");

    /**
     * milliseconds.
     */
    public long timestamp;

    /**
     * milliseconds.
     */
    public int drivingTime;

    /**
     * km.
     */
    public float drivingDistance;

    /**
     * location related data.
     */
    public LocationMetadata locationData = new LocationMetadata();

    /**
     * speed related data.
     */
    public SpeedMetadata speedData = new SpeedMetadata();

    /**
     * engine related data.
     */
    public EngineMetadata engineData = new EngineMetadata();

    /**
     * diagnositc related data.
     */
    public DiagnosticMetadata diagnosticData = new DiagnosticMetadata();

    /**
     * fuel related data.
     */
    public FuelMetadata fuelData = new FuelMetadata();

    public BlackboxMetadata() {
    }

    public void set(BlackboxMetadata other) {
        timestamp = other.timestamp;
        drivingTime = other.drivingTime;
        drivingDistance = other.drivingDistance;
        locationData = other.locationData.clone();
        speedData = other.speedData.clone();
        engineData = other.engineData.clone();
        diagnosticData = other.diagnosticData.clone();
        fuelData = other.fuelData.clone();
    }

    public void clear() {
        locationData.clear();
        speedData.clear();
        engineData.clear();
        diagnosticData.clear();
        fuelData.clear();
    }

    @Override
    protected BlackboxMetadata clone() {
        BlackboxMetadata clone = new BlackboxMetadata();
        clone.set(this);
        return clone;
    }

    // NOTE: should be matched with unflatten() method
    public String flatten() {
        return timestamp + ";"
                + drivingTime + ";"
                + drivingDistance + ";"
                + locationData.flatten()
                + speedData.flatten()
                + engineData.flatten()
                + diagnosticData.flatten()
                + fuelData.flatten();
    }

    // NOTE: should be matched with flatten() method
    public void unflatten(String flattened) {
        String[] tokens = flattened.split(";");
        int index = 0;
        try {
            timestamp = Long.valueOf(tokens[index++]);
            drivingTime = Integer.valueOf(tokens[index++]);
            drivingDistance = Float.valueOf(tokens[index++]);
            index = locationData.unflatten(tokens, index);
            index = speedData.unflatten(tokens, index);
            index = engineData.unflatten(tokens, index);
            index = diagnosticData.unflatten(tokens, index);
            index = fuelData.unflatten(tokens, index);
        } catch (IndexOutOfBoundsException e) {
            Log.e("Metadata", "failed to unflatten", e);
        } catch (NumberFormatException e) {
            Log.e("Metadata", "failed to unflatten", e);
        }
    }

    @Override
    public String toString() {
        return "BlackboxMetadata{" +
                "timestamp=" + timestamp +
                ", drivingTime=" + drivingTime +
                ", drivingDistance=" + drivingDistance +
                ", locationData=" + locationData +
                ", speedData=" + speedData +
                ", engineData=" + engineData +
                ", diagnosticData=" + diagnosticData +
                ", fuelData=" + fuelData +
                '}';
    }

    public static class LocationMetadata implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        /**
         * True if LocationMetadata is valid.
         */
        public boolean isValid;
        /**
         * Degree.
         */
        public double latitude;
        /**
         * Degree.
         */
        public double longitude;
        /**
         * meters.
         */
        public float accuracy;

        @Override
        protected LocationMetadata clone() {
            LocationMetadata clone = new LocationMetadata();
            clone.isValid = isValid;
            clone.latitude = latitude;
            clone.longitude = longitude;
            clone.accuracy = accuracy;
            return clone;
        }

        @Override
        public String toString() {
            return "LocationMetadata{" +
                    "isValid=" + isValid +
                    ", latitude=" + latitude +
                    ", longitude=" + longitude +
                    ", accuracy=" + accuracy +
                    '}';
        }

        private void clear() {
            isValid = false;
        }

        private String flatten() {
            return isValid + ";"
                    + latitude + ";"
                    + longitude + ";"
                    + accuracy + ";";
        }

        private int unflatten(String[] tokens, int index) {
            try {
                isValid = Boolean.valueOf(tokens[index++]);
                latitude = Double.valueOf(tokens[index++]);
                longitude = Double.valueOf(tokens[index++]);
                accuracy = Float.valueOf(tokens[index++]);
            } catch (Exception e) {
                LOGGER.error("[LocationMetadata] failed to unflatten", e);
            }
            return index;
        }
    }

    public static class SpeedMetadata implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        /**
         * True if SpeedMetadata is valid.
         */
        public boolean isValid;
        /**
         * kph.
         */
        public float currentSpeed;
        /**
         * kph.
         */
        public float highestSpeed;
        /**
         * kph.
         */
        public float averageSpeed;
        /**
         * True if in idling state.
         */
        public boolean isIdling;
        /**
         * milliseconds.
         */
        public int idlingTime;
        /**
         * True if in over-speed state.
         */
        public boolean isOverSpeed;
        /**
         * milliseconds.
         */
        public int overSpeedTime;
        /**
         * True if in steady-speed state.
         */
        public boolean isSteadySpeed;
        /**
         * milliseconds.
         */
        public int steadySpeedTime;
        /**
         * True if in economy-speed state.
         */
        public boolean isEconomySpeed;
        /**
         * milliseconds.
         */
        public int economySpeedTime;
        /**
         * True if harsh acceleration occurred.
         */
        public boolean isHarshAccel;
        /**
         * count.
         */
        public int harshAccelCount;
        /**
         * True if harsh deceleration occurred.
         */
        public boolean isHarshBrake;
        /**
         * count.
         */
        public int harshBrakelCount;

        @Override
        protected SpeedMetadata clone() {
            SpeedMetadata clone = new SpeedMetadata();
            clone.isValid = isValid;
            clone.currentSpeed = currentSpeed;
            clone.highestSpeed = highestSpeed;
            clone.averageSpeed = averageSpeed;
            clone.isIdling = isIdling;
            clone.idlingTime = idlingTime;
            clone.isOverSpeed = isOverSpeed;
            clone.overSpeedTime = overSpeedTime;
            clone.isSteadySpeed = isSteadySpeed;
            clone.steadySpeedTime = steadySpeedTime;
            clone.isEconomySpeed = isEconomySpeed;
            clone.economySpeedTime = economySpeedTime;
            clone.isHarshAccel = isHarshAccel;
            clone.harshAccelCount = harshAccelCount;
            clone.isHarshBrake = isHarshBrake;
            clone.harshBrakelCount = harshBrakelCount;
            return clone;
        }

        @Override
        public String toString() {
            return "SpeedMetadata{" +
                    "isValid=" + isValid +
                    ", currentSpeed=" + currentSpeed +
                    ", highestSpeed=" + highestSpeed +
                    ", averageSpeed=" + averageSpeed +
                    ", isIdling=" + isIdling +
                    ", idlingTime=" + idlingTime +
                    ", isOverSpeed=" + isOverSpeed +
                    ", overSpeedTime=" + overSpeedTime +
                    ", isSteadySpeed=" + isSteadySpeed +
                    ", steadySpeedTime=" + steadySpeedTime +
                    ", isEconomySpeed=" + isEconomySpeed +
                    ", economySpeedTime=" + economySpeedTime +
                    ", isHarshAccel=" + isHarshAccel +
                    ", harshAccelCount=" + harshAccelCount +
                    ", isHarshBrake=" + isHarshBrake +
                    ", harshBrakelCount=" + harshBrakelCount +
                    '}';
        }

        private void clear() {
            isValid = false;
            currentSpeed = 0;
            isIdling = false;
            isOverSpeed = false;
            isSteadySpeed = false;
            isEconomySpeed = false;
            isHarshAccel = false;
        }

        private String flatten() {
            return isValid + ";"
                    + currentSpeed + ";"
                    + highestSpeed + ";"
                    + averageSpeed + ";"
                    + isIdling + ";"
                    + idlingTime + ";"
                    + isOverSpeed + ";"
                    + overSpeedTime + ";"
                    + isSteadySpeed + ";"
                    + steadySpeedTime + ";"
                    + isEconomySpeed + ";"
                    + economySpeedTime + ";"
                    + isHarshAccel + ";"
                    + harshAccelCount + ";"
                    + isHarshBrake + ";"
                    + harshBrakelCount + ";";
        }

        private int unflatten(String[] tokens, int index) {
            try {
                isValid = Boolean.valueOf(tokens[index++]);
                currentSpeed = Float.valueOf(tokens[index++]);
                highestSpeed = Float.valueOf(tokens[index++]);
                averageSpeed = Float.valueOf(tokens[index++]);
                isIdling = Boolean.valueOf(tokens[index++]);
                idlingTime = Integer.valueOf(tokens[index++]);
                isOverSpeed = Boolean.valueOf(tokens[index++]);
                overSpeedTime = Integer.valueOf(tokens[index++]);
                isSteadySpeed = Boolean.valueOf(tokens[index++]);
                steadySpeedTime = Integer.valueOf(tokens[index++]);
                isEconomySpeed = Boolean.valueOf(tokens[index++]);
                economySpeedTime = Integer.valueOf(tokens[index++]);
                isHarshAccel = Boolean.valueOf(tokens[index++]);
                harshAccelCount = Integer.valueOf(tokens[index++]);
                isHarshBrake = Boolean.valueOf(tokens[index++]);
                harshBrakelCount = Integer.valueOf(tokens[index++]);
            } catch (Exception e) {
                LOGGER.error("[SpeedMetadata] failed to unflatten", e);
            }
            return index;
        }
    }

    public static class EngineMetadata implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        /**
         * True if EngineMetadata is valid.
         */
        public boolean isValid;
        /**
         * RPM.
         */
        public float engineRpm;
        public boolean isEngineRpmValid;
        /**
         * %.
         */
        public float engineLoad;
        public boolean isEngineLoadValid;
        /**
         * celsius.
         */
        public int engineCoolantTemperature;
        public boolean isEngineCoolantTemperatureValid;
        /**
         * volts.
         */
        public float auxBatteryVoltage;
        public boolean isAuxBatteryVoltageValid;
        /**
         * fuel cut.
         */
        public boolean fuelCut;
        /**
         * milliseconds.
         */
        public int fuelCutTime;
        /**
         * True if fuel-cut is valid.
         */
        public boolean isFuelCutValid;

        public float accelPedalPosition;
        public boolean isAccelPedalPositionValid;

        public float fuelLevelInput;
        public boolean isFuelLevelInputValid;

        public float baroPressure;
        public boolean isBaroPressureValid;

        @Override
        protected EngineMetadata clone() {
            EngineMetadata clone = new EngineMetadata();
            clone.isValid = isValid;
            clone.engineRpm = engineRpm;
            clone.isEngineRpmValid = isEngineRpmValid;
            clone.engineLoad = engineLoad;
            clone.isEngineLoadValid = isEngineLoadValid;
            clone.engineCoolantTemperature = engineCoolantTemperature;
            clone.isEngineCoolantTemperatureValid = isEngineCoolantTemperatureValid;
            clone.auxBatteryVoltage = auxBatteryVoltage;
            clone.isAuxBatteryVoltageValid = isAuxBatteryVoltageValid;
            clone.fuelCut = fuelCut;
            clone.fuelCutTime = fuelCutTime;
            clone.isFuelCutValid = isFuelCutValid;

            clone.accelPedalPosition = accelPedalPosition;
            clone.isAccelPedalPositionValid = isAccelPedalPositionValid;
            clone.fuelLevelInput = fuelLevelInput;
            clone.isFuelLevelInputValid = isFuelLevelInputValid;
            clone.baroPressure = baroPressure;
            clone.isBaroPressureValid = isBaroPressureValid;

            return clone;
        }

        @Override
        public String toString() {
            return "EngineMetadata{" +
                    "isValid=" + isValid +
                    ", engineRpm=" + engineRpm +
                    ", isEngineRpmValid=" + isEngineRpmValid +
                    ", engineLoad=" + engineLoad +
                    ", isEngineLoadValid=" + isEngineLoadValid +
                    ", engineCoolantTemperature=" + engineCoolantTemperature +
                    ", isEngineCoolantTemperatureValid=" + isEngineCoolantTemperatureValid +
                    ", auxBatteryVoltage=" + auxBatteryVoltage +
                    ", isAuxBatteryVoltageValid=" + isAuxBatteryVoltageValid +
                    ", fuelCut=" + fuelCut +
                    ", fuelCutTime=" + fuelCutTime +
                    ", isFuelCutValid=" + isFuelCutValid +
                    ", accelPedalPosition=" + accelPedalPosition +
                    ", isAccelPedalPositionValid=" + isAccelPedalPositionValid +
                    ", fuelLevelInput=" + fuelLevelInput +
                    ", isFuelLevelInputValid=" + isFuelLevelInputValid +
                    ", baroPressure=" + baroPressure +
                    ", isBaroPressureValid=" + isBaroPressureValid +
                    '}';
        }

        private void clear() {
            isValid = false;
            engineRpm = 0;
            isEngineRpmValid = false;
            engineLoad = 0;
            isEngineLoadValid = false;
            engineCoolantTemperature = 0;
            isEngineCoolantTemperatureValid = false;
            auxBatteryVoltage = 0;
            isAuxBatteryVoltageValid = false;
            fuelCut = false;
            isFuelCutValid = false;

            accelPedalPosition = 0;
            isAccelPedalPositionValid = false;
            fuelLevelInput = 0;
            isFuelLevelInputValid = false;
            baroPressure = 0;
            isBaroPressureValid = false;
        }

        private String flatten() {
            return isValid + ";"
                    + engineRpm + ";"
                    + isEngineRpmValid + ";"
                    + engineLoad + ";"
                    + isEngineLoadValid + ";"
                    + engineCoolantTemperature + ";"
                    + isEngineCoolantTemperatureValid + ";"
                    + auxBatteryVoltage + ";"
                    + isAuxBatteryVoltageValid + ";"
                    + fuelCut + ";"
                    + fuelCutTime + ";"
                    + isFuelCutValid + ";"
                    /*+ accelPedalPosition + ";"
                    + isAccelPedalPositionValid + ";"
                    + fuelLevelInput + ";"
                    + isFuelLevelInputValid + ";"
                    + baroPressure + ";"
                    + isBaroPressureValid + ";"*/;
        }

        private int unflatten(String[] tokens, int index) {
            try {
                isValid = Boolean.valueOf(tokens[index++]);
                engineRpm = Float.valueOf(tokens[index++]);
                isEngineRpmValid = Boolean.valueOf(tokens[index++]);
                engineLoad = Float.valueOf(tokens[index++]);
                isEngineLoadValid = Boolean.valueOf(tokens[index++]);
                engineCoolantTemperature = Integer.valueOf(tokens[index++]);
                isEngineCoolantTemperatureValid = Boolean.valueOf(tokens[index++]);
                auxBatteryVoltage = Float.valueOf(tokens[index++]);
                isAuxBatteryVoltageValid = Boolean.valueOf(tokens[index++]);
                fuelCut = Boolean.valueOf(tokens[index++]);
                fuelCutTime = Integer.valueOf(tokens[index++]);
                isFuelCutValid = Boolean.valueOf(tokens[index++]);
                /*accelPedalPosition = Float.valueOf(tokens[index++]);
                isAccelPedalPositionValid = Boolean.valueOf(tokens[index++]);
				fuelLevelInput = Float.valueOf(tokens[index++]);
				isFuelLevelInputValid = Boolean.valueOf(tokens[index++]);
				baroPressure = Float.valueOf(tokens[index++]);
				isBaroPressureValid = Boolean.valueOf(tokens[index++]);*/
            } catch (Exception e) {
                LOGGER.error("[EngineMetadata] failed to unflatten", e);
            }
            return index;
        }
    }

    public static class DiagnosticMetadata implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        /**
         * True if DiagnosticMetadata is valid.
         */
        public boolean isValid;
        /**
         * Malfunction Indicator Lamp.
         */
        public boolean mil;
        /**
         * count.
         */
        public int troubleCodeCount = -1;
        /**
         * separated by comma.
         */
        public String troubleCodes = "none";

        @Override
        protected DiagnosticMetadata clone() {
            DiagnosticMetadata clone = new DiagnosticMetadata();
            clone.isValid = isValid;
            clone.mil = mil;
            clone.troubleCodeCount = troubleCodeCount;
            clone.troubleCodes = troubleCodes;
            return clone;
        }

        @Override
        public String toString() {
            return "DiagnosticMetadata{" +
                    "isValid=" + isValid +
                    ", mil=" + mil +
                    ", troubleCodeCount=" + troubleCodeCount +
                    ", troubleCodes='" + troubleCodes + '\'' +
                    '}';
        }

        private void clear() {
            isValid = false;
        }

        private String flatten() {
            return isValid + ";"
                    + mil + ";"
                    + troubleCodeCount + ";"
                    + troubleCodes + ";";
        }

        private int unflatten(String[] tokens, int index) {
            try {
                isValid = Boolean.valueOf(tokens[index++]);
                mil = Boolean.valueOf(tokens[index++]);
                troubleCodeCount = Integer.valueOf(tokens[index++]);
                troubleCodes = String.valueOf(tokens[index++]);
            } catch (Exception e) {
                LOGGER.error("[EngineMetadata] failed to unflatten", e);
            }
            return index;
        }
    }

    public static class FuelMetadata implements Serializable, Cloneable {
        private static final long serialVersionUID = 1L;

        /**
         * True if FuelMetadata is valid.
         */
        public boolean isValid;
        /**
         * kpl.
         */
        public float averageFuelEconomy;
        /**
         * kpl.
         */
        public float instanceFuelEconomy;
        /**
         * liters.
         */
        public float totalFuelConsumption;
        /**
         * liters.
         */
        public float instanceFuelConsumption;
        /**
         * kg.
         */
        public float totalCo2Emission;
        /**
         * kg.
         */
        public float instanceCo2Emission;

        /**
         * defined level.
         */
        public int safeEcoLevel = -1;

        @Override
        protected FuelMetadata clone() {
            FuelMetadata clone = new FuelMetadata();
            clone.isValid = isValid;
            clone.averageFuelEconomy = averageFuelEconomy;
            clone.instanceFuelEconomy = instanceFuelEconomy;
            clone.totalFuelConsumption = totalFuelConsumption;
            clone.instanceFuelConsumption = instanceFuelConsumption;
            clone.totalCo2Emission = totalCo2Emission;
            clone.instanceCo2Emission = instanceCo2Emission;

            clone.safeEcoLevel = safeEcoLevel;

            return clone;
        }

        @Override
        public String toString() {
            return "FuelMetadata{" +
                    "isValid=" + isValid +
                    ", averageFuelEconomy=" + averageFuelEconomy +
                    ", instanceFuelEconomy=" + instanceFuelEconomy +
                    ", totalFuelConsumption=" + totalFuelConsumption +
                    ", instanceFuelConsumption=" + instanceFuelConsumption +
                    ", totalCo2Emission=" + totalCo2Emission +
                    ", instanceCo2Emission=" + instanceCo2Emission +
                    ", safeEcoLevel=" + safeEcoLevel +
                    '}';
        }

        private void clear() {
            isValid = false;
            instanceFuelEconomy = 0;
            instanceFuelConsumption = 0;
            instanceCo2Emission = 0;

            safeEcoLevel = -1;
        }

        private String flatten() {
            return isValid + ";"
                    + averageFuelEconomy + ";"
                    + instanceFuelEconomy + ";"
                    + totalFuelConsumption + ";"
                    + instanceFuelConsumption + ";"
                    + totalCo2Emission + ";"
                    + instanceCo2Emission + ";"
                    + safeEcoLevel + ";";
        }

        private int unflatten(String[] tokens, int index) {
            try {
                isValid = Boolean.valueOf(tokens[index++]);
                averageFuelEconomy = Float.valueOf(tokens[index++]);
                instanceFuelEconomy = Float.valueOf(tokens[index++]);
                totalFuelConsumption = Float.valueOf(tokens[index++]);
                instanceFuelConsumption = Float.valueOf(tokens[index++]);
                totalCo2Emission = Float.valueOf(tokens[index++]);
                instanceCo2Emission = Float.valueOf(tokens[index++]);
                safeEcoLevel = Integer.valueOf(tokens[index++]);
            } catch (Exception e) {
                LOGGER.error("[FuelMetadata] failed to unflatten", e);
            }
            return index;
        }
    }


    public static class SmiTag {

        public static final String SMI_OPEN_TAG = "<SAMI>";
        public static final String SMI_CLOSE_TAG = "</SAMI>";

        public static final String HEAD_OPEN_TAG = "<HEAD>";
        public static final String HEAD_CLOSE_TAG = "</HEAD>";

        public static final String BODY_OPEN_TAG = "<BODY>";
        public static final String BODY_CLOSE_TAG = "</BODY>";

        public static final String PARAM_OPEN_TAG = "<SAMIParam>";
        public static final String PARAM_CLOSE_TAG = "</SAMIParam>";

        public static final String TITLE_OPEN_TAG = "<TITLE>";
        public static final String TITLE_CLOSE_TAG = "</TITLE>";

        public static final String COMMENTS_OPEN_TAG = "<!--";
        public static final String COMMENTS_CLOSE_TAG = "-->";

        public static final String STYLE_OPEN_TAG = "<STYLE Type=\"text/css\">" + COMMENTS_OPEN_TAG;
        public static final String STYLE_CLOSE_TAG = COMMENTS_CLOSE_TAG + "</STYLE>";

        public static final String SYNC_OPEN_TAG_START = "<SYNC ";
        public static final String SYNC_OPEN_TAG_START_ATTR = "Start=";
        public static final String SYNC_OPEN_TAG_END = ">";
        public static final String SYNC_CLOSE_TAG = "</SYNC>";

        public static final String PARAGRAPH_OPEN_TAG_START = "<P ";
        public static final String PARAGRAPH_OPEN_TAG_CLASS_ATTR = "Class=";
        public static final String PARAGRAPH_OPEN_TAG_END = ">";
        public static final String PARAGRAPH_CLOSE_TAG = "</P>";

        public static final String PARAGRAPH = "P";
        public static final String LEFTBRACE = "{";
        public static final String RIGHTBRACE = "}";

        public static final String SMI_LINE_BREAK = " <BR></BR> ";

        public static final String CSS_LINE_BREAK = "\r\n";
        public static final String SPACE = "&nbsp;";
        public static final String TAB = "\t";

        public static final String EN_CAPTION_NAME = "ENCC ";
        public static final String EN_CAPTION = "." + EN_CAPTION_NAME + LEFTBRACE
                + "Name: EnglishCaptions; lang: en-US; SAMIType:CC;" + RIGHTBRACE;

        public static final String POKE_TITLE_CONTENT = "Pokevian. Metadata File";
        public static final String POKE_TITLE = TITLE_OPEN_TAG + POKE_TITLE_CONTENT + TITLE_CLOSE_TAG;

        public static final String POKE_STYLE =
                STYLE_OPEN_TAG + CSS_LINE_BREAK +
                        PARAGRAPH + LEFTBRACE + CSS_LINE_BREAK +
                        TAB + "margin-left: 29pt; " + CSS_LINE_BREAK +
                        TAB + "margin-right: 29pt; " + CSS_LINE_BREAK +
                        TAB + "font-size: 12pt;" + CSS_LINE_BREAK +
                        TAB + "text-align: left; " + CSS_LINE_BREAK +
                        TAB + "font-family: tahoma, arial, sans-serif;" + CSS_LINE_BREAK +
                        TAB + "font-weight: normal; " + CSS_LINE_BREAK +
                        TAB + "color: white; " + CSS_LINE_BREAK +
                        TAB + "background-color: black;" + CSS_LINE_BREAK +
                        RIGHTBRACE + CSS_LINE_BREAK +
                        EN_CAPTION + CSS_LINE_BREAK +
                        STYLE_CLOSE_TAG;

    }

}

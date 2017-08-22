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

package com.pokevian.app.smartfleet.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.model.AuthTarget;
import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.model.StorageType;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.YoutubePrivacy;
import com.pokevian.app.smartfleet.service.ImpactDetector.ImpactSensitivity;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEngineType;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxQuality;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxRecordType;
import com.pokevian.lib.blackbox.BlackboxProfileInstance.VideoResolution;
import com.pokevian.lib.media.camera.CameraHelper;
import com.pokevian.lib.obd2.defs.Unit;

import java.util.HashSet;
import java.util.Set;

import static com.pokevian.app.smartfleet.setting.Consts.DEFAULT_BLACKBOX_ENGINE_TYPE;

public final class SettingsStore {

    public static final String PREF_DISCLAIMER_AGREED = "disclaimer_agreed";

    public static final String PREF_AUTH_TARGET = "auth_target";
    public static final String PREF_LOGIN_ID = "login_id";
    public static final String PREF_ACCOUNT_ID = "account_id";
    public static final String PREF_ACCOUNT_NAME = "account_name";
    public static final String PREF_ACCOUNT_IMAGE_URL = "account_image_url";

    public static final String PREF_VEHICLE_ID = "vehicle_id";
    public static final String PREF_VEHICLE = "vehicle::";
    public static final String PREF_VEHICLE_ID_LIST = "vehicle_id_list";

    public static final String PREF_UNIT_INITIALIZED = "unit_initialized";
    public static final String PREF_DISTANCE_UNIT = "distance_unit";
    public static final String PREF_SPEED_UNIT = "speed_unit";
    public static final String PREF_VOLUME_UNIT = "volume_unit";
    public static final String PREF_FUEL_ECONOMY_UNIT = "fuel_economy_unit";

    public static final String PREF_BLACKBOX_ENABLED = "blackbox_enabled";
    public static final String PREF_BLACKBOX_ENGINE_TYPE = "blackbox_engine_type";
    public static final String PREF_BLACKBOX_VIDEO_RESOLLUTION = "blackbox_video_resolution";
    public static final String PREF_BLACKBOX_VIDEO_QUALITY = "blackbox_video_quality";
    public static final String PREF_BLACKBOX_AUDIO_ENABLED = "blackbox_audio_enabled";
    public static final String PREF_BLACKBOX_RECORD_TYPE = "blackbox_record_type";
    public static final String PREF_BLACKBOX_NORMAL_FILE_PREFIX = "blackbox_normal_file_prefix";
    public static final String PREF_BLACKBOX_EVENT_FILE_PREFIX = "blackbox_event_file_prefix";
    public static final String PREF_BLACKBOX_NORMAL_VIDEO_DURATION = "blackbox_normal_video_duration";
    public static final String PREF_BLACKBOX_EVENT_VIDEO_DURATION = "blackbox_event_video_duration";
    public static final String PREF_BLACKBOX_STORAGE_TYPE = "blackbox_storage_type";
    public static final String PREF_BLACKBOX_MIN_STORAGE_SIZE = "blackbox_min_storage_size";
    public static final String PREF_BLACKBOX_MAX_STORAGE_SIZE = "blackbox_max_storage_size";
    public static final String PREF_BLACKBOX_NORMAL_DIR_NAME = "blackbox_normal_dir_name";
    public static final String PREF_BLACKBOX_EVENT_DIR_NAME = "blackbox_event_dir_name";
    public static final String PREF_BLACKBOX_ARCHIVE_DIR_NAME = "blackbox_archive_dir_name";
    public static final String PREF_BLACKBOX_DIR_NAME_FORMAT = "blackbox_dir_name_format";
    public static final String PREF_BLACKBOX_FILE_NAME_FORMAT = "blackbox_file_name_format";
    public static final String PREF_BLACKBOX_VIDEO_FILE_EXT = "blackbox_video_file_ext";
    public static final String PREF_BLACKBOX_META_FILE_EXT = "blackbox_meta_file_ext";
    public static final String PREF_BLACKBOX_COLOR_CORRECTION_ENABLED = "blackbox_color_correction_enabled";

    public static final String PREF_IMPACT_SENSITIVITY = "impact_sensitivity";
    public static final String PREF_ERS_ENABLED = "ers_enabled";
    public static final String PREF_ERS_CALL_CONTACT_PHONE_NUMBER = "ers_call_contact_phone_number";
    public static final String PREF_ERS_CALL_CONTACT_NAME = "ers_call_contact_name";
    public static final String PREF_ERS_SMS_CONTACT_PHONE_NUMBER = "ers_sms_contact_phone_number";
    public static final String PREF_ERS_SMS_CONTACT_NAME = "ers_sms_contact_name";
    public static final String PREF_ERS_SMS_MESSAGE = "ers_sms_message";
    public static final String PREF_ERS_YOUTUBE_PRIVACY = "ers_youtube_privacy";
    public static final String PREF_ERS_TARGET = "ers_target";
    public static final String PREF_ERS_YOUTUBE_ACCOUNT_NAME = "ers_youtube_account_name";
    public static final String PREF_QUICK_LAUNCH_APP_NAVI = "quick_launch_app_navi";
    public static final String PREF_AUTO_LAUNCH_APP_NAVI_ENABLED = "auto_launch_navi_app_enabled";
    public static final String PREF_QUICK_LAUNCH_APP_CUSTOM = "quick_launch_app_custom";
    public static final String PREF_OVERSPEED_THRESHOLD = "overspeed_threshold";

    public static final String PREF_TRIP_A_DISTANCE = "trip_a_distance";
    public static final String PREF_TRIP_A_FUEL_CONSUMPTION = "trip_a_fuel_consumption";
    public static final String PREF_TRIP_B_DISTANCE = "trip_b_distance";
    public static final String PREF_TRIP_B_FUEL_CONSUMPTION = "trip_a_fuel_consumption";

    public static final String PREF_DRIVING_HELP_DONE = "driving_help_done";

    public static final String PREF_ENGINE_OFF_DETECTION_ENABLED = "engine_off_detection_enabled";
    public static final String PREF_ENGINE_ON_DETECTION_ENABLED = "engine_on_detection_enabled";

    public static final String PREF_BLACKBOX_FOCUS_MODE = "blackbox-focus-mode";
    public static final String PREF_BLACKBOX_EXPOSURE_EXTRA = "blackbox-exposure-extra";
    public static final String PREF_BLACKBOX_OSD_ENABLED = "osd-enabled";

    public static final String PREF_WAIT_UNTIL_ENGINE_OFF = "wait_until_engine_off";

    private final SharedPreferences mPrefs;
    private final Gson mGson;

    private static SettingsStore mInstance;

    private SettingsStore(Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mGson = new Gson();
    }

    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new SettingsStore(context);
        }
    }

    public static SettingsStore getInstance() {
        if (mInstance == null) {
            throw new RuntimeException("Not initialized!");
        }
        return mInstance;
    }

    public static void commit() {
        if (mInstance != null) {
            boolean result = mInstance.mPrefs.edit().commit();
            Log.i("SettingsStore", "commit=" + result);
        }
    }

//    public void putWaitUntilEngineOff(boolean wait) {
//        SharedPreferences.Editor editor = mPrefs.edit();
//        editor.putBoolean(PREF_WAIT_UNTIL_ENGINE_OFF, wait);
//        editor.apply();
//    }

//    public boolean isWaitUnitlEngineOff() {
//        return mPrefs.getBoolean(PREF_WAIT_UNTIL_ENGINE_OFF, false);
//    }

    public void storeDisclaimerAgreed(boolean agreed) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_DISCLAIMER_AGREED, agreed);
        editor.apply();
    }

    public boolean isDisclaimerAgreed() {
        return mPrefs.getBoolean(PREF_DISCLAIMER_AGREED, false);
    }

    public void storeAuthTarget(AuthTarget authTarget) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_AUTH_TARGET, authTarget.name());
        editor.apply();
    }

    public AuthTarget getAuthTarget() {
        try {
            return AuthTarget.valueOf(mPrefs.getString(PREF_AUTH_TARGET,
                    AuthTarget.NONE.name()));
        } catch (IllegalArgumentException e) {
            return AuthTarget.NONE;
        }
    }

    public void storeLoginId(String loginId) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_LOGIN_ID, loginId);
        editor.apply();
    }

    public String getLoginId() {
        return mPrefs.getString(PREF_LOGIN_ID, null);
    }

    public void storeAccountId(String accountId) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ACCOUNT_ID, accountId);
        editor.apply();
    }

    public String getAccountId() {
        return mPrefs.getString(PREF_ACCOUNT_ID, null);
    }

    public void storeAccountName(String name) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ACCOUNT_NAME, name);
        editor.apply();
    }

    public String getAccountName() {
        return mPrefs.getString(PREF_ACCOUNT_NAME, null);
    }


    public void storeAccountImageUrl(String imageUrl) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ACCOUNT_IMAGE_URL, imageUrl);
        editor.apply();
    }

    public String getAccountImageUrl() {
        return mPrefs.getString(PREF_ACCOUNT_IMAGE_URL, null);
    }

    public boolean isValidAccount() {
        return !TextUtils.isEmpty(getAccountId());
    }

    public void storeVehicleId(String vehicleId) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_VEHICLE_ID, vehicleId);
        editor.apply();
    }

    public String getVehicleId() {
        return mPrefs.getString(PREF_VEHICLE_ID, null);
    }

    public Vehicle getVehicle() {
        String vehicleId = getVehicleId();
        return getVehicle(vehicleId);
    }

    public boolean storeVehicle(Vehicle vehicle) {
        if (vehicle == null || TextUtils.isEmpty(vehicle.getVehicleId())) {
            return false;
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        String key = buildVehicleKey(vehicle.getVehicleId());
        String value = mGson.toJson(vehicle);
        editor.putString(key, value);
        editor.apply();
        return true;
    }

    public Vehicle getVehicle(String vehicleId) {
        String key = buildVehicleKey(vehicleId);
        String value = mPrefs.getString(key, null);
        if (!TextUtils.isEmpty(value)) {
            return mGson.fromJson(value, Vehicle.class);
        } else {
            return null;
        }
    }

    private String buildVehicleKey(String vehicleId) {
        return PREF_VEHICLE + vehicleId;
    }

    public Set<String> getVehicleIds() {
        return mPrefs.getStringSet(PREF_VEHICLE_ID_LIST, new HashSet<String>());
    }

    public void addVehicleId(String vehicleId) {
        Set<String> ids = getVehicleIds();
        if (!ids.contains(vehicleId)) {
            ids.add(vehicleId);
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putStringSet(PREF_VEHICLE_ID_LIST, ids);
            editor.apply();
        }
    }

    public void removeVehicleId(String vehicleId) {
        Set<String> ids = getVehicleIds();
        if (ids.remove(vehicleId)) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putStringSet(PREF_VEHICLE_ID_LIST, ids);
            editor.apply();
        }
    }

    public boolean isValidVehicle() {
        Vehicle vehicle = getVehicle();
        return (vehicle != null && !TextUtils.isEmpty(vehicle.getObdAddress()));
    }

    public boolean isUpdatedObdAddress() {
        return mPrefs.getBoolean("updated-obd-address", false);
    }

    public void storeUpdatedObdAddress(boolean update) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean("updated-obd-address", update);
        editor.apply();
    }

    public void storeUnitInitialied(boolean initialized) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_UNIT_INITIALIZED, initialized);
        editor.apply();
    }

    public boolean isUnitInitialized() {
        return mPrefs.getBoolean(PREF_UNIT_INITIALIZED, false);
    }

    public void storeDistanceUnit(Unit distanceUnit) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_DISTANCE_UNIT, distanceUnit.name());
        editor.apply();
    }

    public Unit getDistanceUnit() {
        try {
            return Unit.valueOf(mPrefs.getString(PREF_DISTANCE_UNIT,
                    Consts.DEFAULT_DISTANCE_UNIT.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_DISTANCE_UNIT;
        }
    }

    public void storeSpeedUnit(Unit speedUnit) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_SPEED_UNIT, speedUnit.name());
        editor.apply();
    }

    public Unit getSpeedUnit() {
        try {
            return Unit.valueOf(mPrefs.getString(PREF_SPEED_UNIT,
                    Consts.DEFAULT_SPEED_UNIT.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_SPEED_UNIT;
        }
    }

    public void storeVolumeUnit(Unit volumeUnit) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_VOLUME_UNIT, volumeUnit.name());
        editor.apply();
    }

    public Unit getVolumeUnit() {
        try {
            return Unit.valueOf(mPrefs.getString(PREF_VOLUME_UNIT,
                    Consts.DEFAULT_VOLUME_UNIT.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_VOLUME_UNIT;
        }
    }

    public void storeFuelEconomyUnit(Unit fuelEconomyUnit) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_FUEL_ECONOMY_UNIT, fuelEconomyUnit.name());
        editor.apply();
    }

    public Unit getFuelEconomyUnit() {
        try {
            return Unit.valueOf(mPrefs.getString(PREF_FUEL_ECONOMY_UNIT,
                    Consts.DEFAULT_FUEL_ECONOMY_UNIT.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_FUEL_ECONOMY_UNIT;
        }
    }

    public void storeBlackboxEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_BLACKBOX_ENABLED, enabled);
        editor.apply();
    }

    public boolean isBlackboxEnabled() {
        return mPrefs.getBoolean(PREF_BLACKBOX_ENABLED, Consts.DEFAULT_BLACKBOX_ENABLED);
    }

    public void storeBlackboxEngineType(BlackboxEngineType engineType) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_ENGINE_TYPE, engineType.name());
        editor.apply();
    }

    public BlackboxEngineType getBlackboxEngineType() {
        try {
            return BlackboxEngineType.valueOf(mPrefs.getString(PREF_BLACKBOX_ENGINE_TYPE,
                    DEFAULT_BLACKBOX_ENGINE_TYPE.name()));
        } catch (IllegalArgumentException e) {
            return DEFAULT_BLACKBOX_ENGINE_TYPE;
        }
    }

    public void storeBlackboxVideoResolution(VideoResolution resolution) {
        SharedPreferences.Editor editor = mPrefs.edit();
        String s = resolution.flatten();
        editor.putString(PREF_BLACKBOX_VIDEO_RESOLLUTION, s);
        editor.apply();
    }

    public VideoResolution getBlackboxVideoResolution() {
        String s = mPrefs.getString(PREF_BLACKBOX_VIDEO_RESOLLUTION,
                new VideoResolution(Consts.DEFAULT_BLACKBOX_VIDEO_RESOLUTION_WIDTH,
                        Consts.DEFAULT_BLACKBOX_VIDEO_RESOLUTION_HEIGHT, false).flatten());
        VideoResolution resolution = new VideoResolution();
        resolution.unflatten(s);
        return resolution;
    }

    public void storeBlackboxVideoQuality(BlackboxQuality quality) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_VIDEO_QUALITY, quality.name());
        editor.apply();
    }

    public BlackboxQuality getBlackboxVideoQuality() {
        try {
            return BlackboxQuality.valueOf(mPrefs.getString(PREF_BLACKBOX_VIDEO_QUALITY,
                    Consts.DEFAULT_BLACKBOX_VIDEO_QUALITY.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_BLACKBOX_VIDEO_QUALITY;
        }
    }

    public void storeBlackboxAudioEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_BLACKBOX_AUDIO_ENABLED, enabled);
        editor.apply();
    }

    public boolean isBlackboxAudioEnabled() {
        return mPrefs.getBoolean(PREF_BLACKBOX_AUDIO_ENABLED,
                Consts.DEFAULT_BLACKBOX_AUDIO_ENABLED);
    }

    public void storeBlackboxRecordType(BlackboxRecordType type) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_RECORD_TYPE, type.name());
        editor.apply();
    }

    public BlackboxRecordType getBlackboxRecordType() {
        try {
            return BlackboxRecordType.valueOf(mPrefs.getString(PREF_BLACKBOX_RECORD_TYPE,
                    Consts.DEFAULT_BLACKBOX_RECORD_TYPE.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_BLACKBOX_RECORD_TYPE;
        }
    }

	/*public void storeBlackboxNormalFilePrefix(String prefix) {
        SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_NORMAL_FILE_PREFIX, prefix);
		editor.apply();
	}*/

    public String getBlackboxNormalFilePrefix() {
        return mPrefs.getString(PREF_BLACKBOX_NORMAL_FILE_PREFIX,
                Consts.DEFAULT_BLACKBOX_NORMAL_FILE_PREFIX);
    }

	/*public void storeBlackboxEventFilePrefix(String prefix) {
        SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_EVENT_FILE_PREFIX, prefix);
		editor.apply();
	}*/

    public String getBlackboxEventFilePrefix() {
        return mPrefs.getString(PREF_BLACKBOX_EVENT_FILE_PREFIX,
                Consts.DEFAULT_BLACKBOX_EVENT_FILE_PREFIX);
    }

    /*public void storeBlackboxNormalVideoDuration(int duration) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_NORMAL_VIDEO_DURATION, String.valueOf(duration));
        editor.apply();
    }*/

    public int getBlackboxNormalVideoDuration() {
        try {
            return Integer.valueOf(mPrefs.getString(PREF_BLACKBOX_NORMAL_VIDEO_DURATION,
                    String.valueOf(Consts.DEFAULT_BLACKBOX_NORMAL_VIDEO_DURATION)));
        } catch (NumberFormatException e) {
            return Consts.DEFAULT_BLACKBOX_NORMAL_VIDEO_DURATION;
        }
    }

    /*public void storeBlackboxEventVideoDuration(int duration) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_EVENT_VIDEO_DURATION, String.valueOf(duration));
        editor.apply();
    }*/

    public int getBlackboxEventVideoDuration() {
        try {
            return Integer.valueOf(mPrefs.getString(PREF_BLACKBOX_EVENT_VIDEO_DURATION,
                    String.valueOf(Consts.DEFAULT_BLACKBOX_EVENT_VIDEO_DURATION)));
        } catch (NumberFormatException e) {
            return Consts.DEFAULT_BLACKBOX_EVENT_VIDEO_DURATION;
        }
    }

    /*public void storeBlackboxStorageType(StorageType type) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_STORAGE_TYPE, type.name());
        editor.apply();
    }*/

    public StorageType getBlackboxStorageType() {
        try {
            return StorageType.valueOf(mPrefs.getString(PREF_BLACKBOX_STORAGE_TYPE,
                    Consts.DEFAULT_BLACKBOX_STORAGE_TYPE.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_BLACKBOX_STORAGE_TYPE;
        }
    }

    public void storeBlackboxMinStorageSize(long size) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(PREF_BLACKBOX_MIN_STORAGE_SIZE, size);
        editor.apply();
    }

    public long getBlackboxMinStorageSize() {
        try {
            return Long.valueOf(mPrefs.getString(PREF_BLACKBOX_MIN_STORAGE_SIZE,
                    String.valueOf(Consts.DEFAULT_BLACKBOX_MIN_STORAGE_SIZE)));
        } catch (NumberFormatException e) {
            return Consts.DEFAULT_BLACKBOX_MIN_STORAGE_SIZE;
        }
    }

    public void storeBlackboxMaxStorageSize(long size) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(PREF_BLACKBOX_MAX_STORAGE_SIZE, size);
        editor.apply();
    }

    public long getBlackboxMaxStorageSize() {
        try {
            return Long.valueOf(mPrefs.getString(PREF_BLACKBOX_MAX_STORAGE_SIZE,
                    String.valueOf(Consts.DEFAULT_BLACKBOX_MAX_STORAGE_SIZE)));
        } catch (NumberFormatException e) {
            return Consts.DEFAULT_BLACKBOX_MAX_STORAGE_SIZE;
        }
    }

	/*public void storeBlackboxNormalDirName(String dirName) {
        SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_NORMAL_DIR_NAME, dirName);
		editor.apply();
	}*/

    public String getBlackboxNormalDirName() {
        return mPrefs.getString(PREF_BLACKBOX_NORMAL_DIR_NAME,
                Consts.DEFAULT_BLACKBOX_NORMAL_DIR_NAME);
    }

	/*public void storeBlackboxEventDirName(String dirName) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_EVENT_DIR_NAME, dirName);
		editor.apply();
	}*/

    public String getBlackboxEventDirName() {
        return mPrefs.getString(PREF_BLACKBOX_EVENT_DIR_NAME,
                Consts.DEFAULT_BLACKBOX_EVENT_DIR_NAME);
    }

	/*public void storeBlackboxArchiveDirName(String dirName) {
	SharedPreferences.Editor editor = mPrefs.edit();
	editor.putString(PREF_BLACKBOX_ARCHIVE_DIR_NAME, dirName);
	editor.apply();
}*/

    public String getBlackboxArchiveDirName() {
        return mPrefs.getString(PREF_BLACKBOX_ARCHIVE_DIR_NAME,
                Consts.DEFAULT_BLACKBOX_ARCHIVE_DIR_NAME);
    }

	/*public void storeBlackboxDirNameFormat(String format) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_DIR_NAME_FORMAT, format);
		editor.apply();
	}*/

    public String getBlackboxDirNameFormat() {
        return mPrefs.getString(PREF_BLACKBOX_DIR_NAME_FORMAT,
                Consts.DEFAULT_BLACKBOX_DIR_NAME_FORMAT);
    }

	/*public void storeBlackboxFileNameFormat(String format) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_FILE_NAME_FORMAT, format);
		editor.apply();
	}*/

    public String getBlackboxFileNameFormat() {
        return mPrefs.getString(PREF_BLACKBOX_FILE_NAME_FORMAT,
                Consts.DEFAULT_BLACKBOX_FILE_NAME_FORMAT);
    }

	/*public void storeBlackboxVideoFileExt(String fileExt) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_VIDEO_FILE_EXT, fileExt);
		editor.apply();
	}*/

    public String getBlackboxVideoFileExt() {
        return mPrefs.getString(PREF_BLACKBOX_VIDEO_FILE_EXT,
                Consts.DEFAULT_BLACKBOX_VIDEO_FILE_EXT);
    }

	/*public void storeBlackboxMetaFileExt(String fileExt) {
		SharedPreferences.Editor editor = mPrefs.edit();
		editor.putString(PREF_BLACKBOX_META_FILE_EXT, fileExt);
		editor.apply();
	}*/

    public String getBlackboxMetaFileExt() {
        return mPrefs.getString(PREF_BLACKBOX_META_FILE_EXT,
                Consts.DEFAULT_BLACKBOX_METADATA_FILE_EXT);
    }

    public void storeBlackboxColorCorrectionEnabled(boolean colorCorrectionEnabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_BLACKBOX_COLOR_CORRECTION_ENABLED, colorCorrectionEnabled);
        editor.apply();
    }

    public boolean isBlackboxColorCorrectionEnabled() {
        return mPrefs.getBoolean(PREF_BLACKBOX_COLOR_CORRECTION_ENABLED,
                Consts.DEFAULT_BLACKBOX_COLOR_CORRECTION_ENABLED);
    }

    public void storeImpactSensitivity(ImpactSensitivity sensitivity) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_IMPACT_SENSITIVITY, sensitivity.name());
        editor.apply();
    }

    public ImpactSensitivity getImpactSensitivity() {
        try {
            return ImpactSensitivity.valueOf(mPrefs.getString(PREF_IMPACT_SENSITIVITY,
                    Consts.DEFAULT_IMPACT_SENSITIVITY.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_IMPACT_SENSITIVITY;
        }
    }

    public void storeErsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_ERS_ENABLED, enabled);
        editor.apply();
    }

    public boolean isErsEnabled() {
        return mPrefs.getBoolean(PREF_ERS_ENABLED, Consts.DEFAULT_ERS_ENABLED);
    }

    public void storeErsCallContactPhoneNumber(String number) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_CALL_CONTACT_PHONE_NUMBER, number);
        editor.apply();
    }

    public String getErsCallContactPhoneNumber() {
        return mPrefs.getString(PREF_ERS_CALL_CONTACT_PHONE_NUMBER, "");
    }

    public void storeErsCallContactName(String name) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_CALL_CONTACT_NAME, name);
        editor.apply();
    }

    public String getErsCallContactName() {
        return mPrefs.getString(PREF_ERS_CALL_CONTACT_NAME, "");
    }

    public void storeErsSmsContactPhoneNumber(String number) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_SMS_CONTACT_PHONE_NUMBER, number);
        editor.apply();
    }

    public String getErsSmsContactPhoneNumber() {
        return mPrefs.getString(PREF_ERS_SMS_CONTACT_PHONE_NUMBER, "");
    }

    public void storeErsSmsContactName(String name) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_SMS_CONTACT_NAME, name);
        editor.apply();
    }

    public String getErsSmsContactName() {
        return mPrefs.getString(PREF_ERS_SMS_CONTACT_NAME, "");
    }

    public void storeErsSmsMessage(String message) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_SMS_MESSAGE, message);
        editor.apply();
    }

    public String getErsSmsMessage() {
        return mPrefs.getString(PREF_ERS_SMS_MESSAGE, "");
    }

    public void storeErsYoutubePrivacy(YoutubePrivacy privacy) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_YOUTUBE_PRIVACY, privacy.name());
        editor.apply();
    }

    public YoutubePrivacy getErsYoutubePrivacy() {
        try {
            return YoutubePrivacy.valueOf(mPrefs.getString(PREF_ERS_YOUTUBE_PRIVACY,
                    Consts.DEFAULT_ERS_YOUTUBE_PRIVACY.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_ERS_YOUTUBE_PRIVACY;
        }
    }

    public void storeErsTarget(ErsTarget target) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_TARGET, target.name());
        editor.apply();
    }

    public ErsTarget getErsTarget() {
        try {
            return ErsTarget.valueOf(mPrefs.getString(PREF_ERS_TARGET,
                    Consts.DEFAULT_ERS_TARGET.name()));
        } catch (IllegalArgumentException e) {
            return Consts.DEFAULT_ERS_TARGET;
        }
    }

    public void storeErsYoutubeAccountName(String accountName) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_ERS_YOUTUBE_ACCOUNT_NAME, accountName);
        editor.apply();
    }

    public String getErsYoutubeAccountName() {
        return mPrefs.getString(PREF_ERS_YOUTUBE_ACCOUNT_NAME, null);
    }

    public void storeQuickLaunchNaviApp(String appId) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_QUICK_LAUNCH_APP_NAVI, appId);
        editor.apply();
    }

    public String getQuickLaunchNaviApp() {
        return mPrefs.getString(PREF_QUICK_LAUNCH_APP_NAVI, null);
    }

    public void storeAutoLaunchNaviAppEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_AUTO_LAUNCH_APP_NAVI_ENABLED, enabled);
        editor.apply();
    }

    public boolean isAutoLaunchNaviAppEnabled() {
        return mPrefs.getBoolean(PREF_AUTO_LAUNCH_APP_NAVI_ENABLED, false);
    }

    public void storeQuickLaunchCustomApp(String appId) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_QUICK_LAUNCH_APP_CUSTOM, appId);
        editor.apply();
    }

    public String getQuickLaunchCustomApp() {
        return mPrefs.getString(PREF_QUICK_LAUNCH_APP_CUSTOM, null);
    }

    /*public void storeOverspeedThreshold(int overspeedThreshold) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_OVERSPEED_THRESHOLD, String.valueOf(overspeedThreshold));
        editor.apply();
    }*/

    public int getOverspeedThreshold() {
        try {
            return Integer.valueOf(mPrefs.getString(PREF_OVERSPEED_THRESHOLD,
                    String.valueOf(Consts.DEFAULT_ECO_OVERSPEED_THRESHOLD)));
        } catch (NumberFormatException e) {
            return Consts.DEFAULT_ECO_OVERSPEED_THRESHOLD;
        }
    }

    public void storeTripA(float distanceInKm, float consumptionInLiters) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat(PREF_TRIP_A_DISTANCE, distanceInKm);
        editor.putFloat(PREF_TRIP_A_FUEL_CONSUMPTION, consumptionInLiters);
        editor.apply();
    }

    public float getTripADistance() {
        return mPrefs.getFloat(PREF_TRIP_A_DISTANCE, 0);
    }

    public float getTripAFuelConsumption() {
        return mPrefs.getFloat(PREF_TRIP_A_FUEL_CONSUMPTION, 0);
    }

    public void resetTripA() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(PREF_TRIP_A_DISTANCE);
        editor.remove(PREF_TRIP_A_FUEL_CONSUMPTION);
        editor.apply();
    }

    public void storeTripB(float distanceInKm, float consumptionInLiters) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putFloat(PREF_TRIP_B_DISTANCE, distanceInKm);
        editor.putFloat(PREF_TRIP_B_FUEL_CONSUMPTION, consumptionInLiters);
        editor.apply();
    }

    public float getTripBDistance() {
        return mPrefs.getFloat(PREF_TRIP_B_DISTANCE, 0);
    }

    public float getTripBFuelConsumption() {
        return mPrefs.getFloat(PREF_TRIP_B_FUEL_CONSUMPTION, 0);
    }

    public void resetTripB() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(PREF_TRIP_B_DISTANCE);
        editor.remove(PREF_TRIP_B_FUEL_CONSUMPTION);
        editor.apply();
    }

    public void storeDrivingHelpDone(boolean done) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_DRIVING_HELP_DONE, done);
        editor.apply();
    }

    public boolean isDrivingHelpDone() {
        return mPrefs.getBoolean(PREF_DRIVING_HELP_DONE, false);
    }

    public void storeEngineOffDetectionEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_ENGINE_OFF_DETECTION_ENABLED, enabled);
        editor.apply();
    }

    public boolean isEngineOffDetectionEnabled() {
        return mPrefs.getBoolean(PREF_ENGINE_OFF_DETECTION_ENABLED, true);
    }

    /*public boolean isIsgSupport() {
        return mPrefs.getBoolean("engine_off_detection_isg", false);
    }*/

    public boolean isEngineOnDetectionEnabled() {
        return mPrefs.getBoolean(PREF_ENGINE_ON_DETECTION_ENABLED, true);
    }


    public void storeBlackboxFocusMode(String mode) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_FOCUS_MODE, mode);
        editor.apply();
    }

    public String getBlackboxFocusMode() {
        return mPrefs.getString(PREF_BLACKBOX_FOCUS_MODE, CameraHelper.FOCUS_MODE_AUTO);
    }

    public void storeBlackboxExposureExtra(int exposure) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_BLACKBOX_EXPOSURE_EXTRA, String.valueOf(exposure));
        editor.apply();
    }

    public int getBlackboxExposureExtra() {
        try {
            return Integer.valueOf(mPrefs.getString(PREF_BLACKBOX_EXPOSURE_EXTRA, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void storeBlackboxOsdEnabled(boolean enabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_BLACKBOX_OSD_ENABLED, enabled);
        editor.apply();
    }

    public boolean  getBlackboxOsdEnabled() {
        return mPrefs.getBoolean(PREF_BLACKBOX_OSD_ENABLED, true);
    }

    public boolean getSoundEffectEnabled() {
        return mPrefs.getBoolean("sound_effect_enabled", true);
    }

    public boolean hasNewEvent() {
        return getNewEventCount() > 0;
    }

    public int getNewEventCount() {
        return mPrefs.getInt("new-event-count", 0);
    }

    public void storeNewEventCount(int count) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("new-event-count",count);
        editor.apply();
    }

    public boolean hasNewNoti() {
        return getNewNotiCount() > 0;
    }

    public int getNewNotiCount() {
        return mPrefs.getInt("new-noti-count", 0);
    }

    public void storeNewNotiCount(int count) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("new-noti-count",count);
        editor.apply();
    }

    public void storeAccount(Account account) {
        SharedPreferences.Editor editor = mPrefs.edit();
        String value = mGson.toJson(account);
        editor.putString("pref-account", value);
        editor.apply();
    }

    public Account getAccount() {
        String value = mPrefs.getString("pref-account", null);
        if (!TextUtils.isEmpty(value)) {
            return mGson.fromJson(value, Account.class);
        } else {
            return null;
        }
    }

    public void storeVehicleMakerTag(String name) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-tag-vehicle-maker", name);
        editor.apply();
    }

    public String getVehicleMakerTag() {
        return mPrefs.getString("pref-tag-vehicle-maker", null);
    }

    public void storeVehicleFuelTag(String name) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-tag-vehicle-fuel", name);
        editor.apply();
    }

    public String getVehicleFuelTag() {
        return mPrefs.getString("pref-tag-vehicle-fuel", null);
    }

    public void storeAccountSexTag(String sex) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-tag-account-sex", sex);
        editor.apply();
    }

    public String getAccountSexTag() {
        return mPrefs.getString("pref-tag-account-sex", null);
    }

    public void storeAccountRegionTag(String region) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-tag-account-region", region);
        editor.apply();
    }

    public String getAccountRegionTag() {
        return mPrefs.getString("pref-tag-account-region", null);
    }

    public void storeIdentity(String identity) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-identity", identity);
        editor.apply();
    }

    public String getIdentity() {
        return mPrefs.getString("pref-identity", null);
    }

    public boolean isFloatingWindowEnabled() {
        return mPrefs.getBoolean("floating_enabled", false);
    }

    public void storeConfigVersion(String version) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString("pref-config-version", version);
        editor.apply();
    }

    public String getConfigVersion() {
        return mPrefs.getString("pref-config-version", null);
    }

//    public boolean isPowerSave() {
//        return mPrefs.getBoolean("engine_on_detection_power_save", false);
//    }

    public String getAutoStartMode() {
        if (Build.VERSION.SDK_INT < 23) {
            return mPrefs.getString("engine_on_detection_enabled_normal", "NORMAL");
        }
        return mPrefs.getString("engine_on_detection_enabled_marshmallow", "HIGH");
    }

}

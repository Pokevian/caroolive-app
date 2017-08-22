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

import android.media.AudioManager;

import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.model.StorageType;
import com.pokevian.app.smartfleet.model.YoutubePrivacy;
import com.pokevian.app.smartfleet.service.ImpactDetector.ImpactSensitivity;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEngineType;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxQuality;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxRecordType;
import com.pokevian.lib.obd2.defs.Unit;

public final class Consts {

    public static final boolean ALPHA = true;

    // Data sender wakeup delay
    public static final int DATA_SENDER_WAKE_UP_DELAY = 10000; // 10 seconds

    // Youtube related constants
    public static final String YOUTUBE_PROJECT_NAME = "CaroO Live";
    public static final String YOUTUBE_TITLE = "Urgent video";
    public static final String YOUTUBE_DESCRIPTION = "Uploaded by CaroO Live™";
    public static final String[] YOUTUBE_TAGS = {"Pokevian", "CaroO Live"};

    // social network
    public static final String SNS_KAKAO_TALK = "http://plus.kakao.com/home/lnql67i3";
    public static final String SNS_GOOGLE_PLUS = "https://plus.google.com/communities/110517586842207212965";
    public static final String SNS_FACE_BOOK = "https://www.facebook.com/groups/caroolive";

    // OBD supplier
    public static final String OBD_SUPPLIER_URL = "http://m.shopping.naver.com/search/all.nhn?query=Elm327+bluetooth&pagingIndex=1&productSet=total&viewType=lst&sort=review&frm=NVSHSRC&selectedFilterTab=category";

    //
    // Location related constants
    //
    public static final int LOCATION_MIN_ACCURACY = 200; // 1000; // 1 km
    public static final int LOCATION_INTERVAL = 1000; // 1 second
    public static final int LOCATION_FASTEST_INTERVAL = 1000; // 1 second


    //
    // Max OBD connection failure count
    //
//    public static final int MAX_OBD_CONNECTION_FAILURE_COUNT = 5;
    public static final int MAX_OBD_CONNECTION_FAILURE_COUNT = Integer.MAX_VALUE;


    //
    // Target Audio stream
    //
    public static final int AUDIO_TARGET_STREAM = AudioManager.STREAM_MUSIC;

    //
    // Auto connect wakeup delay, milliseconds
    //
    public static final int AUTO_CONNECT_WAKEUP_DELAY = 10000; // 10 seconds

    //
    // Default preferences related constants
    //
    public static final Unit DEFAULT_DISTANCE_UNIT = Unit.KM;
    public static final Unit DEFAULT_SPEED_UNIT = Unit.KPH;
    public static final Unit DEFAULT_VOLUME_UNIT = Unit.L;
    public static final Unit DEFAULT_FUEL_ECONOMY_UNIT = Unit.KPL;
    public static final boolean DEFAULT_BLACKBOX_ENABLED = false;
    public static final BlackboxEngineType DEFAULT_BLACKBOX_ENGINE_TYPE = BlackboxEngineType.MEDIA_RECORDER;
    public static final int DEFAULT_BLACKBOX_VIDEO_RESOLUTION_WIDTH = 640;
    public static final int DEFAULT_BLACKBOX_VIDEO_RESOLUTION_HEIGHT = 480;
    public static final BlackboxQuality DEFAULT_BLACKBOX_VIDEO_QUALITY = BlackboxQuality.NORMAL;
    public static final boolean DEFAULT_BLACKBOX_AUDIO_ENABLED = false;
    public static final BlackboxRecordType DEFAULT_BLACKBOX_RECORD_TYPE = BlackboxRecordType.RECORD_NORMAL_EVENT;
    public static final String DEFAULT_BLACKBOX_NORMAL_FILE_PREFIX = "";
    public static final String DEFAULT_BLACKBOX_EVENT_FILE_PREFIX = "";
    public static final int DEFAULT_BLACKBOX_NORMAL_VIDEO_DURATION = 180000; // 3 minutes
    public static final int DEFAULT_BLACKBOX_EVENT_VIDEO_DURATION = 20000; // 20 seconds
    public static final StorageType DEFAULT_BLACKBOX_STORAGE_TYPE = StorageType.INTERNAL;
    public static final long DEFAULT_BLACKBOX_MIN_STORAGE_SIZE = 100 * 1024 * 1024; // 100 MB
    public static final long DEFAULT_BLACKBOX_MAX_STORAGE_SIZE = 1L * 1024 * 1024 * 1024 * 1024; // 1 TB
    public static final String DEFAULT_BLACKBOX_NORMAL_DIR_NAME = "normal";
    public static final String DEFAULT_BLACKBOX_EVENT_DIR_NAME = "event";
    public static final String DEFAULT_BLACKBOX_ARCHIVE_DIR_NAME = "archive";
    public static final String DEFAULT_BLACKBOX_DIR_NAME_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_BLACKBOX_FILE_NAME_FORMAT = "yyyyMMddHHmmss";
    public static final String DEFAULT_BLACKBOX_VIDEO_FILE_EXT = ".mp4";
    public static final String DEFAULT_BLACKBOX_METADATA_FILE_EXT = ".smi";
    public static final boolean DEFAULT_BLACKBOX_COLOR_CORRECTION_ENABLED = false;
    public static final ImpactSensitivity DEFAULT_IMPACT_SENSITIVITY = ImpactSensitivity.NORMAL;
    public static final boolean DEFAULT_ERS_ENABLED = true;
    public static final YoutubePrivacy DEFAULT_ERS_YOUTUBE_PRIVACY = YoutubePrivacy.UNLISTED;
    public static final ErsTarget DEFAULT_ERS_TARGET = ErsTarget.NONE;
    public static final int DEFAULT_ECO_OVERSPEED_THRESHOLD = 110; // km/h
    public static final float DEFAULT_ECO_HARSH_ACCEL_THRESHOLD = 7.0f; // km/h/s
    public static final float DEFAULT_ECO_HARSH_BRAKE_THRESHOLD = -9.0f; // km/h/s
    public static final int DEFAULT_ECO_LOW_FUEL_THRESHOLD = 10; // %
    public static final int DEFAULT_ECO_OVERHEAT_THRESHOLD = 115; // °C
    public static final int DEFAULT_ECO_MIN_ECO_SPEED = 60; // km/h
    public static final int DEFAULT_ECO_MAX_ECO_SPEED = 80; // km/h
    public static final int DEFAULT_ECO_MIN_LONG_TERM_OVERSPEED_TIME = 3 * 60 * 1000; // 3 minutes
    public static final float DEFAULT_ECO_MIN_AUX_BATTERY_LEVEL_VES_OFF = 10.5f;
    public static final float DEFAULT_ECO_MIN_AUX_BATTERY_LEVEL_VES_ON = 13.2f;
    public static final float DEFAULT_ECO_MAX_AUX_BATTERY_LEVEL_VES_ON = 14.8f;
    public static final int DEFAULT_ECO_GASOLINE_IDLING_RESTRICTED_TIME = 3 * 60 * 1000; // gasoline, 3 minutes
    public static final int DEFAULT_ECO_DIESEL_IDLING_RESTRICTED_TIME = 5 * 60 * 1000; // diesel, 5 minutes
    public static final int DEFAULT_ECO_MAX_IDLING_RESTRICTED_TIME = 10 * 60 * 1000; // <5'C or >25'C, 10 minutes
    public static final int DEFAULT_ECO_MIN_HARSH_TURN_SPEED = 15; // km/h
    public static final int DEFAULT_ECO_MIN_HARSH_TURN_INTERVAL = 2 * 1000; // 2 seconds
    public static final int DEFAULT_ECO_MAX_HARSH_TURN_INTERVAL = 4 * 1000; // 4 seconds
    public static final int DEFAULT_ECO_HARSH_LEFT_TURN_FROM_DEGREE = 240; // degree
    public static final int DEFAULT_ECO_HARSH_LEFT_TURN_TO_DEGREE = 300; // degree
    public static final int DEFAULT_ECO_HARSH_RIGHT_TURN_FROM_DEGREE = 60; // degree
    public static final int DEFAULT_ECO_HARSH_RIGHT_TURN_TO_DEGREE = 120; // degree
    public static final int DEFAULT_ECO_HARSH_U_TURN_FROM_DEGREE = 160; // degree
    public static final int DEFAULT_ECO_HARSH_U_TURN_TO_DEGREE = 200; // degree
    public static final int DEFAULT_ECO_MIN_GEAR_SHIFT_TIME = 10 * 1000; // 10 seconds
    public static final int DEFAULT_ECO_MAX_GEAR_SHIFT_ALARM_COUNT = 3;
    public static final int DEFAULT_TOUCH_ALLOWED_SPEED = 20; // km/h

}

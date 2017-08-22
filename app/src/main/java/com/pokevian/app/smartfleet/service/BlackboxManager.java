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
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.HandlerThread;
import android.view.SurfaceHolder;

import com.pokevian.app.smartfleet.model.BlackboxMetadata;
import com.pokevian.app.smartfleet.setting.Consts;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.blackbox.BlackboxConfig;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEngineType;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEventType;
import com.pokevian.lib.blackbox.BlackboxEngine;
import com.pokevian.lib.blackbox.BlackboxEngine.BlackboxError;
import com.pokevian.lib.blackbox.BlackboxEngine.Info;
import com.pokevian.lib.blackbox.mediacodec.MediaCodecEngine;
import com.pokevian.lib.blackbox.mediarecorder.MediaRecorderEngine;
import com.pokevian.lib.blackbox2.mc.MediaCodecEngine2;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;

public class BlackboxManager implements BlackboxEngine.OnStateChangeListener, BlackboxEngine.OnBlackboxInfoListener,
        BlackboxEngine.OnBlackboxErrorListener {

    static final String TAG = "BlackboxManager";
    final Logger logger = Logger.getLogger(TAG);

    private final Context mContext;
    private BlackboxEngine mEngine;
    private boolean mIsStopping;
    private final BlackboxManagerCallbacks mCallbacks;

    private long mEventBeginTime;
    private long mEventEndTime;

    private BlackboxMetadataWriter mMetadataWriter;

    private final int mOverspeedThreshold;

    public BlackboxManager(Context context, BlackboxManagerCallbacks callbacks) {
        mContext = context.getApplicationContext();
        mCallbacks = callbacks;

        SettingsStore settingsStore = SettingsStore.getInstance();
        int overspeedThreshold = settingsStore.getOverspeedThreshold();
        mOverspeedThreshold = overspeedThreshold;
    }

    public void run(BlackboxEngineType engineType, BlackboxConfig config, SurfaceTexture texture, int width, int height) {
        synchronized (this) {
            if (mEngine == null) {
                HandlerThread thread = new HandlerThread("blackbox-engine");
                thread.start();
                if (engineType == BlackboxEngineType.MEDIA_CODEC) {
                    mEngine = new MediaCodecEngine2(mContext, thread.getLooper());
                } else {
                    mEngine = new MediaRecorderEngine(mContext, thread.getLooper());
                }
                mEngine.registerOnStateChangeListener(this);
                mEngine.registerOnInfoListener(this);
                mEngine.registerOnErrorListener(this);
                mEngine.setConfig(config);
                mEngine.setSurfaceTexture(texture, width, height);
                mEngine.start();

                // Metadata writer
                thread = new HandlerThread("blackbox-metadata-writer");
                thread.start();
                mMetadataWriter = new BlackboxMetadataWriter(mContext, thread.getLooper(),
                        Consts.DEFAULT_BLACKBOX_EVENT_VIDEO_DURATION);
            }
        }
    }

    public void run(BlackboxEngineType engineType, BlackboxConfig config, Camera camera,
                    SurfaceHolder surfaceHolder) {
        synchronized (this) {
            if (mEngine == null) {
                HandlerThread thread = new HandlerThread("blackbox-engine");
                thread.start();
                if (engineType == BlackboxEngineType.MEDIA_CODEC) {
                    mEngine = new MediaCodecEngine(mContext, thread.getLooper());

                } else {
                    mEngine = new MediaRecorderEngine(mContext, thread.getLooper());
                }
                mEngine.registerOnStateChangeListener(this);
                mEngine.registerOnInfoListener(this);
                mEngine.registerOnErrorListener(this);
                mEngine.setConfig(config);
                mEngine.setCamera(camera);
                mEngine.setSurfaceHolder(surfaceHolder);
                mEngine.start();

                // Metadata writer
                thread = new HandlerThread("blackbox-metadata-writer");
                thread.start();
                mMetadataWriter = new BlackboxMetadataWriter(mContext, thread.getLooper(),
                        Consts.DEFAULT_BLACKBOX_EVENT_VIDEO_DURATION);
            }
        }
    }

    public void stop() {
        synchronized (this) {
            if (mEngine != null && !mIsStopping) {
                mEngine.stop();

                mIsStopping = true;
            }
        }
    }

    public void onEmergency(BlackboxEventType eventType) {
        synchronized (this) {
            if (mEngine != null && !mIsStopping) {
                mEngine.event(eventType);
            }
        }
    }

    @Override
    public void onBlackboxStarted(BlackboxEngine engine) {
        mCallbacks.onBlackboxStarted();
    }

    @Override
    public void onBlackboxStopped(BlackboxEngine engine) {
        synchronized (this) {
            if (mEngine != null) {
                mEngine.unregisterOnStateChangeListener(this);
                mEngine.unregisterOnInfoListener(this);
                mEngine.unregisterOnErrorListener(this);
                mEngine.exit();

                mEngine = null;
                mIsStopping = false;

                mMetadataWriter.exit();
                mMetadataWriter = null;
            }
        }

        mCallbacks.onBlackboxStopped();
    }

    @Override
    public void onInfo(BlackboxEngine engine, Info info, Object extra) {
        if (info == Info.ON_NORMAL_FILE_WRITE_START) {
            logger.info("ON_NORMAL_FILE_WRITE_START");
            BlackboxEngine.FileWriteOperationInfo opInfo = (BlackboxEngine.FileWriteOperationInfo) extra;
            onNormalFileWriteStart(opInfo);
        } else if (info == Info.ON_NORMAL_FILE_WRITE_STOP) {
            logger.info("ON_NORMAL_FILE_WRITE_STOP");
            BlackboxEngine.FileWriteOperationInfo opInfo = (BlackboxEngine.FileWriteOperationInfo) extra;
            onNormalFileWriteStop(opInfo);
        } else if (info == Info.ON_EVENT_DETECTED) {
            logger.info("ON_EVENT_DETECTED");
            BlackboxEventType eventType = (BlackboxEventType) extra;
            onEventDetected(eventType);
        } else if (info == Info.ON_EVENT_FILE_WRITE_START) {
            logger.info("ON_EVENT_FILE_WRITE_START");
            BlackboxEngine.FileWriteOperationInfo opInfo = (BlackboxEngine.FileWriteOperationInfo) extra;
            onEventFileWriteStart(opInfo);
        } else if (info == Info.ON_EVENT_FILE_WRITE_STOP) {
            logger.info("ON_EVENT_FILE_WRITE_STOP");
            BlackboxEngine.FileWriteOperationInfo opInfo = (BlackboxEngine.FileWriteOperationInfo) extra;
            onEventFileWriteStop(opInfo);
        } else if (info == Info.ON_MIN_STORAGE_SIZE_REACHED) {
            logger.info("ON_MIN_STORAGE_SIZE_REACHED");
            onMinStorageSizeReached();
        } else if (info == Info.ON_MAX_STORAGE_SIZE_REACHED) {
            logger.info("ON_MAX_STORAGE_SIZE_REACHED");
            onMaxStorageSizeReached();
        }
    }

    private void onNormalFileWriteStart(BlackboxEngine.FileWriteOperationInfo opInfo) {
        if (mMetadataWriter != null) {
            File metaFile = getMetaFileFromVideoFile(opInfo.writeFile);
            mMetadataWriter.start(opInfo.operationTime, metaFile);
        }
    }

    private void onNormalFileWriteStop(BlackboxEngine.FileWriteOperationInfo opInfo) {
        if (mMetadataWriter != null) {
            mMetadataWriter.stop(opInfo.operationTime);
        }
    }

    private void onEventDetected(BlackboxEventType eventType) {
        mCallbacks.onBlackboxEventBegin(eventType);
    }

    private void onEventFileWriteStart(BlackboxEngine.FileWriteOperationInfo opInfo) {
        mEventBeginTime = opInfo.operationTime;

        if (mMetadataWriter != null) {
            File metaFile = getMetaFileFromVideoFile(opInfo.writeFile);
            mMetadataWriter.event(opInfo.operationTime, metaFile);
        }
    }

    private void onEventFileWriteStop(BlackboxEngine.FileWriteOperationInfo opInfo) {
        mEventEndTime = opInfo.operationTime;

        if (mMetadataWriter != null) {
            mMetadataWriter.writeEventMetadata(opInfo.operationTime);
        }

        File videoFile = opInfo.writeFile;
        Date beginTime = new Date(mEventBeginTime);
        Date endTime = new Date(mEventEndTime);
        mCallbacks.onBlackboxEventEnd(videoFile, beginTime, endTime);
    }

    private void onMinStorageSizeReached() {
        stop();
        mCallbacks.onBlackboxMinStorageSizeReached();
    }

    private void onMaxStorageSizeReached() {
        stop();
        mCallbacks.onBlackboxMaxStorageSizeReached();
    }

    private File getMetaFileFromVideoFile(File videoFile) {
        String videoFilePath = videoFile.getAbsolutePath();
        String metadataFilePath = videoFilePath.replace(Consts.DEFAULT_BLACKBOX_VIDEO_FILE_EXT,
                Consts.DEFAULT_BLACKBOX_METADATA_FILE_EXT);
        return new File(metadataFilePath);
    }

    @Override
    public void onError(BlackboxEngine engine, BlackboxError error) {
        mCallbacks.onBlackboxError(error);
    }

    public void onObdDataReceived(ObdData data) {
        if (mMetadataWriter != null) {
            BlackboxMetadata metadata = buildMetadata(data);
            mMetadataWriter.writeNormalMetadata(metadata);
        }
    }

    private BlackboxMetadata buildMetadata(ObdData data) {
        BlackboxMetadata meta = new BlackboxMetadata();

        meta.timestamp = data.getLong(KEY.CALC_TIME, System.currentTimeMillis());
        if (data.isValid(KEY.TRIP_DRIVING_TIME)) {
            meta.drivingTime = (int) (data.getFloat(KEY.TRIP_DRIVING_TIME) * 1000); // s -> ms
        }
        if (data.isValid(KEY.TRIP_DRIVING_DIST)) {
            meta.drivingDistance = data.getFloat(KEY.TRIP_DRIVING_DIST);
        }

        if (data.isValid(KEY.LOC_LATITUDE) && data.isValid(KEY.LOC_LONGITUDE)) {
            meta.locationData.latitude = data.getDouble(KEY.LOC_LATITUDE);
            meta.locationData.longitude = data.getDouble(KEY.LOC_LONGITUDE);
            meta.locationData.accuracy = data.getFloat(KEY.LOC_ACCURACY, 0);
            meta.locationData.isValid = true;
        } else {
            meta.locationData.isValid = false;
        }

        if (data.isValid(KEY.SAE_VSS)) {
            int vss = data.getInteger(KEY.SAE_VSS);
            meta.speedData.currentSpeed = vss;
            meta.speedData.highestSpeed = data.getFloat(KEY.TRIP_MAX_VSS, vss);
            meta.speedData.averageSpeed = data.getFloat(KEY.TRIP_AVG_VSS_NI, vss);
            meta.speedData.isIdling = (vss == 0);
            meta.speedData.idlingTime = (int) (data.getFloat(KEY.TRIP_IDLING_TIME, 0) * 1000);
            meta.speedData.isOverSpeed = (vss > mOverspeedThreshold);
            meta.speedData.overSpeedTime = (int) ((data.getFloat(KEY.TRIP_SPEED_ZONE12_TIME, 0)
                    + data.getFloat(KEY.TRIP_SPEED_ZONE13_TIME, 0)
                    + data.getFloat(KEY.TRIP_SPEED_ZONE14_TIME, 0)) * 1000);
            meta.speedData.isSteadySpeed = data.getBoolean(KEY.CALC_STEADY_SPEED, false);
            meta.speedData.steadySpeedTime = (int) (data.getFloat(KEY.TRIP_STEADY_SPEED_TIME, 0) * 1000);
            meta.speedData.isEconomySpeed = data.getBoolean(KEY.CALC_ECO_SPEED, false);
            meta.speedData.economySpeedTime = (int) (data.getFloat(KEY.TRIP_ECO_SPEED_TIME, 0) * 1000);
            meta.speedData.isHarshAccel = data.getBoolean(KEY.CALC_HARSH_ACCEL, false);
            meta.speedData.harshAccelCount = data.getInteger(KEY.TRIP_HARSH_ACCEL_COUNT, 0);
            meta.speedData.isHarshBrake = data.getBoolean(KEY.CALC_HARSH_BRAKE, false);
            meta.speedData.harshBrakelCount = data.getInteger(KEY.TRIP_HARSH_BRAKE_COUNT, 0);
            meta.speedData.isValid = true;
        } else {
            meta.speedData.isValid = false;
        }

        return meta;
    }

    public interface BlackboxManagerCallbacks {
        void onBlackboxStarted();

        void onBlackboxStopped();

        void onBlackboxError(BlackboxError error);

        void onBlackboxEventBegin(BlackboxEventType eventType);

        void onBlackboxEventEnd(File eventFile, Date beginTime, Date endTime);

        void onBlackboxMinStorageSizeReached();

        void onBlackboxMaxStorageSizeReached();
    }

    public BlackboxEngine getEngine() {
        return mEngine;
    }
}

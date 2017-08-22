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

package com.pokevian.app.smartfleet.ui.driving;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.StorageType;
import com.pokevian.app.smartfleet.service.VehicleDataBroadcaster;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.service.VehicleService.VehicleServiceBinder;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.driving.DrivingService.DrivingServiceBinder;
import com.pokevian.app.smartfleet.ui.setup.BlackboxInitDialogFragment;
import com.pokevian.app.smartfleet.util.OrientationUtils;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.lib.blackbox.BlackboxConfig;
import com.pokevian.lib.blackbox.BlackboxConst;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEventType;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxSensorLevel;
import com.pokevian.lib.blackbox.BlackboxEngine.BlackboxError;
import com.pokevian.lib.blackbox.BlackboxProfile;
import com.pokevian.lib.blackbox.BlackboxProfileInstance;
import com.pokevian.lib.blackbox2.mc.MediaCodecEngine2;
import com.pokevian.lib.media.camera.CameraHelper;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlackboxControlFragment extends Fragment implements BlackboxInitDialogFragment.BlackboxInitiCallbacks {

    public static final String TAG = "BlackboxControlFragment";
    final Logger logger = Logger.getLogger(TAG);

    private SettingsStore mSettingsStore;
    private VehicleService mVehicleService;
    private LocalBroadcastManager mBroadcastManager;

    private SurfaceHolder mSurfaceHolder;
    private CameraWrapper mCamera;
    private final AtomicBoolean mPendingStartFlag = new AtomicBoolean();
    private final AtomicBoolean mPendingPrepareFlag = new AtomicBoolean();
    private final AtomicBoolean mPreparedFlag = new AtomicBoolean();
    private final AtomicBoolean mStartedFlag = new AtomicBoolean();
    private Handler mHandler;
    private boolean mBlackboxInitialized = false;
    private BlackboxControlCallbacks mCallbacks;

//    private SurfaceTexture mSurfaceTexture;
//    private int mWidth, mHeight;

    public static BlackboxControlFragment newInstance() {
        BlackboxControlFragment fragment = new BlackboxControlFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (BlackboxControlCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement BlackboxControlCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (BlackboxControlCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement BlackboxControlCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HandlerThread thread = new HandlerThread("blackbox-init-thread");
        thread.start();
        mHandler = new Handler(thread.getLooper());

        if (savedInstanceState == null) {
            mSettingsStore = SettingsStore.getInstance();

            getActivity().bindService(new Intent(getActivity(), VehicleService.class),
                    mVehicleServiceConnection, Context.BIND_AUTO_CREATE);
            getActivity().bindService(new Intent(getActivity(), DrivingService.class),
                    mDrivingServiceConnection, Context.BIND_AUTO_CREATE);

            mBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
            IntentFilter filter = new IntentFilter();
            filter.addAction(VehicleDataBroadcaster.ACTION_BLACKBOX_STARTED);
            filter.addAction(VehicleDataBroadcaster.ACTION_BLACKBOX_STOPPED);
            filter.addAction(VehicleDataBroadcaster.ACTION_BLACKBOX_ERROR);
            filter.addAction(VehicleDataBroadcaster.ACTION_BLACKBOX_EVENT_BEGIN);
            filter.addAction(VehicleDataBroadcaster.ACTION_BLACKBOX_EVENT_END);
            mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
        }

        // Init blackbox profile, should be added always
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(BlackboxInitDialogFragment.TAG);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = BlackboxInitDialogFragment.newInstance();
        ft.add(fragment, BlackboxInitDialogFragment.TAG)
                .commit();
    }

    @Override
    public void onDestroy() {
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
        }
        try {
            getActivity().unbindService(mDrivingServiceConnection);
        } catch (Exception e) {
        }
        try {
            getActivity().unbindService(mVehicleServiceConnection);
        } catch (Exception e) {
        }

        mVehicleService = null;

        mHandler.getLooper().quit();

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("running", true);
        super.onSaveInstanceState(outState);
    }

    private ServiceConnection mVehicleServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            logger.debug("Vehicle service connected");
            mVehicleService = ((VehicleServiceBinder) binder).getService();

            mHandler.post(new StartBlackboxRunnable());
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

//    BlackboxPreview2 mPreview;
    DrivingService mDrivingService;
    private ServiceConnection mDrivingServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            logger.debug("Driving service connected");
            DrivingService drivingService = ((DrivingServiceBinder) binder).getService();
            mDrivingService = ((DrivingServiceBinder) binder).getService();

            BlackboxPreview preview = drivingService.getBlackboxPreview();

            /*if (preview instanceof BlackboxPreview2) {
                if (((BlackboxPreview2) preview).getSurfaceTexture() != null) {
                    mSurfaceTexture = ((BlackboxPreview2) preview).getSurfaceTexture();
                   mWidth =  ((BlackboxPreview2) preview).width;
                   mHeight = ((BlackboxPreview2) preview).height;
                }

            } else*/ {
                SurfaceView surfaceView = (SurfaceView) preview.findViewById(R.id.preview_surface);
                mSurfaceHolder = surfaceView.getHolder();

                mHandler.post(new StartBlackboxRunnable());
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent data) {
            final String action = data.getAction();
            if (VehicleDataBroadcaster.ACTION_BLACKBOX_STARTED.equals(action)) {
                onBlackboxStarted();
            } else if (VehicleDataBroadcaster.ACTION_BLACKBOX_STOPPED.equals(action)) {
                onBlackboxStopped();
            } else if (VehicleDataBroadcaster.ACTION_BLACKBOX_ERROR.equals(action)) {
                int errorCode = data.getIntExtra(VehicleDataBroadcaster.EXTRA_BLACKBOX_ERROR_CODE,
                        BlackboxError.UNKOWN.ordinal());
                onBlackboxError(errorCode);
            } else if (VehicleDataBroadcaster.ACTION_BLACKBOX_EVENT_BEGIN.equals(action)) {
                BlackboxEventType eventType = BlackboxEventType.valueOf(
                        data.getStringExtra(VehicleDataBroadcaster.EXTRA_BLACKBOX_EVENT_TYPE));
                onBlackboxEventBegin(eventType);
            } else if (VehicleDataBroadcaster.ACTION_BLACKBOX_EVENT_END.equals(action)) {
                File videoFile = (File) data.getSerializableExtra(VehicleDataBroadcaster.EXTRA_BLACKBOX_EVENT_VIDEO_FILE);
                Date beginTime = (Date) data.getSerializableExtra(VehicleDataBroadcaster.EXTRA_BLACKBOX_EVENT_BEGINE_TIME);
                Date endTime = (Date) data.getSerializableExtra(VehicleDataBroadcaster.EXTRA_BLACKBOX_EVENT_END_TIME);
                onBlackboxEventEnd(videoFile, beginTime, endTime);
            }
        }
    };

    @Override
    public void onBlackboxInitialized() {
        logger.info("blackbox profile initialized");

        mBlackboxInitialized = true;

        mHandler.postDelayed(new StartBlackboxRunnable(), 1000);
    }

    private class StartBlackboxRunnable implements Runnable {
        public void run() {
            synchronized (BlackboxControlFragment.this) {
                if (!mBlackboxInitialized) {
                    return;
                }

                if (mPendingPrepareFlag.get()) {
                    prepareBlackbox();
                }

                if (mPendingStartFlag.get()) {
                    startBlackbox();
                }

//                startBlackbox(mSurfaceTexture, mWidth, mHeight);
            }
        }
    }

    private void onBlackboxStarted() {
        logger.debug("onBlackboxStarted()");

        if (!isDetached()) {
            mCallbacks.onBlackboxStarted();
        }
    }

    private void onBlackboxStopped() {
        logger.debug("onBlackboxStopped()");

        closeCamera(mCamera);
        mCamera = null;

        if (!isDetached()) {
            mCallbacks.onBlackboxStopped();
        }
    }

    private void onBlackboxError(int errorCode) {
        logger.error("onBlackboxError(): errorCode=" + errorCode);

        if (!isDetached()) {
            mCallbacks.onBlackboxError(errorCode);
        }
    }

    private void onBlackboxEventBegin(BlackboxEventType eventType) {
        logger.debug("onBlackboxEventBegin(): eventType=" + eventType);

        if (!isDetached()) {
            mCallbacks.onBlackboxEventBegin(eventType);
        }
    }

    private void onBlackboxEventEnd(File videoFile, Date beginTime, Date endTime) {
        logger.debug("onBlackboxEventEnd(): videoFile=" + videoFile
                + ", beginTime=" + beginTime + ", endTime=" + endTime);

        if (!isDetached()) {
            mCallbacks.onBlackboxEventEnd(videoFile, beginTime, endTime);
        }
    }

    private int mCameraExposureOffset;

    public boolean prepare() {
        mFocusMode = mSettingsStore.getBlackboxFocusMode();
        initFocusMode();
        setFocusMode();

        return true;
    }

    public boolean prepareBlackbox() {
        if (mSurfaceHolder != null) {
            mPendingPrepareFlag.set(false);

            // Storage
            if (!checkStorage()) {
                return false;
            }

            // Camera
            mCamera = openCamera();
            if (mCamera == null) {
                Toast.makeText(getActivity(), R.string.driving_camera_error, Toast.LENGTH_LONG).show();
                return false;
            }
            mCamera.instance.setErrorCallback(new ErrorCallback() {
                public void onError(int error, Camera camera) {
                    logger.debug("Camera error=" + error);
                    Toast.makeText(getActivity(), R.string.driving_camera_error, Toast.LENGTH_LONG).show();

                    closeCamera(mCamera);
                    mCamera = null;
                    stopBlackbox();
                }
            });

            // Set preview size
            BlackboxProfileInstance profile = BlackboxProfileInstance.getInstance(getActivity());
            BlackboxProfile p = profile.getProfile(mSettingsStore.getBlackboxEngineType(),
                    mSettingsStore.getBlackboxVideoResolution());
            if (p != null) {
                logger.debug("startBlackbox(): set camera preview size: "
                        + p.previewWidth + "x" + p.previewHeight);
                Camera.Parameters params = mCamera.instance.getParameters();
                params.setPreviewSize(p.previewWidth, p.previewHeight);
                mCamera.instance.setParameters(params);
            }

            Camera.Parameters params = mCamera.instance.getParameters();
            if (params != null) {
                int max = params.getMaxExposureCompensation();
                int min = params.getMinExposureCompensation();
                Log.i(TAG, "exposure@" + min + ": " + max);
                if (max != 0 || min != 0) {
                    int range = max - min;

                    mCameraExposureOffset = range / 2;
//                    int exposure = mSettingsPrefs.getBlackboxExposureExtra();
                    int exposure = params.getExposureCompensation();
                    int progress = mCameraExposureOffset + exposure;

                    Log.i(TAG, String.format("cameraReady::exposureBar@%d[%d#%d]", progress, range, exposure));

                    params.setExposureCompensation(exposure);
                    mCamera.instance.setParameters(params);

                } else {
//                    disableExposureBar();
                }
            }
            mFocusMode = mSettingsStore.getBlackboxFocusMode();
            initFocusMode();
            setFocusMode();

            // Start preview
            try {
                mCamera.instance.setPreviewDisplay(mSurfaceHolder);
                mCamera.instance.startPreview();
            } catch (IOException e) {
                logger.error("failed to set preview display");
            }

            mCallbacks.onBlackboxPreview();

            mPreparedFlag.set(true);

            logger.info("blackbox prepared...");
            return true;
        } else {
            mPendingPrepareFlag.set(true);
            return false;
        }
    }

//    public void startBlackbox(SurfaceTexture texture, int width, int height) {
//        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
//
//            if (/*mPreparedFlag.get() &&*/ mVehicleService != null) {
//                mPendingStartFlag.set(false);
//                BlackboxConfig config = buildConfig(null);
//
//                mVehicleService.startBlackbox(mSettingsStore.getBlackboxEngineType(),
//                        config, texture, width, height);
//
//                mStartedFlag.set(true);
//            }
//
//            Log.i(TAG, "start blackbox engine");
//        }
//
//    }

    public boolean startBlackbox() {
        if (mPreparedFlag.get() && mVehicleService != null) {
            mPendingStartFlag.set(false);

            // Configuration
            BlackboxConfig config = buildConfig(mCamera);
            if (config == null) {
                closeCamera(mCamera);
                mCamera = null;
                return false;
            }

            mVehicleService.startBlackbox(mSettingsStore.getBlackboxEngineType(),
                    config, mCamera.instance, mSurfaceHolder);

            mStartedFlag.set(true);

            logger.info("start blackbox");
            return true;
        } else {
            mPendingStartFlag.set(true);
            return false;
        }
    }

    private boolean checkStorage() {
        StorageType storageType = mSettingsStore.getBlackboxStorageType();

        File[] dirs = StorageUtils.getExternalFilesDirs(getActivity(), null);
        return (dirs != null && dirs.length > storageType.ordinal());
    }

    private BlackboxConfig buildConfig(CameraWrapper camera) {
        try {
            BlackboxConfig config = new BlackboxConfig();

            int storageIndex = mSettingsStore.getBlackboxStorageType().ordinal();
            File[] normalRootDirs = StorageUtils.getExternalFilesDirs(getActivity(),
                    mSettingsStore.getBlackboxNormalDirName());
            config.normalRootDir = normalRootDirs[storageIndex];

            File[] eventRootDirs = StorageUtils.getExternalFilesDirs(getActivity(),
                    mSettingsStore.getBlackboxEventDirName());
            config.eventRootDir = eventRootDirs[storageIndex];

            config.normalFilePrefix = mSettingsStore.getBlackboxNormalFilePrefix();
            config.eventFilePrefix = mSettingsStore.getBlackboxEventFilePrefix();
            config.normalDuration = mSettingsStore.getBlackboxNormalVideoDuration();
            config.eventDuration = mSettingsStore.getBlackboxEventVideoDuration();
            config.preBufferDuration = mSettingsStore.getBlackboxEventVideoDuration() / 2;
            config.dirNameFormat = mSettingsStore.getBlackboxDirNameFormat();
            config.fileNameFormat = mSettingsStore.getBlackboxFileNameFormat();
            config.videoFileExtenstion = mSettingsStore.getBlackboxVideoFileExt();
            config.metadataFileExtenstion = mSettingsStore.getBlackboxMetaFileExt();
            config.maxStorageSize = mSettingsStore.getBlackboxMaxStorageSize();
            config.sensorLevel = BlackboxSensorLevel.OFF; // NOT USED

            config.recordType = mSettingsStore.getBlackboxRecordType();
            config.resolution = mSettingsStore.getBlackboxVideoResolution();
            config.quality = mSettingsStore.getBlackboxVideoQuality();
            config.hasAudio = mSettingsStore.isBlackboxAudioEnabled();
            config.colorCorrection = mSettingsStore.isBlackboxColorCorrectionEnabled();

//            if (camera != null) {
//                int degrees = OrientationUtils.getRotationDegree(getActivity());
//                degrees = (camera.info.orientation - degrees + 360) % 360;
//                camera.instance.setDisplayOrientation(degrees);
//                config.videoOrientation = degrees;
//            }
            logger.debug("buildConfig(): config=" + config);
            return config;
        } catch (Exception e) {
            logger.error("buildConfig(): cannot build blackbox configuration", e);
        }
        return null;
    }

    public void stopBlackbox() {
        if (mVehicleService != null) {
            mVehicleService.stopBlackbox();
        }

        if (!mStartedFlag.getAndSet(false)) {
            closeCamera(mCamera);
            mCamera = null;
        }
    }

    private CameraWrapper openCamera() {
        try {
            int count = Camera.getNumberOfCameras();
            for (int id = 0; id < count; id++) {
                CameraInfo info = new CameraInfo();
                Camera.getCameraInfo(id, info);
                if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                    Camera camera = Camera.open(id);
                    if (camera != null) {
                        logger.info("Camera opened: id=" + id);
                        return new CameraWrapper(id, camera, info);
                    } else {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("openCamera(): cannot open camera", e);
        }
        return null;
    }

    private void closeCamera(CameraWrapper camera) {
        if (camera != null && camera.instance != null) {
            camera.instance.stopPreview();
            camera.instance.release();
            camera.instance = null;
            logger.info("Camera closed: id=" + camera.id);
        }
    }

    public CameraWrapper getCamera() {
        return mCamera;
    }

    public void onEmergency() {
        if (mVehicleService != null) {
            mVehicleService.onEmergency();
        }
    }

    class CameraWrapper {
        int id;
        Camera instance;
        CameraInfo info;

        CameraWrapper(int id, Camera camera, CameraInfo info) {
            this.id = id;
            this.instance = camera;
            this.info = info;
        }
    }

    public interface BlackboxControlCallbacks {
        void onBlackboxPreview();

        void onBlackboxStarted();

        void onBlackboxStopped();

        void onBlackboxError(int errorCode);

        void onBlackboxEventBegin(BlackboxEventType eventType);

        void onBlackboxEventEnd(File videoFile, Date beginTime, Date endTime);
    }


    private List<String> mSupporetedFocusModes;
    private String mFocusMode;

    private void initFocusMode() {
        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            if (mVehicleService.getBlackboxEngine() instanceof MediaCodecEngine2) {
                mSupporetedFocusModes = new ArrayList<String>();
                List<Integer> modes = mVehicleService.getBlackboxEngine().getControlAfAvailableModes();
                for (Integer mode : modes) {
                    String s = CameraHelper.getFocusModeByControlAfMode(mode);
                    if (s != null) {
                        mSupporetedFocusModes.add(s);
                    }
                }
                return;
            }
        }

        mSupporetedFocusModes = getSupportedFocusModes();
    }

    public void onClickFocusMode() {
        String mode = getNextSupportedFocusMode();
        setFocusMode(mode);
        mSettingsStore.storeBlackboxFocusMode(mode);
    }

    private void setFocusMode() {
        if (mSupporetedFocusModes.contains(mSettingsStore.getBlackboxFocusMode())) {
            setFocusMode(mSettingsStore.getBlackboxFocusMode());
        } else {
            setFocusMode(CameraHelper.FOCUS_MODE_AUTO);
            mSettingsStore.storeBlackboxFocusMode(CameraHelper.FOCUS_MODE_AUTO);
        }
    }

    private void setFocusMode(final String mode) {
        Log.w("test", "setFocusMode@" + mode);
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mFocusMode = setCameraFocusMode(mode);

                int resId = R.drawable.btn_auto_focus_off;
                if (CameraHelper.FOCUS_MODE_INFINITY.equals(mFocusMode)) {
                    resId = R.drawable.btn_auto_focus_infinity;
                } else if (CameraHelper.FOCUS_MODE_CONTINUOUS_VIDEO.equals(mFocusMode)) {
                    resId = R.drawable.btn_auto_focus_1;
                } else if (CameraHelper.FOCUS_MODE_CONTINUOUS_PICTURE.equals(mFocusMode)) {
                    resId = R.drawable.btn_auto_focus_2;
                } else if (CameraHelper.FOCUS_MODE_FIXED.equals(mFocusMode)) {
                    resId = R.drawable.btn_auto_focus_3;
                } else if (CameraHelper.AF_MODE_EDOF.equals(mFocusMode)) {
                    resId = R.drawable.btn_af_edof;
                }

                ImageView iv = (ImageView)mDrivingService.getBlackboxPreview().findViewById(R.id.iv_preview_auto_focus);
                iv.setImageResource(resId);
            }
        });

    }

    private String getNextSupportedFocusMode() {
        return getNextSupportedFocusMode(mFocusMode);
    }

    private String getNextSupportedFocusMode(String mode) {
        Iterator<String> itr = mSupporetedFocusModes.iterator();
        while(itr.hasNext()) {
            if (itr.next().equals(mode) && itr.hasNext()) {
                return itr.next();
            }
        }
        return mSupporetedFocusModes.get(0);
    }

    private List<String> getSupportedFocusModes() {
        List<String> modes = new ArrayList<String>();
        List<String> list = mCamera.instance.getParameters().getSupportedFocusModes();
        if (list != null) {
            addFocusMode(CameraHelper.FOCUS_MODE_AUTO, modes);
            addFocusMode(CameraHelper.FOCUS_MODE_INFINITY, modes);
            addFocusMode(CameraHelper.FOCUS_MODE_CONTINUOUS_VIDEO, modes);
            addFocusMode(CameraHelper.FOCUS_MODE_CONTINUOUS_PICTURE, modes);
            addFocusMode(CameraHelper.FOCUS_MODE_FIXED, modes);
        }

        StringBuilder sb = new StringBuilder();
        for(String mode: modes) {
            sb.append(mode + "\t");
        }
        Log.i(TAG, "getSupportedFocusModes@" + sb.toString());

        return modes;
    }

    private boolean addFocusMode(String mode, List<String> modes) {
        List<String> list = mCamera.instance.getParameters().getSupportedFocusModes();
        if (modes != null && list.contains(mode)) {
            modes.add(mode);
            return true;
        }

        return false;
    }

    protected String setCameraFocusMode(String focusMode) {
        if (mVehicleService.getBlackboxEngine() instanceof MediaCodecEngine2) {
            try {
                mVehicleService.getBlackboxEngine().setCameraFocusMode(CameraHelper.getControlAfModeByFocusMode(focusMode));
                return focusMode;
            } catch (Exception e) {
            }

            return null;
        }

        try {
            Camera.Parameters params = mCamera.instance.getParameters();
            params.setFocusMode(focusMode);
            mCamera.instance.setParameters(params);
        } catch (RuntimeException e) {
            Logger.getLogger(TAG).error(e.getMessage() + "@setCameraFocusMode");
        }

        return focusMode;
    }

}

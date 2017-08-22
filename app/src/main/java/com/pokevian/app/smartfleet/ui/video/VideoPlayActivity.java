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

package com.pokevian.app.smartfleet.ui.video;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.BlackboxMetadata;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.lib.common.util.RecycleUtils;
import com.pokevian.lib.common.util.ReverseGeocoder;
import com.pokevian.lib.obd2.defs.Unit;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class VideoPlayActivity extends BaseDrivingOnActivity implements View.OnClickListener {

    static final String TAG = "video-play-activity";
    final Logger log = Logger.getLogger(TAG);

    public static enum ViewMode {
        FULLVIDEOSCREEN_MODE, FULLMAPSCREEN_MODE, DUALSCREEN_MODE
    }

    private static final int UPDATE_ADDRESS_DISTANCE = 1000; //m

    public static final String EXTRA_FILES = "files";
    public static final String EXTRA_PLAY_INDEX = "play_index";
    public static final String EXTRA_SHOW_ARCHIVE_MENU = "show_archive_menu";
    public static final String EXTRA_DELETED = "deleted";
    public static final String EXTRA_ARCHIVED = "archived";

    private static final String DEFAULT_SPEED_STR = "--";
    private static final int SYNC_INTERVAL = 500; // 500ms
    private static final int STATE_IDLE = 0;
    private static final int STATE_PLAYING = 1;
    private static final int STATE_PAUSED = 2;

    private static final int MSG_UPDATE_CURRENT_POS = 1;
    private static final int MSG_UPDATE_TIMEDATE = 2;
    private static final int MSG_UPDATE_SPEED = 3;
    private static final int MSG_UPDATE_ADDRESS = 4;

    private RelativeLayout mRootLayout;
    private PlayerVideoFragment mVideoFragment = null;
    private PlayerMapFragment mMapFragment = null;
    private PlayerInfoFragment mInfoFragment = null;

    private TextView mTimedateText;
    private TextView mSpeedText;
    private TextView mSpeedUnitText;
    private RelativeLayout mControllerLayout;
    private TextView mCurrentPosText;
    private TextView mDurationText;
    private SeekBar mSeekBar;
    private ImageView mToggleButton;
    private ImageView mPlayBtn;
    private ImageView mRewBtn;
    private ImageView mFfBtn;
    private ImageView mPrevBtn;
    private ImageView mNextBtn;

    private ViewMode mCurrViewMode = ViewMode.FULLVIDEOSCREEN_MODE;
    private Timer mSyncTimer = null;
    private int mState = STATE_IDLE;

    private LinkedHashMap<Long, BlackboxMetadata> mReadedMetadataMap;
    private Long[] mReadedMetadataMapKeySet;
    private ArrayList<File> mVideoFiles;
    private int mVideoFileIndex;
    private boolean mIsShowArchiveMenu;
    private File mCurrVideoFile;
    private File mCurrMetadataFile;
    private File mArchiveDir;

    private ReverseGeocoder mGeocoderThread = null;

    private int mLastVideoPos = 0;
    private BlackboxMetadata mLastMetadata;

    private Location mLastAddressLoc = null;

    private VideoCopyThread mVideoCopyThread;
    private VideoCopyProgressDialog mCopyProgressDialog;

    private Unit mSpeedUnit;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_video_play);

        //orientation error fix in the lower api level.
        if (Build.VERSION_CODES.GINGERBREAD <= Build.VERSION.SDK_INT) {
        /*if (SdkUtils.isGingerBreadSupported()) {*/
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        SettingsStore settingsStore = SettingsStore.getInstance();

        int storageIndex = settingsStore.getBlackboxStorageType().ordinal();
        File[] archiveRootDirs = StorageUtils.getExternalFilesDirs(this,
                settingsStore.getBlackboxArchiveDirName());
        mArchiveDir = archiveRootDirs[storageIndex];

        mSpeedUnit = settingsStore.getSpeedUnit();

        mSpeedText = (TextView) findViewById(R.id.speed);
        mSpeedUnitText = (TextView) findViewById(R.id.speed_unit);
        mSpeedUnitText.setText(mSpeedUnit.toString());

        mCurrentPosText = (TextView) findViewById(R.id.current_pos);
        mDurationText = (TextView) findViewById(R.id.duration);
        mTimedateText = (TextView) findViewById(R.id.timedate);

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mPlayBtn = (ImageView) findViewById(R.id.play_list);
        mPlayBtn.setOnClickListener(this);
        mRewBtn = (ImageView) findViewById(R.id.rew);
        mRewBtn.setOnClickListener(this);
        mFfBtn = (ImageView) findViewById(R.id.ff);
        mFfBtn.setOnClickListener(this);
        mPrevBtn = (ImageView) findViewById(R.id.prev);
        mPrevBtn.setOnClickListener(this);
        mNextBtn = (ImageView) findViewById(R.id.next);
        mNextBtn.setOnClickListener(this);
        mToggleButton = (ImageView) findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(this);

        mRootLayout = (RelativeLayout) findViewById(R.id.root);

        FragmentManager fm = getFragmentManager();
        mInfoFragment = (PlayerInfoFragment) fm.findFragmentById(R.id.info_fragment);
        mVideoFragment = (PlayerVideoFragment) fm.findFragmentById(R.id.video_fragment);
        mMapFragment = (PlayerMapFragment) fm.findFragmentById(R.id.map_fragment);

        mVideoFragment.setVideoErrorListener(mErrorListener);
        mVideoFragment.setVideoCompleteListener(mCompletionListener);
        mVideoFragment.setVideoPreparedListener(mPreparedListener);

        mControllerLayout = (RelativeLayout) findViewById(R.id.controller);
        ViewTreeObserver vto = mControllerLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressLint("NewApi")
            @SuppressWarnings("deprecation")
            public void onGlobalLayout() {
                if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
                /*if (SdkUtils.isJellyBeanSupported()) {*/
                    mControllerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mControllerLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                //mControllerHeight = mControllerLayout.getMeasuredHeight();
                changeLayout(ViewMode.FULLVIDEOSCREEN_MODE);

                play();
            }
        });

        handleIntent(getIntent());

        initActionBar();
    }

    @SuppressWarnings("unchecked")
    private void handleIntent(Intent intent) {
        mVideoFiles = (ArrayList<File>) intent.getSerializableExtra(EXTRA_FILES);
        mVideoFileIndex = intent.getIntExtra(EXTRA_PLAY_INDEX, 0);
        mIsShowArchiveMenu = intent.getBooleanExtra(EXTRA_SHOW_ARCHIVE_MENU, false);

        if (mVideoFiles == null || mVideoFiles.isEmpty()
                || mVideoFileIndex >= mVideoFiles.size() || mVideoFileIndex < 0) {
            log.error("Invalid video files or Invalid play index");
            finish();
            return;
        }

        mCurrVideoFile = mVideoFiles.get(mVideoFileIndex);
        updateMetadata();
    }

    @Override
    protected void onPause() {
        super.onPause();

        pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        resume();
    }

    @Override
    public void onBackPressed() {
        Intent i = getIntent();
        i.putExtra(EXTRA_PLAY_INDEX, mVideoFileIndex);
        setResult(RESULT_OK, i);

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopGeocoderThread();
        stopSyncTimer();

        // recycle bitmap drawables
        RecycleUtils.recycle(getWindow().getDecorView());
    }

    private void initActionBar() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
    }

    private void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_video_play, menu);

        if (!mIsShowArchiveMenu) {
            MenuItem item = menu.findItem(R.id.menu_archive);
            item.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.menu_archive) {
            onArchiveItemClicked();
            return true;
        } else if (itemId == R.id.menu_share) {
            onShareItemClicked();
            return true;
        } else if (itemId == R.id.menu_delete) {
            onDeleteItemClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onArchiveItemClicked() {
        final ArrayList<File> files = new ArrayList<File>(1);
        File videoFile = mVideoFiles.get(mVideoFileIndex);

        files.add(videoFile);

        DialogFragment fragment = CopyVideoDialogFragment.newInstance(mArchiveDir, files);
        fragment.show(getSupportFragmentManager(), CopyVideoDialogFragment.TAG);
    }

    public static class CopyVideoDialogFragment extends DialogFragment {

        public static final String TAG = "CopyVideoDialogFragment";

        private File mTarget;
        private ArrayList<File> mFiles;

        public static CopyVideoDialogFragment newInstance(File target, ArrayList<File> files) {
            CopyVideoDialogFragment fragment = new CopyVideoDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("target", target);
            args.putSerializable("files", files);
            fragment.setArguments(args);
            return fragment;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTarget = (File) args.getSerializable("target");
                mFiles = (ArrayList<File>) args.getSerializable("files");
            } else {
                mTarget = (File) savedInstanceState.getSerializable("target");
                mFiles = (ArrayList<File>) savedInstanceState.getSerializable("files");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("files", mFiles);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.video_play_title_copy_video_to_archive)
                    .setMessage(R.string.video_play_msg_copy_video_to_archive)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((VideoPlayActivity) getActivity()).doArchive(mFiles, mTarget);
                        }
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .create();
        }
    }

    private void doArchive(ArrayList<File> files, File archiveDir) {
        mVideoCopyThread = new VideoCopyThread(files, archiveDir, VideoPlayActivity.this,
                new VideoCopyThread.Callback() {
                    @Override
                    public void onPreExecute(final int maxValue) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                showCopyProgressDialog(maxValue);
                            }
                        });
                    }

                    @Override
                    public void onProgress(final int progress) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                updateCopyProgressDialog(progress);
                            }
                        });
                    }

                    @Override
                    public void onPostExecute() {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                hideCopyProgressDialog();
                                Intent i = getIntent();
                                i.putExtra(EXTRA_ARCHIVED, true);
                                setIntent(i);
                            }
                        });
                    }
                });
        mVideoCopyThread.start();
    }

    private void showCopyProgressDialog(int max) {
        mCopyProgressDialog = new VideoCopyProgressDialog(this);
        mCopyProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                resume();
                mVideoCopyThread.interrupt();
            }
        });
        mCopyProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                resume();
            }
        });
        mCopyProgressDialog.setMax(max);
        mCopyProgressDialog.show();

        pause();
    }

    private void updateCopyProgressDialog(int value) {
        if (mCopyProgressDialog != null) {
            mCopyProgressDialog.setProgress(value);
        }
    }

    private void hideCopyProgressDialog() {
        if (mCopyProgressDialog != null) {
            try {
                mCopyProgressDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    private void onShareItemClicked() {
        ArrayList<File> files = new ArrayList<File>(1);
        File videoFile = mVideoFiles.get(mVideoFileIndex);
        files.add(videoFile);

        ArrayList<Uri> uris = new ArrayList<Uri>();
        ContentResolver resolver = getContentResolver();
        for (File file : files) {
            Uri uri = MediaStoreUtils.retrieveVideoContentUri(resolver, file);
            if (uri == null) {
                log.info("add video file to media store");
                uri = MediaStoreUtils.insertVideo(resolver, file, "video/mp4");
            }
            if (uri != null) {
                uris.add(uri);

                File metaFile = FileUtils.getMetadataFileFromVideoFile(file);
                if (metaFile != null && metaFile.exists()) {
                    uris.add(Uri.fromFile(metaFile));
                }
            }
        }

        if (uris.size() > 0) {
            Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
            i.setType("video/*");
            i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            try {
                startActivity(Intent.createChooser(i, getString(R.string.video_play_title_share_video)));
            } catch (Exception e) {
                Toast.makeText(this, "No way to share!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onDeleteItemClicked() {
        final ArrayList<File> files = new ArrayList<File>(3);
        File videoFile = mVideoFiles.get(mVideoFileIndex);
        files.add(videoFile);
        File metaFile = FileUtils.getMetadataFileFromVideoFile(videoFile);
        files.add(metaFile);
        /*File oldMetaFile = FileUtils.getOldMetadataFileFromVideoFile(videoFile);
		files.add(oldMetaFile);*/

        DialogFragment fragment = DeleteVideoDialogFragment.newInstance(files);
        fragment.show(getSupportFragmentManager(), DeleteVideoDialogFragment.TAG);
    }

    public static class DeleteVideoDialogFragment extends DialogFragment {

        public static final String TAG = "DeleteVideoDialogFragment";

        private ArrayList<File> mFiles;

        public static DeleteVideoDialogFragment newInstance(ArrayList<File> files) {
            DeleteVideoDialogFragment fragment = new DeleteVideoDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("files", files);
            fragment.setArguments(args);
            return fragment;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mFiles = (ArrayList<File>) args.getSerializable("files");
            } else {
                mFiles = (ArrayList<File>) savedInstanceState.getSerializable("files");
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putSerializable("files", mFiles);
            super.onSaveInstanceState(outState);
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.video_play_title_delete_video)
                    .setMessage(R.string.video_play_msg_delete_video)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((VideoPlayActivity) getActivity()).doDelete(mFiles);
                        }
                    })
                    .setNegativeButton(R.string.btn_no, null)
                    .create();
        }
    }

    private void doDelete(ArrayList<File> files) {
        for (File file : files) {
            file.delete();
        }

        Intent i = getIntent();
        i.putExtra(EXTRA_DELETED, mVideoFileIndex);
        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.toggle) {
            changeLayout(getNextViewMode(mCurrViewMode));
        } else if (id == R.id.play_list) {
            play();
        } else if (id == R.id.rew) {
            rewind();
        } else if (id == R.id.ff) {
            fastforward();
        } else if (id == R.id.prev) {
            previous();
        } else if (id == R.id.next) {
            next();
        }
    }

    private boolean updateMetadata() {
        mCurrMetadataFile = FileUtils.getMetadataFileFromVideoFile(mCurrVideoFile);
        if (mCurrMetadataFile != null && mCurrMetadataFile.exists()) {
            loadMetadata(mCurrMetadataFile);
            return true;
        } else {
            return false;
        }
    }

    private ViewMode getNextViewMode(ViewMode viewMode) {
        if (viewMode == ViewMode.DUALSCREEN_MODE) {
            return ViewMode.FULLVIDEOSCREEN_MODE;
        } else if (viewMode == ViewMode.FULLVIDEOSCREEN_MODE) {
            return ViewMode.FULLMAPSCREEN_MODE;
        } else {
            return ViewMode.DUALSCREEN_MODE;
        }
    }

    private void changeLayout(ViewMode viewMode) {
        int infoHeight = mInfoFragment.getView().getHeight();

        if (viewMode == ViewMode.DUALSCREEN_MODE) {
            setDualScreenMode(infoHeight);
        } else if (viewMode == ViewMode.FULLVIDEOSCREEN_MODE) {
            setFullVideoScreenMode();
        } else if (viewMode == ViewMode.FULLMAPSCREEN_MODE) {
            setFullMapScreenMode(infoHeight);
        }
        mCurrViewMode = viewMode;

        mRootLayout.invalidate();
        mToggleButton.bringToFront();
        mRootLayout.invalidate();
        updatePath();
    }

    private void setDualScreenMode(int infoHeight) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        int actionBarHeight = getSupportActionBar().getHeight();

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mInfoFragment.getView().setLayoutParams(infoParams);

        RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        videoParams.addRule(RelativeLayout.BELOW, mInfoFragment.getId());
        videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        int videoHeight = videoParams.height = screenHeight - (actionBarHeight + infoHeight);
        int videoWidth = videoParams.width = getAspectRatioVideoWidth(videoHeight);
        mVideoFragment.getView().setLayoutParams(videoParams);

        RelativeLayout.LayoutParams rmapParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rmapParams.addRule(RelativeLayout.BELOW, mInfoFragment.getId());
        rmapParams.addRule(RelativeLayout.RIGHT_OF, mVideoFragment.getId());

        rmapParams.height = videoHeight;
        rmapParams.width = screenWidth - videoWidth;
        mMapFragment.getView().setLayoutParams(rmapParams);

        mMapFragment.addTopMarginOfMapControlView(0);
        mMapFragment.getView().setVisibility(View.VISIBLE);
    }

    private void setFullVideoScreenMode() {
        RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        videoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        videoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mVideoFragment.getView().setLayoutParams(videoParams);

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mInfoFragment.getView().setLayoutParams(infoParams);
        mInfoFragment.getView().bringToFront();

        RelativeLayout.LayoutParams rmapParams = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rmapParams.addRule(RelativeLayout.RIGHT_OF, mVideoFragment.getId());
        mMapFragment.getView().setLayoutParams(rmapParams);
        mMapFragment.getView().setVisibility(View.INVISIBLE);
    }

    private void setFullMapScreenMode(int infoHeight) {
        RelativeLayout.LayoutParams rmapParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        rmapParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        rmapParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mMapFragment.getView().setLayoutParams(rmapParams);
        mMapFragment.getView().setVisibility(View.VISIBLE);
        mMapFragment.addTopMarginOfMapControlView(infoHeight);

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mInfoFragment.getView().setLayoutParams(infoParams);
        mInfoFragment.getView().bringToFront();

        RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        videoParams.addRule(RelativeLayout.RIGHT_OF, mMapFragment.getId());
        mVideoFragment.getView().setLayoutParams(videoParams);
    }

    private int getAspectRatioVideoWidth(int height) {
        int width = (int) (height * 1.333f);
        return width;
    }

    private void loadMetadata(File metaFile) {
        log.debug("loadMetadata(): metaFile=" + metaFile);
        InputStream input = null;
        try {
            input = new FileInputStream(metaFile);
            LinkedHashMap<Long, BlackboxMetadata> readedMap = MetadataReader.read(input);
            if (readedMap != null) {
                mReadedMetadataMap = readedMap;

                Set<Long> keySet = mReadedMetadataMap.keySet();
                mReadedMetadataMapKeySet = keySet.toArray(new Long[0]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private BlackboxMetadata searchLastMetadata() {
        BlackboxMetadata meta = null;
        if (mReadedMetadataMap == null) {
            return null;
        } else {
            long lastKey = mReadedMetadataMapKeySet[mReadedMetadataMapKeySet.length - 1];
            meta = mReadedMetadataMap.get(lastKey);
        }
        return meta;
    }

    private BlackboxMetadata searchMetadata(int pos) {
        if (mReadedMetadataMap == null) {
            return null;
        }
        long currDuration = -1;
        long prevDuration = -1;
        for (int i = 0; i < mReadedMetadataMapKeySet.length; i++) {
            prevDuration = currDuration;
            currDuration = mReadedMetadataMapKeySet[i];
            if (pos < currDuration) {
                if (prevDuration == -1) {
                    return mReadedMetadataMap.get(currDuration);
                } else {
                    return mReadedMetadataMap.get(prevDuration);
                }
            }
        }
        if (mReadedMetadataMap.size() > 0) {
            return mReadedMetadataMap.get(currDuration);
        }

        return null;
    }

    private void play() {
        switch (mState) {
            case STATE_IDLE:
                onPlay();
                break;
            case STATE_PLAYING:
                pause();
                break;
            case STATE_PAUSED:
                resume();
                break;
            default:
                break;
        }
    }

    private void onPlay() {
        BlackboxMetadata meta = null;
        if (mLastMetadata != null) {
            meta = mLastMetadata;
        } else {
            meta = searchMetadata(0);
            mLastVideoPos = 0;
            mLastMetadata = meta;
        }

        setActionBarTitle(mVideoFiles.get(mVideoFileIndex).getName());
        updateTimedate(meta);
        updateCaption(mLastVideoPos, meta);
        updateLocation(meta);

        if (mVideoFragment != null) {
            mVideoFragment.setVideoPath(mCurrVideoFile.getAbsolutePath());
            mVideoFragment.seekToVideo(mLastVideoPos);
            mVideoFragment.startVideo();
        }
        startSyncTimer();

        mPlayBtn.setBackgroundResource(R.drawable.btn_pause);

        mState = STATE_PLAYING;
    }

    private void stop() {
        switch (mState) {
            case STATE_PLAYING:
			/* fall through */
            case STATE_PAUSED:
                stopSyncTimer();
                mVideoFragment.stopVideo();
                mPlayBtn.setBackgroundResource(R.drawable.btn_play);
                mCurrentPosText.setText("00:00");
                mSeekBar.setProgress(0);
                mState = STATE_IDLE;
                break;
            default:
                break;
        }
    }

    private void resume() {
        switch (mState) {
            case STATE_PAUSED:
                updateCaption(mLastVideoPos, mLastMetadata);
                updateLocation(mLastMetadata);
                startSyncTimer();
                if (mVideoFragment != null) {
                    mVideoFragment.setVideoPath(mCurrVideoFile.getAbsolutePath());
                    mVideoFragment.seekToVideo(mLastVideoPos);
                    mVideoFragment.startVideo(); // resume() does not working
                }
                mPlayBtn.setBackgroundResource(R.drawable.btn_pause);
                mState = STATE_PLAYING;
                break;
            default:
                break;
        }
    }


    private void pause() {
        switch (mState) {
            case STATE_PLAYING:
                stopSyncTimer();
                mVideoFragment.pauseVideo();
                mPlayBtn.setBackgroundResource(R.drawable.btn_play);
                mState = STATE_PAUSED;
                break;
            default:
                break;
        }
    }


    private void rewind() {
        int nextPos = mVideoFragment.getCurrentPosition() - 10000;
        if (nextPos < 0) {
            nextPos = 0;
        }

        switch (mState) {
            case STATE_PLAYING:
                stopSyncTimer();
                mVideoFragment.seekToVideo(nextPos);
                startSyncTimer();
                break;
            case STATE_PAUSED:
                mLastVideoPos = nextPos;
                resume();
                break;
            default:
                break;
        }
    }

    private void fastforward() {
        int nextPos = mVideoFragment.getCurrentPosition() + 10000;
        if (nextPos > mVideoFragment.getVideoDuration()) {
            nextPos = mVideoFragment.getVideoDuration();
        }

        switch (mState) {
            case STATE_PLAYING:
                stopSyncTimer();
                mVideoFragment.seekToVideo(nextPos);
                startSyncTimer();
                break;
            case STATE_PAUSED:
                mLastVideoPos = nextPos;
                resume();
                break;
            default:
                break;
        }
    }

    private void previous() {
        if (mVideoFileIndex < mVideoFiles.size() - 1) {
            mVideoFileIndex++;
            stop();

            mCurrVideoFile = mVideoFiles.get(mVideoFileIndex);
            if (updateMetadata()) {
                mLastAddressLoc = null;
                updatePath();
            }

            play();
        }
    }

    private void next() {
        if (mVideoFileIndex > 0) {
            mVideoFileIndex--;
            stop();

            mCurrVideoFile = mVideoFiles.get(mVideoFileIndex);
            if (updateMetadata()) {
                mLastAddressLoc = null;
                updatePath();
            }

            play();
        }
    }

    private Handler mUiHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            //LOGGER.debug("[uiHandler] msg={}", msg.what);
            switch (msg.what) {
                case MSG_UPDATE_CURRENT_POS:
                    int seconds = msg.arg1 / 1000;
                    mCurrentPosText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                    break;

                case MSG_UPDATE_TIMEDATE:
                    String timedate = (String) msg.obj;
                    if (mTimedateText != null) {
                        mTimedateText.setText(timedate);
                    }
                    break;

                case MSG_UPDATE_SPEED:
                    int speed = msg.arg1;
                    if (speed >= 0) {
                        mSpeedText.setText(String.valueOf(speed));
                    } else {
                        mSpeedText.setText(DEFAULT_SPEED_STR);
                    }
                    break;
                case MSG_UPDATE_ADDRESS:
                    String address = (String) msg.obj;
                    if (address != null && address.length() > 0) {
                        setActionBarTitle(address);
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void startSyncTimer() {
        if (mSyncTimer != null) {
            mSyncTimer.cancel();
            mSyncTimer.purge();
        }
        mSyncTimer = new Timer();
        mSyncTimer.scheduleAtFixedRate(new SyncTask(), 0, SYNC_INTERVAL);
    }

    private void stopSyncTimer() {
        if (mSyncTimer != null) {
            mSyncTimer.cancel();
            mSyncTimer.purge();
            mSyncTimer = null;
        }
    }

    class SyncTask extends TimerTask {
        boolean isCanceled = false;
        long startTime = 0;

        public SyncTask() {
            startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            if (!isCanceled) {
                if (mState != STATE_PLAYING) {
                    return;
                }

                int pos = 0;
                try {
                    pos = mVideoFragment.getCurrentPosition();
                } catch (IllegalStateException e) {
                    return;
                }
                mLastVideoPos = pos;
                mSeekBar.setProgress(pos);
                updateCurrentPos(pos);

                mLastMetadata = searchMetadata(pos);
                updateTimedate(mLastMetadata);
                updateCaption(pos, mLastMetadata);
                updateLocation(mLastMetadata);
                updateSpeed(mLastMetadata);
            }
        }

        @Override
        public boolean cancel() {
            isCanceled = true;
            return super.cancel();
        }

    }

    private void updateLocation(final BlackboxMetadata meta) {
        if (meta != null && meta.locationData.isValid && mMapFragment != null) {
            mMapFragment.updateLocation(meta);

            if (mLastAddressLoc == null || isUpdateAddress(meta.locationData.latitude, meta.locationData.longitude)) {
                if (mLastAddressLoc == null) {
                    mLastAddressLoc = new Location("");
                }
                mLastAddressLoc.setLatitude(meta.locationData.latitude);
                mLastAddressLoc.setLongitude(meta.locationData.longitude);

                startGeocoderThread(meta.locationData.latitude, meta.locationData.longitude);
            }
        }
    }

    private boolean isUpdateAddress(double lat, double lng) {
        boolean result = false;
        Location loc = new Location("");
        loc.setLatitude(lat);
        loc.setLongitude(lng);
        float distance = mLastAddressLoc.distanceTo(loc);
        if (distance >= UPDATE_ADDRESS_DISTANCE) {
            result = true;
        }
        return result;
    }

    private void updatePath() {
        if (mReadedMetadataMap != null && mMapFragment != null) {
            mMapFragment.updatePath(mReadedMetadataMap);
        }
    }

    private void updateCurrentPos(int pos) {
        Message msg = mUiHandler.obtainMessage(MSG_UPDATE_CURRENT_POS);
        msg.arg1 = pos;
        mUiHandler.sendMessage(msg);
    }

    private void updateTimedate(BlackboxMetadata meta) {
        Message msg = mUiHandler.obtainMessage(MSG_UPDATE_TIMEDATE);
        if (meta != null) {
            long time = meta.timestamp;
            msg.obj = TimeStringUtils.toString(TimeStringUtils.Type.LOCAL_DATE_TIME, time);
        } else {
            msg.obj = "";
        }
        mUiHandler.sendMessage(msg);
    }

    private void updateCaption(int pos, BlackboxMetadata meta) {
        if (meta != null && mInfoFragment != null) {
            mInfoFragment.updateCaption(pos, meta);
        }
    }

    private void updateSpeed(BlackboxMetadata meta) {
        Message msg = mUiHandler.obtainMessage(MSG_UPDATE_SPEED);
        if (meta != null) {
            if (meta.speedData.isValid) {
                float speed = meta.speedData.currentSpeed;
                msg = mUiHandler.obtainMessage(MSG_UPDATE_SPEED);
                msg.arg1 = (int) speed;
            } else {
                msg.arg1 = -1;
            }
        } else {
            msg.arg1 = -1;
        }
        mUiHandler.sendMessage(msg);
    }

    private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {

        private boolean tracking = false;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mLastVideoPos = progress;
            if (tracking) {
                BlackboxMetadata meta = searchMetadata(progress);
                updateCurrentPos(progress);
                updateTimedate(meta);
                updateCaption(progress, meta);
                updateLocation(meta);
                updateSpeed(meta);
                mVideoFragment.seekToVideo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            tracking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            tracking = false;
        }

    };

    private OnPreparedListener mPreparedListener = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mp) {
            int duration = mp.getDuration();
            mSeekBar.setMax(duration);
            int seconds = duration / 1000;
            mDurationText.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }

    };

    private OnCompletionListener mCompletionListener = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            stop();

            int pos = 0;
            try {
                pos = mVideoFragment.getCurrentPosition();
            } catch (IllegalStateException e) {
                return;
            }

            pos = mSeekBar.getMax();
            mSeekBar.setProgress(pos);
            updateCurrentPos(pos);
            BlackboxMetadata meta = searchLastMetadata();
            if (meta != null) {
                updateLocation(meta);
            }

            mLastVideoPos = 0;
            mLastMetadata = null;

            mState = STATE_IDLE;
        }

    };

    private OnErrorListener mErrorListener = new OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
    };

    private void startGeocoderThread(double lat, double lng) {
        stopGeocoderThread();

        mGeocoderThread = new ReverseGeocoder(this, lat, lng, null, mAddressCallback);
        mGeocoderThread.start();
    }

    private void stopGeocoderThread() {
        if (mGeocoderThread != null && mGeocoderThread.isAlive()) {
            mGeocoderThread.interrupt();
        }
    }

    private ReverseGeocoder.Callback mAddressCallback = new ReverseGeocoder.Callback() {

        @Override
        public void onGeocoded(String address, Object tag) {
            if (address != null) {
                Message msg = mUiHandler.obtainMessage(MSG_UPDATE_ADDRESS);
                msg.obj = address;
                mUiHandler.sendMessage(msg);
            }
        }

    };
}

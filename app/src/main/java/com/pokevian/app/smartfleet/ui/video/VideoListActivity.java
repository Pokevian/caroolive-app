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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.TabHost;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.driving.DrivingActivity;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.lib.common.util.RecycleUtils;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;

import org.apache.log4j.Logger;

import java.io.File;


public class VideoListActivity extends BaseDrivingOnActivity implements AlertDialogCallbacks {

    public static final String TAG_NORMAL = "normal_list";
    public static final String TAG_EVENT = "event_list";
    public static final String TAG_ARCHIVE = "archive_list";

    private File mNormalDir;
    private File mEventDir;
    private File mArchiveDir;

    private FragmentTabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        showGooglePlayServiceUpdateDialogIfNeeded();

        SettingsStore settingsStore = SettingsStore.getInstance();

        int storageIndex = settingsStore.getBlackboxStorageType().ordinal();
        File[] normalRootDirs = StorageUtils.getExternalFilesDirs(this, settingsStore.getBlackboxNormalDirName());
        File[] eventRootDirs = StorageUtils.getExternalFilesDirs(this, settingsStore.getBlackboxEventDirName());
        File[] archiveRootDirs = StorageUtils.getExternalFilesDirs(this, settingsStore.getBlackboxArchiveDirName());

        mNormalDir = normalRootDirs[storageIndex];
        mEventDir = eventRootDirs[storageIndex];
        mArchiveDir = archiveRootDirs[storageIndex];

        handleIntent(getIntent());

        initActionBar();
    }

    private boolean isShowingVideo = false;
    protected void setShowVideo(boolean isShow) {
        Logger.getLogger("VideoList").trace("setShowVideo# isShow=" + isShow);
        isShowingVideo = isShow;
    }

    @Override
    protected void finishIfNeeded() {
        if (!isShowingVideo) {
            finish();
        }
    }

    private void showGooglePlayServiceUpdateDialogIfNeeded() {
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS != errorCode) {
            if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
                GooglePlayServicesUtil.getErrorDialog(errorCode, VideoListActivity.this, 777, new OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        showCancelError();
                    }

                }).show();
            }
        }
    }

    private void showCancelError() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment fragment = (DialogFragment) fm.findFragmentByTag("cancel-google-play-service-update-dialog");
        if (fragment == null) {
            fragment = AlertDialogFragment.newInstance(
                    getString(R.string.video_play_title_notice),
                    getString(R.string.video_play_msg_google_play_service_update),
                    null, getString(R.string.btn_ok));
            fm.beginTransaction().add(fragment, "cancel-google-play-service-update-dialog").commitAllowingStateLoss();
        }
    }

    private void handleIntent(Intent intent) {
    }

    private void initActionBar() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.main_video_record);

        mTabHost = (FragmentTabHost) findViewById(R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        TabHost.TabSpec tab1 = mTabHost.newTabSpec(TAG_NORMAL);
        tab1.setIndicator(getString(R.string.video_play_title_normal_video_list));
        Bundle args = new Bundle();
        args.putSerializable(VideoListFragment.ARG_ROOT_DIR, mNormalDir);
        mTabHost.addTab(tab1, VideoListFragment.class, args);

        TabHost.TabSpec tab2 = mTabHost.newTabSpec(TAG_EVENT);
        tab2.setIndicator(getString(R.string.video_play_title_event_video_list));
        args = new Bundle();
        args.putSerializable(VideoListFragment.ARG_ROOT_DIR, mEventDir);
        mTabHost.addTab(tab2, VideoListFragment.class, args);

        TabHost.TabSpec tab3 = mTabHost.newTabSpec(TAG_ARCHIVE);
        tab3.setIndicator(getString(R.string.video_play_title_archive_video_list));
        args = new Bundle();
        args.putSerializable(VideoListFragment.ARG_ROOT_DIR, mArchiveDir);
        mTabHost.addTab(tab3, VideoListFragment.class, args);
    }

    @SuppressWarnings("unused")
    private void updateActionBarTitleEnabled() {
        ActionBar bar = getSupportActionBar();
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            bar.setDisplayShowTitleEnabled(false);
        } else {
            bar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Logger.getLogger("VideoList").trace("onActivityResult# requestCode=" + requestCode + " resultCode=" + resultCode + " data=" + data);
        setShowVideo(false);
        if (resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra(DrivingActivity.EXTRA_REQUEST_EXIT, false)) {
                // Need to exit
                setResult(resultCode, data);
                finish();
            }
        }

        if (777 == requestCode && Activity.RESULT_CANCELED == resultCode) {
            showCancelError();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // recycle bitmap drawables
        RecycleUtils.recycle(getWindow().getDecorView());
    }

    @Override
    public void onBackPressed() {
        if (mTabHost != null) {
            String tag = mTabHost.getCurrentTabTag();
            FragmentManager fm = getSupportFragmentManager();
            VideoListFragment frag = (VideoListFragment) fm.findFragmentByTag(tag);
            if (frag != null) {
                if (frag.onBackPressed()) {
                    return;
                }
            }
        }


        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //updateActionBarTitleEnabled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onDialogButtonClick(DialogFragment fragment, int which) {
//        String tag = fragment.getTag();
//        if ("cancel-google-play-service-update-dialog".equals(tag)) {
//        }
//    }


}

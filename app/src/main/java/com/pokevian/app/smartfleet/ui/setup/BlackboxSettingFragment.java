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

package com.pokevian.app.smartfleet.ui.setup;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.StorageType;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.setup.BlackboxInitDialogFragment.BlackboxInitiCallbacks;
import com.pokevian.app.smartfleet.util.StorageUtils;
import com.pokevian.lib.blackbox.BlackboxConst.BlackboxEngineType;
import com.pokevian.lib.blackbox.BlackboxProfileInstance;
import com.pokevian.lib.blackbox.BlackboxProfileInstance.VideoResolution;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BlackboxSettingFragment extends PreferenceFragment
        implements OnPreferenceChangeListener, BlackboxInitiCallbacks {

    public static final String TAG = "BlackboxSettingFragment";
    final Logger logger = Logger.getLogger(TAG);

    private SettingsStore mSettingStore;

    public static BlackboxSettingFragment newInstance() {
        BlackboxSettingFragment fragment = new BlackboxSettingFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_blackbox_setting);

        mSettingStore = SettingsStore.getInstance();

        registerMediaMountReceiver();
    }

    @Override
    public void onDestroy() {
        unregisterMediaMountReceiver();

        super.onDestroy();
    }

    private void registerMediaMountReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        getActivity().registerReceiver(mMediaMountReceiver, filter);
    }

    private void unregisterMediaMountReceiver() {
        getActivity().unregisterReceiver(mMediaMountReceiver);
    }

    private final BroadcastReceiver mMediaMountReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            String message = null;
            if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                message = getString(R.string.blackbox_setting_media_mounted);
            } else if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
                message = getString(R.string.blackbox_setting_media_unmounted);
            }

            final ProgressDialog dialog = ProgressDialog.show(getActivity(), null, message);
            dialog.setCancelable(false);

            getListView().postDelayed(new Runnable() {
                public void run() {
                    ListPreference storageType = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_STORAGE_TYPE);
                    setStorageTypeEntries(storageType);
                    storageType.setSummary(storageType.getEntry());

                    ListPreference maxStorageSize = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_MAX_STORAGE_SIZE);
                    ListPreference videoResolution = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_VIDEO_RESOLLUTION);
                    setMaxStorageSizeEntries(maxStorageSize, storageType.getValue(), videoResolution.getValue());
                    maxStorageSize.setSummary(maxStorageSize.getEntry());

                    dialog.dismiss();
                }
            }, 5000);
        }
    };

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CheckBoxPreference enabled = (CheckBoxPreference) findPreference(SettingsStore.PREF_BLACKBOX_ENABLED);
        enabled.setOnPreferenceChangeListener(this);

        if (enabled.isChecked()) {
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

        ListPreference listPreference = (ListPreference)findPreference(SettingsStore.PREF_BLACKBOX_ENGINE_TYPE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            getPreferenceScreen().removePreference(listPreference);
        } else {
            listPreference.setSummary(listPreference.getEntry());
            listPreference.setOnPreferenceChangeListener(this);
        }

        ListPreference videoResolution = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_VIDEO_RESOLLUTION);
        setVideoResolutionEntries(videoResolution);
        videoResolution.setSummary(videoResolution.getEntry());
        videoResolution.setOnPreferenceChangeListener(this);

        ListPreference videoQuality = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_VIDEO_QUALITY);
        videoQuality.setSummary(videoQuality.getEntry());
        videoQuality.setOnPreferenceChangeListener(this);

        ListPreference videoDuration = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_NORMAL_VIDEO_DURATION);
        videoDuration.setSummary(videoDuration.getEntry());
        videoDuration.setOnPreferenceChangeListener(this);

        ListPreference storageType = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_STORAGE_TYPE);
        setStorageTypeEntries(storageType);
        storageType.setSummary(storageType.getEntry());
        storageType.setOnPreferenceChangeListener(this);

        ListPreference maxStorageSize = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_MAX_STORAGE_SIZE);
        setMaxStorageSizeEntries(maxStorageSize, storageType.getValue(), videoResolution.getValue());
        maxStorageSize.setSummary(maxStorageSize.getEntry());
        maxStorageSize.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (SettingsStore.PREF_BLACKBOX_ENABLED.equals(key)) {
            // Initialize blackbox
            Boolean enabled = (Boolean) newValue;
            if (enabled) {
                FragmentManager fm = getChildFragmentManager();
                Fragment fragment = BlackboxInitDialogFragment.newInstance();
                fm.beginTransaction().add(fragment, BlackboxInitDialogFragment.TAG)
                        .commit();
            }
        } else if (SettingsStore.PREF_BLACKBOX_VIDEO_RESOLLUTION.equals(key)) {
            ListPreference videoResolution = (ListPreference) preference;
            int index = videoResolution.findIndexOfValue((String) newValue);
            videoResolution.setSummary(videoResolution.getEntries()[index]);

            // Check dependency of 'max storage size'
            ListPreference maxStorageSize = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_MAX_STORAGE_SIZE);
            ListPreference storageType = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_STORAGE_TYPE);
            setMaxStorageSizeEntries(maxStorageSize, storageType.getValue(), (String) newValue);
            maxStorageSize.setSummary(maxStorageSize.getEntry());
        } else if (SettingsStore.PREF_BLACKBOX_VIDEO_QUALITY.equals(key)) {
            ListPreference videoQuality = (ListPreference) preference;
            int index = videoQuality.findIndexOfValue((String) newValue);
            videoQuality.setSummary(videoQuality.getEntries()[index]);
        } else if (SettingsStore.PREF_BLACKBOX_NORMAL_VIDEO_DURATION.equals(key)) {
            ListPreference videoDuration = (ListPreference) preference;
            int index = videoDuration.findIndexOfValue((String) newValue);
            videoDuration.setSummary(videoDuration.getEntries()[index]);
        } else if (SettingsStore.PREF_BLACKBOX_STORAGE_TYPE.equals(key)) {
            ListPreference storageType = (ListPreference) preference;
            int index = storageType.findIndexOfValue((String) newValue);
            storageType.setSummary(storageType.getEntries()[index]);

            // Check dependency of 'max storage size'
            ListPreference maxStorageSize = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_MAX_STORAGE_SIZE);
            ListPreference videoResolution = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_VIDEO_RESOLLUTION);
            setMaxStorageSizeEntries(maxStorageSize, (String) newValue, videoResolution.getValue());
            maxStorageSize.setSummary(maxStorageSize.getEntry());
        } else if (SettingsStore.PREF_BLACKBOX_MAX_STORAGE_SIZE.equals(key)) {
            ListPreference maxStorageSize = (ListPreference) preference;
            int index = maxStorageSize.findIndexOfValue((String) newValue);
            maxStorageSize.setSummary(maxStorageSize.getEntries()[index]);
        } else if (SettingsStore.PREF_BLACKBOX_ENGINE_TYPE.equals(key)) {
            ListPreference lp = (ListPreference)preference;
            lp.setValue((String)newValue);
            lp.setSummary(lp.getEntry());
        }
        return true;
    }

    private void setVideoResolutionEntries(ListPreference videoResolution) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        CharSequence defaultValue = null;

        BlackboxProfileInstance profile = BlackboxProfileInstance.getInstance(getActivity());
        BlackboxEngineType engineType = mSettingStore.getBlackboxEngineType();
        List<VideoResolution> resolutions = profile.getSupportedVideoSize(engineType);
        int displayWidth = getDisplayWidth();
        for (int i = 0; i < resolutions.size(); i++) {
            VideoResolution resolution = resolutions.get(i);

            if (resolution.isFullHD() && (displayWidth < 960)) {
                logger.warn("Full HD supported but display is too small");
                continue;
            }

            String entry = String.format(Locale.getDefault(), "%dx%d%s",
                    resolution.width, resolution.height,
                    resolution.isValid ? "" : "-" + getString(R.string.blackbox_setting_not_recommanded));
            String value = resolution.flatten();

            entries.add(entry);
            values.add(value);

            if (defaultValue == null && resolution.isValid) {
                defaultValue = value;
            }
        }
        if (defaultValue == null) {
            defaultValue = values.get(0);
        }

        // Check value
        String value = videoResolution.getValue();
        if (values.size() > 0 && !values.contains(value)) {
            value = values.get(0).toString();
        }

        if (entries.size() > 0) {
            videoResolution.setEntries(entries.toArray(new CharSequence[entries.size()]));
            videoResolution.setEntryValues(values.toArray(new CharSequence[values.size()]));
            videoResolution.setDefaultValue(defaultValue);
            videoResolution.setValue(value);
        }
    }

    private int getDisplayWidth() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;
    }

    private void setStorageTypeEntries(ListPreference storageType) {
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        CharSequence defaultValue = null;

        File[] dirs = StorageUtils.getExternalFilesDirs(getActivity(), null);
        logger.info("getExternalFilesDirs(): files dirs=" + Arrays.asList(dirs));

        if (dirs.length >= 1) {
            entries.add(getString(R.string.storage_internal));
            values.add(StorageType.INTERNAL.name());
            defaultValue = StorageType.INTERNAL.name();
        }
        if (dirs.length >= 2) {
            entries.add(getString(R.string.storage_external));
            values.add(StorageType.EXTERNAL.name());
        }

        // Check value
        String value = storageType.getValue();
        if (values.size() > 0 && !values.contains(value)) {
            value = values.get(0).toString();
        }

        if (entries.size() > 0) {
            storageType.setEntries(entries.toArray(new CharSequence[entries.size()]));
            storageType.setEntryValues(values.toArray(new CharSequence[values.size()]));
            storageType.setDefaultValue(defaultValue);
            storageType.setValue(value);
        }
    }

    private void setMaxStorageSizeEntries(ListPreference maxStorageSize, String storageTypeValue,
                                          String videoResolutionValue) {
        StorageType storageType = StorageType.valueOf(storageTypeValue);
        VideoResolution videoResolution = new VideoResolution();
        videoResolution.unflatten(videoResolutionValue);

        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        CharSequence defaultValue;

        // 'Full' option
        entries.add(getString(R.string.max_storage_size_full));
        values.add(String.valueOf(StorageUtils.TB));
        defaultValue = String.valueOf(StorageUtils.TB);

        File[] dirs = StorageUtils.getExternalFilesDirs(getActivity(), null);
        if (dirs.length > storageType.ordinal()) {
            File root = dirs[storageType.ordinal()];
            long totalSpaceGB = root.getTotalSpace() / StorageUtils.GB;

            if (totalSpaceGB >= 128) {
                entries.add("128GB");
                values.add(String.valueOf(128 * StorageUtils.GB));
            }
            if (totalSpaceGB >= 64) {
                entries.add("64GB");
                values.add(String.valueOf(64 * StorageUtils.GB));
            }
            if (totalSpaceGB >= 32) {
                entries.add("32GB");
                values.add(String.valueOf(32 * StorageUtils.GB));
            }
            if (totalSpaceGB >= 16) {
                entries.add("16GB");
                values.add(String.valueOf(16 * StorageUtils.GB));
            }
            if (totalSpaceGB >= 8) {
                entries.add("8GB");
                values.add(String.valueOf(8 * StorageUtils.GB));
            }
            if (totalSpaceGB >= 4) {
                entries.add("4GB");
                values.add(String.valueOf(4 * StorageUtils.GB));
            }
            if (!videoResolution.isFullHD() && !videoResolution.isHD()) {
                if (totalSpaceGB >= 2) {
                    entries.add("2GB");
                    values.add(String.valueOf(2 * StorageUtils.GB));
                }
                if (totalSpaceGB >= 1) {
                    entries.add("1GB");
                    values.add(String.valueOf(1 * StorageUtils.GB));
                }
            } else if (videoResolution.isHD()) {
                if (totalSpaceGB >= 2) {
                    entries.add("2GB");
                    values.add(String.valueOf(2 * StorageUtils.GB));
                }
            }
        }

        // Check value
        String value = maxStorageSize.getValue();
        if (values.size() > 0 && !values.contains(value)) {
            value = values.get(0).toString();

            Toast.makeText(getActivity(), R.string.blackbox_setting_check_max_storage_size, Toast.LENGTH_LONG).show();
        }

        if (entries.size() > 0) {
            maxStorageSize.setEntries(entries.toArray(new CharSequence[entries.size()]));
            maxStorageSize.setEntryValues(values.toArray(new CharSequence[values.size()]));
            maxStorageSize.setDefaultValue(defaultValue);
            maxStorageSize.setValue(value);
        }
    }

    @Override
    public void onBlackboxInitialized() {
        logger.debug("blackbox initialized");

        ListPreference videoResolution = (ListPreference) findPreference(SettingsStore.PREF_BLACKBOX_VIDEO_RESOLLUTION);
        setVideoResolutionEntries(videoResolution);
        videoResolution.setSummary(videoResolution.getEntry());
    }

}

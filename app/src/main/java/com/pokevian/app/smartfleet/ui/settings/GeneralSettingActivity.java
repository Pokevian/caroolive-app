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

package com.pokevian.app.smartfleet.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ErsTarget;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.YoutubeAuthActivity;
import com.pokevian.app.smartfleet.ui.main.MainActivity;
import com.pokevian.app.smartfleet.util.PackageUtils;
import com.pokevian.app.smartfleet.util.TelephonyUtils;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GeneralSettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_general_setting);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = GeneralSettingFragment.newInstance();
            fm.beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onInputErsCall(String contactPhoneNumber, String contactName) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof ErsFragment) {
            ((ErsFragment) fragment).setErsCall(contactPhoneNumber, contactName);
        }
    }

    public void onInputErsSms(String contactPhoneNumber, String contactName, String message) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof ErsFragment) {
            ((ErsFragment) fragment).setErsSms(contactPhoneNumber, contactName, message);
        }
    }

    public void onSelectQuickLaunchApp(String key, String appId) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment instanceof GeneralSettingFragment) {
            ((GeneralSettingFragment) fragment).setQuickLaunchApp(key, appId);
        }
    }

    public static class GeneralSettingFragment extends PreferenceFragment
            implements OnPreferenceClickListener, OnPreferenceChangeListener {

        private SettingsStore mSettingsStore;
        private ListPreference mEngineOnDetection;

        public static GeneralSettingFragment newInstance() {
            GeneralSettingFragment fragment = new GeneralSettingFragment();
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT < 23) {
                addPreferencesFromResource(R.xml.preference_general_setting);
            } else {
                addPreferencesFromResource(R.xml.preference_general_setting_marshmallow);
            }

            mSettingsStore = SettingsStore.getInstance();

            mEngineOnDetection = getEngineOnDetectionPreference();
            if (mEngineOnDetection != null) {
                if (mSettingsStore.isBlackboxEnabled()) {
                    mEngineOnDetection.setEnabled(false);
                    mEngineOnDetection.setSummary(getString(R.string.general_setting_engine_on_detection_enabled_warning));
                } else {
                    mEngineOnDetection.setSummary(mEngineOnDetection.getEntry());
                    mEngineOnDetection.setOnPreferenceChangeListener(this);
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            ActionBar bar = ((BaseActivity) getActivity()).getSupportActionBar();
            bar.setTitle(R.string.main_settings);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            ListPreference impactSensitivity = (ListPreference) findPreference(SettingsStore.PREF_IMPACT_SENSITIVITY);
            impactSensitivity.setSummary(impactSensitivity.getEntry());
            impactSensitivity.setOnPreferenceChangeListener(this);

            Preference ers = findPreference("ers");
            ers.setOnPreferenceClickListener(this);
            updateErsSummary(mSettingsStore.getErsTarget());

            Preference quickLaunchNavi = findPreference(SettingsStore.PREF_QUICK_LAUNCH_APP_NAVI);
            quickLaunchNavi.setOnPreferenceClickListener(this);
            updateQuickLaunchAppSummary(quickLaunchNavi, mSettingsStore.getQuickLaunchNaviApp(),
                    R.string.general_setting_quick_launch_navi_summary);

            CheckBoxPreference autoLaunchNaviEnabled = (CheckBoxPreference) findPreference(SettingsStore.PREF_AUTO_LAUNCH_APP_NAVI_ENABLED);
            if (TextUtils.isEmpty(mSettingsStore.getQuickLaunchNaviApp())) {
                autoLaunchNaviEnabled.setEnabled(false);
            } else {
                autoLaunchNaviEnabled.setEnabled(true);
            }

//            Preference quickLaunchCustom = findPreference(SettingsStore.PREF_QUICK_LAUNCH_APP_CUSTOM);
//            quickLaunchCustom.setOnPreferenceClickListener(this);
//            updateQuickLaunchAppSummary(quickLaunchCustom, mSettingsStore.getQuickLaunchCustomApp(),
//                    R.string.general_setting_quick_launch_custom_summary);

            EditTextPreference overspeedThreshold = (EditTextPreference) findPreference(SettingsStore.PREF_OVERSPEED_THRESHOLD);
            overspeedThreshold.setSummary(overspeedThreshold.getText() + " " + mSettingsStore.getSpeedUnit().toString());
            overspeedThreshold.setOnPreferenceChangeListener(this);

//            Preference engineOff =  findPreference("engine_off_detection_preference");
//            engineOff.setOnPreferenceClickListener(this);
//            updateEngineOffSummary(engineOff);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            if ("ers".equals(key)) {
                Fragment fragment = ErsFragment.newInstance();
                getFragmentManager().beginTransaction().replace(R.id.container, fragment)
                        .addToBackStack(null).commitAllowingStateLoss();
            } else if (SettingsStore.PREF_QUICK_LAUNCH_APP_NAVI.equals(key)) {
                String appId = mSettingsStore.getQuickLaunchNaviApp();
                DialogFragment fragment = PickQuickLaunchAppDialogFragment.newInstance(key, appId);
                fragment.show(getFragmentManager(), PickQuickLaunchAppDialogFragment.TAG);
            } else if (SettingsStore.PREF_QUICK_LAUNCH_APP_CUSTOM.equals(key)) {
                String appId = mSettingsStore.getQuickLaunchCustomApp();
                DialogFragment fragment = PickQuickLaunchAppDialogFragment.newInstance(key, appId);
                fragment.show(getFragmentManager(), PickQuickLaunchAppDialogFragment.TAG);
            } else if ("engine_off_detection_preference".equals(key)) {
                Fragment fragment = EngineOffDetection.newInstance();
                getFragmentManager().beginTransaction().replace(R.id.container, fragment)
                        .addToBackStack(null).commitAllowingStateLoss();
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();

            if (SettingsStore.PREF_IMPACT_SENSITIVITY.equals(key)) {
                ListPreference impactSensitivity = (ListPreference) preference;
                int index = impactSensitivity.findIndexOfValue((String) newValue);
                impactSensitivity.setSummary(impactSensitivity.getEntries()[index]);
            } else if (SettingsStore.PREF_OVERSPEED_THRESHOLD.equals(key)) {
                EditTextPreference overspeedThreshold = (EditTextPreference) preference;
                overspeedThreshold.setSummary((String) newValue + " " + mSettingsStore.getSpeedUnit().toString());
            } else if (mEngineOnDetection.getKey().equals(key)) {
                int index = mEngineOnDetection.findIndexOfValue((String) newValue);
                mEngineOnDetection.setSummary(mEngineOnDetection.getEntries()[index]);
            }

            return true;
        }

        /*private void updateEngineOffSummary(Preference preferences) {
            String summary = getString(R.string.summary_disabled);
            if (mSettingsStore.isEngineOffDetectionEnabled()) {
                summary = getString(R.string.summary_enabled);
                if (mSettingsStore.isIsgSupport()) {
                    summary += " / " + getString(R.string.vehicle_setting_isg);
                }
            }

            preferences.setSummary(summary);
        }*/

        private void updateErsSummary(ErsTarget target) {
            String[] targets = getResources().getStringArray(R.array.ers_target_entries);
            Preference ers = findPreference("ers");
            ers.setSummary(targets[target.ordinal()]);
        }

        private void updateQuickLaunchAppSummary(Preference preference, String appId, int defaultSummaryResId) {
            if (!TextUtils.isEmpty(appId)) {
                String label = PackageUtils.loadLabel(getActivity(), appId);
                preference.setSummary(label);
            } else {
                preference.setSummary(defaultSummaryResId);
            }
        }

        public void setQuickLaunchApp(String key, String appId) {
            Preference preference = findPreference(key);
            if (key.equals(SettingsStore.PREF_QUICK_LAUNCH_APP_NAVI)) {
                mSettingsStore.storeQuickLaunchNaviApp(appId);
                updateQuickLaunchAppSummary(preference, appId, R.string.general_setting_quick_launch_navi_summary);

                CheckBoxPreference autoLaunchNaviEnabled = (CheckBoxPreference) findPreference(SettingsStore.PREF_AUTO_LAUNCH_APP_NAVI_ENABLED);
                if (TextUtils.isEmpty(appId)) {
                    autoLaunchNaviEnabled.setEnabled(false);
                } else {
                    autoLaunchNaviEnabled.setEnabled(true);
                }
            } else if (key.equals(SettingsStore.PREF_QUICK_LAUNCH_APP_CUSTOM)) {
                mSettingsStore.storeQuickLaunchCustomApp(appId);
                updateQuickLaunchAppSummary(preference, appId, R.string.general_setting_quick_launch_custom_summary);
            }
        }

        private ListPreference getEngineOnDetectionPreference() {
            PreferenceGroup group = (PreferenceGroup) findPreference("general_preference");
            ListPreference normal = (ListPreference) findPreference("engine_on_detection_enabled_normal");
            ListPreference marshmallow = (ListPreference) findPreference("engine_on_detection_enabled_marshmallow");

            try {
                if (Build.VERSION.SDK_INT < 23) {
//                    group.removePreference(marshmallow);
                    return (ListPreference) findPreference("engine_on_detection_enabled_normal");
                }

//                group.removePreference(normal);
                return (ListPreference) findPreference("engine_on_detection_enabled_marshmallow");
            } catch (NullPointerException e) {
            }

            return null;
        }

    }


    public static class EngineOffDetection extends PreferenceFragment {

        static final String TAG = "engine-off";

        public static EngineOffDetection newInstance() {
            return new EngineOffDetection();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_general_setting_engine_off);
        }

        @Override
        public void onStart() {
            super.onStart();

            ActionBar bar = ((BaseActivity) getActivity()).getSupportActionBar();
            bar.setTitle(R.string.general_setting_engine_off_detection_enabled);
        }
    }


        public static class ErsFragment extends PreferenceFragment
            implements OnPreferenceClickListener, OnPreferenceChangeListener {

        static final String TAG = "ErsFragment";
        final Logger log = Logger.getLogger(TAG);

        private static final int REQUEST_YOUTUBE_AUTH = 1;

        private SettingsStore mSettingsStore;

        public static ErsFragment newInstance() {
            ErsFragment fragment = new ErsFragment();
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_general_setting_ers);

            mSettingsStore = SettingsStore.getInstance();
        }

        @Override
        public void onStart() {
            super.onStart();

            ActionBar bar = ((BaseActivity) getActivity()).getSupportActionBar();
            bar.setTitle(R.string.general_setting_ers);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Preference ersCall = findPreference("ers_call");
            String contactPhoneNumber = mSettingsStore.getErsCallContactPhoneNumber();
            String contactName = mSettingsStore.getErsCallContactName();
            updateErsCallSummary(contactPhoneNumber, contactName);
            ersCall.setOnPreferenceClickListener(this);

            Preference ersSms = findPreference("ers_sms");
            contactPhoneNumber = mSettingsStore.getErsSmsContactPhoneNumber();
            contactName = mSettingsStore.getErsSmsContactName();
            String message = mSettingsStore.getErsSmsMessage();
            updateErsSmsSummary(contactPhoneNumber, contactName, message);
            ersSms.setOnPreferenceClickListener(this);

            ListPreference youtubePrivacy = (ListPreference) findPreference(SettingsStore.PREF_ERS_YOUTUBE_PRIVACY);
            youtubePrivacy.setSummary(youtubePrivacy.getEntry());
            youtubePrivacy.setOnPreferenceChangeListener(this);

            ListPreference ersTarget = (ListPreference) findPreference(SettingsStore.PREF_ERS_TARGET);
            setErsTargetEntries(ersTarget);
            ersTarget.setSummary(ersTarget.getEntry());
            ersTarget.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            String key = preference.getKey();
            if ("ers_call".equals(key)) {
                String contactPhoneNumber = mSettingsStore.getErsCallContactPhoneNumber();
                String contactName = mSettingsStore.getErsCallContactName();
                InputErsCallDialogFragment fragment = InputErsCallDialogFragment.newInstance(contactPhoneNumber,
                        contactName);
                fragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        ListPreference ersTarget = (ListPreference) findPreference(SettingsStore.PREF_ERS_TARGET);
                        setErsTargetEntries(ersTarget);
                        ersTarget.setSummary(ersTarget.getEntry());
                    }
                });
                fragment.show(getFragmentManager(), InputErsCallDialogFragment.TAG);
            } else if ("ers_sms".equals(key)) {
                String contactPhoneNumber = mSettingsStore.getErsSmsContactPhoneNumber();
                String contactName = mSettingsStore.getErsSmsContactName();
                String message = mSettingsStore.getErsSmsMessage();
                if (TextUtils.isEmpty(message)) {
                    message = getString(R.string.ers_sms_message_default);
                }
                InputErsSmsDialogFragment fragment = InputErsSmsDialogFragment.newInstance(contactPhoneNumber,
                        contactName, message);
                fragment.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        ListPreference ersTarget = (ListPreference) findPreference(SettingsStore.PREF_ERS_TARGET);
                        setErsTargetEntries(ersTarget);
                        ersTarget.setSummary(ersTarget.getEntry());
                    }
                });
                fragment.show(getFragmentManager(), InputErsSmsDialogFragment.TAG);
            }
            return false;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (SettingsStore.PREF_ERS_YOUTUBE_PRIVACY.equals(key)) {
                ListPreference youtubePrivacy = (ListPreference) preference;
                int index = youtubePrivacy.findIndexOfValue((String) newValue);
                youtubePrivacy.setSummary(youtubePrivacy.getEntries()[index]);
            } else if (SettingsStore.PREF_ERS_TARGET.equals(key)) {
                ListPreference ersTarget = (ListPreference) preference;
                int index = ersTarget.findIndexOfValue((String) newValue);
                ersTarget.setSummary(ersTarget.getEntries()[index]);

                if (ErsTarget.YOUTUBE.name().equals(newValue)) {
                    WaitDialogFragment fragment = new WaitDialogFragment();
                    fragment.show(getChildFragmentManager(), WaitDialogFragment.TAG);

                    Intent intent = new Intent(getActivity(), YoutubeAuthActivity.class);
                    startActivityForResult(intent, REQUEST_YOUTUBE_AUTH);
                }
            }
            return true;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_YOUTUBE_AUTH) {
                FragmentManager fm = getChildFragmentManager();
                Fragment fragment = fm.findFragmentByTag(WaitDialogFragment.TAG);
                if (fragment != null) {
                    fm.beginTransaction().remove(fragment).commitAllowingStateLoss();
                }

                if (resultCode == Activity.RESULT_OK
                        && data != null && data.hasExtra(YoutubeAuthActivity.EXTRA_ACCOUNT_NAME)) {
                    String accountName = data.getStringExtra(YoutubeAuthActivity.EXTRA_ACCOUNT_NAME);
                    log.debug("onActivityResult(): REQUEST_YOUTUBE_AUTH, accountName=" + accountName);

                    mSettingsStore.storeErsYoutubeAccountName(accountName);
                } else {
                    // Reset ERS target to NONE
                    ListPreference ersTarget = (ListPreference) findPreference(SettingsStore.PREF_ERS_TARGET);
                    ersTarget.setValue(ErsTarget.NONE.name());
                    ersTarget.setSummary(ersTarget.getEntry());
                }
            }
        }

        private void setErsTargetEntries(ListPreference ersTarget) {
            ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> values = new ArrayList<CharSequence>();
            CharSequence defaultValue = null;

            // None entry
            entries.add(getString(R.string.ers_target_none));
            values.add(ErsTarget.NONE.name());
            defaultValue = ErsTarget.NONE.name();

            // Call and SMS
            if (!TextUtils.isEmpty(mSettingsStore.getErsCallContactPhoneNumber())) {
                entries.add(getString(R.string.ers_target_call));
                values.add(ErsTarget.CALL.name());
            }
            if (!TextUtils.isEmpty(mSettingsStore.getErsSmsContactPhoneNumber())
                    && !TextUtils.isEmpty(mSettingsStore.getErsSmsMessage())) {
                entries.add(getString(R.string.ers_target_sms));
                values.add(ErsTarget.SMS.name());
            }

            // Youtube entry
            entries.add(getString(R.string.ers_target_youtube));
            values.add(ErsTarget.YOUTUBE.name());

            // Check value
            String value = ersTarget.getValue();
            if (values.size() > 0 && !values.contains(value)) {
                value = values.get(0).toString();
            }

            if (entries.size() > 0) {
                ersTarget.setEntries(entries.toArray(new CharSequence[entries.size()]));
                ersTarget.setEntryValues(values.toArray(new CharSequence[values.size()]));
                ersTarget.setDefaultValue(defaultValue);
                ersTarget.setValue(value);
            }
        }

        private void updateErsCallSummary(String contactPhoneNumber, String contactName) {
            Preference ersCall = findPreference("ers_call");
            if (!TextUtils.isEmpty(contactPhoneNumber)) {
//                ersCall.setSummary(contactName + "(" + TelephonyUtils.formatPhoneNumber(contactPhoneNumber) + ")");
                ersCall.setSummary(contactName + "(" + contactPhoneNumber + ")");
            } else {
                ersCall.setSummary(getString(R.string.general_setting_ers_call_summary));
            }
        }

        private void updateErsSmsSummary(String contactPhoneNumber, String contactName, String message) {
            Preference ersSms = findPreference("ers_sms");
            if (!TextUtils.isEmpty(contactPhoneNumber) && !TextUtils.isEmpty(message)) {
//                ersSms.setSummary(contactName + "(" + TelephonyUtils.formatPhoneNumber(contactPhoneNumber) + ")\n" + message);
                ersSms.setSummary(contactName + "(" + contactPhoneNumber + ")\n" + message);
            } else {
                ersSms.setSummary(getString(R.string.general_setting_ers_sms_summary));
            }
        }

        public void setErsCall(String contactPhoneNumber, String contactName) {
            Logger.getLogger(TAG).debug("" + contactPhoneNumber + "@setErsCall");
            mSettingsStore.storeErsCallContactPhoneNumber(contactPhoneNumber);
            mSettingsStore.storeErsCallContactName(contactName);
            updateErsCallSummary(contactPhoneNumber, contactName);
        }

        public void setErsSms(String contactPhoneNumber, String contactName, String message) {
            mSettingsStore.storeErsSmsContactPhoneNumber(contactPhoneNumber);
            mSettingsStore.storeErsSmsContactName(contactName);
            mSettingsStore.storeErsSmsMessage(message);
            updateErsSmsSummary(contactPhoneNumber, contactName, message);
        }

    }

    public static class InputErsCallDialogFragment extends DialogFragment
            implements OnClickListener, SelectPhoneNumberDialogFragment.Callbacks {

        public static final String TAG = "input-ers-call-dialog";
        final Logger log = Logger.getLogger(TAG);

        private static final int REQUEST_PICK_CONTACT = 1;

        private EditText mContactNameEdit;
        private EditText mContactPhoneNumberEdit;
        private TextWatcher mContactPhoneNumberWatcher;

        private String mContactName;
        private String mContactPhoneNumber;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public static InputErsCallDialogFragment newInstance(String contactPhoneNumber, String contactName) {
            InputErsCallDialogFragment fragment = new InputErsCallDialogFragment();
            Bundle args = new Bundle();
            args.putString("contact_phone_number", contactPhoneNumber);
            args.putString("contact_name", contactName);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mContactPhoneNumber = args.getString("contact_phone_number");
                mContactName = args.getString("contact_name");
            } else {
                mContactPhoneNumber = savedInstanceState.getString("contact_phone_number");
                mContactName = savedInstanceState.getString("contact_name");
            }

            mContactPhoneNumber = TelephonyUtils.formatPhoneNumber(mContactPhoneNumber);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("contact_phone_number", mContactPhoneNumber);
            outState.putString("contact_name", mContactName);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.preference_ers_call, null);
            mContactPhoneNumberEdit = (EditText) view.findViewById(R.id.contact_phone_number_edit);
            mContactPhoneNumberEdit.setText(mContactPhoneNumber);

            mContactPhoneNumberWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!TextUtils.isEmpty(s)) {
                        mContactPhoneNumberEdit.removeTextChangedListener(mContactPhoneNumberWatcher);

                        String phoneNumber = TelephonyUtils.formatPhoneNumber(s.toString().trim());
                        mContactPhoneNumberEdit.setText(phoneNumber);
                        if (phoneNumber != null) {
                            mContactPhoneNumberEdit.setSelection(phoneNumber.length());
                        }
                        mContactPhoneNumberEdit.addTextChangedListener(mContactPhoneNumberWatcher);
                    }
                }
            };
//            mContactPhoneNumberEdit.addTextChangedListener(mContactPhoneNumberWatcher);

            mContactNameEdit = (EditText) view.findViewById(R.id.contact_name_edit);
            mContactNameEdit.setText(mContactName);
            view.findViewById(R.id.contact_btn).setOnClickListener(this);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.general_setting_ers_call)
                    .setView(view)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String contactName = mContactNameEdit.getText().toString().trim();
                            String contactPhoneNumber = mContactPhoneNumberEdit.getText().toString().trim();
//                            contactPhoneNumber = TelephonyUtils.normalizePhoneNumber(contactPhoneNumber);
                            ((GeneralSettingActivity) getActivity()).onInputErsCall(contactPhoneNumber, contactName);
                        }
                    })
                    .create();
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.contact_btn) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_CONTACT);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_PICK_CONTACT) {
                if (resultCode == Activity.RESULT_OK) {
                    setPhoneNumber(data);
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }

            super.onDismiss(dialog);
        }

        public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mOnDismissListener = listener;
        }

        private void setPhoneNumber(Intent data) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            Cursor cursor = contentResolver.query(data.getData(), null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final String name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    log.debug("setPhoneNumber(): name=" + name);
                    int numberCount = Integer.valueOf(cursor.getString(cursor.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                    log.debug("setPhoneNumber(): number count=" + numberCount);
                    if (numberCount > 0) {
                        ArrayList<String> numbers = new ArrayList<String>();
                        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactId}, null);
                        if (phoneCursor != null) {
                            while (phoneCursor.moveToNext()) {
                                String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                                number = TelephonyUtils.formatPhoneNumber(number);
                                log.debug("setPhoneNumber(): number={}" + number);
                                if (!numbers.contains(number)) {
                                    numbers.add(number);
                                }
                            }
                            phoneCursor.close();
                        }

                        mContactNameEdit.setText(name);

                        if (numbers.size() == 1) {
                            mContactPhoneNumberEdit.setText(numbers.get(0));
                        } else {
                            final String[] items = new String[numbers.size()];
                            numbers.toArray(items);

                            DialogFragment fragment = SelectPhoneNumberDialogFragment.newInstance(name, items);
                            fragment.show(getChildFragmentManager(), SelectPhoneNumberDialogFragment.TAG);
                        }
                    }
                }
                cursor.close();
            }
        }

        @Override
        public void onPhoneNumberSelected(String phoneNumber) {
            log.debug("onPhoneNumberSelected(): phoneNumber=" + phoneNumber);
            mContactPhoneNumberEdit.setText(phoneNumber);
        }

    }

    public static class InputErsSmsDialogFragment extends DialogFragment
            implements OnClickListener, SelectPhoneNumberDialogFragment.Callbacks {

        public static final String TAG = "input-ers-sms-dialog";
        final Logger log = Logger.getLogger(TAG);

        private static final int REQUEST_PICK_CONTACT = 1;

        private EditText mContactNameEdit;
        private EditText mContactPhoneNumberEdit;
        private TextWatcher mContactPhoneNumberWatcher;
        private EditText mMessageEdit;

        private String mContactName;
        private String mContactPhoneNumber;
        private String mMessage;
        private DialogInterface.OnDismissListener mOnDismissListener;

        public static InputErsSmsDialogFragment newInstance(String contactPhoneNumber, String contactName,
                                                            String message) {
            InputErsSmsDialogFragment fragment = new InputErsSmsDialogFragment();
            Bundle args = new Bundle();
            args.putString("contact_phone_number", contactPhoneNumber);
            args.putString("contact_name", contactName);
            args.putString("message", message);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mContactPhoneNumber = args.getString("contact_phone_number");
                mContactName = args.getString("contact_name");
                mMessage = args.getString("message");
            } else {
                mContactPhoneNumber = savedInstanceState.getString("contact_phone_number");
                mContactName = savedInstanceState.getString("contact_name");
                mMessage = savedInstanceState.getString("message");
            }

            mContactPhoneNumber = TelephonyUtils.formatPhoneNumber(mContactPhoneNumber);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("contact_phone_number", mContactPhoneNumber);
            outState.putString("contact_name", mContactName);
            outState.putString("message", mMessage);

            super.onSaveInstanceState(outState);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.preference_ers_sms, null);
            mContactPhoneNumberEdit = (EditText) view.findViewById(R.id.contact_phone_number_edit);
            mContactPhoneNumberEdit.setText(mContactPhoneNumber);

            mContactPhoneNumberWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (!TextUtils.isEmpty(s)) {
                        mContactPhoneNumberEdit.removeTextChangedListener(mContactPhoneNumberWatcher);

                        String phoneNumber = TelephonyUtils.formatPhoneNumber(s.toString().trim());
                        mContactPhoneNumberEdit.setText(phoneNumber);
                        if (phoneNumber != null) {
                            mContactPhoneNumberEdit.setSelection(phoneNumber.length());
                        }

                        mContactPhoneNumberEdit.addTextChangedListener(mContactPhoneNumberWatcher);
                    }
                }
            };
//            mContactPhoneNumberEdit.addTextChangedListener(mContactPhoneNumberWatcher);

            mContactNameEdit = (EditText) view.findViewById(R.id.contact_name_edit);
            mContactNameEdit.setText(mContactName);

            mMessageEdit = (EditText) view.findViewById(R.id.message_edit);
            mMessageEdit.setText(mMessage);
            view.findViewById(R.id.contact_btn).setOnClickListener(this);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.general_setting_ers_sms)
                    .setView(view)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String contactName = mContactNameEdit.getText().toString().trim();
                            String contactPhoneNumber = mContactPhoneNumberEdit.getText().toString().trim();
                            contactPhoneNumber = TelephonyUtils.normalizePhoneNumber(contactPhoneNumber);
                            String message = mMessageEdit.getText().toString().trim();

                            ((GeneralSettingActivity) getActivity()).onInputErsSms(contactPhoneNumber, contactName,
                                    message);
                        }
                    })
                    .create();
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.contact_btn) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_PICK_CONTACT);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_PICK_CONTACT) {
                if (resultCode == Activity.RESULT_OK) {
                    setPhoneNumber(data);
                }
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mOnDismissListener != null) {
                mOnDismissListener.onDismiss(dialog);
            }

            super.onDismiss(dialog);
        }

        public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mOnDismissListener = listener;
        }

        private void setPhoneNumber(Intent data) {
            ContentResolver contentResolver = getActivity().getContentResolver();
            Cursor cursor = contentResolver.query(data.getData(), null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    final String name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    log.debug("setPhoneNumber(): name=" + name);
                    int numberCount = Integer.valueOf(cursor.getString(cursor.getColumnIndex(
                            ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                    log.debug("setPhoneNumber(): number count={}" + numberCount);
                    if (numberCount > 0) {
                        ArrayList<String> numbers = new ArrayList<String>();
                        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactId}, null);
                        if (phoneCursor != null) {
                            while (phoneCursor.moveToNext()) {
                                String number = phoneCursor.getString(phoneCursor.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                                number = TelephonyUtils.formatPhoneNumber(number);
                                log.debug("setPhoneNumber(): number=" + number);
                                if (!numbers.contains(number)) {
                                    numbers.add(number);
                                }
                            }
                            phoneCursor.close();
                        }

                        mContactNameEdit.setText(name);

                        if (numbers.size() == 1) {
                            mContactPhoneNumberEdit.setText(numbers.get(0));
                        } else {
                            final String[] items = new String[numbers.size()];
                            numbers.toArray(items);

                            DialogFragment fragment = SelectPhoneNumberDialogFragment.newInstance(name, items);
                            fragment.show(getChildFragmentManager(), SelectPhoneNumberDialogFragment.TAG);
                        }
                    }
                }
                cursor.close();
            }
        }

        @Override
        public void onPhoneNumberSelected(String phoneNumber) {
            log.debug("onPhoneNumberSelected(): phoneNumber=" + phoneNumber);
            mContactPhoneNumberEdit.setText(phoneNumber);
        }

    }

    public static class SelectPhoneNumberDialogFragment extends DialogFragment {

        public static final String TAG = "select-phone-number-dialog";

        private String mTitle;
        private String[] mNumbers;

        public static SelectPhoneNumberDialogFragment newInstance(String title, String[] numbers) {
            SelectPhoneNumberDialogFragment fragment = new SelectPhoneNumberDialogFragment();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putStringArray("numbers", numbers);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mTitle = args.getString("title");
                mNumbers = args.getStringArray("numbers");
            } else {
                mTitle = savedInstanceState.getString("title");
                mNumbers = savedInstanceState.getStringArray("numbers");
            }
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(mTitle)
                    .setNegativeButton(R.string.btn_cancel, null)
                    .setItems(mNumbers, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String selectedNumber = mNumbers[which];
                            ((Callbacks) (getParentFragment())).onPhoneNumberSelected(selectedNumber);
                            dismiss();
                        }
                    })
                    .create();
        }

        public interface Callbacks {
            void onPhoneNumberSelected(String phoneNumber);
        }

    }

    public static class PickQuickLaunchAppDialogFragment extends DialogFragment {

        public static final String TAG = "pick-quick-launch-app-dialog";

        private String mKey;
        private String mAppId;

        public static PickQuickLaunchAppDialogFragment newInstance(String key, String appId) {
            PickQuickLaunchAppDialogFragment fragment = new PickQuickLaunchAppDialogFragment();
            Bundle args = new Bundle();
            args.putString("key", key);
            args.putString("app_id", appId);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mKey = args.getString("key");
                mAppId = args.getString("app_id");
            } else {
                mKey = savedInstanceState.getString("key");
                mAppId = savedInstanceState.getString("app_id");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            List<ResolveInfo> apps = PackageUtils.getAppResolveInfos(getActivity());
            Collections.sort(apps, new Comparator<ResolveInfo>() {
                public int compare(ResolveInfo lhs, ResolveInfo rhs) {
                    String lhsLabel = PackageUtils.loadLabel(getActivity(), lhs);
                    String rhsLabel = PackageUtils.loadLabel(getActivity(), rhs);
                    return lhsLabel.compareToIgnoreCase(rhsLabel);
                }
            });
            apps.add(0, new ResolveInfo()); // none option

            final AppListAdapter adapter = new AppListAdapter(getActivity(), apps);
            int checkedItem = getCheckedItem(adapter);

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.general_setting_quick_launch_navi)
                    .setSingleChoiceItems(adapter, checkedItem, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ResolveInfo app = adapter.getItem(which);
                            String appId = null;
                            if (app.activityInfo != null) {
                                appId = PackageUtils.toAppId(app);
                            }
                            ((GeneralSettingActivity) getActivity()).onSelectQuickLaunchApp(mKey, appId);
                            dismiss();
                        }
                    })
                    .setNegativeButton(R.string.btn_cancel, null)
                    .create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putString("key", mKey);
            outState.putString("app_id", mAppId);

            super.onSaveInstanceState(outState);
        }

        private int getCheckedItem(AppListAdapter adapter) {
            if (mAppId == null) {
                return 0;
            }

            int count = adapter.getCount();
            int checkedItem = 0;
            for (int i = 0; i < count; i++) {
                ResolveInfo info = adapter.getItem(i);
                if (info.activityInfo == null) {
                    continue;
                }
                if (mAppId.equals(PackageUtils.toAppId(info))) {
                    checkedItem = i;
                    break;
                }
            }
            return checkedItem;
        }

        private class AppListAdapter extends ArrayAdapter<ResolveInfo> {

            private final LayoutInflater mInflater;
            private final Rect mIconBounds;
            private final int mIconPadding;

            public AppListAdapter(Context context, List<ResolveInfo> items) {
                super(context, 0, items);

                mInflater = LayoutInflater.from(context);
                int wh = MainActivity.dp2px(context, 40);
                mIconBounds = new Rect(0, 0, wh, wh);
                mIconPadding = MainActivity.dp2px(context, 10);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;

                if (convertView == null) {
                    convertView = mInflater.inflate(android.R.layout.select_dialog_singlechoice, null);
                    holder = new ViewHolder(convertView);
                    convertView.setTag(holder);
                } else {
                    holder = (ViewHolder) convertView.getTag();
                }

                ResolveInfo item = getItem(position);
                if (item.activityInfo == null) {
                    holder.textView.setCompoundDrawables(null, null, null, null);
                    holder.textView.setText(R.string.general_setting_quick_launch_none);
                } else {
                    Drawable icon = PackageUtils.loadIcon(getContext(), item);
                    icon.setBounds(mIconBounds);
                    holder.textView.setCompoundDrawables(icon, null, null, null);
                    holder.textView.setText(PackageUtils.loadLabel(getContext(), item));
                }

                return convertView;
            }

            class ViewHolder {
                CheckedTextView textView;

                ViewHolder(View parent) {
                    textView = (CheckedTextView) parent.findViewById(android.R.id.text1);
                    textView.setCompoundDrawablePadding(mIconPadding);
                }
            }

        }

    }

    public static class WaitDialogFragment extends DialogFragment {

        public static final String TAG = "wait-dialog";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getString(R.string.dialog_message_wait_for));
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            return dialog;
        }

    }

}

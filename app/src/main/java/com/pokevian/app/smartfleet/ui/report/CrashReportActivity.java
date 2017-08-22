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

package com.pokevian.app.smartfleet.ui.report;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.util.PackageUtils;

public class CrashReportActivity extends BaseActivity {

    public static final String EXTRA_EXCEPTION = "extra.EXCEPTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent data = getIntent();
            Throwable th = (Throwable) data.getSerializableExtra(EXTRA_EXCEPTION);
            DialogFragment fragment = CrashReportDialogFragment.newInstance(th);
            fragment.show(getSupportFragmentManager(), "crash-report-dialog");
        }
    }

    public static class CrashReportDialogFragment extends DialogFragment {

        public static CrashReportDialogFragment newInstance(Throwable th) {
            CrashReportDialogFragment fragment = new CrashReportDialogFragment();
            Bundle args = new Bundle();
            args.putSerializable("throwable", th);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity(), R.style.AppTheme_Light_Dialog)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.crash_message)
                    .setNegativeButton(R.string.btn_no, null)
                    .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                            String subject = getString(R.string.crash_report,
                                    PackageUtils.loadLabel(getActivity(), getActivity().getApplicationInfo()));
                            Bundle args = getArguments();
                            Throwable th = (Throwable) args.getSerializable("throwable");
                            Fragment fragment = LogReportFragment.newInstance(subject, th);
                            ft.add(fragment, LogReportFragment.TAG).commit();
                        }
                    })
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            // kill myself
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }

}

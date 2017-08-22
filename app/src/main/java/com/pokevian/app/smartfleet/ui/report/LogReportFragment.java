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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.util.LogUtils;

import java.io.File;
import java.util.ArrayList;

public class LogReportFragment extends DialogFragment {

    public static final String TAG = "LogReportFragment";

    private String mSubject;
    private Throwable mTh;

    public static LogReportFragment newInstance(String subject, Throwable th) {
        LogReportFragment fragment = new LogReportFragment();
        Bundle args = new Bundle();
        args.putString("subject", subject);
        args.putSerializable("throwable", th);
        fragment.setArguments(args);
        return fragment;
    }

    public static LogReportFragment newInstance(String subject) {
        return newInstance(subject, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mSubject = args.getString("subject");
            mTh = (Throwable) args.getSerializable("throwable");
        } else {
            mSubject = savedInstanceState.getString("subject");
            mTh = (Throwable) savedInstanceState.getSerializable("throwable");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("subject", mSubject);
        outState.putSerializable("throwable", mTh);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sendEmail();

        dismiss();
    }

    private void sendEmail() {
        String[] to = getMailTo();
        String[] cc = getMailCC();

        Intent target = new Intent(Intent.ACTION_SEND_MULTIPLE);
        target.setType("text/plain");
        target.putExtra(Intent.EXTRA_EMAIL, to);
//        target.putExtra(Intent.EXTRA_BCC, cc);
        target.putExtra(Intent.EXTRA_SUBJECT, mSubject);
        target.putExtra(Intent.EXTRA_TEXT, buildMessage());

        File[] logFiles = LogUtils.getLogFiles(getActivity());
        if (logFiles != null && logFiles.length > 0) {
            ArrayList<Uri> uris = new ArrayList<>();
            for (File logFile : logFiles) {
                Uri u = Uri.fromFile(logFile);
                uris.add(u);
            }
            target.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }

        startActivity(Intent.createChooser(target, null));
    }

    private String[] getMailTo() {
        return new String[]{"support@pokevian.com"};
    }

    private String[] getMailCC() {
        return new String[]{"i.choi@pokevian.com"};
    }

    private String buildMessage() {
        StringBuilder buffer = new StringBuilder();

        if (mTh != null) {
            buffer.append(LogUtils.buildStackTrack(mTh));
            buffer.append("\n");
        }

        buffer.append(LogUtils.buildPackageLog(getActivity()));
        buffer.append("\n");

        buffer.append(LogUtils.buildPhoneLog());
        buffer.append("\n");

        buffer.append(LogUtils.buildStorageLog(getActivity()));
        buffer.append("\n");

        buffer.append(LogUtils.buildBlackboxLog(getActivity()));
        buffer.append("\n");

        buffer.append(LogUtils.buildPreferencesLog(getActivity()));
        buffer.append("\n");

        return buffer.toString();
    }

}

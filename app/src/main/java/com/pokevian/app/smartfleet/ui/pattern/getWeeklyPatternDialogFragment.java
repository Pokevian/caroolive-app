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

package com.pokevian.app.smartfleet.ui.pattern;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.android.volley.RequestQueue;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.request.GetScoreStarRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.ui.rank.WeekUtils;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

public class getWeeklyPatternDialogFragment extends DialogFragment implements AlertDialogCallbacks {
    public static final String TAG = "getWeeklyPatternDialogFragment";

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private LoadPatternDataCallback mCallback;
    private String mMemberNo;
    private String mDate;
    private String mPeriod;

    public static getWeeklyPatternDialogFragment newInstance(String memberNo, boolean lastWeek, String period) {
        getWeeklyPatternDialogFragment fragment = new getWeeklyPatternDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("last-week", lastWeek);
        args.putString("memberNo", memberNo);
        args.putString("period", period);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (LoadPatternDataCallback) getParentFragment();
        } catch (ClassCastException e) { }
        if (mCallback == null) {
            try {
                mCallback = (LoadPatternDataCallback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadPatternDataCallback");
            }
        }
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);
        super.onDestroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new WaitForDialog(getActivity());
        dialog.setCanceledOnTouchOutside(false);
        setCancelable(false);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() != null) {
            mMemberNo = getArguments().getString("memberNo");
            mPeriod = getArguments().getString("period");
            mDate = WeekUtils.getTodayString(getArguments().getBoolean("last-week", false));

            request();
        }
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                request();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallback.onFailure();
                dismiss();
            }
        }
    }

    private void request() {
        GetScoreStarRequest request = new GetScoreStarRequest(mMemberNo, mDate, mPeriod, new GetWeeklyPatternDataListenr());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private class GetWeeklyPatternDataListenr extends VolleyListener<ScoreRank> {

        @Override
        public void onErrorResponse(VolleyError error) {
            Logger.getLogger("volley").error(error.getMessage(), error);
            if (error instanceof ServerError) {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_server_error),
                        getString(R.string.dialog_message_server_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

        @Override
        public void onResponse(ScoreRank response) {
            mCallback.onSuccess(response);
            dismissAllowingStateLoss();
        }
    }

    public interface LoadPatternDataCallback {
        void onSuccess(ScoreRank scoreStar);
        void onFailure();
    }
}
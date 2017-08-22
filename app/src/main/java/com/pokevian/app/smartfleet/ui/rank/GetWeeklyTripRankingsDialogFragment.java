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

package com.pokevian.app.smartfleet.ui.rank;

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
import com.pokevian.app.smartfleet.model.MemberEcoData;
import com.pokevian.app.smartfleet.request.GetBestTripListRequest;
import com.pokevian.app.smartfleet.request.GetBestTripRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GetWeeklyTripRankingsDialogFragment extends DialogFragment implements AlertDialogCallbacks  {

    public static final String TAG = "LoadWeeklyBestTrip";
    public static final int REQUEST_ID_BEST_TRIP = 0;
    public static final int REQUEST_ID_BEST_TRIP_LIST = 1;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private GetBestTripDialogCallback mCallback;

    private int mRequestId = REQUEST_ID_BEST_TRIP;
    private String mMemberNo;
    private String mDate;
    private String mPeriod = "weekly";
    private String mLimit = "15";
    private List<MemberEcoData> mList;

    public static GetWeeklyTripRankingsDialogFragment newInstance(String mMemberNo) {
        return newInstance(mMemberNo, false);
    }

    public static GetWeeklyTripRankingsDialogFragment newInstance(String memberNo, boolean lastWeek) {
        GetWeeklyTripRankingsDialogFragment fragment = new GetWeeklyTripRankingsDialogFragment();

        Bundle args = new Bundle();
        args.putString("memberNo", memberNo);
        args.putBoolean("last-week", lastWeek);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (GetBestTripDialogCallback) getParentFragment();
        } catch (ClassCastException e) {
        }
        if (mCallback == null) {
            try {
                mCallback = (GetBestTripDialogCallback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement GetBestTripDialogCallback");
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
            mDate = WeekUtils.getTodayString(getArguments().getBoolean("last-week", false));
            request(REQUEST_ID_BEST_TRIP_LIST);
        }
        Logger.getLogger(TAG).debug("getArguments#" + mMemberNo + "@" + mDate);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                request();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallback.onFailure(GetWeeklyTripRankingsDialogFragment.this);
                dismiss();
            }
        }
    }

    private void request(int requestId) {
        mRequestId = requestId;
        request();
    }

    private void request() {
        mRequestQueue.add(getRequest().setTag(TAG));
    }

    private GsonRequest getRequest() {
        switch (mRequestId) {
            case REQUEST_ID_BEST_TRIP:
                return new GetBestTripRequest(mDate, mPeriod, mMemberNo, new GetBestTripListener());
            case REQUEST_ID_BEST_TRIP_LIST:
                return  new GetBestTripListRequest(mDate, mPeriod, mLimit, new GetBestTripListListener());
        }

        return null;
    }

    private class GetBestTripListener extends VolleyListener<MemberEcoData> {

        @Override
        public void onErrorResponse(VolleyError error) {
            onVolleyError(error);
        }

        @Override
        public void onResponse(MemberEcoData response) {
            mCallback.onSuccess(GetWeeklyTripRankingsDialogFragment.this, mList, response);
            dismissAllowingStateLoss();
        }
    }

    private class GetBestTripListListener extends VolleyListener<MemberEcoData[]> {

        @Override
        public void onErrorResponse(VolleyError error) {
            onVolleyError(error);
        }

        @Override
        public void onResponse(MemberEcoData[] response) {
            Logger.getLogger(TAG).trace("onResponse#" + response);
            MemberEcoData myData = null;
            if (response != null && response.length > 0) {
                mList = new ArrayList<>();
                for (MemberEcoData data : response) {
                    mList.add(data);
                    if (data.getMemberNo().equals(mMemberNo)) {
                        myData = data;
                    }
                }

                if (myData != null) {
                    mCallback.onSuccess(GetWeeklyTripRankingsDialogFragment.this, mList, myData);
                    dismissAllowingStateLoss();
                } else {
                    request(REQUEST_ID_BEST_TRIP);
                }
            } else {
                mCallback.onSuccess(GetWeeklyTripRankingsDialogFragment.this, null, null);
                dismissAllowingStateLoss();
            }
        }
    }

    private void onVolleyError(VolleyError error) {
        Logger.getLogger(TAG).error(error.getMessage(), error);
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

    public interface GetBestTripDialogCallback {
        void onSuccess(DialogFragment fragment, List<MemberEcoData> list, MemberEcoData myTrip);
        void onFailure(DialogFragment fragment);
    }
}

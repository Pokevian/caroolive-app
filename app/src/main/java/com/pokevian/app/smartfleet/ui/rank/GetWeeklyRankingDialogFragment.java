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
import com.pokevian.app.smartfleet.model.Rank;
import com.pokevian.app.smartfleet.request.GetRankingRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

public class GetWeeklyRankingDialogFragment extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "getWeeklyRank";

    private String mAccountId;
    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private GetRankingDialogCallback mCallback;
    private String mDate;

    public static GetWeeklyRankingDialogFragment newInstance(String memberNo, boolean lastWeek) {
        GetWeeklyRankingDialogFragment fragment = new GetWeeklyRankingDialogFragment();
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
            mCallback = (GetRankingDialogCallback) getParentFragment();
        } catch (ClassCastException e) {
        }
        if (mCallback == null) {
            try {
                mCallback = (GetRankingDialogCallback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement GetRankingDialogCallback");
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
            mAccountId = getArguments().getString("memberNo");
            mDate = WeekUtils.getTodayString(getArguments().getBoolean("last-week", true));
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
                mCallback.onFailure(GetWeeklyRankingDialogFragment.this);
                dismiss();
            }
        }
    }

    private void request() {
        GetRankingRequest request = new GetRankingRequest("weekly", mAccountId, mDate, new GetRankingListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    private class GetRankingListener extends VolleyListener<Rank[]> {

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
        public void onResponse(Rank[] response) {
            mCallback.onSuccess(GetWeeklyRankingDialogFragment.this, response != null && response.length > 0 ? response[0] : null);
            dismissAllowingStateLoss();
        }
    }
}

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
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.request.GetRankingListRequest;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GetRankingTop10DialogFragment extends DialogFragment implements AlertDialogCallbacks  {

    public static final String TAG = "GetRankingTop10DialogFragment";

    public static final String REQUEST_ID_RANKING_TOP10_LEVEL = "request-id-ranking-top10-level";
    public static final String REQUEST_ID_RANKING_TOP10_POINT = "request-id-ranking-top10-point";

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private GetRankingsDialogCallback mCallback;
    private String mRequestId;

    public static GetRankingTop10DialogFragment newInstance(String requestId) {
        GetRankingTop10DialogFragment fragment = new GetRankingTop10DialogFragment();
        Bundle args = new Bundle();
        args.putString("request-id", requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (GetRankingsDialogCallback) getParentFragment();
        } catch (ClassCastException e) { }

        if (mCallback == null) {
            try {
                mCallback = (GetRankingsDialogCallback) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadWeeklyRankDialogCallback");
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
        setCancelable(true);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            mRequestId = getArguments().getString("request-id", null);
            request();
        }
        Logger.getLogger(TAG).trace("getArguments#" + mRequestId);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                request();
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallback.onFailure(GetRankingTop10DialogFragment.this);
                dismiss();
            }
        }
    }

    private void request() {
        if (mRequestId != null) {
            mRequestQueue.add(getRequest(mRequestId));
        }
    }

    private GsonRequest getRequest(String id) {
        GsonRequest request = null;
        if (REQUEST_ID_RANKING_TOP10_LEVEL.equals(id)) {
            request = GetRankingListRequest.getTop10Request(ServerUrl.GET_RANKING_TOP10_LEVEL_API, new GetRankingListListener());
        } else if (REQUEST_ID_RANKING_TOP10_POINT.equals(id)) {
            request = GetRankingListRequest.getTop10RequestByOrder(ServerUrl.GET_RANKING_TOP10_POINT_API, new GetRankingListListener());
        }

        if (request != null) {
            request.setTag(id);
        }
        return request;
    }

    private class GetRankingListListener extends VolleyListener<ScoreRank[]> {

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
        public void onResponse(ScoreRank[] response) {
            List<ScoreRank> list = null;
            if (response != null && response.length > 0) {
                list = new ArrayList<>();
                for (ScoreRank rank : response) {
                    list.add(rank);
                }
            }
            mCallback.onSuccess(GetRankingTop10DialogFragment.this, list);
            dismissAllowingStateLoss();
        }
    }
}

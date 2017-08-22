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
import com.pokevian.app.smartfleet.model.VehicleModelList;
import com.pokevian.app.smartfleet.request.GetVehicleModelListRequest;
import com.pokevian.app.smartfleet.request.GetVehicleTypeListRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

public class LoadVehicleModelDialogFragment extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "LoadVehicleModelDialogFragment";

    private String mMakerCode;
    private String mTypeCode;
    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private LoadVehicleModelCallbacks mCallbacks;

    public static LoadVehicleModelDialogFragment newInstance(String makerCode, String typeCode) {
        LoadVehicleModelDialogFragment fragment = new LoadVehicleModelDialogFragment();
        Bundle args = new Bundle();
        args.putString("maker_code", makerCode);
        args.putString("type_code", typeCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (LoadVehicleModelCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement LoadVehicleModelCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (LoadVehicleModelCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadVehicleModelCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mMakerCode = args.getString("maker_code");
            mTypeCode = args.getString("type_code");
        } else {
            mMakerCode = savedInstanceState.getString("maker_code");
            mTypeCode = savedInstanceState.getString("type_code");
        }
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("maker_code", mMakerCode);
        outState.putString("type_code", mTypeCode);

        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new WaitForDialog(getActivity());
        setCancelable(false);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        GetVehicleModelListRequest request = new GetVehicleModelListRequest(mMakerCode, mTypeCode,
                new GetVehicleModelListListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-vehicle-model-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                GetVehicleModelListRequest request = new GetVehicleModelListRequest(mMakerCode, mTypeCode,
                        new GetVehicleModelListListener());
                request.setTag(TAG);
                mRequestQueue.add(request);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallbacks.onLoadVehicleModelFailure();
                dismiss();
            }
        }
    }

    private class GetVehicleModelListListener extends VolleyListener<VehicleModelList> {

        @Override
        public void onResponse(VehicleModelList modelList) {
            mCallbacks.onLoadVehicleModelSuccess(modelList);
            dismiss();
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (error instanceof ServerError) {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_server_error),
                        getString(R.string.dialog_message_server_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-vehicle-model-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-vehicle-model-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

    }

    public interface LoadVehicleModelCallbacks {
        void onLoadVehicleModelSuccess(VehicleModelList modelList);

        void onLoadVehicleModelFailure();
    }

}

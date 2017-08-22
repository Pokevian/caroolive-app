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
import com.pokevian.app.smartfleet.request.GetVehicleFuelListRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

public class LoadVehicleFuelDialogFragment extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "LoadVehicleFuelDialogFragment";

    private String mMakerCode;
    private String mModelCode;
    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private LoadVehicleFuelCallbacks mCallbacks;

    public static LoadVehicleFuelDialogFragment newInstance(String makerCode, String modelCode) {
        LoadVehicleFuelDialogFragment fragment = new LoadVehicleFuelDialogFragment();
        Bundle args = new Bundle();
        args.putString("maker_code", makerCode);
        args.putString("model_code", modelCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (LoadVehicleFuelCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement LoadVehicleFuelCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (LoadVehicleFuelCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadVehicleFuelCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mMakerCode = args.getString("maker_code");
            mModelCode = args.getString("model_code");
        } else {
            mMakerCode = savedInstanceState.getString("maker_code");
            mModelCode = savedInstanceState.getString("model_code");
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
        outState.putString("model_code", mModelCode);

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

        GetVehicleFuelListRequest request = new GetVehicleFuelListRequest(mMakerCode, mModelCode,
                new GetVehicleFuelListListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-vehicle-fuel-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                GetVehicleFuelListRequest request = new GetVehicleFuelListRequest(mMakerCode, mModelCode,
                        new GetVehicleFuelListListener());
                request.setTag(TAG);
                mRequestQueue.add(request);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallbacks.onLoadVehicleFuelFailure();
                dismiss();
            }
        }
    }

    private class GetVehicleFuelListListener extends VolleyListener<VehicleModelList> {

        @Override
        public void onResponse(VehicleModelList modelList) {
            mCallbacks.onLoadVehicleFuelSuccess(modelList);
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
                fm.beginTransaction().add(fragment, "load-vehicle-fuel-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-vehicle-fuel-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

    }

    public interface LoadVehicleFuelCallbacks {
        void onLoadVehicleFuelSuccess(VehicleModelList modelList);

        void onLoadVehicleFuelFailure();
    }

}

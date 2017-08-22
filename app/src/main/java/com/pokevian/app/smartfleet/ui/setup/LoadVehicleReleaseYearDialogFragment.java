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
import com.pokevian.app.smartfleet.request.GetVehicleDisplacementListRequest;
import com.pokevian.app.smartfleet.request.GetVehicleReleaseYearListRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

public class LoadVehicleReleaseYearDialogFragment extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "LoadVehicleReleaseYearDialogFragment";

    private String mMakerCode;
    private String mModelCode;
    private String mFuelCode;
    private int mDisplacement;
    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private LoadVehicleReleaseYearCallbacks mCallbacks;

    public static LoadVehicleReleaseYearDialogFragment newInstance(String makerCode, String modelCode, String fuelCode, int displacement) {
        LoadVehicleReleaseYearDialogFragment fragment = new LoadVehicleReleaseYearDialogFragment();
        Bundle args = new Bundle();
        args.putString("maker_code", makerCode);
        args.putString("model_code", modelCode);
        args.putString("fuel_code", fuelCode);
        args.putInt("eng_disp", displacement);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (LoadVehicleReleaseYearCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement LoadVehicleReleaseYearCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (LoadVehicleReleaseYearCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadVehicleReleaseYearCallbacks");
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
            mFuelCode = args.getString("fuel_code");
            mDisplacement = args.getInt("eng_disp");
        } else {
            mMakerCode = savedInstanceState.getString("maker_code");
            mModelCode = savedInstanceState.getString("model_code");
            mFuelCode = savedInstanceState.getString("fuel_code");
            mDisplacement = savedInstanceState.getInt("eng_disp");
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
        outState.putString("fuel_code", mFuelCode);
        outState.putInt("eng_disp", mDisplacement);

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

        GetVehicleReleaseYearListRequest request = new GetVehicleReleaseYearListRequest(
                mMakerCode, mModelCode, mFuelCode, mDisplacement, new GetVehicleReleaseYearListListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-vehicle-release-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                GetVehicleDisplacementListRequest request = new GetVehicleDisplacementListRequest(
                        mMakerCode, mModelCode, mFuelCode, new GetVehicleReleaseYearListListener());
                request.setTag(TAG);
                mRequestQueue.add(request);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallbacks.onLoadVehicleReleaseYearFailure();
                dismiss();
            }
        }
    }

    private class GetVehicleReleaseYearListListener extends VolleyListener<VehicleModelList> {

        @Override
        public void onResponse(VehicleModelList modelList) {
            mCallbacks.onLoadVehicleReleaseYearSuccess(modelList);
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
                fm.beginTransaction().add(fragment, "load-vehicle-release-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-vehicle-release-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

    }

    public interface LoadVehicleReleaseYearCallbacks {
        void onLoadVehicleReleaseYearSuccess(VehicleModelList modelList);

        void onLoadVehicleReleaseYearFailure();
    }

}

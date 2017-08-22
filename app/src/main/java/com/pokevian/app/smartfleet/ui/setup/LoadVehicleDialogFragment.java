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
import com.pokevian.app.smartfleet.model.VehicleList;
import com.pokevian.app.smartfleet.request.GetVehicleListRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

public class LoadVehicleDialogFragment extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "LoadVehicleDialogFragment";

    private String mAccountId;
    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private LoadVehicleDialogCallbacks mCallbacks;

    public static LoadVehicleDialogFragment newInstance() {
        LoadVehicleDialogFragment fragment = new LoadVehicleDialogFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (LoadVehicleDialogCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement LoadVehicleDialogCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (LoadVehicleDialogCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement LoadVehicleDialogCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsStore settingsStore = SettingsStore.getInstance();
        mAccountId = settingsStore.getAccountId();
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

        GetVehicleListRequest request = new GetVehicleListRequest(mAccountId,
                new GetVehicleListListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("load-vehicle-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                GetVehicleListRequest request = new GetVehicleListRequest(mAccountId,
                        new GetVehicleListListener());
                request.setTag(TAG);
                mRequestQueue.add(request);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallbacks.onLoadVehicleFailure();
                dismiss();
            }
        }
    }

    private class GetVehicleListListener extends VolleyListener<VehicleList> {

        @Override
        public void onResponse(VehicleList vehicleList) {
            mCallbacks.onLoadVehicleSuccess(vehicleList);
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
                fm.beginTransaction().add(fragment, "load-vehicle-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "load-vehicle-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

    }

    public interface LoadVehicleDialogCallbacks {
        void onLoadVehicleSuccess(VehicleList list);
        void onLoadVehicleFailure();
    }

}

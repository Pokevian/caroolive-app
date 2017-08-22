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
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.request.UpdateVehicleRequest;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;
import com.pokevian.app.smartfleet.ui.common.WaitForDialog;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

public class UpdateVehicleFragmentNew extends DialogFragment implements AlertDialogCallbacks {

    public static final String TAG = "update-vehicle-dialog";

    private String mAccountId;
    private Vehicle mVehicle;

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();
    private UpdateVehicleCallbacks mCallbacks;

    public static UpdateVehicleFragmentNew newInstance(String accountId, Vehicle vehicle) {
        UpdateVehicleFragmentNew fragment = new UpdateVehicleFragmentNew();
        Bundle args = new Bundle();
        args.putString("account_id", accountId);
        args.putSerializable("vehicle", vehicle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (UpdateVehicleCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement UpdateOdometerCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (UpdateVehicleCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement UpdateOdometerCallbacks");
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mAccountId = args.getString("account_id");
            mVehicle = (Vehicle) args.getSerializable("vehicle");
        } else {
            mAccountId = savedInstanceState.getString("account_id");
            mVehicle = (Vehicle) savedInstanceState.getSerializable("vehicle");
        }
    }

    @Override
    public void onDestroy() {
        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("account_id", mAccountId);
        outState.putSerializable("vehicle", mVehicle);

        super.onSaveInstanceState(outState);
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

        UpdateVehicleRequest request = new UpdateVehicleRequest(mAccountId, mVehicle,
                new UpdateVehicleListener());
        request.setTag(TAG);
        mRequestQueue.add(request);
    }

    @Override
    public void onDialogButtonClick(DialogFragment fragment, int which) {
        String tag = fragment.getTag();

        if ("update-vehicle-failure-dialog".equals(tag)) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                UpdateVehicleRequest request = new UpdateVehicleRequest(mAccountId, mVehicle,
                        new UpdateVehicleListener());
                request.setTag(TAG);
                mRequestQueue.add(request);
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                mCallbacks.onUpdateVehiclerFailure(UpdateVehicleFragmentNew.this, mVehicle);
                dismiss();
            }
        }
    }

    private class UpdateVehicleListener extends VolleyListener<Void> {

        @Override
        public void onResponse(Void unused) {
            mCallbacks.onUpdateVehicleSuccess(UpdateVehicleFragmentNew.this, mVehicle);
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
                fm.beginTransaction().add(fragment, "update-vehicle-failure-dialog")
                        .commitAllowingStateLoss();
            } else {
                FragmentManager fm = getChildFragmentManager();
                DialogFragment fragment = AlertDialogFragment.newInstance(
                        getString(R.string.dialog_title_network_error),
                        getString(R.string.dialog_message_network_error),
                        getString(R.string.btn_no),
                        getString(R.string.btn_yes));
                fm.beginTransaction().add(fragment, "update-vehicle-failure-dialog")
                        .commitAllowingStateLoss();
            }
        }

    }

    public interface UpdateVehicleCallbacks {
        void onUpdateVehicleSuccess(DialogFragment fragment, Vehicle vehicle);

        void onUpdateVehiclerFailure(DialogFragment fragment, Vehicle vehicle);
    }

}

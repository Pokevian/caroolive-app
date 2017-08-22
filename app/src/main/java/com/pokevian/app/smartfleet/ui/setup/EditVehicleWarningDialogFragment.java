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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.common.AlertDialogFragment.AlertDialogCallbacks;

public class EditVehicleWarningDialogFragment extends DialogFragment {

    public static final String TAG = "LogoutWarningDialogFragment";

    private AlertDialogCallbacks mCallbacks;

    public static EditVehicleWarningDialogFragment newInstance() {
        EditVehicleWarningDialogFragment fragment = new EditVehicleWarningDialogFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (AlertDialogCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (AlertDialogCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement DialogFragmentCallbacks");
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_title_edit_vehicle)
                .setMessage(R.string.dialog_message_edit_vehicle)
                .setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(EditVehicleWarningDialogFragment.this, which);
                    }
                })
                .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mCallbacks.onDialogButtonClick(EditVehicleWarningDialogFragment.this, which);
                    }
                })
                .create();
    }

}

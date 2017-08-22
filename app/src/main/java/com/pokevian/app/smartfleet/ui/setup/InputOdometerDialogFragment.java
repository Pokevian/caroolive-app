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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.pokevian.app.smartfleet.R;

public class InputOdometerDialogFragment extends DialogFragment {

    public static final String TAG = "Input-odometer";

    private EditText mEdit;
    private InputOdometerCallbacks mCallbacks;

    public static InputOdometerDialogFragment newInstance() {
        InputOdometerDialogFragment fragment = new InputOdometerDialogFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (InputOdometerCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement InputOdometerCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (InputOdometerCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement InputOdometerCallbacks");
            }
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_input_odometer, null);

        mEdit = (EditText) view.findViewById(R.id.edit);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_title_input_odometer)
                .setView(view)
                .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);

                        int odometer = 0;
                        try {
                            String s = mEdit.getText().toString().trim();
                            odometer = Integer.valueOf(s);
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage());
                        }
                        mCallbacks.onOdometerInput(odometer);
                    }
                })
                .create();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mCallbacks.onOdometerInput(0);
    }

    public interface InputOdometerCallbacks {
        void onOdometerInput(int odometer);
    }

}

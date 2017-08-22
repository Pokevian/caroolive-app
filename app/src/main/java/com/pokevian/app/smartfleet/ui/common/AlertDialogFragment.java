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

package com.pokevian.app.smartfleet.ui.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.pokevian.app.smartfleet.R;

public class AlertDialogFragment extends DialogFragment {

    private AlertDialogCallbacks mCallbacks;

    private boolean mHasNegativeButton;
    private boolean mHasNeutralButton;

    public static AlertDialogFragment newInstance(String title, String message,
                                                  String negativeButtonText, String positiveButtonText, String neutralButtonText) {
        AlertDialogFragment fragment = new AlertDialogFragment();

        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putString("negative_button_text", negativeButtonText);
        args.putString("positive_button_text", positiveButtonText);
        args.putString("neutral_button_text", neutralButtonText);
        fragment.setArguments(args);

        return fragment;
    }

    public static AlertDialogFragment newInstance(String title, String message,
                                                  String negativeButtonText, String positiveButtonText) {
        return newInstance(title, message, negativeButtonText, positiveButtonText, null);
    }

    public static AlertDialogFragment newInstance(String title, String message,
                                                  String neutralButtonText) {
        return newInstance(title, message, null, null, neutralButtonText);
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
    public void onSaveInstanceState(Bundle outState) {
        Bundle args = getArguments();
        outState.putAll(args);

        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = null;
        String message = null;
        String negativeButtonText = null;
        String positiveButtonText = null;
        String neutralButtonText = null;

        if (savedInstanceState == null) {
            Bundle args = getArguments();
            title = args.getString("title");
            message = args.getString("message");
            negativeButtonText = args.getString("negative_button_text");
            positiveButtonText = args.getString("positive_button_text");
            neutralButtonText = args.getString("neutral_button_text");
        } else {
            title = savedInstanceState.getString("title");
            message = savedInstanceState.getString("message");
            negativeButtonText = savedInstanceState.getString("negative_button_text");
            positiveButtonText = savedInstanceState.getString("positive_button_text");
            neutralButtonText = savedInstanceState.getString("neutral_button_text");
        }

        mHasNegativeButton = !TextUtils.isEmpty(negativeButtonText);
        boolean hasPositiveButton = !TextUtils.isEmpty(positiveButtonText);
        mHasNeutralButton = !TextUtils.isEmpty(neutralButtonText);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()/*, R.style.AppTheme_Light_Dialog*/)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false);
        if (mHasNegativeButton) {
            builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onDialogButtonClick(AlertDialogFragment.this, which);
                }
            });
        }
        if (hasPositiveButton) {
            builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onDialogButtonClick(AlertDialogFragment.this, which);
                }
            });
        }
        if (mHasNeutralButton) {
            builder.setNeutralButton(neutralButtonText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mCallbacks.onDialogButtonClick(AlertDialogFragment.this, which);
                }
            });
        }

        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;

    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        if (mHasNegativeButton) {
            mCallbacks.onDialogButtonClick(AlertDialogFragment.this, DialogInterface.BUTTON_NEGATIVE);
        } else if (mHasNeutralButton) {
            mCallbacks.onDialogButtonClick(AlertDialogFragment.this, DialogInterface.BUTTON_NEUTRAL);
        }
    }


    public interface AlertDialogCallbacks {
        void onDialogButtonClick(DialogFragment fragment, int which);
    }

}

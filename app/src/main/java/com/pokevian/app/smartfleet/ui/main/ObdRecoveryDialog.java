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

package com.pokevian.app.smartfleet.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;

import com.pokevian.app.smartfleet.R;

import org.apache.log4j.Logger;

public class ObdRecoveryDialog {

    static final String TAG = "ObdRecoveryDialog";
    final Logger log = Logger.getLogger(TAG);

    private final Context mContext;
    private AlertDialog mDialog;
    private ObdRecoveryDialogCallbacks mCallbacks;

    public ObdRecoveryDialog(Activity activity) {
        mContext = activity;

        try {
            mCallbacks = (ObdRecoveryDialogCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement ObdRecoveryDialogCallbacks");
        }
    }

    public void show() {
        initDialog();

        mDialog.show();
    }

    public void dismiss() {
        mDialog.dismiss();
    }

    private void initDialog() {
        mDialog = new AlertDialog.Builder(mContext, R.style.AppTheme_Dialog_System).create();

        mDialog.setMessage(mContext.getString(R.string.dialog_message_obd_recovery));
        mDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.btn_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        mDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.btn_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        log.info("User disable bluetooth!");

                        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                        btAdapter.disable();
                        // NOTE: BT will be enabled in the BluetoothCongtrolFragment
                    }
                });

        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mCallbacks.onObdRecoveryDialogDismiss();
            }
        });
    }

    public static interface ObdRecoveryDialogCallbacks {
        public void onObdRecoveryBluetoothDisabled();

        public void onObdRecoveryDialogDismiss();
    }

}

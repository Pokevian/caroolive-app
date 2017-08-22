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
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.apache.log4j.Logger;

public class BluetoothControlFragment extends Fragment {

    public static final String TAG = "BluetoothControlFragment";
    final Logger logger = Logger.getLogger(TAG);

    private BluetoothAdapter mBtAdapter;
    private BroadcastReceiver mBtAdapterReceiver;
    private BluetoothControlCallbacks mCallbacks;

    private Dialog mDialog;

    public static BluetoothControlFragment newInstance() {
        BluetoothControlFragment fragment = new BluetoothControlFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (BluetoothControlCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement BluetoothControlCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (BluetoothControlCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement BluetoothControlCallbacks");
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        int state = mBtAdapter.getState();
        if (state == BluetoothAdapter.STATE_TURNING_OFF) {
            logger.debug("Bluetooth state=TURNING_OFF");
            mBtAdapterReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_OFF) {
                        mBtAdapter.enable();
                    } else if (state == BluetoothAdapter.STATE_ON) {
                        onBluetoothTurnedOn(true);
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            };
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBtAdapterReceiver, filter);

            mDialog = createDialog();
            mDialog.show();
        } else if (state == BluetoothAdapter.STATE_OFF) {
            logger.debug("Bluetooth state=OFF");
            mBtAdapterReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_ON) {
                        onBluetoothTurnedOn(true);
                        mDialog.dismiss();
                        mDialog = null;
                    }
                }
            };
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBtAdapterReceiver, filter);
            mBtAdapter.enable();

            mDialog = createDialog();
            mDialog.show();
        } else if (state == BluetoothAdapter.STATE_ON) {
            logger.debug("Bluetooth state=ON");
            onBluetoothTurnedOn(false);
        }
    }

    @Override
    public void onDestroy() {
        if (mBtAdapterReceiver != null) {
            getActivity().unregisterReceiver(mBtAdapterReceiver);
        }

        super.onDestroy();
    }

    private void onBluetoothTurnedOn(boolean isEnabled) {
        // Callback
        mCallbacks.onBluetoothTurnedOn(isEnabled);

        if (mBtAdapterReceiver != null) {
            getActivity().unregisterReceiver(mBtAdapterReceiver);
            mBtAdapterReceiver = null;
        }
    }

    private Dialog createDialog() {
        Dialog dialog = new WaitForDialog(getActivity());
        dialog.setCancelable(false);
        return dialog;
    }

    public interface BluetoothControlCallbacks {
        void onBluetoothTurnedOn(boolean isEnabled);
    }

}

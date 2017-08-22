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

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class BtConnectionStore {

    private static final String PREFS = "bt-connection-store";
    private static final String PREF_BT_ENABLED = "bt_enabled";
    private static final String PREF_MY_BT_DEVICE = "my_bt_device";
    private static final String PREF_CONNECTED_BT_DEVICES = "connected_bt_devices";

    private final SharedPreferences mPrefs;

    private static BtConnectionStore mInstance;

    private BtConnectionStore(Context context) {
        mPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static BtConnectionStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BtConnectionStore(context);
        }
        return mInstance;
    }

    public void storeBtEnabled(boolean btEnabled) {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(PREF_BT_ENABLED, btEnabled);
        editor.apply();
    }

    public boolean isBtEnabled() {
        return mPrefs.getBoolean(PREF_BT_ENABLED, false);
    }

    public void addConnectedBtDevice(BluetoothDevice device) {
        if (device != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            Set<String> devices = mPrefs.getStringSet(PREF_CONNECTED_BT_DEVICES, new HashSet<String>());
            devices.add(getDeviceString(device));
            editor.putStringSet(PREF_CONNECTED_BT_DEVICES, devices);
            editor.apply();
        }
    }

    public void removeConnectedBtDevices(BluetoothDevice device) {
        if (device != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            Set<String> devices = mPrefs.getStringSet(PREF_CONNECTED_BT_DEVICES, new HashSet<String>());
            devices.remove(getDeviceString(device));
            editor.putStringSet(PREF_CONNECTED_BT_DEVICES, devices);
            editor.apply();
        }
    }

    public Set<String> getConnectedBtDevices() {
        return mPrefs.getStringSet(PREF_CONNECTED_BT_DEVICES, new HashSet<String>());
    }

    public void storeMyDevice(BluetoothDevice device) {
        if (device != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(PREF_MY_BT_DEVICE, getDeviceString(device));
            editor.apply();
        }
    }

    public String getMyBtDevice() {
        return mPrefs.getString(PREF_MY_BT_DEVICE, null);
    }

    public void reset() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();
        editor.apply();
    }

    private static String getDeviceString(BluetoothDevice device) {
        return device.getName() + "@" + device.getAddress();
    }

}

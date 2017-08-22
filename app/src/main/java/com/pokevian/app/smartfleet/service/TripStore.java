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

package com.pokevian.app.smartfleet.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pokevian.app.smartfleet.model.VehicleData.VehicleTrip;
import com.pokevian.app.smartfleet.volley.GsonRequest.GsonDateTypeAdapter;

import java.sql.Date;

@SuppressWarnings("unused")
public class TripStore {

    private final String PREFS_NAME = "trip-store";
    private final String PREF_TRIP = "trip";

    private final SharedPreferences mPrefs;
    private final Gson mGson;

    private static TripStore mInstance;

    private TripStore(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mGson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
                .create();
    }

    public static TripStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TripStore(context);
        }
        return mInstance;
    }

    public void storeTrip(VehicleTrip trip) {
        SharedPreferences.Editor editor = mPrefs.edit();
        String value = mGson.toJson(trip);
        editor.putString(PREF_TRIP, value);
        editor.apply();
    }

    public VehicleTrip getTrip() {
        String value = mPrefs.getString(PREF_TRIP, null);
        if (!TextUtils.isEmpty(value)) {
            return mGson.fromJson(value, VehicleTrip.class);
        } else {
            return null;
        }
    }

    public boolean hasTrip() {
        return mPrefs.contains(PREF_TRIP);
    }

    public void clear() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.clear();
        editor.apply();
    }

}

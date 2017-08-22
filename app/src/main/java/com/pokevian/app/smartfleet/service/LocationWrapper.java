/*
 * Copyright (c) 2015. Pokevian Ltd.
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
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;


import java.util.ArrayList;

public class LocationWrapper implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String TAG = "obd-location";

    public static int MIN_ACCURACY = 50; // [meters]
    public static int INTERVAL = 1000; // [seconds]
    public static int FASTEST_INTERVAL = 1000; // [seconds]

    private Context mContext;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest REQUEST;
    private final ArrayList<OnLocationChangedListener> mOnLocationChangedListeners =
            new ArrayList<OnLocationChangedListener>();
    private boolean mAccuracyFilterEnabled = false;
    private int mMinAccuracy = MIN_ACCURACY;
    private boolean mRequestLocationUpdates = false;
    private Object LOCK = new Object();

    public LocationWrapper(Context context) {
        mContext = context;
    }

    public void setAccuracyFilterEnabled(boolean enabled, int minAccuracy) {
        mAccuracyFilterEnabled = enabled;
        mMinAccuracy = minAccuracy;
    }

    public void requestUpdates() {
        synchronized (LOCK) {
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();

                REQUEST = LocationRequest.create()
                        .setInterval(INTERVAL)
                        .setFastestInterval(FASTEST_INTERVAL)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                mRequestLocationUpdates = true;
                mGoogleApiClient.connect();
            }
        }
    }

    public void cancelUpdates() {
        synchronized (LOCK) {
            if (mGoogleApiClient != null) {
                mRequestLocationUpdates = false;
                if (mGoogleApiClient.isConnected()) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
                }
                mGoogleApiClient.disconnect();
                mGoogleApiClient = null;
            }
        }
    }

    public void registerOnLocationChangedListener(OnLocationChangedListener listener) {
        synchronized (mOnLocationChangedListeners) {
            mOnLocationChangedListeners.add(listener);
        }
    }

    public void unregisterOnLocationChangedListener(OnLocationChangedListener listener) {
        synchronized (mOnLocationChangedListeners) {
            mOnLocationChangedListeners.remove(listener);
        }
    }

    /**
     * Callback called when connected to GCore. Implementation of {@link com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks}.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        synchronized (LOCK) {
            if (mGoogleApiClient != null) {
                if (mRequestLocationUpdates) {
                    Log.d(TAG, "onConnected(): request location updates");
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, REQUEST, this);
                    mRequestLocationUpdates = false;
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Called when the client is temporarily in a disconnected state.
        Log.d(TAG, "onConnectionFailed(): cause=" + cause);
    }

    /**
     * Implementation of {@link com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener}.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed(): result=" + result);
    }

    /**
     * Implementation of {@link com.google.android.gms.location.LocationListener}.
     */
    @Override
    public void onLocationChanged(Location location) {
        if (mAccuracyFilterEnabled) {
            Log.d(TAG, ">> " + location.getAccuracy() + ": " + mMinAccuracy);
            if (!location.hasAccuracy()) {
                Log.w(TAG, "No accuracy -> ignore");
                return;
            } else if (location.getAccuracy() > mMinAccuracy) {
                Log.w(TAG, "Too low accuracy: " + location.getAccuracy() + " -> ignore");
                return;
            }
            Log.d(TAG, location.toString());
        }

        if (location.getTime() == 0) {
            location.setTime(System.currentTimeMillis());
        }

        synchronized (mOnLocationChangedListeners) {
            for (OnLocationChangedListener listener : mOnLocationChangedListeners) {
                listener.onLocationChanged(location);
            }
        }
    }

}

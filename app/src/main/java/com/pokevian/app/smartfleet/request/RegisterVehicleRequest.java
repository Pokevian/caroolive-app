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

package com.pokevian.app.smartfleet.request;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.caroo.common.model.TwoState;
import com.pokevian.caroo.common.smart.model.SmartCar;
import com.pokevian.caroo.common.smart.model.SmartResponse;

public class RegisterVehicleRequest {

    private final RequestQueue mRequestQueue;
    private final String mAccountId;
    private final Vehicle mVehicle;
    private final String mInactiveVehicleId;
    private final VolleyListener<String> mListener;

    private String mTag;

    public RegisterVehicleRequest(RequestQueue requestQueue, String accountId, Vehicle vehicle,
                                  String inactiveVehicleId, final VolleyListener<String> listener) {
        mRequestQueue = requestQueue;
        mAccountId = accountId;
        mVehicle = vehicle;
        mInactiveVehicleId = inactiveVehicleId;
        mListener = listener;
    }

    public void request(String tag) {
        mTag = tag;

        if (mInactiveVehicleId != null) {
            requestInactivate();
        } else {
            requestRegister();
        }
    }

    private void requestInactivate() {
        GsonRequest<SmartResponse> request = new GsonRequest<>(
                ServerUrl.DATA_API, SmartResponse.class, new ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                mListener.onErrorResponse(error);
            }
        });

        SmartCar data = new SmartCar();
        data.setMemberNo(mAccountId);
        data.setCarNo(mInactiveVehicleId);
        data.setActiveYn(TwoState.N);

        VolleyParams params = new VolleyParams();
        params.put("cmd", "updateCar");
        params.put("data", request.getGson().toJson(data));
        request.setParams(params);

        request.setListener(new Listener<SmartResponse>() {
            public void onResponse(SmartResponse response) {
                if (SmartResponse.RESULT_OK.equals(response.getResult())) {
                    requestRegister();
                } else {
                    mListener.onErrorResponse(new ServerError());
                }
            }
        });

        request.setTag(mTag);

        mRequestQueue.add(request);
    }

    private void requestRegister() {
        GsonRequest<SmartResponse> request = new GsonRequest<>(
                ServerUrl.DATA_API, SmartResponse.class, new ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                mListener.onErrorResponse(error);
            }
        });

        SmartCar data = mVehicle.toServerBean(mAccountId);
        VolleyParams params = new VolleyParams();
        params.put("cmd", "insertCar");
        params.put("data", request.getGson().toJson(data));
        request.setParams(params);

        request.setListener(new Listener<SmartResponse>() {
            public void onResponse(SmartResponse response) {
                if (SmartResponse.RESULT_OK.equals(response.getResult())) {
                    mListener.onResponse(response.getCarNo());
                } else {
                    mListener.onErrorResponse(new ServerError());
                }
            }
        });

        request.setTag(mTag);

        mRequestQueue.add(request);
    }

}

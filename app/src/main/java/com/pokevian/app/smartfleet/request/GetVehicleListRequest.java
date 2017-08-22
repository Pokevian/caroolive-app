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

import com.android.volley.Response.Listener;
import com.android.volley.ServerError;
import com.pokevian.app.smartfleet.model.Vehicle;
import com.pokevian.app.smartfleet.model.VehicleList;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.caroo.common.smart.model.SmartCar;
import com.pokevian.caroo.common.smart.model.SmartMemberCarList;
import com.pokevian.caroo.common.smart.model.SmartResponse;

import java.util.ArrayList;
import java.util.List;

public class GetVehicleListRequest extends GsonRequest<SmartMemberCarList> {

    public GetVehicleListRequest(String accountId, final VolleyListener<VehicleList> listener) {
        super(ServerUrl.DATA_API, SmartMemberCarList.class, listener);

        SmartMemberCarList data = new SmartMemberCarList();
        /*data.setLocale(Locale.getDefault());*/
        data.setMemberNo(accountId);

        VolleyParams params = new VolleyParams();
        params.put("cmd", "getMemberCarList");
        params.put("data", getGson().toJson(data));
        setParams(params);

        setListener(new Listener<SmartMemberCarList>() {
            public void onResponse(SmartMemberCarList response) {
                if (SmartResponse.RESULT_OK.equals(response.getResponse().getResult())) {
                    VehicleList vehicleList = new VehicleList();
                    ArrayList<Vehicle> vehicles = new ArrayList<>();
                    vehicleList.setList(vehicles);

                    List<SmartCar> scList = response.getCarList();
                    if (scList != null && !scList.isEmpty()) {
                        for (SmartCar sc : scList) {
//                            if (sc.getActiveYn() == TwoState.Y) {
//                                vehicles.add(new Vehicle(sc));
//                            }
                            vehicles.add(new Vehicle(sc));
                        }
                    }

                    listener.onResponse(vehicleList);
                } else {
                    listener.onErrorResponse(new ServerError());
                }
            }
        });
    }

}

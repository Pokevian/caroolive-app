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
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.caroo.common.smart.model.SmartMember;
import com.pokevian.caroo.common.smart.model.SmartResponse;

public class WithdrawalRequest extends GsonRequest<SmartResponse> {

    public WithdrawalRequest(String memberNumber, final VolleyListener<Boolean> listener) {
        super(ServerUrl.DATA_API, SmartResponse.class, listener);

        // set parameters
        SmartMember data = new SmartMember();
        data.setMemberNo(memberNumber);

        VolleyParams params = new VolleyParams();
        params.put("cmd", "withdrawMember");
        params.put("data", getGson().toJson(data));
        setParams(params);

        setListener(new Listener<SmartResponse>() {
            public void onResponse(SmartResponse response) {
                if (SmartResponse.RESULT_OK.equals(response.getResult())) {
                    listener.onResponse(true);
                } else {
                    listener.onResponse(false);
                }
            }
        });
    }

}

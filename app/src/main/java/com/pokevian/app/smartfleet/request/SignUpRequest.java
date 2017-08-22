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

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.ServerError;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.caroo.common.smart.model.SmartMember;
import com.pokevian.caroo.common.smart.model.SmartResponse;

public class SignUpRequest extends GsonRequest<SmartResponse> {

    private Account mAccount;

    public SignUpRequest(Account account, final VolleyListener<String> listener) {
        super(ServerUrl.DATA_API, SmartResponse.class, listener);

        mAccount = account;

        SmartMember data = account.toServerBean();
        VolleyParams params = new VolleyParams();
        params.put("cmd", "regMember");
        params.put("data", getGson().toJson(data));
        setParams(params);

        setListener(new Listener<SmartResponse>() {
            public void onResponse(SmartResponse response) {
                if (SmartResponse.RESULT_OK.equals(response.getResult())) {
                    listener.onResponse(response.getMemberNo());
                } else {
                    listener.onErrorResponse(new ServerError());
                }
            }
        });
    }

    @Override
    protected Response<SmartResponse> parseNetworkResponse(
            NetworkResponse response) {
        Response<SmartResponse> parsed = super.parseNetworkResponse(response);

        if (parsed.isSuccess()) {
            try {
                SignInRequest.sessionLogin(getGson(), mAccount.getLoginId());
            } catch (Exception e) {
                return Response.error(new ServerError());
            }
        }

        return parsed;
    }

}

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
import com.pokevian.app.smartfleet.model.Code;
import com.pokevian.app.smartfleet.model.CodeList;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.caroo.common.smart.model.SmartCode;
import com.pokevian.caroo.common.smart.model.SmartCodeList;
import com.pokevian.caroo.common.smart.model.SmartResponse;

import java.util.ArrayList;
import java.util.List;

public class GetRegionCodeListRequest extends GsonRequest<SmartCodeList> {

    public GetRegionCodeListRequest(String countryCode, final VolleyListener<CodeList> listener) {
        super(ServerUrl.DATA_API, SmartCodeList.class, listener);

        SmartCodeList data = new SmartCodeList();
        /*data.setLocale(Locale.getDefault());*/
        data.setGrpCd("Local" + countryCode);

        VolleyParams params = new VolleyParams();
        params.put("cmd", "getCodeList");
        params.put("data", getGson().toJson(data));
        setParams(params);

        setListener(new Listener<SmartCodeList>() {
            public void onResponse(SmartCodeList response) {
                if (SmartResponse.RESULT_OK.equals(response.getResponse().getResult())) {
                    CodeList codeList = new CodeList();
                    ArrayList<Code> codes = new ArrayList<>();
                    codeList.setList(codes);

                    List<SmartCode> scList = response.getSmartCodeList();
                    for (SmartCode sc : scList) {
                        Code code = new Code(sc);
                        codes.add(code);
                    }

                    listener.onResponse(codeList);
                } else {
                    listener.onErrorResponse(new ServerError());
                }
            }
        });

    }

}

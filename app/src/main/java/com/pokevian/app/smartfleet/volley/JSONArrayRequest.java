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

package com.pokevian.app.smartfleet.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Map;


class JSONArrayRequest extends Request<JSONArray> {

    private static final int SOCKET_TIMEOUT = 5000;
    private static final int MAX_RETRY_COUNT = 2;
    private static final float BACKOFF_MULTIPLIER = 1.5f;

    protected Response.Listener<JSONArray> listener;
    private VolleyParams params;

    public JSONArrayRequest(int method, String url, final VolleyListener<JSONArray> listener) {
        super(method, url, listener);
        this.listener = listener;

        setRetryPolicy(new DefaultRetryPolicy(
                SOCKET_TIMEOUT,
                MAX_RETRY_COUNT,
                BACKOFF_MULTIPLIER));
    }

    public void setParams(VolleyParams params) {
        this.params = params;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return params;
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            Logger.getLogger("volley").error(e.getMessage());
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            Logger.getLogger("volley").error(je.getMessage());
            return Response.error(new ParseError(je));
        }
    }

    @Override
    protected void deliverResponse(JSONArray response) {
        listener.onResponse(response.isNull(0) ? null : response);
    }

}

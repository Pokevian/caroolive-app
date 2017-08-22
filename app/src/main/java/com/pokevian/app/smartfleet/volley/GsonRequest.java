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
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Volley adapter for JSON requests that will be parsed into Java objects by Gson.
 */
public class GsonRequest<T> extends Request<T> {

    static final String TAG = "GsonRequest";
    final Logger logger = Logger.getLogger(TAG);

    private static final int SOCKET_TIMEOUT = 5000;
    private static final int MAX_RETRY_COUNT = 2;
    private static final float BACKOFF_MULTIPLIER = 1.5f;

    protected final Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonDateTypeAdapter())
            .create();
    private Class<T> mClazz;
    private Listener<T> mListener;
    private VolleyHeaders mHeaders;
    private VolleyParams mParams;

    /**
     * Make a POST request and return a parsed object from JSON.
     *
     * @param url           URL of the request to make
     * @param clazz         Relevant class object, for Gson's reflection
     * @param errorListener error listener
     */
    public GsonRequest(String url, Class<T> clazz, ErrorListener errorListener) {
        super(Method.POST, url, errorListener);

        mClazz = clazz;

        setRetryPolicy(new DefaultRetryPolicy(
                SOCKET_TIMEOUT,
                MAX_RETRY_COUNT,
                BACKOFF_MULTIPLIER));
    }

    public Gson getGson() {
        return mGson;
    }

    public void setListener(Listener<T> listener) {
        mListener = listener;
    }

    public void setHeaders(VolleyHeaders headers) {
        mHeaders = headers;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return (mHeaders != null) ? mHeaders : super.getHeaders();
    }

    public void setParams(VolleyParams params) {
        mParams = params;
        logger.trace(getUrl() + ": " + mParams);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            logger.trace(getUrl() + ": " + json);

            return Response.success(mGson.fromJson(json, mClazz), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            logger.error(getUrl() + ": " + e.getMessage(), e);
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            logger.error(getUrl() + ": " + e.getMessage(), e);
            return Response.error(new ParseError(e));
        }
    }

    public static class GsonDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

        private final SimpleDateFormat mFormatter;

        public GsonDateTypeAdapter() {
            mFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            /*mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));*/
        }

        @Override
        public JsonElement serialize(Date date, Type type, JsonSerializationContext context) {
            synchronized (mFormatter) {
                return new JsonPrimitive(mFormatter.format(date));
            }
        }

        @Override
        public Date deserialize(JsonElement element, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            synchronized (mFormatter) {
                try {
                    return mFormatter.parse(element.getAsString());
                } catch (ParseException e) {
                    return null;
                }
            }
        }

    }
}

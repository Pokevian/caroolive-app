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

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.google.api.client.util.SslUtils;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public final class VolleySingleton {

    private static VolleySingleton mInstance = null;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private RequestQueue mRequestQueueForCookie;
    private CookieStore mCookieStore;

    public static void init(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
    }

    public static VolleySingleton getInstance() {
        if (mInstance == null) {
            throw new RuntimeException("Not initialized!");
        }
        return mInstance;
    }

    private VolleySingleton(Context context) {
        // Create webkit cookie instance
        CookieSyncManager.createInstance(context);

        mRequestQueue = Volley.newRequestQueue(context);
        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap> mCache = new LruCache<>(10);

                    public void putBitmap(String url, Bitmap bitmap) {
                        mCache.put(url, bitmap);
                    }

                    public Bitmap getBitmap(String url) {
                        return mCache.get(url);
                    }
                });

        AbstractHttpClient client = new DefaultHttpClient();
        mCookieStore = client.getCookieStore();
        mRequestQueueForCookie = Volley.newRequestQueue(context, new HttpClientStack(client));

        // Trust all SSL
        trustAllSSLContext();
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public RequestQueue getRequestQueueForCookie() {
        return mRequestQueueForCookie;
    }

    public CookieStore getCookieStore() {
        return mCookieStore;
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    private void trustAllSSLContext() {
        try {
            SSLContext sslContext = SslUtils.trustAllSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (GeneralSecurityException e) {
            Log.e("ssl", "failed to set ssl context!");
        }
    }

}

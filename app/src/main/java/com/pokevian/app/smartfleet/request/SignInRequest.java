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

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.ServerError;
import com.google.gson.Gson;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.volley.GsonRequest;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleyParams;
import com.pokevian.app.smartfleet.volley.VolleySingleton;
import com.pokevian.caroo.common.smart.model.SmartMember;
import com.pokevian.caroo.common.smart.model.SmartResponse;
import com.pokevian.caroo.mbr.common.model.MemberSession;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


public class SignInRequest extends GsonRequest<SmartMember> {

    private final String mLoginId;

    public SignInRequest(String loginId, final VolleyListener<Account> listener) {
        super(ServerUrl.DATA_API, SmartMember.class, listener);

        mLoginId = loginId;

        // set parameters
        SmartMember data = new SmartMember();
        data.setLoginId(loginId);

        VolleyParams params = new VolleyParams();
        params.put("cmd", "checkMember");
        params.put("data", getGson().toJson(data));
        setParams(params);

        // set listener
        setListener(new Listener<SmartMember>() {
            public void onResponse(SmartMember response) {
                if (SmartResponse.RESULT_OK.equals(response.getResponse().getResult())) {
                    listener.onResponse(new Account(response));
                } else {
                    listener.onResponse(null);
                }
            }
        });
    }

    @Override
    protected Response<SmartMember> parseNetworkResponse(
            NetworkResponse response) {
        Response<SmartMember> parsed = super.parseNetworkResponse(response);

        if (parsed.isSuccess()) {
            try {
                sessionLogin(getGson(), mLoginId);
            } catch (Exception e) {
                return Response.error(new ServerError());
            }
        }

        return parsed;
    }

    public static void sessionLogin(Gson gson, String loginId) throws Exception {
        /*DefaultHttpClient client = MySSLSocketFactory.getHttpClient();*/

        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);
        DefaultHttpClient client = new DefaultHttpClient(ccm,params);

        HttpPost post = new HttpPost(ServerUrl.SIGN_IN_API);

        MemberSession data = new MemberSession();
        data.setLoginId(loginId);
        /*data.setLocale(Locale.getDefault());*/
        /*data.setTimeZoneId(TimeZone.getDefault().getID());*/

        List<NameValuePair> paramss = new ArrayList<>();
        paramss.add(new BasicNameValuePair("cmd", "login.json"));
        paramss.add(new BasicNameValuePair("data", gson.toJson(data)));
        post.setEntity(new UrlEncodedFormEntity(paramss, "UTF-8"));

        client.execute(post);

        CookieManager cookieManager = CookieManager.getInstance();

        List<Cookie> cookies = client.getCookieStore().getCookies();
        for (Cookie cookie : cookies) {
            String cookieString = cookie.getName() + "=" + cookie.getValue()
                    + "; path=" + cookie.getPath();
            Logger logger = Logger.getLogger("sessionLogin");
            logger.debug(String.format("cookie:%s : %s", cookie.getDomain(), cookieString));

            cookieManager.setCookie(cookie.getDomain(), cookieString);
            CookieSyncManager.getInstance().sync();

            // set cookie
            CookieStore cs = VolleySingleton.getInstance().getCookieStore();
            cs.addCookie(cookie);

            logger.debug(cs.getCookies().toString());
        }
    }

    /*public static class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

            };

            TrustManager[] tms = new TrustManager[]{
                    tm
            };

            sslContext.init(null, tms, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port,
                                   boolean autoClose) throws IOException {
            return sslContext.getSocketFactory().createSocket(socket, host,
                    port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }

        public static DefaultHttpClient getHttpClient() {
            final int connectionTimeout = 3000;
            final int soTimeout = 5000;
            final int httpPort = 80;
            final int httpsPort = 443;

            try {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);

                SchemeRegistry registry = new SchemeRegistry();
                PlainSocketFactory plainSocketFactory = PlainSocketFactory.getSocketFactory();
                registry.register(new Scheme("http", plainSocketFactory, httpPort));

                SSLSocketFactory sslSocketFactory = new MySSLSocketFactory(trustStore);
                sslSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                registry.register(new Scheme("https", sslSocketFactory, httpsPort));

                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
                HttpConnectionParams.setSoTimeout(params, soTimeout);

                ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

                return new DefaultHttpClient(ccm, params);
            } catch (Exception e) {

                return new DefaultHttpClient();
            }
        }

    }*/

}

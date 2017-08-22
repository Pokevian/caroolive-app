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

package com.pokevian.app.smartfleet.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.model.Account;
import com.pokevian.app.smartfleet.request.SignUpRequest;
import com.pokevian.app.smartfleet.service.CrashDetectService;
import com.pokevian.app.smartfleet.service.VehicleService;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.PackageUtils;
import com.pokevian.app.smartfleet.volley.VolleyListener;
import com.pokevian.app.smartfleet.volley.VolleySingleton;

import org.apache.log4j.Logger;
public class MainFragment extends Fragment {

    public static final String TAG = "main-fragment";

    private final RequestQueue mRequestQueue = VolleySingleton.getInstance().getRequestQueue();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        getActivity().startService(new Intent(getActivity(), VehicleService.class));
        getActivity().startService(new Intent(getActivity(), CrashDetectService.class));

        Account account = SettingsStore.getInstance().getAccount();
        String appVer = PackageUtils.getVersionName(getActivity());
        if (appVer == null) return;

        if (appVer.contains("-SNAPSHOT")) {
            appVer = appVer.substring(0, appVer.indexOf("-"));
        }
        Logger.getLogger(TAG).debug("MainFragment#" + account.getAppVer());
        if (!appVer.equals(account.getAppVer())) {
            account.setAppVer(appVer);
            SignUpRequest request = new SignUpRequest(account, new VolleyListener<String>() {
                @Override
                public void onErrorResponse(VolleyError error) {
//                    Logger.getLogger(TAG).error("onErrorResponse#" + error.getMessage());
                }

                @Override
                public void onResponse(String response) {
                    Logger.getLogger(TAG).debug("onResponse#" + response);

                }
            });
            request.setTag(TAG);
            mRequestQueue.add(request);
        }
    }

    @Override
    public void onDestroy() {
        getActivity().stopService(new Intent(getActivity(), VehicleService.class));
        getActivity().stopService(new Intent(getActivity(), CrashDetectService.class));

        mRequestQueue.cancelAll(TAG);

        super.onDestroy();
    }
}

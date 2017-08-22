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

package com.pokevian.app.smartfleet.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseActivity;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = new PlaceholderFragment();
            fm.beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    public static class PlaceholderFragment extends Fragment implements OnClickListener {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            view.findViewById(R.id.vehicle_setting_btn).setOnClickListener(this);
            view.findViewById(R.id.blackbox_setting_btn).setOnClickListener(this);
            view.findViewById(R.id.general_setting_btn).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.vehicle_setting_btn) {
                Intent intent = new Intent(getActivity(), VehicleSettingActivity.class);
                startActivity(intent);
            } else if (id == R.id.blackbox_setting_btn) {
                Intent intent = new Intent(getActivity(), BlackboxSettingActivity.class);
                startActivity(intent);
            } else if (id == R.id.general_setting_btn) {
                Intent intent = new Intent(getActivity(), GeneralSettingActivity.class);
                startActivity(intent);
            }
        }

    }

}

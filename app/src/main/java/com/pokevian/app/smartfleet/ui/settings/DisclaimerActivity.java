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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.common.WebViewFragment;

public class DisclaimerActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disclaimer);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.disclaimer_title);

        findViewById(R.id.detail_personal).setOnClickListener(this);
        findViewById(R.id.detail_service).setOnClickListener(this);
        findViewById(R.id.detail_location).setOnClickListener(this);

        findViewById(R.id.agree_personal).setEnabled(false);
        findViewById(R.id.agree_service).setEnabled(false);
        findViewById(R.id.agree_location).setEnabled(false);

//        findViewById(R.id.disclaimer_title).setVisibility(View.GONE);
//        findViewById(R.id.fl_agree_all).setVisibility(View.GONE);
//        findViewById(R.id.button_bar).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (R.id.detail_personal == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_PERSONAL);
            getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        } else if (R.id.detail_service == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_SERVICE);
            getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        } else if (R.id.detail_location == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_LOCATION);
            getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

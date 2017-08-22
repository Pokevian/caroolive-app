package com.pokevian.app.smartfleet.ui.web;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;

/**
 * Created by ian on 2016-04-26.
 */
public class EventActivity extends WebViewActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.navigation_drawer_menu_event);

        mBrowser.mShouldOverrideUrlLoading = true;
        loadUrl(ServerUrl.EVENT_PAGE_URL);
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) {
            getSupportActionBar().setTitle(R.string.navigation_drawer_menu_event);
        } else {
            super.onBackPressed();
        }
    }

}

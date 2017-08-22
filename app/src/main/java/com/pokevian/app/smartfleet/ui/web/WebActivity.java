package com.pokevian.app.smartfleet.ui.web;

import android.os.Bundle;
import android.support.v7.app.ActionBar;

/**
 * Created by ian on 2016-04-26.
 */
public class WebActivity extends WebViewActivity {

    public static final String EXTRA_URL = "extra-URL";
    public static final String EXTRA_TITLE = "extra-title";

    private String mUrl;
    private String mTitle;
    private boolean mHasHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(EXTRA_URL);
            mTitle = savedInstanceState.getString(EXTRA_TITLE);
        } else if (getIntent() != null) {
            mUrl = getIntent().getStringExtra(EXTRA_URL);
            mTitle = getIntent().getStringExtra(EXTRA_TITLE);
        }

        init();
        loadUrl(mUrl);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_URL, mUrl);
        outState.putString(EXTRA_TITLE, mTitle);
    }

    @Override
    public void onBackPressed() {
        if (mHasHistory && mBrowser.canGoBack()) {
            return;
        }

        super.onBackPressed();
    }

    private void init() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        if (mTitle != null) {
            bar.setTitle(mTitle);
        }
    }

}



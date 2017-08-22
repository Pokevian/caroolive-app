package com.pokevian.app.smartfleet.ui.web;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.request.SignInRequest;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;

/**
 * Created by ian on 2016-09-28.
 */

public class RankingViewActivity extends BaseDrivingOnActivity implements WebViewFragment.OnSessionCloseListener {

    protected RankingView mBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_ranking);

        mBrowser = (RankingView) getSupportFragmentManager().findFragmentByTag("web-view");
        init();
        loadUrl(ServerUrl.MEMBER_RANKING_URL);
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

    @Override
    public void onSessionClosed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String id = SettingsStore.getInstance().getLoginId();
                    SignInRequest.sessionLogin(new Gson(), id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void init() {
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(getString(R.string.weekly_ranking));
    }

    protected void loadUrl(String url) {
        mBrowser.loadUrl(url);
    }

    protected boolean canGoBack() {
        return mBrowser.canGoBack();
    }
}

package com.pokevian.app.smartfleet.ui.web;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.request.SignInRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;

/**
 * Created by ian on 2016-09-28.
 */

public class WebViewActivity extends BaseDrivingOnActivity implements WebViewFragment.OnSessionCloseListener {

    protected WebViewFragment mBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        mBrowser = (WebViewFragment) getSupportFragmentManager().findFragmentByTag("web-view");
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

    protected void loadUrl(String url) {
        mBrowser.loadUrl(url);
    }

    protected boolean canGoBack() {
        return mBrowser.canGoBack();
    }
}

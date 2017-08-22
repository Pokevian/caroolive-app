package com.pokevian.app.smartfleet.ui.web;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.pokevian.app.fingerpush.PushManagerHelper;
import com.pokevian.app.smartfleet.util.WebUtils;

/**
 * Created by ian on 2016-04-29.
 */
public class WebLinkActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String tag = getIntent().getStringExtra("msgTag");
        String type = getIntent().getStringExtra("mode");
        String uri = getIntent().getStringExtra("webLink");

        if (!TextUtils.isEmpty(tag) && !TextUtils.isEmpty(type) && !TextUtils.isEmpty(uri)) {
            PushManagerHelper.checkPush(this, tag, type);

            WebUtils.launchWebLink(this, uri);
        }

        finish();
    }
}

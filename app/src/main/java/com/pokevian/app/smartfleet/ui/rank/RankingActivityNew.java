package com.pokevian.app.smartfleet.ui.rank;


import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.ui.web.WebViewActivity;

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-03-03.
 */
public class RankingActivityNew extends WebViewActivity {
    private static final String TAG = "ranking-activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.weekly_ranking);

        loadUrl(ServerUrl.WEEKLY_RANKING_URL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_week, menu);
        Switch week = (Switch) menu.findItem(R.id.action_select_week).getActionView().findViewById(R.id.switch_week);
        week.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Logger.getLogger(TAG).info("onCheckedChanged#" + isChecked);
                if (isChecked) {
                    loadUrl("javascript:selectLastWeek()");
                } else {
                    loadUrl("javascript:selectThisWeek()");
                }
            }
        });

        return true;
    }
}


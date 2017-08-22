package com.pokevian.app.smartfleet.ui.pattern;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;
import com.pokevian.app.smartfleet.util.ShareUtils;

import org.apache.log4j.Logger;

import java.io.FileNotFoundException;

/**
 * Created by ian on 2016-03-03.
 */
public class DrivingPatternActivity extends BaseDrivingOnActivity {
    public static final String TAG = "DrivingPatternActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.drv_pattern);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(WeeklyPatternFragment.TAG);
        if (fragment == null) {
            fm.beginTransaction().replace(R.id.container, WeeklyPatternFragment.newInstance(), WeeklyPatternFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (android.R.id.home == id) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
            return true;
        } else if (R.id.action_share == id) {
            final View view = findViewById(R.id.container);
            view.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        ShareUtils.doShare(DrivingPatternActivity.this, view);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pattern, menu);

        Switch week = (Switch) menu.findItem(R.id.action_select_week).getActionView().findViewById(R.id.switch_week);
        week.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Logger.getLogger(TAG).trace("onCheckedChanged#" + isChecked);
                WeeklyPatternFragment fragment = (WeeklyPatternFragment) getSupportFragmentManager().findFragmentByTag(WeeklyPatternFragment.TAG);
                if (fragment != null) {
                    fragment.updateWeeklyPattern(isChecked);
                }
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}


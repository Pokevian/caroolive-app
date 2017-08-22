package com.pokevian.app.smartfleet.ui.tripmonitor;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.request.SignInRequest;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.BaseActivity;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;
import com.pokevian.app.smartfleet.ui.driving.BaseDrivingActivity;
import com.pokevian.app.smartfleet.ui.intro.IntroActivity;

/**
 * Created by ian on 2015-10-27.
 */
public class TripMonitorActivity extends BaseDrivingOnActivity {
    public static final String EXTRA_POSITION = "extra.Position";
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_trip_monitor);

        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent != null) {
                mPosition = intent.getIntExtra(EXTRA_POSITION, 0);
            }
        } else {
            mPosition = savedInstanceState.getInt(EXTRA_POSITION, 0);
        }

        if (TripMonitorFragment.MENU_TRIP == mPosition) {
            bar.setTitle(R.string.main_trip_record);
        } else if (TripMonitorFragment.MENU_CHART == mPosition) {
            bar.setTitle(R.string.main_chart);
        } else if (TripMonitorFragment.MENU_VIDEO == mPosition) {
            bar.setTitle(R.string.navigation_drawer_menu_video);
        } else if (TripMonitorFragment.MENU_NOTICE == mPosition) {
            bar.setTitle(R.string.navigation_drawer_menu_notice);
        }

        FragmentManager fm = getSupportFragmentManager();
        TripMonitorFragment fragment = (TripMonitorFragment) fm.findFragmentById(R.id.container);
        if (fragment == null) {
            fm.beginTransaction()
                    .replace(R.id.container, TripMonitorFragment.newInstance(mPosition))
                    .commitAllowingStateLoss();
        } else {
            fragment.loadServiceUrl(mPosition);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_POSITION, mPosition);

        super.onSaveInstanceState(outState);
    }

    public void onSessionClosed() {
//        logger.warn("session closed!");

//        Intent intent = new Intent(this, IntroActivity.class);
//        startActivity(intent);
//
//        finish();

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
}

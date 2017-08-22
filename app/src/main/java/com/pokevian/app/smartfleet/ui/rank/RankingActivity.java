package com.pokevian.app.smartfleet.ui.rank;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseDrivingOnActivity;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ian on 2016-03-03.
 */
public class RankingActivity extends BaseDrivingOnActivity {
    private static final String TAG = "RankingActivityNew";

    private PagerAdapter mAdapter;
//    private boolean mLastWeek = false;

//    private TripRankingFragment mTripRankingFragment;
//    private WeeklyRankingFragment mWeeklyRankingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbar_tab);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.weekly_ranking);

        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mAdapter.addFragment(TripRankingFragment.newInstance(), getString(R.string.rank_trip));
        mAdapter.addFragment(WeeklyRankingFragment.newInstance(WeeklyRankingFragment.RANK_DRV_SCORE), getString(R.string.rank_weekly));
        mAdapter.addFragment(WeeklyRankingFragment.newInstance(WeeklyRankingFragment.RANK_FUEL_SAVE), getString(R.string.rank_fuel_saved));

        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
        viewPager.setAdapter(mAdapter);

        ((TabLayout) findViewById(R.id.tab_layout)).setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_week, menu);
        Switch week = (Switch) menu.findItem(R.id.action_select_week).getActionView().findViewById(R.id.switch_week);
//        ToggleButton week = (ToggleButton) menu.findItem(R.id.action_select_week).getActionView().findViewById(R.id.switch_week);
        week.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Logger.getLogger(TAG).trace("onCheckedChanged#" + isChecked);
                mAdapter.setLastWeek(isChecked);
                mAdapter.notifyDataSetChanged();
            }
        });

        return true;
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
        }

        return super.onOptionsItemSelected(item);
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mTitles = new ArrayList<>();

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mTitles.add(title);
        }

        public void setLastWeek(boolean isLastWeek) {
            for (Fragment fragment : mFragments) {
                ((ScoreRankingFragment) fragment).setLastWeek(isLastWeek);
            }
        }

        @Override
        public Fragment getItem(int position) {
            Logger.getLogger(TAG).trace("getItem#" + position);
            return mFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public int getItemPosition(Object object) {
            Logger.getLogger(TAG).debug("getItemPosition");
//            return super.getItemPosition(object);
            return POSITION_NONE;
        }
    }
}


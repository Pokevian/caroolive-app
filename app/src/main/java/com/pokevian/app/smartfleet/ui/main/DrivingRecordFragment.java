package com.pokevian.app.smartfleet.ui.main;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Rank;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.rank.GetRankingDialogCallback;
import com.pokevian.app.smartfleet.ui.rank.GetRankingDialogFragment;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by ian on 2016-06-12.
 */
public class DrivingRecordFragment extends Fragment implements ViewPager.OnPageChangeListener, View.OnClickListener, GetRankingDialogCallback {
    public static final String TAG = "DrivingRecordFragment";

    private ViewPager mViewPager;
    private LinearLayout mPageIndicatorPane;
    private int mPrevPageIndicatorIdx = 0;
    private FuelCostFragment mFuelCostFragment = new FuelCostFragment();
    private LevelRecordFragment mLevelRecordFragment = new LevelRecordFragment();
    private PointRecordFragment mPointRecordFragment = new PointRecordFragment();

    private String mAccountId;
    private Rank mDrivingRecord;

    private DrivingRecordUpdateCallback mCallback;

    public interface DrivingRecordUpdateCallback {
        void onUpdate(int level);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mCallback = (DrivingRecordUpdateCallback) activity;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_record, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAccountId = SettingsStore.getInstance().getAccountId();

        mPrevPageIndicatorIdx = 0;
        if (savedInstanceState != null) {
            mPrevPageIndicatorIdx = savedInstanceState.getInt("position");
            mDrivingRecord = (Rank) savedInstanceState.getSerializable("driving-record");
        }

        Logger.getLogger("DrivingRecordFragment").trace("onViewCreated#" + mDrivingRecord);

        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        PagerAdapter adapter = new PagerAdapter(getChildFragmentManager());

        adapter.addFragment(mLevelRecordFragment);
        adapter.addFragment(mFuelCostFragment);
        adapter.addFragment(mPointRecordFragment);

        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(this);

        if (mDrivingRecord != null) {
            update();
        }

        mPageIndicatorPane = (LinearLayout) view.findViewById(R.id.page_indicator);
        for (int i = 0; i < adapter.getCount(); i++) {
            ImageView indicator = new ImageView(getActivity());
            if (i == mPrevPageIndicatorIdx) {
                indicator.setImageResource(R.drawable.ic_slide_on);
            } else {
                indicator.setImageResource(R.drawable.ic_slide_off);
            }
            mPageIndicatorPane.addView(indicator);
        }

        mViewPager.setCurrentItem(mPrevPageIndicatorIdx);

        if (savedInstanceState == null) {
            getDrivingRecord();
        }

        view.findViewById(R.id.btn_hof).setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("position", mViewPager.getCurrentItem());
        outState.putSerializable("driving-record", mDrivingRecord);
    }

    public void update(final Rank rank) {
        Logger.getLogger("DrivingRecordFragment").trace("update#" + rank);
        mDrivingRecord = rank;
        update();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        ImageView indicator = (ImageView) mPageIndicatorPane.getChildAt(mPrevPageIndicatorIdx);
        indicator.setImageResource(R.drawable.ic_slide_off);

        // Set current indicator on
        indicator = (ImageView) mPageIndicatorPane.getChildAt(position);
        indicator.setImageResource(R.drawable.ic_slide_on);

        mPrevPageIndicatorIdx = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onSuccess(DialogFragment fragment, Rank rank) {
        fragment.dismissAllowingStateLoss();
        mDrivingRecord = rank;

        update();
    }

    @Override
    public void onFailure(DialogFragment fragment) {
        fragment.dismissAllowingStateLoss();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (R.id.btn_hof == id) {
            ((MainActivity)getActivity()).startHofActivity();
        }
    }

    private void update() {
        if (mDrivingRecord != null) {
            mCallback.onUpdate(mDrivingRecord.getDrivingLevel());

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    mFuelCostFragment.onUpdate(mDrivingRecord);
                    mLevelRecordFragment.onUpdate(mDrivingRecord);
                    mPointRecordFragment.onUpdate(mDrivingRecord);
                }
            });
        }
    }

    private void getDrivingRecord() {
        getChildFragmentManager().beginTransaction().add(GetRankingDialogFragment.newInstance(mAccountId), GetRankingDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    public class PagerAdapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment) {
            mFragments.add(fragment);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

    }

    public interface OnUpdateListener {
        void onUpdate(Rank rank);
    }

   protected void setCircleImageResource(ImageView imageView, float index) {
       TypedArray circles = getResources().obtainTypedArray(R.array.circle_level);
       imageView.setImageResource(circles.getResourceId(Math.round(index), 0));
   }

}

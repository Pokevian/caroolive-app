package com.pokevian.app.smartfleet.ui.main;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Rank;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.rank.GetRankingDialogCallback;
import com.pokevian.app.smartfleet.ui.rank.GetWeeklyRankingDialogFragment;
import com.pokevian.app.smartfleet.util.TextViewUtils;

/**
 * Created by ian on 2016-06-15.
 */
public class WeeklyRecordFragment extends Fragment implements View.OnClickListener, GetRankingDialogCallback {
    public static final String TAG = "WeeklyRecordFragment";

    private String mAccountId;
    private Rank mWeeklyRecord;
    private View mView;
    private boolean mIsUpdated;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountId = SettingsStore.getInstance().getAccountId();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_weekly_record, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        mView.findViewById(R.id.btn_pattern).setOnClickListener(this);
        mView.findViewById(R.id.btn_ranking).setOnClickListener(this);

        if (savedInstanceState == null) {
            getWeeklyRecord();
        } else {
            mWeeklyRecord = (Rank) savedInstanceState.getSerializable("weekly-record");
            mIsUpdated = savedInstanceState.getBoolean("is-updated", false);
            if (mIsUpdated) {
                update();
            } else {
                getWeeklyRecord();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("weekly-record", mWeeklyRecord);
        outState.putBoolean("is-updated", mIsUpdated);
    }

    @Override
    public void onSuccess(DialogFragment fragment, Rank rank) {
        mWeeklyRecord = rank;
        mIsUpdated = true;
        update();
    }

    @Override
    public void onFailure(DialogFragment fragment) {
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (R.id.btn_ranking == id) {
            ((MainActivity)getActivity()).startRankingActivity();
        } else if (R.id.btn_pattern == id) {
            ((MainActivity)getActivity()).startDrivingPatternActivity();
        }
    }

    protected void updateBadge(int index) {
        TypedArray badge = getResources().obtainTypedArray(R.array.badge_white);
        int length = badge.length() - 1;
        ((ImageView) mView.findViewById(R.id.circle_badge)).setImageResource(badge.getResourceId(index > length ? length : index, 0));
        badge.recycle();
    }

    private void update() {
        if (mWeeklyRecord != null) {
            TextViewUtils.setRankingText((TextView) mView.findViewById(R.id.weekly_ranking), mWeeklyRecord.getDrivingScoreRanking());
            TextViewUtils.setScoreText((TextView) mView.findViewById(R.id.weekly_score), mWeeklyRecord.getDrivingScore());
            TextViewUtils.setNumberFormatText((TextView) mView.findViewById(R.id.drv_count), mWeeklyRecord.getTripCount());
            TextViewUtils.setFuelEconomyText((TextView)mView.findViewById(R.id.fuel_ecocomy), mWeeklyRecord.getFuelEconomy());
            TextViewUtils.setDistanceText((TextView) mView.findViewById(R.id.drv_distance), mWeeklyRecord.getRunDistance());
        }
    }

    private void getWeeklyRecord() {
        getChildFragmentManager().beginTransaction().add(GetWeeklyRankingDialogFragment.newInstance(mAccountId, false), GetWeeklyRankingDialogFragment.TAG)
                .commitAllowingStateLoss();
    }
}

package com.pokevian.app.smartfleet.ui.rank;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.util.TextViewUtils;

import java.util.List;

/**
 * Created by ian on 2016-06-10.
 */
public class HofLevelFragment extends Fragment implements GetRankingsDialogCallback {

    private ViewGroup mViewGroup;
    private List<ScoreRank> mList;

    public static HofLevelFragment newInstance() {
        return new HofLevelFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranking_level, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewGroup = (ViewGroup) view;

        initChildView();

        getChildFragmentManager().beginTransaction().add(GetRankingTop10DialogFragment.newInstance(GetRankingTop10DialogFragment.REQUEST_ID_RANKING_TOP10_LEVEL), GetRankingTop10DialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    @Override
    public void onSuccess(DialogFragment fragment, List<ScoreRank> list) {
        fragment.dismissAllowingStateLoss();
        mList = list;

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                updateChildView();
            }
        });
    }

    @Override
    public void onFailure(DialogFragment fragment) {
        fragment.dismissAllowingStateLoss();
    }

    private void updateChildView() {
        initChildView();

        if (mList != null) {
            try {
                for (int i = 0; i < mViewGroup.getChildCount(); i++) {
                    updateView(mViewGroup.getChildAt(i), mList.get(i));
                }
            } catch (IndexOutOfBoundsException e) {
            }
        }
    }

    private void updateView(View view, ScoreRank scoreRank) {
        ((TextView) view.findViewById(R.id.member_nm)).setText(scoreRank.getMemberNm());
        ((TextView) view.findViewById(R.id.driving_score)).setText(Integer.toString(Math.round(scoreRank.getDrivingScore())));
        ((TextView) view.findViewById(R.id.level)).setText(Integer.toString(scoreRank.getDrivingLevel()));
        ((ProgressBar) view.findViewById(R.id.progress)).setProgress(100 * scoreRank.getScoreCount() / scoreRank.getDrivingLevel());

        TextView tv = (TextView) view.findViewById(R.id.trip_count);
        if (tv != null) {
            tv.setText(Integer.toString(scoreRank.getTripCount()));
        }
        tv = (TextView) view.findViewById(R.id.run_distance);
        if (tv != null) {
//            tv.setText(Integer.toString(Math.round(scoreRank.getRunDistance())));
            TextViewUtils.setNumberFormatText(tv, scoreRank.getRunDistance());
        }
    }

    private void initChildView() {
        for (int i = 0; i < mViewGroup.getChildCount(); i++) {
            View view = mViewGroup.getChildAt(i);

            ((TextView) view.findViewById(R.id.member_nm)).setText(getString(R.string.empty_string_value));
            ((TextView) view.findViewById(R.id.driving_score)).setText(getString(R.string.empty_int_value));
            ((TextView) view.findViewById(R.id.level)).setText(getString(R.string.empty_int_value));
            ((ProgressBar) view.findViewById(R.id.progress)).setProgress(0);

            TextView tv = (TextView) view.findViewById(R.id.driving_score_ranking);
            if (tv != null) {
                tv.setText(Integer.toString(i + 1));
            }
            tv = (TextView) view.findViewById(R.id.trip_count);
            if (tv != null) {
                tv.setText(getString(R.string.empty_int_value));
            }
            tv = (TextView) view.findViewById(R.id.run_distance);
            if (tv != null) {
                tv.setText(getString(R.string.empty_float_value));
            }
        }
    }

}

package com.pokevian.app.smartfleet.ui.rank;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.util.TextViewUtils;

import java.util.List;

/**
 * Created by ian on 2016-06-10.
 */
public class HofPointFragment extends Fragment implements GetRankingsDialogCallback {

    private ViewGroup mViewGroup;
    private List<ScoreRank> mList;

    public static HofPointFragment newInstance() {
        return new HofPointFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranking_point, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewGroup = (ViewGroup) view;

        initChildView();

        getChildFragmentManager().beginTransaction().add(GetRankingTop10DialogFragment.newInstance(GetRankingTop10DialogFragment.REQUEST_ID_RANKING_TOP10_POINT), GetRankingTop10DialogFragment.TAG)
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

        if (scoreRank.getMemberPtRes() != null) {
            TextViewUtils.setNumberFormatText((TextView) view.findViewById(R.id.residue), scoreRank.getMemberPtRes());
        }
        if (scoreRank.getMemberPtAcc() != null) {
            TextViewUtils.setNumberFormatText((TextView) view.findViewById(R.id.accumulation), scoreRank.getMemberPtAcc());
        }
    }

    private void initChildView() {
        for (int i = 0; i < mViewGroup.getChildCount(); i++) {
            View view = mViewGroup.getChildAt(i);

            TextView tv = (TextView) view.findViewById(R.id.driving_score_ranking);
            if (tv != null) {
                TextViewUtils.setIntegerFormatText(tv, i + 1);
            }

            ((TextView) view.findViewById(R.id.member_nm)).setText(getString(R.string.empty_string_value));
            ((TextView) view.findViewById(R.id.residue)).setText(getString(R.string.empty_int_value));
            ((TextView) view.findViewById(R.id.accumulation)).setText(getString(R.string.empty_int_value));
        }
    }
}

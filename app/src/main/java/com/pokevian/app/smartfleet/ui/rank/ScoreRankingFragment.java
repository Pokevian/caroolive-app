package com.pokevian.app.smartfleet.ui.rank;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;

/**
 * Created by ian on 2016-03-07.
 */
public abstract class ScoreRankingFragment extends Fragment {

    private final int CHILD_VIEW_COUNT = 10;

    private ViewGroup mViewGroup;
    protected boolean mLastWeek = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ranking_score, null);
        mViewGroup = (ViewGroup) view;
        initView();
        return view;
    }

    public void setLastWeek(boolean isLastWeek) {
        mLastWeek = isLastWeek;
    }

    protected void update() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                updateView();
            }
        });
    }

    private void initView() {
        mViewGroup.removeAllViews();

        for (int i = 0; i < CHILD_VIEW_COUNT; i++) {
            View v = inflateChildView(i);
            addView(v, i, 1f);
        }
    }

    private void updateView() {
        initView();

        try {
            for (int i = 0; i < CHILD_VIEW_COUNT; i++) {
                updateChildView(mViewGroup.getChildAt(i), i);
            }
        } catch (IndexOutOfBoundsException e) {
        } catch (NullPointerException e) {
        }

        View view = inflateMyRankingView();
        if (view != null) {
            int ranking = getMyRanking();
            if (CHILD_VIEW_COUNT < ranking) {
                ranking = CHILD_VIEW_COUNT;
            }
            addMyRankingView(view, ranking - 1);
        }
    }

    private void addView(View view, int index, float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        if (index + 1 < CHILD_VIEW_COUNT) {
            params.bottomMargin = Math.round(8 * getResources().getDisplayMetrics().density);
        }
        view.setLayoutParams(params);
        mViewGroup.addView(view, index);
    }

    private void addMyRankingView(View view, int index) {
        mViewGroup.removeViewAt(index);
        addView(view, index, 2f);
    }

    protected void setText(View view, CharSequence text) {
        ((TextView) view).setText(text);
    }

    protected void setTextAppearance(View view, int resId) {
        ((TextView) view).setTextAppearance(getActivity(), resId);
    }

    protected View inflateChildView(int index) {
        View v = View.inflate(getActivity(), R.layout.item_ranking_regular, null);
        ((TextView) v.findViewById(R.id.driving_score_ranking)).setText(Integer.toString(index + 1));
        ((TextView) v.findViewById(R.id.car_model).findViewById(R.id.main_text)).setTextAppearance(getActivity(), R.style.rankingInfoText);
        v.findViewById(R.id.car_model).findViewById(R.id.base_text).setVisibility(View.GONE);
        v.findViewById(R.id.car_model).findViewById(R.id.sub_text).setVisibility(View.GONE);

        return v;
    }

//    abstract protected View inflateChildView(int index);
    abstract protected View inflateMyRankingView();
    abstract protected void updateChildView(View view, int index);
    abstract protected int getMyRanking();

//    public String Time2String(int second) {
//        int h = second / 3600;
//        int m = (second % 3600) / 60;
//        return String.format("%02d:%02d", h, m);
//    }

}

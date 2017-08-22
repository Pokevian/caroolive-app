package com.pokevian.app.smartfleet.ui.rank;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Rank;
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.TextViewUtils;

import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by ian on 2016-03-07.
 */
public class WeeklyRankingFragment extends ScoreRankingFragment implements GetRankingsDialogCallback, GetRankingDialogCallback {
    public static final String TAG = "WeeklyRankingFragment";

    public static final String RANK_DRV_SCORE = "rank-drv-score";
    public static final String RANK_ECO_SPEED = "rank-eco-speed";
    public static final String RANK_FUEL_CUT = "rank-fuel-cut";
    public static final String RANK_HARSH_SPEED = "rank-harsh-speed";
    public static final String RANK_FUEL_SAVE = "rank-fuel-save";

    private String mMemberNo;
    private List<ScoreRank> mList;
    private Rank mMyRanking;
    private String mRequestId;

    public static WeeklyRankingFragment newInstance(String tag) {
        WeeklyRankingFragment fragment = new WeeklyRankingFragment();
        Bundle args = new Bundle();
        args.putString("memberNo", SettingsStore.getInstance().getAccountId());
        args.putString("tag", tag);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.getLogger("RankingActivityNew").trace("onCreate@" + TAG + "#" + savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            mMemberNo = getArguments().getString("memberNo");
            mRequestId = getRequestId(getArguments().getString("tag"));
            Logger.getLogger(TAG).trace("onViewCreated#" + mMemberNo);
        }

        getWeeklyRankingTop10();
    }

    @Override
    public void onSuccess(DialogFragment fragment, List<ScoreRank> list) {
        Logger.getLogger("fuel_saved").debug("onSuccess#" + list);
        fragment.dismissAllowingStateLoss();
        mList = list;

//        if (mLastWeek) {
//            mMyRanking = null;
//            update();
//        } else {
//            getWeeklyRanking();
//        }
        update();
    }

    @Override
    public void onSuccess(DialogFragment fragment, Rank rank) {
        fragment.dismissAllowingStateLoss();
        if (mList != null) {
            mMyRanking = rank;
            update();
        }
    }

    @Override
    public void onFailure(DialogFragment fragment) {
        fragment.dismissAllowingStateLoss();
    }

    protected String getRequestId(String request) {
        if (RANK_DRV_SCORE.equals(request)) {
            return GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_DRV_SCORE;
        }
        if (RANK_ECO_SPEED.equals(request)) {
            return GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_ECO_SPEED;
        }
        if (RANK_FUEL_CUT.equals(request)) {
            return GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_FUEL_CUT;
        }
        if (RANK_HARSH_SPEED.equals(request)) {
            return GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_HARSH_SPEED;
        }
        if (RANK_FUEL_SAVE.equals(request)) {
            return GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_FUEL_SAVE;
        }

        return null;
    }

    @Override
    protected View inflateChildView(int index) {
        View v = View.inflate(getActivity(), R.layout.item_ranking_regular, null);
        ((TextView) v.findViewById(R.id.driving_score_ranking)).setText(Integer.toString(index + 1));
        ((TextView) v.findViewById(R.id.car_model).findViewById(R.id.main_text)).setTextAppearance(getActivity(), R.style.rankingInfoText);

        if (GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_DRV_SCORE.equals(mRequestId)) {
            v.findViewById(R.id.car_model).findViewById(R.id.base_text).setVisibility(View.GONE);
            v.findViewById(R.id.car_model).findViewById(R.id.sub_text).setVisibility(View.GONE);
        } else if (GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_FUEL_SAVE.equals(mRequestId)) {
            ((TextView) v.findViewById(R.id.info1)).setText(getString(R.string.empty_float_value));
            ((TextView) v.findViewById(R.id.info1_unit)).setText(getString(R.string.fuel_economy_unit_kpl));
            ((TextView) v.findViewById(R.id.info2)).setText(getString(R.string.empty_float_value));
            ((TextView) v.findViewById(R.id.info2_unit)).setText(getString(R.string.volume_unit_l));
            v.findViewById(R.id.info2_unit).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.car_model).findViewById(R.id.base_text)).setTextAppearance(getActivity(), R.style.rankingInfoText);
            ((TextView) v.findViewById(R.id.car_model).findViewById(R.id.sub_text)).setTextAppearance(getActivity(), R.style.rankingInfoText);
        }

        return v;
    }

    @Override
    protected View inflateMyRankingView() {
        if (mMyRanking != null) {
            View v = View.inflate(getActivity(), R.layout.item_my_weekly_ranking, null);

//            ((TextView) v.findViewById(R.id.driving_score_ranking)).setText(Integer.toString(mMyRanking.getDrivingScoreRanking()));
            TextViewUtils.setNumberFormatText((TextView) v.findViewById(R.id.driving_score_ranking), mMyRanking.getDrivingScoreRanking());
            ((TextView) v.findViewById(R.id.member_nm)).setText(mMyRanking.getMemberNm());
            TextViewUtils.setNumberFormatText((TextView) v.findViewById(R.id.trip_count), mMyRanking.getTripCount());
//            ((TextView) v.findViewById(R.id.trip_count)).setText(Integer.toString(mMyRanking.getTripCount()));
//            ((TextView) v.findViewById(R.id.run_distance)).setText(Float.toString(mMyRanking.getRunDistance()));
            TextViewUtils.setDistanceText((TextView) v.findViewById(R.id.run_time), mMyRanking.getRunDistance());
            TextViewUtils.setRunTimeText((TextView) v.findViewById(R.id.run_time), mMyRanking.getRunTime());
//            ((TextView) v.findViewById(R.id.run_time)).setText(Time2String(mMyRanking.getRunTime()));
//            ((TextView) v.findViewById(R.id.driving_score)).setText(Integer.toString(Math.round(mMyRanking.getDrivingScore())));
            TextViewUtils.setScoreText((TextView) v.findViewById(R.id.driving_score), mMyRanking.getDrivingScore());
            return v;
        }

        return null;
    }

    @Override
    protected int getMyRanking() {
        if (mMyRanking != null) {
            return mMyRanking.getDrivingScoreRanking();
        }
        return -1;
    }

    @Override
    protected void updateChildView(View view, int index) {
        ScoreRank scoreRank  = mList.get(index);
//        ((TextView) view.findViewById(R.id.member_nm)).setText(scoreRank.getMemberNm());
//        ((TextView) view.findViewById(R.id.local_cd_name)).setText(String.format(getString(R.string.round_bracket), scoreRank.getLocalCdName()));

        View member = view.findViewById(R.id.member);
        ((TextView) member.findViewById(R.id.main_text)).setText(scoreRank.getMemberNm());
        String local = String.format(getString(R.string.round_bracket), scoreRank.getLocalCdName());
        ((TextView) member.findViewById(R.id.sub_text)).setText(local);
        ((TextView) member.findViewById(R.id.base_text)).setText(local);

        View car = view.findViewById(R.id.car_model);
        ((TextView) car.findViewById(R.id.main_text)).setText(scoreRank.getCarModelName());
        String fuel = String.format(getString(R.string.round_bracket), scoreRank.getCarFuelName());
        ((TextView) car.findViewById(R.id.sub_text)).setText(fuel);
        ((TextView) car.findViewById(R.id.base_text)).setText(fuel);

//        ((TextView) view.findViewById(R.id.car_model_name)).setText(scoreRank.getCarModelName());
//        ((TextView) view.findViewById(R.id.car_model_name2)).setText(scoreRank.getCarModelName());
//        ((TextView) view.findViewById(R.id.car_fuel_name)).setText(String.format(getString(R.string.round_bracket), scoreRank.getCarFuelName()));

        updateChildView(view, scoreRank);
    }

    private void getWeeklyRankingTop10() {
        if (mRequestId != null) {
            getChildFragmentManager().beginTransaction().add(GetWeeklyRankingListDialogFragment.newInstance(mRequestId, mLastWeek), GetWeeklyRankingListDialogFragment.TAG)
                    .commitAllowingStateLoss();
        }
    }

    private void getWeeklyRanking() {
        getChildFragmentManager().beginTransaction().add(GetWeeklyRankingDialogFragment.newInstance(mMemberNo, mLastWeek), GetWeeklyRankingDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    private void updateChildView(View view, ScoreRank scoreRank) {
        switch (mRequestId) {
            case GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_DRV_SCORE:
                updateScoreRankingView(view, scoreRank);
                break;
            case GetWeeklyRankingListDialogFragment.REQUEST_ID_WEEKLY_FUEL_SAVE:
                updateFuelSaveRankingView(view, scoreRank);
                break;
        }
    }

    private void updateScoreRankingView(View view, ScoreRank scoreRank) {
        TextViewUtils.setDistanceText((TextView) view.findViewById(R.id.info1), scoreRank.getRunDistance());
//        TextViewUtils.setHHmmText((TextView) view.findViewById(R.id.info2), scoreRank.getRunTime());
        TextViewUtils.setRunTimeText((TextView) view.findViewById(R.id.info2), scoreRank.getRunTime());
        TextViewUtils.setScoreText((TextView) view.findViewById(R.id.value), scoreRank.getDrivingScore());
    }

    private void updateFuelSaveRankingView(View view, ScoreRank scoreRank) {
        TextViewUtils.setFuelEconomyText((TextView) view.findViewById(R.id.info1), scoreRank.getFuelEconomy());
        TextViewUtils.setFuelConsumptionText((TextView) view.findViewById(R.id.info2), scoreRank.getFuelConsumption());

        float save = scoreRank.getFuelCostSaveTk();

        Logger.getLogger("fuel_saved").debug("" + save);
        String cost = "";
        DecimalFormat decimalFormat = new DecimalFormat("###,###.#");
        if (100000 < Math.abs(save)) {
            cost = String.format(getString(R.string.fuel_save_10t_won), decimalFormat.format(Math.round(save / 10000)));
        } else if (9999 <  Math.abs(save)) {
            cost = String.format(getString(R.string.fuel_save_10t_won), decimalFormat.format(save / 10000));
        } else {
            cost = String.format(getString(R.string.fuel_save_won), decimalFormat.format(Math.round(save)));
        }

        ((TextView) view.findViewById(R.id.value)).setTextAppearance(getActivity(), R.style.fuelSavedText);
        ((TextView) view.findViewById(R.id.value)).setText(cost);
    }
}

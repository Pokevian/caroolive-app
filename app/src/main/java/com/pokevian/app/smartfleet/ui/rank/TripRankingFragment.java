package com.pokevian.app.smartfleet.ui.rank;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.MemberEcoData;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.TextViewUtils;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by ian on 2016-06-10.
 */
public class TripRankingFragment extends ScoreRankingFragment implements GetWeeklyTripRankingsDialogFragment.GetBestTripDialogCallback {

    public static final String TAG = "TripRankingFragment";

    private String mMemberNo;
    private MemberEcoData mMyTrip;
    private List<MemberEcoData> mList;

    public static TripRankingFragment newInstance() {
        TripRankingFragment fragment = new TripRankingFragment();
        Bundle args = new Bundle();
        args.putString("memberNo", SettingsStore.getInstance().getAccountId());
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
            Logger.getLogger("RankingActivityNew").trace("onViewCreated#" + mMemberNo + ": " + mLastWeek);
        }

        getWeeklyTripRankings();
    }

    @Override
    public void onSuccess(DialogFragment fragment, List<MemberEcoData> list, MemberEcoData myTrip) {
        fragment.dismissAllowingStateLoss();
        mList = list;
        mMyTrip = myTrip;

        update();
    }

    @Override
    public void onFailure(DialogFragment fragment) {
        fragment.dismissAllowingStateLoss();
    }

//    @Override
//    protected View inflateChildView(int index) {
//        View v = View.inflate(getActivity(), R.layout.item_ranking_trip, null);
//        ((TextView) v.findViewById(R.id.driving_score_ranking)).setText(Integer.toString(index + 1));
//        return v;
//    }

    @Override
    protected View inflateMyRankingView() {
        if (mMyTrip != null) {
            View v = View.inflate(getActivity(), R.layout.item_my_trip_ranking, null);

            setTextAppearance(v.findViewById(R.id.main_text), R.style.myRankingMemberNameText);
            setTextAppearance(v.findViewById(R.id.base_text), R.style.myRankingValueText);
            setTextAppearance(v.findViewById(R.id.sub_text), R.style.myRankingValueText);

            setText(v.findViewById(R.id.driving_score_ranking), Integer.toString(mMyTrip.getRank()));
            setText(v.findViewById(R.id.main_text), mMyTrip.getMemberNm());
            String local = String.format(getString(R.string.round_bracket), mMyTrip.getLocalName());
            setText(v.findViewById(R.id.base_text), local);
            setText(v.findViewById(R.id.sub_text), local);

            setText(v.findViewById(R.id.date), mMyTrip.getDate());
            setDistanceTextView(v.findViewById(R.id.run_distance), mMyTrip.getDrivingDistance());
            setRunTimeTextView(v.findViewById(R.id.run_time), mMyTrip.getDrivingTime());
            setText(v.findViewById(R.id.car_model_name), mMyTrip.getCarModelName());

//            TextViewUtils.setScoreText((TextView) v.findViewById(R.id.driving_score), mMyTrip.getSafeEco());
            TextViewUtils.setNumberFormatText((TextView) v.findViewById(R.id.driving_score), mMyTrip.getSafeEco());

            return v;
        }
        return null;
    }

    @Override
    protected int getMyRanking() {
        if (mMyTrip != null) {
            return mMyTrip.getRank();
        }
        return -1;
    }

    @Override
    protected void updateChildView(View view, int index) {
        MemberEcoData trip = mList.get(index);

        View member = view.findViewById(R.id.member);
        setText(member.findViewById(R.id.main_text), trip.getMemberNm());
        String local = String.format(getString(R.string.round_bracket), trip.getLocalName());
        setText(member.findViewById(R.id.base_text), local);
        setText(member.findViewById(R.id.sub_text), local);

        setText(view.findViewById(R.id.car_model).findViewById(R.id.main_text), trip.getCarModelName());

        setDistanceTextView(view.findViewById(R.id.info1), trip.getDrivingDistance());
        setRunTimeTextView(view.findViewById(R.id.info2), trip.getDrivingTime());
        setNumberFormatTextView(view.findViewById(R.id.value), trip.getSafeEco());
    }

    private void getWeeklyTripRankings() {
        getChildFragmentManager().beginTransaction().add(GetWeeklyTripRankingsDialogFragment.newInstance(mMemberNo, mLastWeek), GetWeeklyTripRankingsDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    private void setDistanceTextView(View view, float distace) {
        TextViewUtils.setDistanceText((TextView) view, distace);
    }

    private void setRunTimeTextView(View view, int runtime) {
        TextViewUtils.setRunTimeText((TextView) view, runtime);
    }

    private void setNumberFormatTextView(View view, float value) {
        TextViewUtils.setNumberFormatText((TextView) view, value);
    }
 }

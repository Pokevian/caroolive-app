package com.pokevian.app.smartfleet.ui.pattern;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.ScoreRank;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.util.TextViewUtils;
import com.pokevian.lib.obd2.defs.Unit;

/**
 * Created by ian on 2016-03-07.
 */
public class WeeklyPatternFragment extends Fragment implements getWeeklyPatternDialogFragment.LoadPatternDataCallback {
    public static final String TAG = "WeeklyPatternFragment";
    private View mView;
    private String mAccountId;
    private boolean mLastWeek = false;

    public static WeeklyPatternFragment newInstance() {
        return new WeeklyPatternFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsStore settingsStore = SettingsStore.getInstance();
        mAccountId = settingsStore.getAccountId();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pattern_driving, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mView = view;
        initView();

        updateWeeklyPattern(false);
    }

    @Override
    public void onSuccess(ScoreRank scoreStar) {
        if (scoreStar != null) {
            TextViewUtils.setIntegerFormatText((TextView) mView.findViewById(R.id.driving_score), scoreStar.getDrivingScore());
            setScoreCircle((ImageView) mView.findViewById(R.id.circle), Math.round(scoreStar.getDrivingScore()));

            View view = mView.findViewById(R.id.pattern_time);
//            setRunTimeText((TextView) view.findViewById(R.id.circle_value), scoreStar.getRunTime());
            TextViewUtils.setRunTimeText((TextView) view.findViewById(R.id.circle_value), scoreStar.getRunTime());

            view = mView.findViewById(R.id.pattern_harsh_accel);
            ((TextView) view.findViewById(R.id.circle_value)).setText(Integer.toString(scoreStar.getHarshAccelCount()));

            view = mView.findViewById(R.id.pattern_top_speed);
            TextViewUtils.setSpeedText((TextView) view.findViewById(R.id.circle_value), scoreStar.getMaxSpeed());
//            ((TextView) view.findViewById(R.id.circle_value)).setText(Integer.toString(scoreStar.getMaxSpeed()));

            view = mView.findViewById(R.id.pattern_distance);
            ((TextView) view.findViewById(R.id.circle_value)).setText(Float.toString(scoreStar.getRunDistance()));

            view = mView.findViewById(R.id.pattern_harsh_decel);
            ((TextView) view.findViewById(R.id.circle_value)).setText(Integer.toString(scoreStar.getHarshBrakeCount()));

            view = mView.findViewById(R.id.pattern_avg_speed);
            int time = scoreStar.getRunTime();
            int distance = (int) (scoreStar.getRunDistance() * 1000);
            if (time > 0) {
                int speed = (int) (distance / time * 3.6);
                ((TextView) view.findViewById(R.id.circle_value)).setText(Integer.toString(speed));
            }

            setScore(mView.findViewById(R.id.pattern_over_speed), scoreStar.getOverSpeedScoreStar());
            setScore(mView.findViewById(R.id.pattern_harsh_speed), scoreStar.getHarshSpeedScoreStar());
            setScore(mView.findViewById(R.id.pattern_engine_load), scoreStar.getHighLoadScoreStar());
            setScore(mView.findViewById(R.id.pattern_idling), scoreStar.getIdlingScoreStar());
            setScore(mView.findViewById(R.id.pattern_eco_speed), scoreStar.getEcoSpeedScoreStar());
            setScore(mView.findViewById(R.id.pattern_fuel_cut), scoreStar.getFuelCutScoreStar());
        }
    }

    @Override
    public void onFailure() {

    }

    private void initView() {
        ((TextView) mView.findViewById(R.id.driving_score)).setText(R.string.empty_int_value);
        ((ImageView) mView.findViewById(R.id.circle)).setImageResource(R.drawable.donut_0);

        View view = mView.findViewById(R.id.pattern_time);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.pattern_drv_time);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_mmss_value);
        view.findViewById(R.id.unit).setVisibility(View.GONE);

        view = mView.findViewById(R.id.pattern_harsh_accel);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.driving_harsh_acceleration);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_int_value);
        ((TextView) view.findViewById(R.id.unit)).setText(R.string.rank_drv_times);

        view = mView.findViewById(R.id.pattern_top_speed);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.pattern_top_speed);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_int_value);
        ((TextView) view.findViewById(R.id.unit)).setText(Unit.KPH.toString());

        view = mView.findViewById(R.id.pattern_distance);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.drv_distance);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_float_value);
        ((TextView) view.findViewById(R.id.unit)).setText(Unit.KM.toString());

        view = mView.findViewById(R.id.pattern_harsh_decel);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.driving_harsh_deceleration);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_int_value);
        ((TextView) view.findViewById(R.id.unit)).setText(R.string.rank_drv_times);

        view = mView.findViewById(R.id.pattern_avg_speed);
        ((TextView) view.findViewById(R.id.title)).setText(R.string.pattern_avg_speed);
        ((TextView) view.findViewById(R.id.circle_value)).setText(R.string.empty_int_value);
        ((TextView) view.findViewById(R.id.unit)).setText(Unit.KPH.toString());

        initScoreView(mView.findViewById(R.id.pattern_over_speed), getString(R.string.pattern_overspeed));
        initScoreView(mView.findViewById(R.id.pattern_harsh_speed), getString(R.string.harsh_speed));
        initScoreView(mView.findViewById(R.id.pattern_engine_load), getString(R.string.engine_load));
        initScoreView(mView.findViewById(R.id.pattern_idling), getString(R.string.pattern_idling));
        initScoreView(mView.findViewById(R.id.pattern_eco_speed), getString(R.string.eco_speed));
        initScoreView(mView.findViewById(R.id.pattern_fuel_cut), getString(R.string.fuel_cut));
    }

    private void initScoreView(View view, String title) {
        ((TextView) view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.driving_score)).setText(R.string.empty_int_value);
        ((ProgressBar) view.findViewById(R.id.progress)).setProgress(0);
    }

    public void updateWeeklyPattern(boolean lastWeek) {
        initView();
        getChildFragmentManager().beginTransaction().add(getWeeklyPatternDialogFragment.newInstance(mAccountId, lastWeek, "weekly"), getWeeklyPatternDialogFragment.TAG)
                .commitAllowingStateLoss();
    }

    private void setScoreCircle(ImageView view, int score) {
        if (score >= 100) {
            view.setImageResource(R.drawable.donut_100);
        } else if (score > 90) {
            view.setImageResource(R.drawable.donut_90);
        } else if (score > 80) {
            view.setImageResource(R.drawable.donut_80);
        } else if (score > 70) {
            view.setImageResource(R.drawable.donut_70);
        } else if (score > 60) {
            view.setImageResource(R.drawable.donut_60);
        } else if (score > 50) {
            view.setImageResource(R.drawable.donut_50);
        } else if (score > 40) {
            view.setImageResource(R.drawable.donut_40);
        } else if (score > 30) {
            view.setImageResource(R.drawable.donut_30);
        } else if (score > 20) {
            view.setImageResource(R.drawable.donut_20);
        } else if (score > 10) {
            view.setImageResource(R.drawable.donut_10);
        } else {
            view.setImageResource(R.drawable.donut_0);
        }
    }

    private void setScore(View view, int value) {
        ((TextView) view.findViewById(R.id.driving_score)).setText(Integer.toString(value));
        ProgressBar pb = (ProgressBar) view.findViewById(R.id.progress);
        if (value > 90) {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_blue));
        } else if (value > 80) {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_sky_blue));
        } else if (value > 70) {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_green));
        } else if (value > 60) {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_yellow));
        } else if (value > 50) {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_orenge));
        } else  {
            pb.setProgressDrawable(getResources().getDrawable(R.drawable.pb_pattern_index_red));
        }
        pb.setProgress(value);
    }

    private void setStarValue(View view, int value) {
//        initStarView(view);
        ((TextView) view.findViewById(R.id.driving_score)).setText(Integer.toString(value));
        ViewGroup ll = (LinearLayout) view.findViewById(R.id.star_group);

        if (value > 80) {
            for (int i = 0; i < 5; i++) {
                ((ImageView)ll.getChildAt(i)).setImageResource(R.drawable.star_blue);
            }
        } else if(value > 60) {
            for (int i = 0; i < 4; i++) {
                ((ImageView)ll.getChildAt(i)).setImageResource(R.drawable.star_green);
            }
        } else if (value > 40) {
            for (int i = 0; i < 3; i++) {
                ((ImageView)ll.getChildAt(i)).setImageResource(R.drawable.star_green);
            }
        } else if (value > 20) {
            for (int i = 0; i < 2; i++) {
                ((ImageView)ll.getChildAt(i)).setImageResource(R.drawable.star_green);
            }
        } else {
            ((ImageView)ll.getChildAt(0)).setImageResource(R.drawable.star_red);
        }
    }

    private void setRunTimeText(TextView view, int second) {
        int h = second / 3600;
        int m = (second % 3600) / 60;
        view.setText(String.format("%02d:%02d", h, m));
    }

    private void initStarView(ViewGroup view) {
        for (int i = 0; i < 5; i++) {
            ImageView iv = new ImageView(getActivity());
            iv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            iv.setImageResource(R.drawable.star_null);

            view.addView(iv);
        }
    }

}

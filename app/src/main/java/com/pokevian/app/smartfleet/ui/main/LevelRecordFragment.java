package com.pokevian.app.smartfleet.ui.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.Rank;
import com.pokevian.app.smartfleet.util.TextViewUtils;

/**
 * Created by ian on 2016-06-12.
 */
public class LevelRecordFragment extends Fragment implements DrivingRecordFragment.OnUpdateListener {

    private View mView;
    private Rank mRecord;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_record_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mRecord = (Rank) savedInstanceState.getSerializable("level-record");
        }

        mView = view;

        init();
        update();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("level-record", mRecord);
    }

    @Override
    public void onUpdate(Rank rank) {
        mRecord = rank;
        if (isVisible()) {
            update();
        }
    }

    private void init() {
        setCircleImageResource(mView.findViewById(R.id.circle), 0);
        setText(mView.findViewById(R.id.circle_value), R.string.empty_int_value);
        setText(mView.findViewById(R.id.circle_unit), R.string.rank_level);

//        init(R.id.record_1, R.drawable.ic_main_avg_score, R.string.hof_point, R.string.empty_int_value, R.string.unit_point);
//        init(R.id.record_2, R.drawable.ic_main_driving_time, R.string.drv_run_time, R.string.empty_hmmss_value, 0);
//        init(R.id.record_3, R.drawable.ic_main_driving_distance, R.string.drv_distance, R.string.empty_int_value, R.string.distance_unit_km);
//        init(R.id.record_4, R.drawable.ic_main_driving_count, R.string.drv_count, R.string.empty_int_value, R.string.rank_drv_times);

        init(R.id.record_1, R.drawable.ic_main_avg_score, R.string.point_residue, R.string.empty_int_value, R.string.unit_point);
        init(R.id.record_2, R.drawable.ic_main_point_all, R.string.point_accumulation, R.string.empty_int_value, R.string.unit_point);
        init(R.id.record_3, R.drawable.ic_main_point_use, R.string.point_used, R.string.empty_int_value, R.string.unit_point);
        init(R.id.record_4, R.drawable.ic_main_point_extinction, R.string.point_extinction, R.string.empty_int_value, R.string.unit_point);
    }

    private void update() {
        if (mRecord != null) {
            float index = 10 * mRecord.getScoreCount() / mRecord.getDrivingLevel();
            setCircleImageResource(mView.findViewById(R.id.circle), index);
            TextViewUtils.setIntegerFormatText((TextView) mView.findViewById(R.id.circle_value), mRecord.getDrivingLevel());

            update(R.id.record_1, mRecord.getMemberPtRes());
            update(R.id.record_2, mRecord.getMemberPtAcc());
            update(R.id.record_3, mRecord.getMemberPtUse());
            update(R.id.record_4, mRecord.getMemberPtExp());
        }
    }

    private void init(int resId, int drawable, int title, int value, int unit) {
        View v = mView.findViewById(resId);
        TextView tv = (TextView) v.findViewById(R.id.record_title);
        tv.setText(title);
        tv.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        ((TextView) v.findViewById(R.id.record_value)).setText(value);
        if (unit == 0) {
            v.findViewById(R.id.record_unit).setVisibility(View.GONE);
        } else {
            ((TextView) v.findViewById(R.id.record_unit)).setText(unit);
        }
    }

    private void update(int resId, int value) {
        View view = mView.findViewById(resId);
        TextViewUtils.setNumberFormatText((TextView) view.findViewById(R.id.record_value), value);
    }

    private void setText(View v, int resId) {
        ((TextView) v).setText(resId);
    }

    private void setCircleImageResource(View view, float index) {
        ((DrivingRecordFragment) getParentFragment()).setCircleImageResource((ImageView) view, index);
    }
}

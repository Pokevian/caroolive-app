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

import org.apache.log4j.Logger;

/**
 * Created by ian on 2016-06-12.
 */
public class FuelCostFragment extends Fragment implements DrivingRecordFragment.OnUpdateListener {

//    private View mView;
    private Rank mFuelCost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       return inflater.inflate(R.layout.fragment_main_record_page, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            mFuelCost = (Rank) savedInstanceState.getSerializable("fuel-cost");
        }

        init();
        update();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Logger.getLogger("DrivingRecordFragment").trace("onSaveInstanceState#" + mFuelCost);
        super.onSaveInstanceState(outState);
        outState.putSerializable("fuel-cost", mFuelCost);
    }

    @Override
    public void onUpdate(Rank rank) {
        mFuelCost = rank;
        Logger.getLogger("DrivingRecordFragment").trace("onUpdate#" + isVisible());
        if (isVisible()) {
            update();
        }
    }

    private void init() {
        View view = getView();
        setCircleImageResource(view.findViewById(R.id.circle), 0);
        setText(view.findViewById(R.id.circle_value), R.string.empty_float_value);
        setText(view.findViewById(R.id.circle_unit), R.string.fuel_economy_unit_kpl);

        init(R.id.record_1, R.drawable.ic_main_fuel_economy, R.string.driving_fuel_consumption, R.string.volume_unit_l);
        init(R.id.record_2, R.drawable.ic_main_fuelpay, R.string.record_fuel_cost);
        init(R.id.record_3, R.drawable.ic_main_fuelsave,R.string.record_fuel_cost_saved);
        init(R.id.record_4, R.drawable.ic_main_fuelsave10k, R.string.record_fuel_cost_saved_tk);
    }

    private void init(int resId, int drawable, int title) {
        init(resId, drawable, title, R.string.unit_won);
    }

    private void init(int resId, int drawable, int title, int unit) {
        View v = getView().findViewById(resId);
        TextView tv = (TextView) v.findViewById(R.id.record_title);
        tv.setText(title);
        tv.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        ((TextView) v.findViewById(R.id.record_value)).setText(R.string.empty_int_value);
        ((TextView) v.findViewById(R.id.record_unit)).setText(unit);
    }

    private void update() {
        if (mFuelCost != null) {
            float index = mFuelCost.getFuelEconomy() / 30 * 10;
            setCircleImageResource(getView().findViewById(R.id.circle), index);
            TextViewUtils.setFuelEconomyText((TextView) getView().findViewById(R.id.circle_value), mFuelCost.getFuelEconomy());

            update(R.id.record_1, mFuelCost.getFuelConsumption());
            update(R.id.record_2, mFuelCost.getFuelCost());
            update(R.id.record_3, mFuelCost.getFuelCostSaved());
            update(R.id.record_4, mFuelCost.getFuelCostSaveTk());
        }
    }

    private void update(int resId, float value) {
        View view = getView().findViewById(resId);
        TextViewUtils.setNumberFormatText((TextView) view.findViewById(R.id.record_value), value);
    }

    private void setText(View v, int resId) {
        ((TextView) v).setText(resId);
    }

    private void setCircleImageResource(View view, float index) {
        ((DrivingRecordFragment) getParentFragment()).setCircleImageResource((ImageView) view, index);
    }
}

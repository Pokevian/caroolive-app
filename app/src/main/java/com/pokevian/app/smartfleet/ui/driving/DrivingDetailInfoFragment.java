/*
 * Copyright (c) 2014. Pokevian Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pokevian.app.smartfleet.ui.driving;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.app.smartfleet.ui.main.MainActivity;
import com.pokevian.app.smartfleet.util.TextViewUtils;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.Unit;

import java.util.ArrayList;

public class DrivingDetailInfoFragment extends Fragment implements OnPageChangeListener {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private LinearLayout mPageIndicatorPane;
    private int mPrevPageIndicatorIdx = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_driving_detail_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPager = (ViewPager) view.findViewById(R.id.pager);
        mPagerAdapter = new PagerAdapter(getChildFragmentManager(), createPageFragments());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(mPagerAdapter.getCount());
        mPager.setOnPageChangeListener(this);

        int position = 1;
        if (savedInstanceState != null) {
            position = savedInstanceState.getInt("position");
        }

        mPageIndicatorPane = (LinearLayout) view.findViewById(R.id.page_indicator_pane);
        int pageCount = mPagerAdapter.getCount();
        int padding = (int) MainActivity.dp2px(getActivity(), 4);
        for (int i = 0; i < pageCount - 2; i++) {
            ImageView indicator = new ImageView(getActivity());
            indicator.setPadding(padding, padding, padding, 0);
            if (i == (position - 1)) {
                indicator.setImageResource(R.drawable.page_select);
            } else {
                indicator.setImageResource(R.drawable.page_non);
            }
            mPageIndicatorPane.addView(indicator);
        }

        mPager.setCurrentItem(position);

        // Set view to transparent
        view.setAlpha(0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("position", mPager.getCurrentItem());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            int currentPosition = mPager.getCurrentItem();
            int lastRealPosition = mPagerAdapter.getCount() - 2;
            if (currentPosition == 0) {
                mPager.setCurrentItem(lastRealPosition, false);
            } else if (currentPosition > lastRealPosition) {
                mPager.setCurrentItem(1, false);
            }
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        // Set previous indicator off
        if (mPrevPageIndicatorIdx != -1) {
            ImageView indicator = (ImageView) mPageIndicatorPane.getChildAt(mPrevPageIndicatorIdx);
            indicator.setImageResource(R.drawable.page_non);
        }

        // Get current indicator index
        int indicatorIdx = position - 1;
        int lastRealPosition = mPagerAdapter.getCount() - 2;
        if (position == 0) {
            indicatorIdx = lastRealPosition - 1;
        } else if (position > lastRealPosition) {
            indicatorIdx = 0;
        }

        // Set current indicator on
        ImageView indicator = (ImageView) mPageIndicatorPane.getChildAt(indicatorIdx);
        indicator.setImageResource(R.drawable.page_select);

        mPrevPageIndicatorIdx = indicatorIdx;
    }

    public void onObdDataReceived(ObdData data) {
        if (!isVisible()) return;

        int item = mPager.getCurrentItem();
        PageFragment fragment = mPagerAdapter.getItem(item);
        if (fragment != null) {
            fragment.onObdDataReceived(data);
        }
    }

    public void onObdExtraDataReceived(float rpm, int vss) {
        if (!isVisible()) return;

        int item = mPager.getCurrentItem();
        PageFragment fragment = mPagerAdapter.getItem(item);
        if (fragment != null) {
            fragment.onObdExtraDataReceived(rpm, vss);
        }
    }

    private ArrayList<PageFragment> createPageFragments() {
        ArrayList<PageFragment> fragments = new ArrayList<PageFragment>();

        int pageCount = 3;

        // Add last real page to first (virtual)
        fragments.add(PageFragment.newInstance(pageCount - 1));

        // Add real pages
        for (int i = 0; i < pageCount; i++) {
            fragments.add(PageFragment.newInstance(i));
        }

        // Add first real page to last (virtual)
        fragments.add(PageFragment.newInstance(0));

        return fragments;
    }

    class PagerAdapter extends FragmentPagerAdapter {

        private final ArrayList<PageFragment> mItems;

        PagerAdapter(FragmentManager fm, ArrayList<PageFragment> items) {
            super(fm);
            mItems = items;
        }

        @Override
        public PageFragment getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

    }

    public static class PageFragment extends Fragment {

        // Page 1
        private static final int VSS = 111;
        private static final int RPM = 112;
        private static final int FUEL_LEVEL = 113;
        private static final int ACCEL_POS = 121;
        private static final int LOAD_PCT = 122;
        private static final int THROTTLE_POSITION = 123;
        private static final int AUX_BAT = 131;
        private static final int COOLANT = 132;
        private static final int MIL = 133;

        // Page 2
        private static final int SHORT_TERM_FUEL_TRIM = 211;
        private static final int LONG_TERM_FUEL_TRIM = 212;
        private static final int FUEL_PRESSURE = 213;
        private static final int INTAKE_AIR_TEMPERATURE = 221;
        private static final int AMBIENT_AIR_TEMPERATURE = 222;
        private static final int BARO = 223;
        private static final int DISTANCE_MIL = 231;
        private static final int ENGINE_OIL_TEMPERATURE = 232;
        private static final int OXYGEN_SENSOR_VOLTAGE = 233;

        // Page 3
        private static final int FUEL_ECONOMY = 311;
        private static final int FUEL_CONSUMPTION = 312;
        private static final int CO2_EMISSION = 313;
        private static final int FUEL_CUT_TIME = 321;
        private static final int HARSH_ACCEL_COUNT = 322;
        private static final int HARSH_BRAKE_COUNT = 323;
        private static final int ECO_SPEED_TIME = 331;
        private static final int OVERSPEED_TIME = 332;
        private static final int IDLING_TIME = 333;

        private int mPage;
        private SparseArray<ValueView> mValues;
        private Unit mSpeedUnit;

        public static PageFragment newInstance(int page) {
            PageFragment fragment = new PageFragment();
            Bundle args = new Bundle();
            args.putInt("page", page);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (savedInstanceState == null) {
                mPage = getArguments().getInt("page");
            } else {
                mPage = savedInstanceState.getInt("page");
            }
            mValues = new SparseArray<>(9);
            mSpeedUnit = SettingsStore.getInstance().getSpeedUnit();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            outState.putInt("page", mPage);

            super.onSaveInstanceState(outState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_driving_detail_info_page, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            switch (mPage) {
                case 0: // Page 1: ACCEL_POS ~ MIL
                    setupPage1(view);
                    break;
                case 1:
                    setupPage2(view);
                    break;
                case 2: // Page 3: FUEL_ECONOMY ~ IDLE_TIME
                    setupPage3(view);
                    break;
                default:
                    break;
            }
        }

        private void setupPage1(View view) {
            mValues.put(VSS, (ValueView) view.findViewById(R.id.value_11));
            mValues.put(RPM, (ValueView) view.findViewById(R.id.value_12));
            mValues.put(FUEL_LEVEL, (ValueView) view.findViewById(R.id.value_13));
            mValues.put(ACCEL_POS, (ValueView) view.findViewById(R.id.value_21));
            mValues.put(LOAD_PCT, (ValueView) view.findViewById(R.id.value_22));
            mValues.put(THROTTLE_POSITION, (ValueView) view.findViewById(R.id.value_23));
            mValues.put(AUX_BAT, (ValueView) view.findViewById(R.id.value_31));
            mValues.put(COOLANT, (ValueView) view.findViewById(R.id.value_32));
            mValues.put(MIL, (ValueView) view.findViewById(R.id.value_33));

            // Set title, icon, unit
            ValueView vv = mValues.get(VSS);
            vv.getTitleView().setText(R.string.driving_value_vehicle_speed);
            vv.getUnitView().setText(mSpeedUnit.toString());

            vv = mValues.get(RPM);
            vv.getTitleView().setText(R.string.driving_value_engine_rpm);
            vv.getUnitView().setText(Unit.RPM.toString());

            vv = mValues.get(FUEL_LEVEL);
            vv.getTitleView().setText(R.string.driving_value_fuel_level);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(ACCEL_POS);
            vv.getTitleView().setText(R.string.driving_value_accel_position);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(LOAD_PCT);
            vv.getTitleView().setText(R.string.driving_value_engine_load);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(THROTTLE_POSITION);
            vv.getTitleView().setText(R.string.driving_value_throttle_position);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(AUX_BAT);
            vv.getTitleView().setText(R.string.driving_value_aux_battery_level);
            vv.getUnitView().setText(Unit.VOLT.toString());
            vv.getIconView().setImageResource(R.drawable.ico_volt_gray);

            vv = mValues.get(COOLANT);
            vv.getTitleView().setText(R.string.driving_value_coolant_temperature);
            vv.getUnitView().setText(Unit.C.toString());
            vv.getIconView().setImageResource(R.drawable.ico_temp_gray);

            vv = mValues.get(MIL);
            vv.getTitleView().setText(R.string.driving_value_mil);
            vv.getUnitView().setText(Unit.EA.toString());
            vv.getIconView().setImageResource(R.drawable.ico_error_gray);
//            vv.setOnLongClickListener(new OnLongClickListener() {
//                public boolean onLongClick(View v) {
//                    ((DrivingActivity) getActivity()).clearStoredDTC();
//                    return true;
//                }
//            });
        }

        private void setupPage2(View view) {
            mValues.put(SHORT_TERM_FUEL_TRIM, (ValueView) view.findViewById(R.id.value_11));
            mValues.put(LONG_TERM_FUEL_TRIM, (ValueView) view.findViewById(R.id.value_12));
            mValues.put(FUEL_PRESSURE, (ValueView) view.findViewById(R.id.value_13));

            mValues.put(INTAKE_AIR_TEMPERATURE, (ValueView) view.findViewById(R.id.value_21));
            mValues.put(AMBIENT_AIR_TEMPERATURE, (ValueView) view.findViewById(R.id.value_22));
            mValues.put(BARO, (ValueView) view.findViewById(R.id.value_23));

            mValues.put(DISTANCE_MIL, (ValueView) view.findViewById(R.id.value_31));
            mValues.put(ENGINE_OIL_TEMPERATURE, (ValueView) view.findViewById(R.id.value_32));
            mValues.put(OXYGEN_SENSOR_VOLTAGE, (ValueView) view.findViewById(R.id.value_33));

            // Set title, icon, unit
            ValueView vv = mValues.get(SHORT_TERM_FUEL_TRIM);
            vv.getTitleView().setText(R.string.driving_value_short_term_fuel_trim);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(LONG_TERM_FUEL_TRIM);
            vv.getTitleView().setText(R.string.driving_value_long_term_fuel_trim);
            vv.getUnitView().setText(Unit.PERCENT.toString());

            vv = mValues.get(FUEL_PRESSURE);
            vv.getTitleView().setText(R.string.driving_value_fule_pressure);
            vv.getUnitView().setText(Unit.KPA.toString());

            vv = mValues.get(INTAKE_AIR_TEMPERATURE);
            vv.getTitleView().setText(R.string.driving_value_intake_air_temperature);
            vv.getUnitView().setText(Unit.C.toString());

            vv = mValues.get(AMBIENT_AIR_TEMPERATURE);
            vv.getTitleView().setText(R.string.driving_value_ambient_air_temperature);
            vv.getUnitView().setText(Unit.C.toString());

            vv = mValues.get(BARO);
            vv.getTitleView().setText(R.string.driving_value_barometer);
            vv.getUnitView().setText(Unit.KPA.toString());

            vv = mValues.get(DISTANCE_MIL);
            vv.getTitleView().setText(R.string.driving_value_distance_mil);
            vv.getUnitView().setText(Unit.KM.toString());
//            vv.getIconView().setImageResource(R.drawable.ico_eco_gray);
            vv.getIconView().setVisibility(View.GONE);

            vv = mValues.get(ENGINE_OIL_TEMPERATURE);
            vv.getTitleView().setText(R.string.driving_value_engine_oil_temperature);
            vv.getUnitView().setText(Unit.C.toString());
//            vv.getIconView().setImageResource(R.drawable.ico_overspeed_gry);
            vv.getIconView().setVisibility(View.GONE);

            vv = mValues.get(OXYGEN_SENSOR_VOLTAGE);
            vv.getTitleView().setText(R.string.driving_value_oxygen_sensor_voltage);
            vv.getUnitView().setText(Unit.VOLT.toString());
//            vv.getIconView().setImageResource(R.drawable.ico_idle_gray);
            vv.getIconView().setVisibility(View.GONE);
        }

        private void setupPage3(View view) {
            mValues.put(FUEL_ECONOMY, (ValueView) view.findViewById(R.id.value_11));
            mValues.put(FUEL_CONSUMPTION, (ValueView) view.findViewById(R.id.value_12));
            mValues.put(CO2_EMISSION, (ValueView) view.findViewById(R.id.value_13));

            mValues.put(FUEL_CUT_TIME, (ValueView) view.findViewById(R.id.value_21));
            mValues.put(HARSH_ACCEL_COUNT, (ValueView) view.findViewById(R.id.value_22));
            mValues.put(HARSH_BRAKE_COUNT, (ValueView) view.findViewById(R.id.value_23));

            mValues.put(ECO_SPEED_TIME, (ValueView) view.findViewById(R.id.value_31));
            mValues.put(OVERSPEED_TIME, (ValueView) view.findViewById(R.id.value_32));
            mValues.put(IDLING_TIME, (ValueView) view.findViewById(R.id.value_33));

            // Set title, icon, unit
            ValueView vv = mValues.get(FUEL_ECONOMY);
            vv.getTitleView().setText(R.string.driving_value_fuel_economy);
            vv.getUnitView().setText(Unit.KPL.toString());

            vv = mValues.get(FUEL_CONSUMPTION);
            vv.getTitleView().setText(R.string.driving_value_fuel_consumption);
            vv.getUnitView().setText(Unit.L.toString());

            vv = mValues.get(CO2_EMISSION);
            vv.getTitleView().setText(R.string.driving_value_co2_emission);
            vv.getUnitView().setText(Unit.KG.toString());

            vv = mValues.get(FUEL_CUT_TIME);
            vv.getTitleView().setText(R.string.driving_value_fuel_cut_time);
            vv.getValueView().setText(R.string.driving_no_time_exp);

            vv = mValues.get(HARSH_ACCEL_COUNT);
            vv.getTitleView().setText(R.string.driving_value_harsh_accel_count);

            vv = mValues.get(HARSH_BRAKE_COUNT);
            vv.getTitleView().setText(R.string.driving_value_harsh_decel_count);

            vv = mValues.get(ECO_SPEED_TIME);
            vv.getTitleView().setText(R.string.driving_value_eco_speed_time);
            vv.getIconView().setImageResource(R.drawable.ico_eco_gray);

            vv = mValues.get(OVERSPEED_TIME);
            vv.getTitleView().setText(R.string.driving_value_overspeed_time);
            vv.getIconView().setImageResource(R.drawable.ico_overspeed_gry);

            vv = mValues.get(IDLING_TIME);
            vv.getTitleView().setText(R.string.driving_value_idling_time);
            vv.getIconView().setImageResource(R.drawable.ico_idle_gray);
        }

        public void onObdDataReceived(ObdData data) {
            if (mValues != null) {
                switch (mPage) {
                    case 0:
                        onObdDataReceivedPage1(data);
                        break;
                    case 1:
                        onObdDataReceivedPage2(data);
                        break;
                    case 2:
                        onObdDataReceivedPage3(data);
                        break;
                    default:
                        break;
                }
            }
        }

        private void onObdDataReceivedPage1(ObdData data) {
            ValueView vv = null;

            if (data.isValid(KEY.SAE_ACCEL_D)) {
                vv = mValues.get(ACCEL_POS);
                vv.getValueView().setText(String.format("%d", data.getFloat(KEY.SAE_ACCEL_D).intValue()));
            }
            if (data.isValid(KEY.SAE_FLI)) {
                vv = mValues.get(FUEL_LEVEL);
                vv.getValueView().setText(String.format("%d", data.getFloat(KEY.SAE_FLI).intValue()));
            }

            if (data.isValid(KEY.SAE_LOAD_PCT)) {
                vv = mValues.get(LOAD_PCT);
                vv.getValueView().setText(String.format("%d", data.getFloat(KEY.SAE_LOAD_PCT).intValue()));
            }
            if (data.isValid(KEY.SAE_TP)) {
                vv = mValues.get(THROTTLE_POSITION);
                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.SAE_TP)));
            }

            if (data.isValid(KEY.CALC_AUX_BAT)) {
                vv = mValues.get(AUX_BAT);
                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.CALC_AUX_BAT)));

                if (data.getBoolean(KEY.WARN_UNDER_AUX_BAT, false)
                        || data.getBoolean(KEY.WARN_OVER_AUX_BAT, false)) {
                    vv.getIconView().setImageResource(R.drawable.ico_volt_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                } else {
                    vv.getIconView().setImageResource(R.drawable.ico_volt_blue);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                }
            }
            if (data.isValid(KEY.SAE_ECT)) {
                vv = mValues.get(COOLANT);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_ECT)));

                if (data.getBoolean(KEY.WARN_OVERHEAT, false)) {
                    vv.getIconView().setImageResource(R.drawable.ico_temp_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                } else {
                    vv.getIconView().setImageResource(R.drawable.ico_temp_blue);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                }
            }

            String dtc = ((DrivingActivity) getActivity()).getDTC(data);
            int dtcCnt = 0;
            if (!TextUtils.isEmpty(dtc)) {
                dtcCnt =  dtc.split(",").length;
            }
            vv = mValues.get(MIL);
            vv.getValueView().setText(String.format("%d", dtcCnt));

            if (dtcCnt > 0) {
                vv.getIconView().setImageResource(R.drawable.ico_error_red);
                vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
            } else {
                vv.getIconView().setImageResource(R.drawable.ico_error_blue);
                vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
            }
        }

        private void onObdDataReceivedPage2(ObdData data) {
            ValueView vv;

            if (data.isValid(KEY.SAE_SFT_B1)) {
                vv = mValues.get(SHORT_TERM_FUEL_TRIM);
                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.SAE_SFT_B1)));
            }
            if (data.isValid(KEY.SAE_LFT_B1)) {
                vv = mValues.get(LONG_TERM_FUEL_TRIM);
                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.SAE_LFT_B1)));
            }
            if (data.isValid(KEY.SAE_FP)) {
                vv = mValues.get(FUEL_PRESSURE);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_FP)));
            }
            if (data.isValid(KEY.SAE_IAT)) {
                vv = mValues.get(INTAKE_AIR_TEMPERATURE);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_IAT)));
            }
            if (data.isValid(KEY.SAE_AAT)) {
                vv = mValues.get(AMBIENT_AIR_TEMPERATURE);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_AAT)));
            }
            if (data.isValid(KEY.SAE_BARO)) {
                vv = mValues.get(BARO);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_BARO)));
            }
            if (data.isValid(KEY.SAE_DIST_MIL)) {
                vv = mValues.get(DISTANCE_MIL);
                vv.getValueView().setText(String.format("%d", data.getInteger(KEY.SAE_DIST_MIL)));
                vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
            }
            if (data.isValid(KEY.SAE_EOT)) {
                int eot = data.getInteger(KEY.SAE_EOT).intValue();
                vv = mValues.get(ENGINE_OIL_TEMPERATURE);
                vv.getValueView().setText(String.format("%d", eot));
                if (eot > 120) {
//                    vv.getIconView().setImageResource(R.drawable.ico_overspeed_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                } else {
//                    vv.getIconView().setImageResource(R.drawable.ico_overspeed_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                }
            }
            if (data.isValid(KEY.SAE_O2S2_V_B1)) {
                float volt = data.getFloat(KEY.SAE_O2S2_V_B1).floatValue();
                vv = mValues.get(OXYGEN_SENSOR_VOLTAGE);
                vv.getValueView().setText(String.format("%.1f", volt));
//                if (volt < 0.5) {
//                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
//                } else {
//                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
//                }
                vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
            }
        }

        private void onObdDataReceivedPage3(ObdData data) {
            ValueView vv;

            if (data.isValid(KEY.TRIP_FUEL_ECONOMY)) {
                vv = mValues.get(FUEL_ECONOMY);
//                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.TRIP_FUEL_ECONOMY)));
                TextViewUtils.setFuelEconomyText(vv.getValueView(), data.getFloat(KEY.TRIP_FUEL_ECONOMY));

            }
            if (data.isValid(KEY.TRIP_FUEL_CONSUMPTION)) {
                vv = mValues.get(FUEL_CONSUMPTION);
//                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.TRIP_FUEL_CONSUMPTION)));
                TextViewUtils.setFuelConsumptionText(vv.getValueView(), data.getFloat(KEY.TRIP_FUEL_CONSUMPTION));

            }
            if (data.isValid(KEY.TRIP_CO2)) {
                vv = mValues.get(CO2_EMISSION);
//                vv.getValueView().setText(String.format("%.1f", data.getFloat(KEY.TRIP_CO2)));
                TextViewUtils.setFloatText(vv.getValueView(), data.getFloat(KEY.TRIP_CO2));
            }

//            if (data.isValid(KEY.TRIP_FUEL_CUT_TIME)) {
                int fuelCut = data.getFloat(KEY.TRIP_FUEL_CUT_TIME, 0).intValue();
                vv = mValues.get(FUEL_CUT_TIME);
                vv.getValueView().setText(DateUtils.formatElapsedTime(fuelCut));
//            }

            if (data.isValid(KEY.TRIP_HARSH_ACCEL_COUNT)) {
                int count = data.getInteger(KEY.TRIP_HARSH_ACCEL_COUNT);
                vv = mValues.get(HARSH_ACCEL_COUNT);
                vv.getValueView().setText(String.format("%d", count));
            }

            if (data.isValid(KEY.TRIP_HARSH_BRAKE_COUNT)) {
                int count = data.getInteger(KEY.TRIP_HARSH_BRAKE_COUNT);
                vv = mValues.get(HARSH_BRAKE_COUNT);
                vv.getValueView().setText(String.format("%d", count));
            }

            int trip_time = data.getFloat(KEY.TRIP_DRIVING_TIME, 0).intValue();

            if (data.isValid(KEY.TRIP_ECO_SPEED_TIME)) {
                int time = data.getFloat(KEY.TRIP_ECO_SPEED_TIME).intValue();
                vv = mValues.get(ECO_SPEED_TIME);
                vv.getValueView().setText(DateUtils.formatElapsedTime(time));

                if (trip_time > 0) {
                    vv.getUnitView().setText(String.format("(%d%%)", time * 100 / trip_time));
                }

                if (time > 0) {
                    vv.getIconView().setImageResource(R.drawable.ico_eco_blue);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                } else {
                    vv.getIconView().setImageResource(R.drawable.ico_eco_gray);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                }
            }

            if (data.isValid(KEY.TRIP_OVERSPEED_TIME)) {
                int time = data.getFloat(KEY.TRIP_OVERSPEED_TIME).intValue();
                vv = mValues.get(OVERSPEED_TIME);
                vv.getValueView().setText(DateUtils.formatElapsedTime(time));

                if (trip_time > 0) {
                    vv.getUnitView().setText(String.format("(%d%%)", time * 100 / trip_time));
                }

                if (time > 0) {
                    vv.getIconView().setImageResource(R.drawable.ico_overspeed_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                } else {
                    vv.getIconView().setImageResource(R.drawable.ico_overspeed_gry);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                }
            }

            if (data.isValid(KEY.TRIP_IDLING_TIME)) {
                int time = data.getFloat(KEY.TRIP_IDLING_TIME).intValue();
                vv = mValues.get(IDLING_TIME);
                vv.getValueView().setText(DateUtils.formatElapsedTime(time));

                if (trip_time > 0) {
                    vv.getUnitView().setText(String.format("(%d%%)", time * 100 / trip_time));
                }

                if (time > 0) {
                    vv.getIconView().setImageResource(R.drawable.ico_idle_red);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_on);
                } else {
                    vv.getIconView().setImageResource(R.drawable.ico_idle_gray);
                    vv.setBackgroundResource(R.drawable.bg_value_dark_large_off);
                }
            }
        }

        public void onObdExtraDataReceived(float rpm, int vss) {
            if (mValues != null) {
                switch (mPage) {
                    case 0: // Page 1: ACCEL_POS ~ MIL
                        onObdExraDataReceivedPage1(rpm, vss);
                        break;
                    case 1: // Page 2: FUEL_ECONOMY ~ IDLE_TIME
                        break;
                    default:
                        break;
                }
            }
        }

        private void onObdExraDataReceivedPage1(float rpm, int vss) {

            ValueView vv = mValues.get(VSS);
            vv.getValueView().setText(String.format("%d", vss));

            vv = mValues.get(RPM);
            vv.getValueView().setText(String.format("%d", (int) rpm));
        }

    }

}

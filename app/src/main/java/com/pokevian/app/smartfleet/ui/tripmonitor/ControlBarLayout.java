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

package com.pokevian.app.smartfleet.ui.tripmonitor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.tripmonitor.NoticeCategory.CategoryData;

import java.util.List;

public class ControlBarLayout extends RelativeLayout {

    private Context mContext;
    private RelativeLayout mLayout;


    private FrameLayout mDatePaneLayout;
    private FrameLayout mChartPaneLayout;
    private FrameLayout mVideoPaneLayout;
    private FrameLayout mNoticePaneLayout;

    private TextView mTextDate;
    private Button mBtnChartList;
    private RadioGroup mRadioGroupChart;
    private RadioButton mRadioChartDay;
    private RadioButton mRadioChartWeek;
    private RadioButton mRadioChartMonth;

    private RadioGroup mRadioGroupVideo;
    private RadioButton mRadioVideoDay;
    private RadioButton mRadioVideoWeek;
    private RadioButton mRadioVideoMonth;
    private RadioButton mRadioVideoAll;

    private RadioGroup mRadioGroupNotice;

    public ControlBarLayout(Context context) {
        super(context);
        init(context);

    }

    public ControlBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    public ControlBarLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {

        mContext = context;

        final LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (RelativeLayout) inflater.inflate(R.layout.view_controlbar, this);

        mDatePaneLayout = (FrameLayout) mLayout.findViewById(R.id.date_pane);
        mChartPaneLayout = (FrameLayout) mLayout.findViewById(R.id.select_chart_period_pane);
        mVideoPaneLayout = (FrameLayout) mLayout.findViewById(R.id.select_video_period_pane);
        mNoticePaneLayout = (FrameLayout) mLayout.findViewById(R.id.select_notice_category_pane);

        mTextDate = (TextView) mLayout.findViewById(R.id.current_date);
        mBtnChartList = (Button) mLayout.findViewById(R.id.btn_chart_list);

        mRadioGroupChart = (RadioGroup) mLayout.findViewById(R.id.radiogroup_chart);
        mRadioChartDay = (RadioButton) mLayout.findViewById(R.id.btn_chart_day);
        mRadioChartWeek = (RadioButton) mLayout.findViewById(R.id.btn_chart_week);
        mRadioChartMonth = (RadioButton) mLayout.findViewById(R.id.btn_chart_month);

        mRadioGroupVideo = (RadioGroup) mLayout.findViewById(R.id.radiogroup_video);
        mRadioVideoDay = (RadioButton) mLayout.findViewById(R.id.btn_video_day);
        mRadioVideoWeek = (RadioButton) mLayout.findViewById(R.id.btn_video_week);
        mRadioVideoMonth = (RadioButton) mLayout.findViewById(R.id.btn_video_month);
        mRadioVideoAll = (RadioButton) mLayout.findViewById(R.id.btn_video_all);

        mRadioGroupNotice = (RadioGroup) mLayout.findViewById(R.id.radiogroup_notice);

    }

    public void enableDateContoller(boolean enable) {

        mDatePaneLayout.setVisibility(enable ? View.VISIBLE : View.GONE);

    }

    public void enableChartContoller(boolean enable) {

        mBtnChartList.setVisibility(enable ? View.VISIBLE : View.GONE);
        mChartPaneLayout.setVisibility(enable ? View.VISIBLE : View.GONE);

    }

    public void enableVideoContoller(boolean enable) {

        mVideoPaneLayout.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    public void enableNoticeContoller(boolean enable) {

        mNoticePaneLayout.setVisibility(enable ? View.VISIBLE : View.GONE);

    }


    public TextView getTextDate() {
        return mTextDate;
    }

    public Button getBtnChartList() {
        return mBtnChartList;
    }

    public RadioGroup getRadioGroupChart() {
        return mRadioGroupChart;
    }

    public RadioButton getRadioChartDay() {
        return mRadioChartDay;
    }

    public RadioButton getRadioChartWeek() {
        return mRadioChartWeek;
    }

    public RadioButton getRadioChartMonth() {
        return mRadioChartMonth;
    }

    public RadioGroup getRadioGroupVideo() {
        return mRadioGroupVideo;
    }

    public RadioButton getRadioVideoDay() {
        return mRadioVideoDay;
    }

    public RadioButton getRadioVideoWeek() {
        return mRadioVideoWeek;
    }

    public RadioButton getRadioVideoMonth() {
        return mRadioVideoMonth;
    }

    public RadioButton getRadioVideoAll() {
        return mRadioVideoAll;
    }

    public RadioGroup getRadioGroupNotice() {
        return mRadioGroupNotice;
    }

    public void makeNoticeCategory(List<CategoryData> categoryList) {

        if (categoryList == null || categoryList.size() == 0) {
            return;
        }


        // Add 'All' Category
        RadioButton rb = new RadioButton(mContext);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams p = new RadioGroup.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );

        rb.setText(getResources().getString(R.string.notice_category_all));
        //rb.setButtonDrawable(getResources().getDrawable(R.drawable.btn_notice_period));
        rb.setBackgroundResource(R.drawable.btn_notice_period);
        //rb.setBackground(getResources().getDrawable(R.drawable.btn_notice_period));
        rb.setButtonDrawable(new StateListDrawable());
        rb.setGravity(Gravity.CENTER);
        rb.setTextColor(Color.parseColor("#666666"));
        //rb.setId(View.generateViewId());

        mRadioGroupNotice.addView(rb, p);

        ImageView iv = new ImageView(mContext);
        iv.setImageResource(R.drawable.tab_divide_01);

        mRadioGroupNotice.addView(iv, params);


        // Add category from server

        int index = 0;
        for (CategoryData item : categoryList) {

            if (mContext == null) {
                return;
            }

            rb = new RadioButton(mContext);
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            p = new RadioGroup.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
            );

            rb.setText(item.getCATEGORY_NM());
            //rb.setBackground(getResources().getDrawable(R.drawable.btn_notice_period));
            rb.setBackgroundResource(R.drawable.btn_notice_period);
            //rb.setButtonDrawable(getResources().getDrawable(R.drawable.btn_notice_period));
            rb.setButtonDrawable(new StateListDrawable());
            rb.setGravity(Gravity.CENTER);
            rb.setTextColor(Color.parseColor("#666666"));
            //rb.setId(View.generateViewId());

            mRadioGroupNotice.addView(rb, p);

            if (index == categoryList.size() - 1) {
                break;
            }


            iv = new ImageView(mContext);
            iv.setImageResource(R.drawable.tab_divide_01);

            mRadioGroupNotice.addView(iv, params);


            index++;

        }

        mRadioGroupNotice.invalidate();
    }
}

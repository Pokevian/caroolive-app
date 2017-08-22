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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;

import java.util.ArrayList;

public class ChartTypeFragment extends DialogFragment {

    private ListView mListChart;
    private ChartTypeListener mChartTypeListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chart_type, container, false);

        getDialog().setTitle(getResources().getString(R.string.popup_title_chart_type));

        mListChart = (ListView) rootView.findViewById(R.id.list_chart_type);

        String[] chartTypes = getResources().getStringArray(R.array.chart_items);
        ArrayList<String> chartItems = new ArrayList<>();
        for (String chartType : chartTypes) {
            chartItems.add(chartType);
        }

        ChartTypeAdapter adapterChart = new ChartTypeAdapter(getActivity(), chartItems);

        mListChart.setAdapter(adapterChart);
        mListChart.setOnItemClickListener(new ChartItemClickListener());

        return rootView;
    }

    public ChartTypeListener getChartTypeListener() {
        return mChartTypeListener;
    }

    public void setChartTypeListener(ChartTypeListener chartTypeListener) {
        this.mChartTypeListener = chartTypeListener;
    }


    public class ChartTypeAdapter extends BaseAdapter {

        private Context mContext;
        private ArrayList<String> mItems;

        public ChartTypeAdapter(Context context, ArrayList<String> items) {
            mContext = context;
            mItems = items;

        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.chart_row_menu, null);
            }

            TextView menuTitle = (TextView) convertView.findViewById(R.id.row_title);
            menuTitle.setText(mItems.get(position));

            return convertView;
        }

    }

    private class ChartItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mChartTypeListener != null) {
                mChartTypeListener.onItemSelected(position);
            }
        }
    }

}

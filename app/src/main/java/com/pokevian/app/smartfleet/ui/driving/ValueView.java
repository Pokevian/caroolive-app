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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.main.MainActivity;


public class ValueView extends LinearLayout {

    private TextView mTitleText;
    private ImageView mIconImg;
    private TextView mValueText;
    private TextView mUnitText;

    private int mLayoutResId;
    private boolean mhasLargeIcon;

//    public ValueView(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//        init(context);
//    }

    public ValueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.ValueView, 0, 0);

        try {
            mLayoutResId = ta.getResourceId(R.styleable.ValueView_viewLayout, R.layout.widget_value);
            mhasLargeIcon = ta.getBoolean(R.styleable.ValueView_includeLargeIcon, true);
        } finally {
            ta.recycle();
        }

        init(context);
    }

    public ValueView(Context context) {
        super(context);
        init(context);
    }

//    public ValueView(Context context, int resource) {
//        super(context);
//        init(context, resource);
//    }


    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        int padding = MainActivity.dp2px(context, 4);
        setPadding(padding, padding, padding, padding);

        View view = LayoutInflater.from(context).inflate(mLayoutResId, this);
        mTitleText = (TextView) view.findViewById(R.id.title);
        mIconImg = (ImageView) view.findViewById(R.id.icon);
        mValueText = (TextView) view.findViewById(R.id.circle_value);
        mUnitText = (TextView) view.findViewById(R.id.unit);

        if (!mhasLargeIcon) {
            mIconImg.setVisibility(View.GONE);
        }
    }

    public TextView getTitleView() {
        return mTitleText;
    }

    public ImageView getIconView() {
        return mIconImg;
    }

    public TextView getValueView() {
        return mValueText;
    }

    public TextView getUnitView() {
        return mUnitText;
    }

}

package com.pokevian.app.smartfleet.ui.driving;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;


public class SpeedMeter extends LinearLayout {

    private TextView mValueText;
    private TextView mUnitText;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SpeedMeter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public SpeedMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SpeedMeter(Context context) {
        super(context);
        init(context, null);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);

        // value
        mValueText = new TextView(context);
        mValueText.setGravity(Gravity.CENTER);
        mValueText.setIncludeFontPadding(false);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mValueText, params);

        // unit
        mUnitText = new TextView(context);
        mUnitText.setGravity(Gravity.CENTER);
        mUnitText.setIncludeFontPadding(false);
        params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -10,
                getResources().getDisplayMetrics());
        params.topMargin = topMargin;
        addView(mUnitText, params);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SpeedMeter);

            // value
            CharSequence text = a.getText(R.styleable.SpeedMeter_speedText);
            if (text != null) {
                mValueText.setText(text);
            }
            int color = a.getColor(R.styleable.SpeedMeter_speedTextColor, 0);
            if (color != 0) {
                mValueText.setTextColor(color);
            }
            int textSize = a.getDimensionPixelSize(R.styleable.SpeedMeter_speedTextSize, 65);
            mValueText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            int backgroundResId = a.getResourceId(R.styleable.SpeedMeter_speedBackground, 0);
            if (backgroundResId != 0) {
                mValueText.setBackgroundResource(backgroundResId);
            } else {
                color = a.getColor(R.styleable.SpeedMeter_speedBackground, Color.TRANSPARENT);
                mValueText.setBackgroundColor(color);
            }

            // unit
            text = a.getText(R.styleable.SpeedMeter_speedUnitText);
            if (text != null) {
                mUnitText.setText(text);
            }
            color = a.getColor(R.styleable.SpeedMeter_speedUnitTextColor, 0);
            if (color != 0) {
                mUnitText.setTextColor(color);
            }
            textSize = a.getDimensionPixelSize(R.styleable.SpeedMeter_speedUnitTextSize, 12);
            mUnitText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            backgroundResId = a.getResourceId(R.styleable.SpeedMeter_speedUnitBackground, 0);
            if (backgroundResId != 0) {
                mUnitText.setBackgroundResource(backgroundResId);
            } else {
                color = a.getColor(R.styleable.SpeedMeter_speedUnitBackground, 0);
                if (color != 0) {
                    mUnitText.setBackgroundColor(color);
                }
            }

            int textShadowColor = a.getColor(R.styleable.SpeedMeter_speedTextShadowColor, 0);
            float textShadowDx = a.getFloat(R.styleable.SpeedMeter_speedTextShadowDx, 0);
            float textShadowDy = a.getFloat(R.styleable.SpeedMeter_speedTextShadowDy, 0);
            float textShadowRadius = a.getFloat(R.styleable.SpeedMeter_speedTextShadowRadius, 0);
            if (textShadowColor != 0) {
                mValueText.setShadowLayer(textShadowRadius, textShadowDx, textShadowDy, textShadowColor);
            }

            int shadowColor = a.getColor(R.styleable.SpeedMeter_speedUnitShadowColor, 0);
            float shadowDx = a.getFloat(R.styleable.SpeedMeter_speedUnitShadowDx, 0);
            float shadowDy = a.getFloat(R.styleable.SpeedMeter_speedUnitShadowDy, 0);
            float shadowRadius = a.getFloat(R.styleable.SpeedMeter_speedUnitShadowRadius, 0);
            if (shadowColor != 0) {
                mUnitText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
            }

            a.recycle();
        }
    }

    public void setValueText(CharSequence text) {
        mValueText.setText(text);
    }

    public void setUnitText(CharSequence text) {
        mUnitText.setText(text);
    }

}

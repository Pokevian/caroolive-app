/*
 * Copyright (c) 2015. Pokevian Ltd.
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

package com.pokevian.app.smartfleet.service.floatinghead;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.util.TextViewUtils;
import com.pokevian.app.smartfleet.widget.CheckableImageView;

import org.apache.log4j.Logger;


/**
 * Created by dg.kim on 2015-04-15.
 */
public class DrivingView extends FrameLayout {

    public DrivingView(Context context) {
        super(context);
    }

    public DrivingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrivingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void setObdEnabled(boolean enabled) {
        findViewById(R.id.ft_obd).setEnabled(enabled);
    }

    public void setBatteryEnabled(boolean enabled) {
        findViewById(R.id.ft_battery).setEnabled(enabled);
        if (!enabled) {
            setBatteryWarning(false);
        }
        Log.i("floating", "setBatteryEnabled#" + enabled);
    }

    public void setCoolantEnabled(boolean enabled) {
        findViewById(R.id.ft_coolant).setEnabled(enabled);
        if (!enabled) {
            setCoolantWarning(false);
        }
        Log.i("floating", "setCoolantEnabled#" + enabled);
    }

    public void setMilEnabled(boolean enabled) {
        findViewById(R.id.ft_mil).setEnabled(enabled);
    }

    public void setFuelEconomy(float value) {
        if (value > -1) {
            TextViewUtils.setFuelEconomyText((TextView) findViewById(R.id.ft_fuel), value > 99.9f ? 99.9f : value);
        } else {
            ((TextView) findViewById(R.id.ft_fuel)).setText(getContext().getString(R.string.empty_float_value));
        }
    }

    public void setFuelCut() {
        ((TextView) findViewById(R.id.ft_fuel)).setText("MAX");
    }


    public void setBatteryWarning(boolean checked) {
        ((CheckableImageView) findViewById(R.id.ft_battery)).setChecked(checked);
    }

    public void setCoolantWarning(boolean checked) {
        ((CheckableImageView) findViewById(R.id.ft_coolant)).setChecked(checked);
    }

    public void init() {
        setObdEnabled(false);
        clearEngineLamp();

    }

    public void clearEngineLamp() {
        setFuelEconomy(-1f);
        setBatteryEnabled(false);
        setCoolantEnabled(false);
        setMilEnabled(false);
        setBackground(0);
        setRpm(-1f);
        setVss(-1);
        findViewById(R.id.ft_fuel_ico).setVisibility(View.INVISIBLE);
    }

//    public void onEngineOn() {
//        setBatteryEnabled(true);
//        setCoolantEnabled(true);
//    }

    protected void setBackground(int level) {
        int id = R.drawable.bg_floating;

        if (level == 1) {
            id = R.drawable.bg_floating_green;
        } else if (level == 2 || level == 3) {
            id = R.drawable.bg_floating_yellow;
        } else if (level == 4 || level == 5) {
            id = R.drawable.bg_floating_red;
        }

        findViewById(R.id.driving).setBackgroundResource(id);
    }

    private void setViewChecked(int id, boolean checked) {
        ((CheckableImageView) findViewById(R.id.ft_battery)).setChecked(checked);
    }

    public void setRpm(float rpm) {
        TextView tv = (TextView) findViewById(R.id.ft_rpm);
        if (rpm > -1) {
            TextViewUtils.setIntegerFormatText(tv, rpm);
        } else {
            tv.setText(getContext().getString(R.string.empty_int_value));
        }
    }

    public void setVss(int vss) {
        TextView tv = (TextView) findViewById(R.id.ft_vss);
        if (vss > -1) {
            TextViewUtils.setIntegerFormatText(tv, vss);
        } else {
            tv.setText(getContext().getString(R.string.empty_int_value));
        }
    }

    public void setFuelEonomy(boolean instant) {
        View v = findViewById(R.id.ft_fuel_ico);
        v.setVisibility(VISIBLE);
        v.setEnabled(instant);
    }
}

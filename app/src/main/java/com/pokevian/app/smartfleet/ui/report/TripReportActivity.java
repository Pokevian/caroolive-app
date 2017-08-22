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

package com.pokevian.app.smartfleet.ui.report;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.pokevian.app.smartfleet.model.VehicleData.VehicleTrip;
import com.pokevian.app.smartfleet.service.TripReportService;
import com.pokevian.app.smartfleet.ui.BaseActivity;

import org.apache.log4j.Logger;

public class TripReportActivity extends BaseActivity {

    public static final String EXTRA_TRIP = "extra.TRIP";

    private VehicleTrip mTrip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.getLogger("trip").debug("onCreate@activity");

        if (savedInstanceState == null) {
            Intent data = getIntent();
            mTrip = (VehicleTrip) data.getSerializableExtra(EXTRA_TRIP);
        } else {
            mTrip = (VehicleTrip) savedInstanceState.getSerializable(EXTRA_TRIP);
        }

        TextView tv = new TextView(this);
        tv.setText(mTrip.toString());
        setContentView(tv);

        // stop trip report service
        Intent service = new Intent(getApplicationContext(), TripReportService.class);
        stopService(service);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_TRIP, mTrip);
        super.onSaveInstanceState(outState);
    }

}

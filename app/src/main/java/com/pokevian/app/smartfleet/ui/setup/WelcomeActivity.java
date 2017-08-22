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

package com.pokevian.app.smartfleet.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.ui.BaseActivity;

public class WelcomeActivity extends BaseActivity {

    public static final String EXTRA_NEED_TO_REGISTER_VEHICLE = "extra.NEED_TO_REGISTER_VEHICLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = new PlaceholderFragment();
            fm.beginTransaction().add(R.id.container, fragment).commit();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back key
    }

    public static class PlaceholderFragment extends Fragment implements OnClickListener {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_welcome, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Button prevBtn = (Button) view.findViewById(R.id.prev_btn);
            prevBtn.setText(R.string.btn_later);
            prevBtn.setOnClickListener(this);

            Button nextBtn = (Button) view.findViewById(R.id.next_btn);
            nextBtn.setText(R.string.btn_register_vehicle);
            nextBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.prev_btn) {
                Intent result = new Intent();
                result.putExtra(EXTRA_NEED_TO_REGISTER_VEHICLE, false);

                getActivity().setResult(RESULT_OK, result);
                getActivity().finish();
            } else if (id == R.id.next_btn) {
                Intent result = new Intent();
                result.putExtra(EXTRA_NEED_TO_REGISTER_VEHICLE, true);

                getActivity().setResult(RESULT_OK, result);
                getActivity().finish();
            }
        }

    }

}

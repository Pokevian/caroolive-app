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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.setting.ServerUrl;
import com.pokevian.app.smartfleet.ui.common.WebViewFragment;

public class DisclaimerFragment extends Fragment implements OnClickListener, OnCheckedChangeListener {

    public static final String TAG = "DisclaimerFragment";

    private Button mNextBtn;
    private DisclaimerCallbacks mCallbacks;
    private CheckBox mCbPersonal, mCbService, mCbLocation, mCbAll;

    public static DisclaimerFragment newInstance() {
        DisclaimerFragment fragment = new DisclaimerFragment();
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbacks = (DisclaimerCallbacks) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity + " must implement AgreementCallbacks");
        }
        if (mCallbacks == null) {
            try {
                mCallbacks = (DisclaimerCallbacks) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity + " must implement AgreementCallbacks");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_disclaimer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.detail_personal).setOnClickListener(this);
        view.findViewById(R.id.detail_service).setOnClickListener(this);
        view.findViewById(R.id.detail_location).setOnClickListener(this);

        mCbPersonal = (CheckBox) view.findViewById(R.id.agree_personal);
        mCbPersonal.setOnCheckedChangeListener(this);
        mCbService = (CheckBox) view.findViewById(R.id.agree_service);
        mCbService.setOnCheckedChangeListener(this);
        mCbLocation = (CheckBox) view.findViewById(R.id.agree_location);
        mCbLocation.setOnCheckedChangeListener(this);
        mCbAll = (CheckBox) view.findViewById(R.id.agree_all);
        mCbAll.setOnCheckedChangeListener(this);

        Button prevBtn = (Button) view.findViewById(R.id.prev_btn);
        prevBtn.setVisibility(View.INVISIBLE);

        mNextBtn = (Button) view.findViewById(R.id.next_btn);
        mNextBtn.setEnabled(false);
        mNextBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (R.id.detail_personal == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_PERSONAL);
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        } else if (R.id.detail_service == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_SERVICE);
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        } else if (R.id.detail_location == id) {
            WebViewFragment fragment = WebViewFragment.newInstance(ServerUrl.CONTRACT_DETAIL_LOCATION);
            getActivity().getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).addToBackStack(null).commit();
        }  else if (id == R.id.next_btn) {
            mCallbacks.onDisclaimerAgreed();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();

        if (R.id.agree_personal == id) {
            mNextBtn.setEnabled(isAgree());
        } else if (R.id.agree_service == id) {
            mNextBtn.setEnabled(isAgree());
        } else if (R.id.agree_location == id) {
            mNextBtn.setEnabled(isAgree());
        } else if (R.id.agree_all == id && isChecked) {
            mCbPersonal.setChecked(true);
            mCbService.setChecked(true);
            mCbLocation.setChecked(true);
        }
        if (!isChecked) {
            checkedAgreeAllCheckBox();
        }
    }

    private void checkedAgreeAllCheckBox() {
        if (mCbAll.isChecked() && !isAgree()) {
            mCbAll.setChecked(false);
        }
    }

    private boolean isAgree() {
        return mCbPersonal.isChecked() && mCbService.isChecked() && mCbLocation.isChecked();
    }

    public interface DisclaimerCallbacks {
        void onDisclaimerAgreed();
    }

}

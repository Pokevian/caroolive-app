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

package com.pokevian.app.smartfleet.ui.video;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.pokevian.app.smartfleet.R;
import com.pokevian.app.smartfleet.model.BlackboxMetadata;


@SuppressLint("ValidFragment")
public class PlayerInfoFragment extends Fragment {

    private static final int MSG_UPDATE_CAPTION = 0;

    private View mfragmentView;
    private ImageView mEcoDrivingLamp;
    private ImageView mOverSpeedLamp;
    private ImageView mIdlingLamp;
    private ImageView mAccDecLamp;
    private ImageView mTroubleLamp;

    public PlayerInfoFragment() {

    }

    public static Fragment newInstance() {
        return new PlayerInfoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mfragmentView = inflater.inflate(R.layout.fragment_player_info, container, false);
        mfragmentView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);

        mEcoDrivingLamp = (ImageView) mfragmentView.findViewById(R.id.ecodriving_lamp);
        mOverSpeedLamp = (ImageView) mfragmentView.findViewById(R.id.overspeed_lamp);
        mIdlingLamp = (ImageView) mfragmentView.findViewById(R.id.idling_lamp);
        mAccDecLamp = (ImageView) mfragmentView.findViewById(R.id.accdec_lamp);
        mTroubleLamp = (ImageView) mfragmentView.findViewById(R.id.trouble_lamp);

        return mfragmentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (mListener != null) {
            mListener.onCreatedView();
        }
        super.onActivityCreated(savedInstanceState);
    }

    public void updateCaption(int pos, BlackboxMetadata meta) {
        Message msg = mHandler.obtainMessage(MSG_UPDATE_CAPTION);
        msg.obj = meta;
        msg.arg1 = pos;
        mHandler.sendMessage(msg);
    }


    PlayerInfoFragmentListener mListener;

    public interface PlayerInfoFragmentListener {
        void onCreatedView();

        void onGlobalLayout();
    }

    public void registerCreateViewDoneListener(
            PlayerInfoFragmentListener listener) {
        mListener = listener;
    }

    ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        @SuppressWarnings("deprecation")
        @SuppressLint("NewApi")
        @Override
        public void onGlobalLayout() {
            /*if (!SdkUtils.isJellyBeanSupported()) {*/
            if (Build.VERSION_CODES.JELLY_BEAN <= Build.VERSION.SDK_INT) {
                mfragmentView.getViewTreeObserver()
                        .removeGlobalOnLayoutListener(this);
            } else {
                mfragmentView.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);
            }

            if (mListener != null) {
                mListener.onGlobalLayout();
            }
        }
    };

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CAPTION:
                    BlackboxMetadata meta = (BlackboxMetadata) msg.obj;
                    updateLamp(meta);
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    private void updateLamp(final BlackboxMetadata meta) {
        if (mEcoDrivingLamp != null) {
            if (meta.speedData.isEconomySpeed) mEcoDrivingLamp.setImageResource(R.drawable.ic_lamp_green);
            else mEcoDrivingLamp.setImageResource(R.drawable.ic_lamp_normal);
        }
        if (mOverSpeedLamp != null) {
            if (meta.speedData.isOverSpeed) mOverSpeedLamp.setImageResource(R.drawable.ic_lamp_red);
            else mOverSpeedLamp.setImageResource(R.drawable.ic_lamp_normal);
        }
        if (mIdlingLamp != null) {
            if (meta.speedData.isIdling) mIdlingLamp.setImageResource(R.drawable.ic_lamp_red);
            else mIdlingLamp.setImageResource(R.drawable.ic_lamp_normal);
        }
        if (mAccDecLamp != null) {
            if (meta.speedData.isHarshAccel || meta.speedData.isHarshBrake)
                mAccDecLamp.setImageResource(R.drawable.ic_lamp_red);
            else mAccDecLamp.setImageResource(R.drawable.ic_lamp_normal);
        }
        if (mTroubleLamp != null) {
            if (meta.diagnosticData.mil) mTroubleLamp.setImageResource(R.drawable.ic_lamp_red);
            else mTroubleLamp.setImageResource(R.drawable.ic_lamp_normal);
        }
    }

}

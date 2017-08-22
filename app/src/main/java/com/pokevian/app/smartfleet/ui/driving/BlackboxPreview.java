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

import android.app.Dialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import com.pokevian.app.smartfleet.R;

import org.apache.log4j.Logger;

public class BlackboxPreview extends Dialog {

    static final String TAG = "BlackboxPreview";
    final Logger logger = Logger.getLogger(TAG);

    protected View mContentView;
    private View mControlView;

    private int mVisibility = View.GONE;
    private int mInvisiblePosX;
    private int mInvisiblePosY;

    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener;

    public BlackboxPreview(Context context, int contentLayoutResId) {
        super(context, R.style.AppTheme_Transparent_NoDim);

        toSystemWindow();

        mContentView = LayoutInflater.from(context).inflate(contentLayoutResId, null);
        setContentView(mContentView);

        ViewGroup controlPanel = (ViewGroup) mContentView.findViewById(R.id.preview_control);
//                    mControlView = LayoutInflater.from(getContext()).inflate(R.layout.blackbox_preview_control, controlPanel);
        mControlView = LayoutInflater.from(getContext()).inflate(R.layout.blackbox_preview_overlay, controlPanel);

        initInvisiblePositions(context);

        setCallback();
    }

    protected void setCallback() {
        final SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_surface);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            private boolean once = false;

            public void surfaceCreated(SurfaceHolder holder) {
                logger.debug("blackbox preview surface created");
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                logger.debug("blackbox preview surface destroyed");
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                logger.debug("blackbox preview surface changed: size=" + width + "x" + height);

                // check landscape
                if (width > height && !once) {
                    once = true;

                    ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
                    lp.width = width - 2;
                    lp.height = height;
                    surfaceView.setLayoutParams(lp);

//                    ViewGroup controlPanel = (ViewGroup) mContentView.findViewById(R.id.preview_control);
////                    mControlView = LayoutInflater.from(getContext()).inflate(R.layout.blackbox_preview_control, controlPanel);
//                    mControlView = LayoutInflater.from(getContext()).inflate(R.layout.blackbox_preview_overlay, controlPanel);
                }
            }
        });
    }

    private void initInvisiblePositions(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int maxPixels = dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;
        logger.debug("initInvisiblePositions(): maxPixels=" + maxPixels);

        mInvisiblePosX = -1 * maxPixels;
        mInvisiblePosY = -1 * maxPixels;

        updatePosition(mInvisiblePosX, mInvisiblePosY);
    }

    public int getVisibility() {
        synchronized (this) {
            return mVisibility;
        }
    }

    public void setVisibility(int visibility) {
        if (mVisibility == visibility) return;

        synchronized (this) {
            logger.debug("setVisibility() : visibility=" + visibility);
            if (visibility == View.VISIBLE) {
                mVisibility = visibility;
                updatePosition(0, 0);
            } else {
                mVisibility = visibility;
                updatePosition(mInvisiblePosX, mInvisiblePosY);
            }
        }
    }

    private void toSystemWindow() {
        Window window = getWindow();

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = WindowManager.LayoutParams.TYPE_PHONE;
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.x = mInvisiblePosX;
        lp.y = mInvisiblePosY;
        lp.windowAnimations = 0;
        window.setAttributes(lp);

        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        window.setFlags(flags, flags);
    }

    private void updatePosition(int x, int y) {
        Window window = getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = x;
        params.y = y;
        window.setAttributes(params);
    }

}

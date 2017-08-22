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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by dg.kim on 2015-03-23.
 */
abstract class BaseFloatingHeadWindow {

    protected static final String TAG = "floating-head-window";

    private final Context mContext;
    private final Integer mTag;
    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private View mView;
    private boolean mViewAdded;

    private ObjectAnimator mAnimator;

    protected BaseFloatingHeadWindow(Context context, Integer tag) {
        this(context, tag, new DecelerateInterpolator(1.0f));
    }

    protected BaseFloatingHeadWindow(Context context, Integer tag, Interpolator interpolator) {
        mContext = context;
        mTag = tag;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        create(interpolator);
    }

    protected Context getContext() {
        return mContext;
    }

    public Integer getTag() {
        return mTag;
    }

    private void create(Interpolator interpolator) {
        mView = onCreateView(LayoutInflater.from(mContext));

        mLayoutParams = createLayoutParams();

        mAnimator = ObjectAnimator.ofPropertyValuesHolder(this);
        mAnimator.setInterpolator(interpolator);
        mAnimator.addListener(new AnimatorListener());
    }

    protected abstract View onCreateView(LayoutInflater inflater);

    public void destroy() {
        onDestroyedView();

        hide();
        mView = null;
//        mLayoutParams = null;

        mAnimator.removeAllListeners();
        mAnimator = null;
    }

    protected abstract void onDestroyedView();

    protected WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = -mView.getWidth();
        params.y = -mView.getHeight();
        return params;
    }

    public void show() {
        if (mView != null && !mViewAdded) {
            mWindowManager.addView(mView, mLayoutParams);
            mViewAdded = true;
        }
    }

    public void hide() {
        if (mView != null && mViewAdded) {
            mWindowManager.removeView(mView);
            mViewAdded = false;
        }
    }

    public abstract void onOrientationChanged(int orientation);

    public void moveTo(Point pos) {
        if (pos != null) {
            moveTo(pos.x, pos.y);
        }
    }

    public void moveTo(int x, int y) {
        if (mView != null && mViewAdded) {
            mLayoutParams.x = x;
            mLayoutParams.y = y;
            mWindowManager.updateViewLayout(mView, mLayoutParams);
        }
    }

    public void moveBy(int dx, int dy) {
        if (mView != null && mViewAdded) {
            mLayoutParams.x += dx;
            mLayoutParams.y += dy;
            mWindowManager.updateViewLayout(mView, mLayoutParams);
        }
    }

    public int getX() {
        return mLayoutParams.x;
    }

    public void setX(int x) {
        if (mView != null && mViewAdded) {
            mLayoutParams.x = x;
            mWindowManager.updateViewLayout(mView, mLayoutParams);
        }
    }

    public int getY() {
        return mLayoutParams.y;
    }

    public void setY(int y) {
        if (mView != null && mViewAdded) {
            mLayoutParams.y = y;
            mWindowManager.updateViewLayout(mView, mLayoutParams);
        }
    }

    public int getWidth() {
        if (mView != null) {
            return mView.getMeasuredWidth();
        } else {
            return 0;
        }
    }

    public int getHeight() {
        if (mView != null) {
            return mView.getMeasuredHeight();
        } else {
            return 0;
        }
    }

    public Rect getBounds() {
        return new Rect(getX(), getY(), getX() + getWidth(), getY() + getHeight());
    }

    protected int getPixelSize(int dimensionResId) {
        return mContext.getResources().getDimensionPixelSize(dimensionResId);
    }

    public void animateTo(Point pos) {
        if (pos != null) {
            animateTo(pos.x, pos.y);
        }
    }

    public void animateTo(int x, int y) {
        if (mAnimator != null) {
            PropertyValuesHolder xHolder = PropertyValuesHolder.ofInt("x", x);
            PropertyValuesHolder yHolder = PropertyValuesHolder.ofInt("y", y);
            mAnimator.setValues(xHolder, yHolder);
            mAnimator.setDuration(200);
            mAnimator.start();
        }
    }

    public void cancelAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    private class AnimatorListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            BaseFloatingHeadWindow.this.onAnimationEnd();
        }
    }

    protected boolean isOnAnimating() {
        if (mAnimator != null) {
            return mAnimator.isStarted() && mAnimator.isRunning();
        }
        return false;
    }

    protected void onAnimationEnd() {
    }
}

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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import org.apache.log4j.Logger;

/**
 * Created by dg.kim on 2015-04-15.
 */
public class FloatingHeadView extends FrameLayout {

    private static final String TAG = "FloatingHeadView";

    private final GestureDetector mGestureDetector;
    private final int mTouchSlop;
    private int mDownX;
    private int mDownY;
    private int mLastX;
    private int mLastY;
    private boolean mDragStarted;

    private Callbacks mCallbacks;

    public FloatingHeadView(Context context) {
        this(context, null);
    }

    public FloatingHeadView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingHeadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mGestureDetector = new GestureDetector(context, new GestureListener());
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setCallbacks(Callbacks cb) {
        mCallbacks = cb;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Logger.getLogger(TAG).debug("onTouchEvent(): " + event);

        mGestureDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        final int x = (int) event.getRawX();
        final int y = (int) event.getRawY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mLastX = x;
                mLastY = y;
                mDragStarted = false;
                setPressed(true);
                break;

            case MotionEvent.ACTION_MOVE:
                int dx = x - mLastX;
                int dy = y - mLastY;
                mLastX = x;
                mLastY = y;

                if (!mDragStarted) {
                    if (Math.abs(x - mDownX) > mTouchSlop || Math.abs(y - mDownY) > mTouchSlop) {
                        mDragStarted = true;
                        if (mCallbacks != null) {
                            mCallbacks.onDragStart(x, y);
                        }
                    }
                }

                if (mDragStarted) {
                    if (mCallbacks != null) {
                        mCallbacks.onDrag(dx, dy);
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                setPressed(false);
                if (mCallbacks != null) {
                    mCallbacks.onDragEnd(x, y);
                }
                break;

            default:
                break;
        }

        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Logger.getLogger(TAG).info("click");
            if (mCallbacks != null) {
                mCallbacks.onClick();
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Logger.getLogger(TAG).info("double click");
            if (mCallbacks != null) {
                mCallbacks.onDoubleClick();
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Logger.getLogger(TAG).info("long click");
            if (mCallbacks != null) {
                mCallbacks.onLongClick();
            }
        }
    }

    public interface Callbacks {
        void onDragStart(int x, int y);

        void onDrag(int dx, int dy);

        void onDragEnd(int x, int y);

        void onClick();

        void onDoubleClick();

        void onLongClick();
    }
}

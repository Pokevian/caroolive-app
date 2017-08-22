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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;


public class PlayerVideoView extends VideoView {

    public PlayerVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PlayerVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerVideoView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int width;

        if (specMode == MeasureSpec.EXACTLY) {
            width = specSize;
        } else {
            width = pixelFromDip(getContext(), 128);
            if (specMode == MeasureSpec.AT_MOST) {
                width = Math.min(width, specSize);
            }
        }

        return width;
    }

    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int height;

        if (specMode == MeasureSpec.EXACTLY) {
            height = specSize;
        } else {
            height = pixelFromDip(getContext(), 96);
            if (specMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, specSize);
            }
        }

        return height;
    }

    public static int dipFromPixel(Context context, int pixel) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pixel / scale);
    }

    public static int pixelFromDip(Context context, int dip) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dip * scale);
    }

}

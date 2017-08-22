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
import android.graphics.drawable.AnimationDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.pokevian.app.smartfleet.R;


/**
 * Created by dg.kim on 2015-04-15.
 */
public class TtsView extends FrameLayout {

    private ImageView mImage;

    public TtsView(Context context) {
        super(context);
    }

    public TtsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TtsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mImage = (ImageView) findViewById(R.id.image);
    }

    public void startAnimation() {
        /*if (mImage != null) {
            AnimationDrawable drawable = (AnimationDrawable) getResources().getDrawable(R.drawable.ic_floating_tts_speak);
            if (drawable != null) {
                mImage.setImageDrawable(drawable);
                drawable.start();
            }
        }*/
    }

    public void cancelAnimation() {
        /*if (mImage != null) {
            mImage.setImageResource(R.drawable.ic_floating_tts);
        }*/
    }
}

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
import android.widget.FrameLayout;
import android.widget.ImageView;


/**
 * Created by dg.kim on 2015-04-15.
 */
public class SpeakerphoneView extends FrameLayout {

    private ImageView mToggleImage;

    public SpeakerphoneView(Context context) {
        super(context);
    }

    public SpeakerphoneView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpeakerphoneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        /*mToggleImage = (ImageView) findViewById(R.id.speakerphone_toggle);*/
    }

    public void setSpeakerphoneOn(boolean on) {
        /*if (mToggleImage != null) {
            if (on) {
                mToggleImage.setImageResource(R.drawable.ic_floating_speaker_off);
            } else {
                mToggleImage.setImageResource(R.drawable.ic_floating_speaker_on);
            }
        }*/
    }
}

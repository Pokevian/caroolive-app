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

package com.pokevian.app.smartfleet.util;

import android.content.res.Resources;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;

import com.pokevian.app.smartfleet.R;

public final class DimenUtils {

    private DimenUtils() {
    }

    public static int getStatusBarHeight(Resources ress) {
        int dimen = 0;
        int resourceId = ress.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            dimen = ress.getDimensionPixelSize(resourceId);
        }
        return dimen;
    }

    public static boolean hasActionBar(ActionBar actionBar) {
        boolean hasActionBar = (actionBar != null);
        if (actionBar != null) {
            TypedValue tv = new TypedValue();
            if (actionBar.getThemedContext().getTheme().resolveAttribute(R.attr.windowActionBarOverlay, tv, true)) {
                boolean windowActionBarOverlay = (tv.data != 0);
                hasActionBar = !windowActionBarOverlay;
            }
        }
        return hasActionBar;
    }

    public static int getActionBarHeight(Resources ress) {
        int dimen = 0;
        int resourceId = ress.getIdentifier("action_bar_default_height", "dimen", "android");
        if (resourceId > 0) {
            dimen = ress.getDimensionPixelSize(resourceId);
        }
        return dimen;
    }

}

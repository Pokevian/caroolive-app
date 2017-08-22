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

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public final class OrientationUtils {

    public static int getRotation(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return display.getRotation();
    }

    public static int getRotationDegree(Context context) {
        int degrees = 0;
        int rotation = getRotation(context);
        switch (rotation) {
            default:
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    public static int getOrientation(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return config.orientation;
    }

    public static int getDeviceOrientation(Context context) {
        int rotation = getRotation(context);
        int orientation = context.getResources().getConfiguration().orientation;
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else if ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
                && orientation == Configuration.ORIENTATION_PORTRAIT) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
                && orientation == Configuration.ORIENTATION_PORTRAIT) {
            return Configuration.ORIENTATION_PORTRAIT;
        } else if ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
                && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return Configuration.ORIENTATION_PORTRAIT;
        } else {
            return Configuration.ORIENTATION_UNDEFINED;
        }
    }

    public static int getScreenOrientation(Context context) {
        int rotation = getRotation(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height)) {
            // if the device's natural orientation is portrait:
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    Log.e("", "Unknown screen orientation. Defaulting to portrait.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        } else {
            // if the device's natural orientation is landscape or if the device is square:
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    Log.e("", "Unknown screen orientation. Defaulting to landscape.");
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }

}

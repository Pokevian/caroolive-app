package com.pokevian.app.smartfleet.util;

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/**
 * Created by ian on 2016-09-20.
 */
public class ViewCompatUtils {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setBackground(View view, Drawable drawable) {
        if (Build.VERSION_CODES.JELLY_BEAN > Build.VERSION.SDK_INT) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

}

package com.pokevian.app.smartfleet.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by ian on 2016-04-29.
 */
public class WebUtils {

    public static void launchWebLink(Context context, String uriString) {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .setData(Uri.parse(uriString));
        context.startActivity(intent);
    }
}

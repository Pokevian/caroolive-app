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
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;

import org.apache.log4j.Logger;

import java.util.List;

public final class PackageUtils {

    private PackageUtils() {
    }

    public static List<ResolveInfo> getAppResolveInfos(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
        String myPackageName = context.getPackageName();
        for (ResolveInfo info : infos) {
            if (info.activityInfo.packageName.equals(myPackageName)) {
                infos.remove(info);
                break;
            }
        }
        return infos;
    }

    public static String toAppId(ResolveInfo info) {
        return info.activityInfo.packageName + "@" + info.activityInfo.name;
    }

    public static String parsePackageName(String appId) {
        if (!TextUtils.isEmpty(appId)) {
            String[] tokens = appId.split("@");
            if (tokens.length == 2) {
                return tokens[0];
            }
        }
        return null;
    }

    public static String parseClassName(String appId) {
        if (!TextUtils.isEmpty(appId)) {
            String[] tokens = appId.split("@");
            if (tokens.length == 2) {
                return tokens[1];
            }
        }
        return null;
    }

    public static Drawable loadIcon(Context context, ResolveInfo info) {
        PackageManager pm = context.getPackageManager();
        return info.loadIcon(pm);
    }

    public static Drawable loadIcon(Context context, String appId) {
        if (!TextUtils.isEmpty(appId)) {
            String packageName = parsePackageName(appId);
            String className = parseClassName(appId);
            ResolveInfo info = findResolveInfo(context, packageName, className);
            if (info != null) {
                PackageManager pm = context.getPackageManager();
                return info.loadIcon(pm);
            }
        }
        return null;
    }

    public static String loadLabel(Context context, ResolveInfo info) {
        PackageManager pm = context.getPackageManager();
        return info.loadLabel(pm).toString();
    }

    public static String loadLabel(Context context, String appId) {
        if (!TextUtils.isEmpty(appId)) {
            String[] tokens = appId.split("@");
            if (tokens.length == 2) {
                String packageName = tokens[0];
                String className = tokens[1];
                ResolveInfo info = findResolveInfo(context, packageName, className);
                if (info != null) {
                    PackageManager pm = context.getPackageManager();
                    return info.loadLabel(pm).toString();
                }
            }
        }
        return null;
    }

    public static String loadLabel(Context context, ApplicationInfo info) {
        PackageManager pm = context.getPackageManager();
        return info.loadLabel(pm).toString();
    }

    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException e) {
        }
        return "NameNotFound";
    }

    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (NameNotFoundException e) {
        }
        return 0;
    }

    private static ResolveInfo findResolveInfo(Context context, String packageName, String className) {
        List<ResolveInfo> infos = getAppResolveInfos(context);
        for (ResolveInfo info : infos) {
            if (packageName.equals(info.activityInfo.packageName)
                    && className.equals(info.activityInfo.name)) {
                return info;
            }
        }
        return null;
    }

    public static boolean isPackageInstalled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        return intent != null;
    }

    public static boolean installPackage(Context context, String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
//            Logger.getLogger(TAG).error("cannot start google play app");
            return false;
        }
    }

    public static boolean launchPackage(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
//            Logger.getLogger(TAG).error("cannot start olleh navi");
        }
        return false;
    }

}

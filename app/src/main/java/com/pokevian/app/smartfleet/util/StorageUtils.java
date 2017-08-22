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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

public final class StorageUtils {

    public static final long MB = 1024 * 1024;
    public static final long GB = 1024 * MB;
    public static final long TB = 1024 * GB;


    private StorageUtils() {
    }

    public static File[] getExternalFilesDirs(Context context, String type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return getExternalFilesDirsNew(context, type);
        } else {
            return getExternalFilesDirsOld(context, type);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static File[] getExternalFilesDirsNew(Context context, String type) {
        File[] paths = context.getExternalFilesDirs(type);

        ArrayList<File> dirs = new ArrayList<>();
        for (File dir : paths) {
            if (dir != null) {
                dir.mkdirs();
                if (dir.exists()) {
                    dirs.add(dir);
                }
            }
        }

        File[] array = new File[dirs.size()];
        dirs.toArray(array);
        return array;
    }

    private static File[] getExternalFilesDirsOld(Context context, String type) {
        final Pattern dirSeporator = Pattern.compile(String.valueOf(File.separatorChar));
        final String privatePath = File.separator + "Android"
                + File.separatorChar + "data"
                + File.separatorChar + context.getPackageName()
                + File.separatorChar + "files"
                + (type != null ? File.separatorChar + type : "");

        // Final set of paths
        final ArrayList<String> paths = new ArrayList<>();
        // Primary physical SD-CARD (not emulated)
        final String envExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // Primary emulated SD-CARD
        final String envEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");

        // Add primary SD-CARD
        if (TextUtils.isEmpty(envEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if (TextUtils.isEmpty(envExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                paths.add(File.separator + "storage" + File.separator + "sdcard0");
            } else {
                paths.add(envExternalStorage);
            }
        } else {
            // Device has emulated storage;
            // external storage paths should have userId burned into them.
            final String rawUserId;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = dirSeporator.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch (NumberFormatException ignored) {
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if (TextUtils.isEmpty(rawUserId)) {
                paths.add(envEmulatedStorageTarget);
            } else {
                paths.add(envEmulatedStorageTarget + File.separator + rawUserId);
            }
        }

        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String envSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");

        // Add all secondary storages
        if (!TextUtils.isEmpty(envSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = envSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(paths, rawSecondaryStorages);
        }

        if (paths.size() > 0) {
            ArrayList<File> dirs = new ArrayList<>();
            Iterator<String> iter = paths.iterator();
            while (iter.hasNext()) {
                String path = iter.next() + privatePath;
                File dir = new File(path);
                dir.mkdirs();
                if (dir.exists()) {
                    dirs.add(dir);
                }
            }
            if (dirs.size() > 0) {
                File[] array = new File[dirs.size()];
                dirs.toArray(array);
                return array;
            }
        }
        return null;
    }

}

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
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;

import com.pokevian.app.smartfleet.BuildConfig;
import com.pokevian.app.smartfleet.setting.SettingsStore;
import com.pokevian.lib.blackbox.BlackboxProfileInstance;

import org.apache.log4j.DailyMaxRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogCatAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


public final class LogUtils {

    private static final String ROOT_LOG_FILE_NAME = "caroo-live-log.txt";

    private LogUtils() {
    }

    public static File getLogDir(Context context) {
        File dir = context.getExternalCacheDir();
        if (dir == null) {
            dir = context.getCacheDir();
        }
        if (dir != null) {
            dir = new File(dir, "log");
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return dir;
    }

    public static void clearLog(Context context) {
        File dir = getLogDir(context);
        if (dir != null) {
            File[] files = dir.listFiles();
            for (File file : files) {
                file.delete();
            }
        }

        configLog(context);
    }

    public static void configLog(Context context) {
        LogManager.getLoggerRepository().resetConfiguration();

        // Log cat
        String logCatPattern = "%m [%F:%L]%n";
        configureLogCatAppender(logCatPattern);

        // Root
        final Logger root = Logger.getRootLogger();
        File dir = LogUtils.getLogDir(context);
        File file = new File(dir, ROOT_LOG_FILE_NAME);
        String filePattern = "[%d{yy-MM-dd HH:mm:ss}][%-5p][%M(%F:%L)] - %m%n";
        String datePattern = ".yyyy-MM-dd";
        configureFileAppender(root, filePattern, file.getAbsolutePath(), datePattern, 1);
        if (BuildConfig.DEBUG) {
            root.setLevel(Level.DEBUG);
        } else {
            root.setLevel(Level.DEBUG);
        }

    }

    private static void configureFileAppender(Logger logger, String filePattern, String filePath,
                                              String datePattern, int maxBackupDay) {
        final DailyMaxRollingFileAppender appender;
        final Layout layout = new PatternLayout(filePattern);

        try {
            appender = new DailyMaxRollingFileAppender(layout, filePath, datePattern);
        } catch (final IOException e) {
            throw new RuntimeException("Exception configuring log system", e);
        }

        appender.setMaxBackupDay(maxBackupDay);
        appender.setImmediateFlush(true);

        logger.addAppender(appender);
    }

    private static void configureLogCatAppender(String logCatPattern) {
        final Logger root = Logger.getRootLogger();
        final Layout logCatLayout = new PatternLayout(logCatPattern);
        final LogCatAppender logCatAppender = new LogCatAppender(logCatLayout);

        root.addAppender(logCatAppender);
    }

    public static File[] getLogFiles(Context context) {
        File dir = getLogDir(context);
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.startsWith(ROOT_LOG_FILE_NAME);
            }
        });
        return files;
    }

    public static String buildStackTrack(Throwable th) {
        StringWriter body = new StringWriter();
        PrintWriter writer = new PrintWriter(body, true);
        writer.write("[Stack Trace]\n");
        if (th != null) {
            th.printStackTrace(writer);
        }
        writer.write("\n");
        writer.close();
        return body.toString();
    }

    public static String buildPackageLog(Context context) {
        StringBuilder buffer = new StringBuilder("[PACKAGE]\n");
        String pkgName = context.getPackageName();
        buffer.append("\t- Package: " + pkgName + "\n");
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, 0);
            buffer.append("\t- App Name: " + pkgInfo.applicationInfo.loadLabel(pm) + "\n");
            buffer.append("\t- App Version: " + pkgInfo.versionName + "\n");
        } catch (NameNotFoundException e) {
        }

        return buffer.toString();
    }

    public static String buildPhoneLog() {
        StringBuilder buffer = new StringBuilder("[PHONE]\n");
        buffer.append("\t- Manufacturer: " + Build.MANUFACTURER + "\n");
        buffer.append("\t- Model: " + Build.MODEL + "\n");
        buffer.append("\t- SDK Version: " + Build.VERSION.RELEASE + "\n");
        return buffer.toString();
    }

    public static String buildStorageLog(Context context) {
        StringBuilder buffer = new StringBuilder("[STORAGE]\n");

        File[] dirs = StorageUtils.getExternalFilesDirs(context, "");
        for (File dir : dirs) {
            buffer.append("\t- " + dir.toString() + "\n");
        }

        return buffer.toString();
    }

    public static String buildBlackboxLog(Context context) {
        StringBuilder buffer = new StringBuilder("[BLACKBOX]\n");

        if (SettingsStore.getInstance().isBlackboxEnabled()) {
            BlackboxProfileInstance profileInstance = BlackboxProfileInstance.getInstance(context);
            String info = profileInstance.getBlackboxProfiles();
            if (info != null) {
                buffer.append("\t- Profiles:\n");
                buffer.append(info + "\n");
            }
            info = profileInstance.getCameraParameters();
            if (info != null) {
                buffer.append("\t- Camera:\n");
                buffer.append(info + "\n");
            }
        } else {
            buffer.append("\t-Not enabled!\n");
        }

        return buffer.toString();
    }

    public static String buildPreferencesLog(Context context) {
        StringBuilder buffer = new StringBuilder("[PREFERENCES]\n");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        buffer.append(prefs.getAll().toString());

        return buffer.toString();
    }

}

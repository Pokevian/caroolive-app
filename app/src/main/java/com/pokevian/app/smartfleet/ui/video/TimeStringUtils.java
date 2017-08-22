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

import android.annotation.SuppressLint;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeStringUtils {

    private TimeStringUtils() {
    }

    public static enum Type {
        LOCAL_DATE_TIME, LOCAL_DATE, HHMMSS, HMMSS, MMSS;
    }

    public static String toString(Type type, long time) {
        switch (type) {
            case LOCAL_DATE_TIME:
                return toLocalDateTimeString(time);
            case LOCAL_DATE:
                return toLocalDateString(time);
            case HHMMSS:
                return toHHMMSSString(time);
            case HMMSS:
                return toHMMSSString(time);
            case MMSS:
                return toMMSSString(time);
            default:
                return new Date(time).toString();
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String toString(Type type, String time, String timeFormat) {
        SimpleDateFormat format = new SimpleDateFormat(timeFormat);
        Date date = null;
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            return null;
        }
        return toString(type, date.getTime());
    }

    public static String toLocalDateTimeString(long time) {
        return DateFormat.getDateTimeInstance().format(new Date(time));
    }

    public static String toLocalDateString(long time) {
        return DateFormat.getDateInstance().format(new Date(time));
    }

    public static String toHHMMSSString(long time) {
        int seconds = (int) (time / 1000);
        int minutes = seconds / 60;
        int hour = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hour, minutes, seconds);
    }

    public static String toHMMSSString(long time) {
        int seconds = (int) (time / 1000);
        int minutes = seconds / 60;
        int hour = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format(Locale.US, "%01d:%02d:%02d", hour, minutes, seconds);
    }

    public static String toMMSSString(long time) {
        int seconds = (int) (time / 1000);
        int minutes = seconds / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

}

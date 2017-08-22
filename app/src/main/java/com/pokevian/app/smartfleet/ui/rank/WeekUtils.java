package com.pokevian.app.smartfleet.ui.rank;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by ian on 2016-03-17.
 */
public class WeekUtils {

    public static String getThisWeekString() {
        return getTodayString(false);
    }

    public static String getLastWeekString() {
        return getTodayString(true);
    }

    public static String getTodayString(boolean lastWeek) {
        return new SimpleDateFormat("yyyyMMdd").format(getToday(lastWeek));
    }

    public static Date getToday(boolean lastWeek) {
        Calendar calendar = Calendar.getInstance(Locale.KOREA);
        if (lastWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, -7);
        }
        return calendar.getTime();
    }
}

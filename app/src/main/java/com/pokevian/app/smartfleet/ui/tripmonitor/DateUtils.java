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

package com.pokevian.app.smartfleet.ui.tripmonitor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {


    static final String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Date LocalTimeToUTCTime(Date localDateTime) {
        /*return StringDateToDate(GetUTCdatetimeAsString(localDateTime));*/
        return localDateTime;
    }

    public static Date UTCTimeToLocalTime(Date utcDateTime) {
        /*return StringDateToDate(GetLocaldatetimeAsString(utcDateTime));*/
        return utcDateTime;
    }


    @SuppressWarnings("unused")
    private static Date GetUTCdatetimeAsDate() {
        //note: doesn't check for null
        return StringDateToDate(GetUTCdatetimeAsString());
    }

    private static String GetUTCdatetimeAsString(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(date);

        return utcTime;
    }

    private static String GetLocaldatetimeAsString(Date date) {

        long longUTCTime = date.getTime();
        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getOffset(longUTCTime);
        long longLocalTime = longUTCTime + offset;

        Date localDate = new Date();
        localDate.setTime(longLocalTime);

        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        final String localTime = sdf.format(localDate);

        return localTime;
    }

    private static String GetUTCdatetimeAsString() {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        return utcTime;
    }

    public static Date StringDateToDate(String StrDate) {
        Date dateToReturn = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT, Locale.US);

        try {
            dateToReturn = (Date) dateFormat.parse(StrDate);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

        return dateToReturn;
    }
}

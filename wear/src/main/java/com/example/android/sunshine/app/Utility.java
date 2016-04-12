/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.example.android.sunshine.app;


import android.content.res.Resources;
import android.support.annotation.NonNull;
import java.util.Calendar;


public class Utility {
    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    @NonNull
    static public String getMonthOfYearString(Resources resources, int monthOfYear) {
        String monthOfYearString;
        switch(monthOfYear) {
            case Calendar.JANUARY:
                monthOfYearString = resources.getString(R.string.january);
                break;
            case Calendar.FEBRUARY:
                monthOfYearString = resources.getString(R.string.february);
                break;
            case Calendar.MARCH:
                monthOfYearString = resources.getString(R.string.march);
                break;
            case Calendar.APRIL:
                monthOfYearString = resources.getString(R.string.april);
                break;
            case Calendar.MAY:
                monthOfYearString = resources.getString(R.string.may);
                break;
            case Calendar.JUNE:
                monthOfYearString = resources.getString(R.string.june);
                break;
            case Calendar.JULY:
                monthOfYearString = resources.getString(R.string.july);
                break;
            case Calendar.AUGUST:
                monthOfYearString = resources.getString(R.string.august);
                break;
            case Calendar.SEPTEMBER:
                monthOfYearString = resources.getString(R.string.september);
                break;
            case Calendar.OCTOBER:
                monthOfYearString = resources.getString(R.string.october);
                break;
            case Calendar.NOVEMBER:
                monthOfYearString = resources.getString(R.string.november);
                break;
            case Calendar.DECEMBER:
                monthOfYearString = resources.getString(R.string.december);
                break;
            default:
                monthOfYearString = "";
        }
        return monthOfYearString;
    }

    @NonNull
    static public String getDayOfWeekString(Resources resources, int day) {
        String dayOfWeekString;
        switch (day) {
            case Calendar.SUNDAY:
                dayOfWeekString = resources.getString(R.string.sunday);
                break;
            case Calendar.MONDAY:
                dayOfWeekString = resources.getString(R.string.monday);
                break;
            case Calendar.TUESDAY:
                dayOfWeekString = resources.getString(R.string.tuesday);
                break;
            case Calendar.WEDNESDAY:
                dayOfWeekString = resources.getString(R.string.wednesday);
                break;
            case Calendar.THURSDAY:
                dayOfWeekString = resources.getString(R.string.thursday);
                break;
            case Calendar.FRIDAY:
                dayOfWeekString = resources.getString(R.string.friday);
                break;
            case Calendar.SATURDAY:
                dayOfWeekString = resources.getString(R.string.saturday);
                break;
            default:
                dayOfWeekString = "";
        }
        return dayOfWeekString;
    }

    @NonNull
    static public String getAmPmString(Resources resources, int amPm) {
        if (amPm == Calendar.AM)
            return resources.getString(R.string.am);
        else if (amPm == Calendar.PM)
            return resources.getString(R.string.pm);
        else
            return "";
    }
}
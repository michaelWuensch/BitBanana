package app.michaelwuensch.bitbanana.util;

import android.content.Context;

import java.text.DateFormat;
import java.util.Date;

import app.michaelwuensch.bitbanana.R;

public class TimeFormatUtil {

    /**
     * Returns a nicely formatted time.
     *
     * @param time    in seconds
     * @param context
     * @return
     */
    public static String formatTimeAndDateLong(long time, Context context) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, context.getResources().getConfiguration().locale);
        String formattedDate = df.format(new Date(time * 1000L));
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.MEDIUM, context.getResources().getConfiguration().locale);
        String formattedTime = tf.format(new Date(time * 1000L));
        return (formattedDate + ", " + formattedTime);
    }

    /**
     * Returns a nicely formatted duration.
     *
     * @param duration in seconds
     * @return
     */
    public static String formattedDuration(long duration, Context context) {
        String formattedString = "";

        int hours = (int) duration / 3600;
        String hoursString = context.getResources().getQuantityString(R.plurals.duration_hour, hours, hours);
        int days = (int) duration / 86400;
        String daysString = context.getResources().getQuantityString(R.plurals.duration_day, days, days);
        int minutes = (int) (duration % 3600) / 60;
        String minutesUnit = context.getResources().getString(R.string.duration_minute_short);
        String secondsUnit = context.getResources().getString(R.string.duration_second_short);

        if (duration < 3600) {
            formattedString = String.format("%02d %s, %02d %s", (duration % 3600) / 60, minutesUnit, (duration % 60), secondsUnit);
        } else if (duration < 86400) {
            formattedString = hoursString;
        } else {
            formattedString = daysString;
        }

        return formattedString;
    }

    /**
     * Returns a nicely formatted duration.
     * This always shows only one unit to keep it short.
     *
     * @param duration in seconds
     * @return
     */
    public static String formattedDurationShort(long duration, Context context) {
        String formattedString = "";

        int seconds = (int) duration;
        String secondsString = context.getResources().getQuantityString(R.plurals.duration_second, seconds, seconds);
        int minutes = (int) (duration % 3600) / 60;
        String minutesString = context.getResources().getQuantityString(R.plurals.duration_minute, minutes, minutes);
        int hours = (int) duration / 3600;
        String hoursString = context.getResources().getQuantityString(R.plurals.duration_hour, hours, hours);
        int days = (int) duration / 86400;
        String daysString = context.getResources().getQuantityString(R.plurals.duration_day, days, days);
        int years = (int) duration / (86400 * 365);
        String yearsString = context.getResources().getQuantityString(R.plurals.duration_year, years, years);

        if (duration < 60) {
            formattedString = secondsString;
        } else if (duration < 3600) {
            formattedString = minutesString;
        } else if (duration < 86400) {
            formattedString = hoursString;
        } else if (duration < 86400 * 365) {
            formattedString = daysString;
        } else {
            formattedString = yearsString;
        }

        return formattedString;
    }

    /**
     * Returns a nicely formatted duration for block times. Seconds are irrelevant here.
     *
     * @param blocks number of blocks
     * @return
     */
    public static String formattedBlockDuration(long blocks, Context context) {

        // duration in seconds
        long duration = blocks * 10 * 60;

        String formattedString = "";

        int minutes = (int) (duration % 3600) / 60;
        String minutesString = "";
        if (minutes != 0)
            minutesString = context.getResources().getQuantityString(R.plurals.duration_minute, minutes, minutes);
        int hours = (int) (duration % 86400) / 3600;
        String hoursString = "";
        if (hours != 0)
            hoursString = context.getResources().getQuantityString(R.plurals.duration_hour, hours, hours);
        int days = (int) duration / 86400;
        String daysString = "";
        if (days != 0)
            daysString = context.getResources().getQuantityString(R.plurals.duration_day, days, days);

        if (duration < 86400) {
            formattedString = hoursString + divider(hoursString, minutesString) + minutesString;
        } else {
            formattedString = daysString + divider(daysString, hoursString) + hoursString;
        }

        return formattedString;
    }

    /**
     * Adds a comma separation between to strings if both exist and are not empty
     *
     * @param a String to the left
     * @param b String to the right
     * @return
     */
    private static String divider(String a, String b) {
        if (a == null || b == null)
            return "";
        if (a.isEmpty() || b.isEmpty())
            return "";
        return ", ";
    }

    /**
     * Converts nanoseconds to milliseconds
     *
     * @param NS nanoseconds
     * @return milliseconds
     */
    public static long NStoMS(Long NS) {
        return NS / 1000000;
    }
}

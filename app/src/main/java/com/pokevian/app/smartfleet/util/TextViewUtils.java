package com.pokevian.app.smartfleet.util;

import android.text.format.DateUtils;
import android.widget.TextView;

import org.apache.log4j.Logger;

import java.text.DecimalFormat;

/**
 * Created by ian on 2016-06-13.
 */
public class TextViewUtils {


    public static void setRunTimeText(TextView textView, long second) {
        setText(textView, getRunTimeString(second));
    }

    public static void setSpeedText(TextView view, Object value) {
        setText(view, getDecimalString(value));
    }

    public static void setDistanceText(TextView view, Object value) {
        setText(view, getDecimalPointOneDigitString(value));
//        setText(view, getDistanceString(value));
    }

    public static void setHHmmText(TextView textView, int second) {
        int h = second / 3600;
        int m = (second % 3600) / 60;
        textView.setText(String.format("%02d:%02d", h, m));
    }

    public static void setScoreText(TextView textView, Object value) {
        setText(textView, getDecimalPointOneDigitString(value));
    }

    public static void setRankingText(TextView textView, int value) {
        setText(textView, Integer.toString(value));
    }

    public static void setFloatText(TextView textView, Object value) {
        setText(textView, getDecimalPointOneDigitString(value));
    }

    public static void setFuelConsumptionText(TextView textView, Object value) {
        setText(textView, getDecimalPointOneDigitString(value));
    }

    public static void setFuelEconomyText(TextView textView, Object value) {
//        NumberFormat format = new DecimalFormat("#0.0", new DecimalFormatSymbols());
//        setText(textView, format.format(value));

        setText(textView, getDecimalPointOneDigitString(value));

//        mFuelEconomyFormatter = new DecimalFormat("#0.0", new DecimalFormatSymbols());
//        mFuelConsumptionFormatter = new DecimalFormat("#0.00", new DecimalFormatSymbols());
    }

    public static void setNumberFormatText(TextView textView, Object value) {
//        DecimalFormat decimalFormat = new DecimalFormat("###,###");
//        setText(textView, decimalFormat.format(value));
//        NumberFormat format = new DecimalFormat("###,###", new DecimalFormatSymbols());
        setText(textView, getNaturalNumberString(value));
    }

    public static void setIntegerFormatText(TextView textView, int value) {
        setText(textView, Integer.toString(value));
    }

    public static void setIntegerFormatText(TextView textView, float value) {
        setIntegerFormatText(textView, Math.round(value));
    }

    public static String getRunTimeString(long second) {
        return DateUtils.formatElapsedTime(second);
    }

    public static String getDistanceString(Object value) {
        return getNaturalNumberString(value);
    }

    private static void setText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    private static String getNaturalNumberString(Object value) {
        DecimalFormat format = new DecimalFormat("###,###");
        return format.format(value);
    }

    private static String getDecimalPointOneDigitString(Object value) {
        DecimalFormat format = new DecimalFormat("0.0");
        if (value instanceof Float && (float) value < 0.1f) {
            format = new DecimalFormat("#");
        }

        return format.format(value);
    }

    private static String getDecimalPointTwoDigitString(Object value) {
        DecimalFormat format = new DecimalFormat("0.0#");
        return format.format(value);
    }

    private static String getDecimalString(Object value) {
        DecimalFormat format = new DecimalFormat("#");
        return format.format(value);
    }

}

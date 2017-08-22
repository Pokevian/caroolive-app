package com.pokevian.app.smartfleet.ui.diagnostic;

import java.util.Locale;

public class DtcParser {

    public static final int DTC_GROUP_01 = 0;
    public static final int DTC_GROUP_02 = 1;
    public static final int DTC_GROUP_03 = 2;
    public static final int DTC_GROUP_04 = 3;
    public static final int DTC_GROUP_05 = 4;
    public static final int DTC_GROUP_06 = 5;
    public static final int DTC_GROUP_07 = 6;
    public static final int DTC_GROUP_08 = 7;
    public static final int DTC_GROUP_09 = 8;
    public static final int DTC_GROUP_10 = 9;
    public static final int DTC_GROUP_UNKNOWN = -1;

    private int mFaults;
    private int[] mDtcGroups;
    private String mDtc;
    private String[] mCodes;

    public DtcParser(String dtc) {
        mDtc = dtc;
        splitDtc();
        parse();
    }

    public String getDtc() {
        return mDtc;
    }

    public int getDtcGroupCount() {
        int count = 0;
        for (int i = DtcParser.DTC_GROUP_01; i <= DtcParser.DTC_GROUP_10; i++) {
            if (isValidGroup(i)) {
                count++;
            }
        }
        return count;
    }

    public String bindDtcGroup(int groupId) {
        StringBuilder sb = new StringBuilder();
        for (String code : mCodes) {
            if (groupId == getGroupIdByDtc(code)) {
                sb.append(String.format("%s ", code));
            }
        }
        return sb.toString();
    }

    public boolean isValidGroup(int groupId) {
        return (mFaults & (1 << groupId)) != 0;
    }

    private void parse() {
        mFaults = 0x00;
        mDtcGroups = null;

        if (mCodes != null) {
            int index = 0;
            mDtcGroups = new int[mCodes.length];
            for (String code : mCodes) {
                int group = getGroupIdByDtc(code);
                if (group > -1 && (mFaults & (1 << group)) == 0) {
                    mFaults |= (1 << group);
                    mDtcGroups[index++] = group;
                }
            }
        }
    }

    private int getGroupIdByDtc(String code) {
        char category = code.toUpperCase(Locale.US).charAt(0);
        int no;
        try {
            no = Integer.valueOf(code.substring(1), 16);
        } catch (NumberFormatException e) {
            return DTC_GROUP_UNKNOWN;
        }

        if ('P' == category) {
            if (0x0001 <= no && no <= 0x0199) {
                return DTC_GROUP_01;
            } else if (0x0200 <= no && no <= 0x0299) {
                return DTC_GROUP_02;
            } else if (0x0300 <= no && no <= 0x0399) {
                return DTC_GROUP_03;
            } else if (0x0400 <= no && no <= 0x0499) {
                return DTC_GROUP_01;
            } else if (0x0500 <= no && no <= 0x0599) {
                return DTC_GROUP_04;
            } else if (0x0600 <= no && no <= 0x0699) {
                return DTC_GROUP_05;
            } else if (0x0700 <= no && no <= 0x0899) {
                return DTC_GROUP_06;
            } else if (0x1000 <= no) {
                return DTC_GROUP_07;
            }
        } else if ('B' == category) {
            return DTC_GROUP_08;
        } else if ('C' == category) {
            return DTC_GROUP_09;
        } else if ('U' == category) {
            return DTC_GROUP_10;
        }

        return DTC_GROUP_UNKNOWN;
    }

    private void splitDtc() {
        if (mDtc != null) {
            String delimiter = "[,]";
            mCodes = mDtc.split(delimiter);
        }
    }

}

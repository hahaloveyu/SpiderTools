package com.toptime.webspider.plugins.tools.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Calendar.*;

public class MyDateUtil {

    private static Logger logger = Logger.getLogger(MyDateUtil.class);

    /**
     * 输入时间，自动转换成日期long
     *
     * @param timeStr
     * @return
     */
    public static long convergeTime(String timeStr) {

        if (timeStr.matches("\\d{10}")) {
            return Long.valueOf(timeStr) * 1000;
        } else if (timeStr.matches("\\d{13}")) {
            return Long.valueOf(timeStr);
        }

        String formate = "";
        String dateFormate = "yyyy-MM-dd HH:mm";
        Long longDate = 0L;

        timeStr = timeStr.replace("　", " ");
        timeStr = timeStr.replace(",", " ");
        // timeStr = timeStr.replace(".", " ");
        // 多个空格或制表符替换成一个
        timeStr = timeStr.replaceAll("[\\s]+", " ");
        // 预处理
        timeStr = timeStr.trim();

        Calendar calendar = getInstance();
        int year = calendar.get(YEAR);
        int month = calendar.get(MONTH) + 1;
        int day = calendar.get(DATE);
        String regex;
        String partDate;
        try {

            /** 常用 时间开始*/
            // 2014-01-01
            regex = "\\d{4}-\\d{1,2}-\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate.length() > 0) {
                partDate = getStringByRegex(timeStr, regex, 0, 2) + " 00:00";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014/3/28 10:29
            regex = "[\\d]{2,4}/[\\d]{1,2}/[\\d]{1,2}[\\s][\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("/", "-");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014/3/28 10:29:06
            regex = "[\\d]{2,4}/[\\d]{1,2}/[\\d]{1,2}[\\s][\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("/", "-");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014年08月22日 22:15:26
            regex = "[\\d]{4}年[\\d]{1,2}月[\\d]{1,2}日[\\s]*?[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                longDate = string2long(partDate, "yyyy-MM-dd HH:mm:ss");
                return longDate;
            }

            // 2014年1月2日
            regex = "\\d{4}年\\d{1,2}月\\d{1,2}日";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", "") + " 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2000-2019年xx月xx日 xx:xx
            regex = "20[01][\\d]年[\\d]{1,2}月[\\d]{1,2}日 [\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate.length() > 0) {
                partDate = partDate.replaceAll("[年月]", "-");
                partDate = partDate.replaceAll("[日]", "");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014年08月22日 22:15
            regex = "[\\d]{4}年[\\d]{1,2}月[\\d]{1,2}日[\\s]*?[\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014年08月22日 22时15分
            regex = "[\\d]{4}年[\\d]{1,2}月[\\d]{1,2}日[\\s]*?[\\d]{1,2}时[\\d]{1,2}分";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                partDate = partDate.replaceAll("[时]", ":");
                partDate = partDate.replaceAll("[分]", " ");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 1月2日下午3点
            regex = "\\d{1,2}月\\d{1,2}日下";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", "");
                partDate = partDate.replaceAll("[下]", " ") + "00:00";
                formate = year + "-" + partDate;
                longDate = string2long(formate, dateFormate);
                return longDate;
            }

            // 1月2日上午3点
            regex = "\\d{1,2}月\\d{1,2}日上";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", "");
                partDate = partDate.replaceAll("[上]", " ") + "00:00";
                formate = year + "-" + partDate;
                longDate = string2long(formate, dateFormate);
                return longDate;
            }

            // 2014年1月2日下午3点
            regex = "\\d{4}年\\d{1,2}月\\d{1,2}日下";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                partDate = partDate.replaceAll("[下]", " ") + "00:00";
                // partDate = partDate.replaceAll("[分]", " ");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            //2017.10.17
            regex = "\\d{4}\\.\\d{2}.\\d{2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (StringUtils.isNotEmpty(partDate)) {
                partDate = partDate.replace(".", "-");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014年1月2日下午3点
            regex = "\\d{4}年\\d{1,2}月\\d{1,2}日上";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                partDate = partDate.replaceAll("[上]", " ") + "00:00";
                // partDate = partDate.replaceAll("[分]", " ");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 14-07-01
            regex = "\\d{2}-\\d{1,2}-\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = "20" + partDate;
                dateFormate = "yyyy-MM-dd";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // xx(月)/xx(日) xx:xx
            regex = "[\\d]{1,2}/[\\d]{1,2} [\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate.length() > 0) {
                partDate = partDate.replaceAll("/", "-");
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // xx(月)-xx(日) xx:xx
            regex = "[\\d]{1,2}-[\\d]{1,2}[\\s][\\d]{1,2}:?[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate.length() > 0) {
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // (10-9999)-xx-xx xx:xx
            regex = "[\\d]{2,4}-[\\d]{1,2}-[\\d]{1,2}[\\s][\\d]{1,2}:?[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // (10-9999)-xx-xx xx:xx:xx(毫秒)
            regex = "[\\d]{2,4}-[\\d]{1,2}-[\\d]{1,2}[\\s]*?[\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // xx月xx日 xx:xx
            regex = "[\\d]{1,2}月[\\d]{1,2}日[\\s]*?[\\d]{1,2}:[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[月]", "-");
                partDate = partDate.replaceAll("[日]", " ");
                formate = year + "-" + partDate;
                longDate = string2long(formate, dateFormate);
                return longDate;
            }

            // 2014年1月
            regex = "\\d{4}年\\d{1,2}月";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-") + "-01 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 01月12
            regex = "\\d{1,2}月\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[月]", "-");
                formate = year + "-" + partDate;
                longDate = string2long(formate + " 00:01",
                        dateFormate);
                return longDate;
            }

            // 23/09/2014
            regex = "\\d{1,2}/\\d{1,2}/\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "dd/MM/yyyy HH:mm";
                longDate = string2long(partDate + " 00:01", dateFormate);
                return longDate;
            }
            /**  常用 时间结束*/

            /**  中文 秒前 分钱类似时间开始*/
            // xx秒前
            regex = "([\\d]{1,2})[\\s]*?秒前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);

            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(Calendar.SECOND, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // xx分钟前/分前
            regex = "([\\d]{1,2})[\\s]*?分[钟]?前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(Calendar.MINUTE, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // xx小时前
            regex = "([\\d]{1,2})[\\s]*?小时前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(Calendar.HOUR, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // xx天前
            regex = "([\\d]{1,2})[\\s]*?天前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(Calendar.DATE, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // xx月前
            regex = "([\\d]{1,2})[\\s]*?月前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(MONTH, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // xx年前
            regex = "([\\d]{1,2})[\\s]*?年前";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(YEAR, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // 今天xx(时):xx(分) 24小时制
            regex = "今天[\\s]([\\d]{1,2}:[\\d]{1,2})";
            partDate = getStringByRegex(timeStr, regex, 1, 2);

            if (partDate.length() > 0) {
                formate = year + "-" + month + "-" + day + " " + partDate;
                longDate = string2long(formate, dateFormate);
                return longDate;
            }

            // 昨天xx(时):xx(分) 24小时制
            regex = "昨天[\\s]([\\d]{1,2}:[\\d]{1,2})";
            partDate = getStringByRegex(timeStr, regex, 1, 2);

            if (partDate.length() > 0) {
                calendar.add(5, -1);
                formate = calendar.get(1) + "-" + (calendar.get(2) + 1) + "-"
                        + calendar.get(5) + " " + partDate;
                longDate = string2long(formate, dateFormate);
                return longDate;
            }
            /**  中文 秒前 分钱类似时间结束*/

            /**   英文 时间开始*/
            // Friday December 19 2014
            regex = "[a-zA-Z]{3,10}\\s[a-zA-Z]{3,10}\\s\\d{1,2}\\s\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "EEE MMM dd yyyy HH:mm";
                longDate = string2longEnglish(partDate + " 00:01", dateFormate);
                return longDate;
            }

            // 2011 Feb/February 13
            regex = "\\d{4}[\\s][a-zA-Z]{3,10}[\\s]\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "yyyy MMM dd HH:mm";
                longDate = string2longEnglish(partDate + " 00:01", dateFormate);
                return longDate;
            }

            // January 12 2015 8:53
            regex = "[a-zA-Z]{3,10}\\s\\d{1,2}\\s\\d{4}\\s\\d{1,2}:\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "MMM dd yyyy HH:mm";
                longDate = string2longEnglish(partDate, dateFormate);
                return longDate;
            }

            // 13 Feb/February 2011
            // regex = "\\d{1,2}[\\s][a-zA-Z]{3,10}[\\s]\\d{4}";
            // partDate = getStringByRegex(timeStr, regex, 0, 2);
            // if ((partDate != null) && (partDate.length() > 0)) {
            // dateFormate = "dd MMM yyyy HH:mm";
            // longDate = Long.valueOf(string2longEnglish(partDate + " 00:01",
            // dateFormate));
            // return longDate.longValue();
            // }

            // Feb/February 13 2011
            regex = "[a-zA-Z]{3,10}[\\s]*?\\d{1,2}[\\s]\\d{4}";
            String timeStrEnglish = timeStr.replaceAll(",", " ");
            partDate = getStringByRegex(timeStrEnglish, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "MMM dd yyyy HH:mm";
                longDate = string2longEnglish(partDate + " 00:01", dateFormate);
                return longDate;
            }

            // 3 days ago
            regex = "(\\d{1,2})\\s*?days?\\s*?ago";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(5, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // 3 months ago
            regex = "(\\d{1,2})\\s*?months?\\s*?ago";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(MONTH, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // 3 years ago
            regex = "(\\d{1,2})\\s*?years?\\s*?ago";
            partDate = getStringByRegex(timeStr, regex, 1, 2);
            if (partDate.length() > 0) {
                int timeInt = Integer.parseInt(partDate);
                calendar.add(YEAR, -timeInt);
                longDate = calendar.getTimeInMillis();
                return longDate;
            }

            // 08/03/2014, 06:55:PM
            regex = "\\d{1,2}/\\d{1,2}/\\d{4}\\s?,\\s?\\d{1,2}:\\d{1,2}:[A|P]M";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                dateFormate = "MM/dd/yyyy, hh:mm:aaa";
                longDate = string2longEnglish(partDate, dateFormate);
                return longDate;
            }
            /**  英文 时间结束*/

            /**  不常用 时间开始*/
            // 2014年
            regex = "\\d{4}年";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "") + "-01-01 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 2014
            regex = "\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate + "-01-01 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 14年
            regex = "\\d{2}年";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                int time = Integer.parseInt("20" + partDate.replace("年", ""));
                if (time > year) {
                    time = Integer.parseInt("19" + partDate.replace("年", ""));
                }
                partDate = time + "-01-01 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 14
            regex = "\\d{2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                int time = Integer.parseInt("20" + partDate);
                if (time > year) {
                    time = Integer.parseInt("19" + partDate);
                }
                partDate = time + "-01-01 00:01";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            /**  不常用 时间结束*/

            // 94665606 -------> 2000年 以秒为单位
            regex = "\\d{10}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                longDate = Long.valueOf(partDate) * 1000;
                return longDate;
            }

            // 946656060000---->2000年 以毫秒为单位
            regex = "\\d{12,13}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                longDate = Long.valueOf(partDate);
                return longDate;
            }
            // 05h00 ----
            regex = "\\d{1,2}h\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate != null && partDate.length() > 0) {
                partDate = year + "-" + month + "-" + day + " "
                        + partDate.replace("h", ":");
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 02/12/2015 &agrave;s 16h33 2015-12-9 修改
            regex = "\\d{1,2}/\\d{1,2}/20\\d{1,2} &agrave;s \\d{1,2}h\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if (partDate != null && partDate.length() > 0) {
                partDate = partDate.replace("&agrave;s", "");
                partDate = partDate.replace("h", ":");
                dateFormate = "dd/MM/yyyy HH:mm";
                longDate = string2long(partDate, dateFormate);
                return longDate;

            }
            // 05/07/14
            regex = "\\d{1,2}/\\d{1,2}/\\d{2,4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                String s = partDate.substring(0, 6) + "20"
                        + partDate.substring(6, partDate.length());
                partDate = s;
                dateFormate = "dd/MM/yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }
            // 2015/12/8
            regex = "[\\d]{2,4}/[\\d]{1,2}/[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("/", "-") + " 00:00";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }
            // / 12/8
            regex = "[\\d]{1,2}/[\\d]{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("/", "-") + " 00:00";
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // / 2015年12月8
            regex = "\\d{2,4}年\\d{1,2}月\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[年]", "-");
                partDate = partDate.replaceAll("[月]", "-") + " 00:00";
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // / 12月8日
            regex = "\\d{1,2}月\\d{1,2}日";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("月", "-");
                partDate = partDate.replaceAll("日", "") + " 00:00";
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // / 12-8
            regex = "\\d{1,2}-\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("-", "-") + " 00:00";
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }
            // / 12月8
            regex = "\\d{1,2}月\\d{1,2}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("[月]", "-") + " 00:00";
                partDate = year + "-" + partDate;
                longDate = string2long(partDate, dateFormate);
                return longDate;
            }

            // 01.01.2014
            regex = "\\d{1,2}\\.\\d{1,2}\\.\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {

                partDate = partDate.replaceAll("\\.", "-");
                dateFormate = "dd-MM-yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }
            /*
             * //6 de jan de 2016 regex =
             * "\\d{1,2}\\s*de\\s*[a-zA-Z]{3,10}\\s*de\\s*\\d{4}"; partDate =
             * getStringByRegex(timeStr, regex, 0, 2); if ((partDate != null) &&
             * (partDate.length() > 0)) {
             *
             * partDate = partDate.replaceAll("jan", "1"); partDate =
             * partDate.replaceAll("fev", "2"); partDate =
             * partDate.replaceAll("mar", "3"); partDate =
             * partDate.replaceAll("abr", "4"); partDate =
             * partDate.replaceAll("mai", "5"); partDate =
             * partDate.replaceAll("jun", "6"); partDate =
             * partDate.replaceAll("jul", "7"); partDate =
             * partDate.replaceAll("ago", "8"); partDate =
             * partDate.replaceAll("set", "9"); partDate =
             * partDate.replaceAll("out", "10"); partDate =
             * partDate.replaceAll("nov", "11"); partDate =
             * partDate.replaceAll("dez", "12"); partDate =
             * partDate.replaceAll("de", "-"); dateFormate = "dd-MM-yyyy HH:mm";
             * longDate = Long.valueOf(string2long(partDate.replace(" ", "") +
             * " 00:01", dateFormate)); return longDate.longValue(); }
             */

            // Enero, 7.2016
            regex = "[a-zA-Z]{3,10}\\s*\\d{1,2}.\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {

                partDate = partDate.replaceAll("(?i)Enero", "1");
                partDate = partDate.replaceAll("(?i)Febrero", "2");
                partDate = partDate.replaceAll("(?i)Marzo", "3");
                partDate = partDate.replaceAll("(?i)Abril", "4");
                partDate = partDate.replaceAll("(?i)Mayo", "5");
                partDate = partDate.replaceAll("(?i)Junio", "6");
                partDate = partDate.replaceAll("(?i)Julio", "7");
                partDate = partDate.replaceAll("(?i)Agosto", "8");
                partDate = partDate.replaceAll("(?i)Septiembre", "9");
                partDate = partDate.replaceAll("(?i)Octubre", "10");
                partDate = partDate.replaceAll("(?i)Noviembre", "11");
                partDate = partDate.replaceAll("(?i)Diciembre", "12");
                partDate = partDate.replaceAll(",", "-");
                partDate = partDate.replaceAll("\\.", "-");
                partDate = partDate.replaceAll(" ", "-");
                partDate = partDate.replaceAll("(?i)de", "-");
                dateFormate = "MM-DD-yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }

            //Apr. 01, 2016
            regex = "[a-zA-Z]{3,10}\\.\\s*\\d{1,2}\\s*\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {
                partDate = partDate.replaceAll("(?i)Enero", "1");
                partDate = partDate.replaceAll("(?i)Febrero", "2");
                partDate = partDate.replaceAll("(?i)Marzo", "3");
                partDate = partDate.replaceAll("(?i)Abril", "4");
                partDate = partDate.replaceAll("(?i)Mayo", "5");
                partDate = partDate.replaceAll("(?i)Junio", "6");
                partDate = partDate.replaceAll("(?i)Julio", "7");
                partDate = partDate.replaceAll("(?i)Agosto", "8");
                partDate = partDate.replaceAll("(?i)Septiembre", "9");
                partDate = partDate.replaceAll("(?i)Octubre", "10");
                partDate = partDate.replaceAll("(?i)Noviembre", "11");
                partDate = partDate.replaceAll("(?i)Diciembre", "12");
                partDate = partDate.replaceAll("(?i)Jan", "1");
                partDate = partDate.replaceAll("(?i)Feb", "2");
                partDate = partDate.replaceAll("(?i)Mar", "3");
                partDate = partDate.replaceAll("(?i)Apr", "4");
                partDate = partDate.replaceAll("(?i)May", "5");
                partDate = partDate.replaceAll("(?i)Jun", "6");
                partDate = partDate.replaceAll("(?i)Jul", "7");
                partDate = partDate.replaceAll("(?i)Aug", "8");
                partDate = partDate.replaceAll("(?i)Sept", "9");
                partDate = partDate.replaceAll("(?i)Sep", "9");
                partDate = partDate.replaceAll("(?i)Oct", "10");
                partDate = partDate.replaceAll("(?i)Nov", "11");
                partDate = partDate.replaceAll("(?i)Dec", "12");
                partDate = partDate.replaceAll(",", "-");
                partDate = partDate.replaceAll("\\.", "");
                partDate = partDate.replaceAll(" ", "-");
                partDate = partDate.replaceAll("(?i)de", "-");
                dateFormate = "MM-DD-yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }


            // 12 Enero, 2016
            regex = "\\d{1,2}\\s*[a-zA-Z]{3,10}\\s*\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {

                partDate = partDate.replaceAll("(?i)Enero", "1");
                partDate = partDate.replaceAll("(?i)Febrero", "2");
                partDate = partDate.replaceAll("(?i)Marzo", "3");
                partDate = partDate.replaceAll("(?i)Abril", "4");
                partDate = partDate.replaceAll("(?i)Mayo", "5");
                partDate = partDate.replaceAll("(?i)Junio", "6");
                partDate = partDate.replaceAll("(?i)Julio", "7");
                partDate = partDate.replaceAll("(?i)Agosto", "8");
                partDate = partDate.replaceAll("(?i)Septiembre", "9");
                partDate = partDate.replaceAll("(?i)Octubre", "10");
                partDate = partDate.replaceAll("(?i)Noviembre", "11");
                partDate = partDate.replaceAll("(?i)Diciembre", "12");
                partDate = partDate.replaceAll("(?i)Jan", "1");
                partDate = partDate.replaceAll("(?i)Feb", "2");
                partDate = partDate.replaceAll("(?i)Mar", "3");
                partDate = partDate.replaceAll("(?i)Apr", "4");
                partDate = partDate.replaceAll("(?i)May", "5");
                partDate = partDate.replaceAll("(?i)Jun", "6");
                partDate = partDate.replaceAll("(?i)Jul", "7");
                partDate = partDate.replaceAll("(?i)Aug", "8");
                partDate = partDate.replaceAll("(?i)Sept", "9");
                partDate = partDate.replaceAll("(?i)Sep", "9");
                partDate = partDate.replaceAll("(?i)Oct", "10");
                partDate = partDate.replaceAll("(?i)Nov", "11");
                partDate = partDate.replaceAll("(?i)Dec", "12");
                partDate = partDate.replaceAll(",", "-");
                partDate = partDate.replaceAll(" ", "-");
                dateFormate = "dd-MM-yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }

            // 6 de jan de 2016
            regex = "\\d{1,2}\\s*[a-zA-Z]{1,2}\\s*[a-zA-Z]{3,10}\\s*[a-zA-Z]{1,2}\\s*\\d{4}";
            partDate = getStringByRegex(timeStr, regex, 0, 2);
            if ((partDate != null) && (partDate.length() > 0)) {

                partDate = partDate.replaceAll("(?i)Enero", "1");
                partDate = partDate.replaceAll("(?i)Febrero", "2");
                partDate = partDate.replaceAll("(?i)Marzo", "3");
                partDate = partDate.replaceAll("(?i)Abril", "4");
                partDate = partDate.replaceAll("(?i)Mayo", "5");
                partDate = partDate.replaceAll("(?i)Junio", "6");
                partDate = partDate.replaceAll("(?i)Julio", "7");
                partDate = partDate.replaceAll("(?i)Agosto", "8");
                partDate = partDate.replaceAll("(?i)Septiembre", "9");
                partDate = partDate.replaceAll("(?i)Octubre", "10");
                partDate = partDate.replaceAll("(?i)Noviembre", "11");
                partDate = partDate.replaceAll("(?i)Diciembre", "12");
                // partDate = partDate.replaceAll("", "12");
                partDate = partDate.replaceAll("(?i)de", "-");
                dateFormate = "dd-MM-yyyy HH:mm";
                longDate = string2long(partDate.replace(" ", "") + " 00:01", dateFormate);
                return longDate;
            }
            logger.error("没有匹配的时间处理");
        } catch (Exception e) {
            longDate = System.currentTimeMillis();
            logger.error(e.getMessage());
        }
        return longDate;
    }

    /**
     * @param sourceCode
     * @param regex
     * @param groupNum
     * @param patternCase
     * @return
     */
    private static String getStringByRegex(String sourceCode, String regex,
                                           int groupNum, int patternCase) {
        String ret = "";
        Pattern pTemp = Pattern.compile(regex, patternCase);
        Matcher mTemp = pTemp.matcher(sourceCode);
        if (mTemp.matches()) {
            ret = mTemp.group(groupNum);
        }
        return ret;
    }

    /**
     * 中文环境下的时间转换
     *
     * @param nowTime
     * @param dataFormat
     * @return
     */
    private static long string2long(String nowTime, String dataFormat) {
        return string2longOther(nowTime, dataFormat, Locale.CHINA);
    }

    /**
     * 进行英语的时间处理 (2011 Apr 13) 要在英语语言环境下处理
     *
     * @param nowTime
     * @param dataFormat
     * @return
     */
    private static long string2longEnglish(String nowTime, String dataFormat) {
        return string2longOther(nowTime, dataFormat, Locale.ENGLISH);
    }

    /**
     * 可选择语种
     *
     * @param nowTime
     * @param dataFormat
     * @param locale     语种
     * @return
     */
    private static long string2longOther(String nowTime, String dataFormat,
                                         Locale locale) {
        if (StringUtils.isEmpty(nowTime)) {
            return 0L;
        }
        long longTime = 0L;
        // 英语语言环境
        DateFormat f = new SimpleDateFormat(dataFormat, locale);
        try {
            Date d = f.parse(nowTime);
            longTime = d.getTime();
        } catch (ParseException e) {
            logger.info(e.getMessage());
        }
        return longTime;
    }

    /**
     * 将时间(2015-03-06)转换为 long 型
     *
     * @param dateStr
     * @param ymdStyle yyyy-MM-dd
     */
    public static long string2long(String dateStr, String ymdStyle, String url) {

        long time = 0L;
        dateStr = dateStr.replace("年", "-");
        dateStr = dateStr.replace("月", "-");
        dateStr = dateStr.replace("日", "");
        dateStr = dateStr.replace(".", "-");
        dateStr = dateStr.replace("/", "-");


        try {
            SimpleDateFormat sdf = new SimpleDateFormat(ymdStyle);
            time = sdf.parse(dateStr.trim()).getTime();
            //当前时间
            long currentTime = System.currentTimeMillis();
            //当前时间 + 一年 < 获得的时间
            if (currentTime + 365 * 24 * 3600000L < time) {
                time = 0L;
            }
        } catch (ParseException e) {
            dateStr = dateStr.replaceAll("st|nd|rd|th]", "")
                    .replace("Augu", "August");
            time = string2longEnglish(dateStr, ymdStyle);
        }

        return time;

    }

}

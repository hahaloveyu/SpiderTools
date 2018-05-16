package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.config.ToolsConfig;
import com.toptime.webspider.plugins.tools.util.MyDateUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.xsoup.Xsoup;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间日期提取服务
 * Created by bjoso on 2017/6/15.
 */
public class DatetimeParser {

    private static Logger logger = LoggerFactory.getLogger(AutoFormat.class);
    /**
     * 日期格式化
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * 时区设置
     */
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();


    /**
     * 解析时间
     *
     * @param text 网页源码
     * @param url  网页地址
     * @return 时间集合
     */
    public List<Long> parserDateTimeAuto(String text, String url) {

        List<Long> datetimes = new ArrayList<>();
        //先过滤掉干扰的时间
        for (String filterDateTimeRegexp : ToolsConfig.filterDateTimeRegexps) {
            text = text.replaceAll(filterDateTimeRegexp, "");
        }
        //提取时间
        long tempDateTime;
        Pattern pattern;
        Matcher matcher;
        for (String dateTimeRegexp : ToolsConfig.dateTimeRegexpMap.keySet()) {
            pattern = Pattern.compile(dateTimeRegexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            matcher = pattern.matcher(text);
            while (matcher.find()) {
                //获得的时间格式
                tempDateTime = MyDateUtil.string2long(matcher.group().trim(), ToolsConfig.dateTimeRegexpMap.get(dateTimeRegexp), url);//时间格式
                long endOfDay = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
                //不能大于当前时间或者早于1970年
                if (tempDateTime < endOfDay && tempDateTime >= 0) {
                    datetimes.add(tempDateTime);
                }
                //刚才匹配的要删除
                text = text.replace(matcher.group().trim(), "");
            }
        }
        //还没有提取到时间 提取当前链接的时间
        if (datetimes.size() == 0) {
            for (String dateTimeRegexp : ToolsConfig.dateTimeRegexpMap.keySet()) {
                pattern = Pattern.compile(dateTimeRegexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                matcher = pattern.matcher(url);
                if (matcher.find()) {
                    //获得的时间格式
                    tempDateTime = MyDateUtil.string2long(matcher.group().trim(), ToolsConfig.dateTimeRegexpMap.get(dateTimeRegexp), url);//时间格式
                    long endOfDay = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
                    if (tempDateTime < endOfDay && tempDateTime >= 0) {
                        datetimes.add(tempDateTime);
                    }
                }
            }
        }

        List<Long> times = new ArrayList<>();
        List<Long> dates = new ArrayList<>();
        //分割提取的时间
        datetimes.forEach(aLong -> {
            //获取日期对应当天开始时间
            LocalDateTime startOfDay = LocalDateTime.ofInstant(Instant.ofEpochMilli(aLong), ZoneId.systemDefault()).toLocalDate().atStartOfDay();
            long startDay = startOfDay.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
            if (aLong == startDay) {
                dates.add(aLong);
            }else {
                times.add(aLong);
            }
        });
        //降序排序
        times.sort(Collections.reverseOrder());
        dates.sort(Collections.reverseOrder());
        datetimes.clear();
        datetimes.addAll(times);
        datetimes.addAll(dates);
        return datetimes;
    }

    /**
     * 根据模板提取时间
     *
     * @param pageSource 网页源码
     * @param url        网页地址
     * @param rule       模板规则
     * @param ruleType   规则类型 0:xpath 1:正则
     */
    public List<Long> parserDateTimeTemplate(String pageSource, String url, String rule, int ruleType) {
        List<Long> datetimes = new ArrayList<>();
        if (ruleType == 0) {
            Document document = Jsoup.parse(pageSource);
            //根据xpath匹配元素
            Elements results = Xsoup.compile(rule).evaluate(document).getElements();
            results.forEach(element -> {
                String datastr = element.text();
                long datetime = MyDateUtil.convergeTime(datastr);
                if (datetime > 0) {
                    datetimes.add(datetime);
                }
            });
        } else if (ruleType == 1) {
            //正则匹配
            Pattern pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(pageSource);
            if (matcher.find()) {
                long datetime = MyDateUtil.convergeTime(matcher.group());
                if (matcher.groupCount() >= 1) {
                    datetime = MyDateUtil.convergeTime(matcher.group(1));
                }
                if (datetime > 0) {
                    datetimes.add(datetime);
                }
            }
        }
        //降序排序
        datetimes.sort(Collections.reverseOrder());
        return datetimes;
    }

}

package com.toptime.webspider.plugins.tools.util;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * string工具类
 * Created by bjoso on 2017/8/29.
 */
public class MyStringUtils {
    /**
     * 常规字符
     */
    private static Pattern REGULAR_PATTERN = Pattern.compile("[^\u4e00-\u9fa5\uac00-\ud7a3\\w]++", Pattern.CASE_INSENSITIVE);
    /**
     * 标点符号
     */
    private static Pattern SYMBOL_PATTERN = Pattern.compile("(?![-+])[\\pP\\pZ\\pS\r\n\t]", Pattern.CASE_INSENSITIVE);

    /**
     * 附件清洗文本
     *
     * @param string
     * @return
     */
    public static String filtration(String string) {
        string = string.replaceAll("\\s*|\t|\r|\n", "")
                .replace("\\n", "")
                .replace("\\t", "")
                .replace("null", "");
        Matcher matcher = REGULAR_PATTERN.matcher(string);
        while (matcher.find()) {
            string = string.replace(matcher.group(), " ");
        }

        return string;
    }

    /**
     * 正文过滤
     *
     * @param string
     * @return
     */
    public static String filter(String string) {
        string = string.trim().replaceAll("<[\\s\\S]*?>", "")
                .replaceAll("\\s{5}", "");
        //正则性能瓶颈
        Matcher matcher = SYMBOL_PATTERN.matcher(string);
        while (matcher.find()) {
            string = string.replace(matcher.group(), " ");
        }

        return string;
    }

    /**
     * 正则匹配
     *
     * @param string 字符串
     * @param regex  正则
     * @return 结果
     */
    public static String regexMatch(String string, String regex) {
        String result = "";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            result = matcher.group();
            if (matcher.groupCount() >= 1) {
                result = matcher.group(1);
            }
            result = StringEscapeUtils.unescapeHtml4(result);
        }
        return result;
    }

    /**
     * 正则匹配
     *
     * @param string 字符串
     * @param regex  正则
     * @return 结果
     */
    public static List<String> regexMatchGroup(String string, String regex) {
        List<String> results = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            String result = matcher.group();
            if (matcher.groupCount() >= 1) {
                result = matcher.group(1);
            }
            result = StringEscapeUtils.unescapeHtml4(result);
            results.add(result);
        }
        return results;
    }

    /**
     * 适用于HTML文本正则提取
     * @param txt
     * @param regex
     * @return
     */
    public static String regexParse(String txt, String regex) {
        String source = "";
        // 取出属性相关数据
        Pattern pattern = Pattern.compile(regex);
        Map<String, Integer> map = new HashMap<String, Integer>();
        txt = txt.replaceAll("\\|", "\n");
        String[] lines = txt.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher matcher = pattern.matcher(line);
            if(matcher.find()) {
                line = line.replaceAll("[:：]", "").trim();
                String[] temps = line.split(regex);
                String s = temps.length >= 2 ? temps[1].trim() : "";
                s = s.matches("[,，]") ? "" : s;
                if(!s.isEmpty()) {
                    map.put(s, (map.get(s) == null ? 0 : map.get(s)) + 1);
                } else {
                    if((i+1) > lines.length) {
                        break;
                    }
                    lines[i+1] = regex + lines[i+1];
                }
            }
        }

        // 没匹配到，说明没有属性数据
        if(map.size() == 0) {
            return source;
        }

        // 可能有多个属性值，对其进行排序，取出现次数最多的
        Object[] obj = map.values().toArray();
        Arrays.sort(obj);

        // 获取最大次数
        int max = Integer.parseInt(obj[map.size()-1].toString());
        for (String k: map.keySet()) {
            if(map.get(k) == max) {
                source = k;
                break;
            }
        }

        // 处理尾巴
        String[] sources = source.split(" ");
        source = sources.length > 0 ? sources[0] : source;
        return source;
    }
}

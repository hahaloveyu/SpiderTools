package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.plugins.tools.util.MyStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import us.codecraft.xsoup.Xsoup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标题提取
 * Created by bjoso on 2017/7/3.
 */
public class HtmlTitleParser {
    /**
     * 提取标题
     *
     * @param pageSource 网页源码
     * @param url        网页地址
     * @return title
     */
    public String getTitle(String pageSource, String url) {
        Document doc = Jsoup.parse(pageSource, url);
        //获取head中的title
        String title = doc.title().trim();
        //面包屑分割
        String[] titles = title.split("\\||_|-|&minus;");
        if (titles.length > 1) {
            title = titles[0];
            return title;
        }
        if (title.length() < 8) {
            for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
                Elements headers = doc.getElementsByTag("h" + headerIndex);
                if (headers.size() > 0) {
                    title = headers.get(0).text();
                    break;
                }
            }
        }
        return title;
    }

    /**
     * 根据模板抽取
     *
     * @param pageSource 网页源码
     * @param url        网页地址
     * @param rule       模板规则
     * @param ruleType   规则类型 0:xpath 1:正则
     */
    public String getTitleTemplate(String pageSource, String url, String rule, int ruleType) {
        String title = "";
        if (ruleType == 0) {
            Document document = Jsoup.parse(pageSource, url);
            //根据xpath匹配元素
            Elements results = Xsoup.compile(rule).evaluate(document).getElements();
            title = results.text();
        } else if (ruleType == 1) {
            //正则匹配
            Pattern pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(pageSource);
            if (matcher.find()) {
                title = matcher.group();
                if (matcher.groupCount() >= 1) {
                    title = matcher.group(1);
                }
            }
        }
        return title;
    }

    /**
     * 提取标题
     *
     * @param doc
     * @return
     */
    public String getTitle(Document doc) {
        //获取head中的title
        String title = doc.title().trim();
        //面包屑分割
        String[] titles = title.split("\\||_|-|&minus;");
        if (titles.length > 1) {
            title = titles[0];
            return title;
        }
        if (title.length() < 8) {
            for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
                Elements headers = doc.getElementsByTag("h" + headerIndex);
                if (headers.size() > 0) {
                    title = headers.get(0).text();
                    break;
                }
            }
        }
        return title;
    }

    /**
     * 提取标题
     *
     * @param html
     * @param url
     * @param titleRule
     * @return
     */
    public String getTitle(String html, String url, String titleRule) {
        if (StringUtils.isNotEmpty(titleRule)) {
            return MyStringUtils.regexParse(html, titleRule);
        }
        Document doc = Jsoup.parse(html, url);
        //获取head中的title
        String title = doc.title().trim();
        //面包屑分割
        String[] titles = title.split("\\||_|-|&minus;");
        if (titles.length > 1) {
            title = titles[0];
            return title;
        }
        if (title.length() < 8) {
            for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
                Elements headers = doc.getElementsByTag("h" + headerIndex);
                if (headers.size() > 0) {
                    title = headers.get(0).text();
                    break;
                }
            }
        }
        return title;
    }
}

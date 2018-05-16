package com.toptime.webspider.plugins.tools;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import us.codecraft.xsoup.Xsoup;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 文章提取服务
 * Created by bjoso on 2017/6/15.
 */
public class HtmlArticleParser {

    /**
     * 根据模板抽取[release]
     *
     * @param pageSource 网页源码
     * @param url        网页地址
     * @param rule       模板规则
     * @param ruleType   规则类型 0:xpath 1:正则
     */
    public String[] getContent4Template(String pageSource, String url, String rule, int ruleType) {
        String content = "";
        String showContent = "";
        if (ruleType == 0) {
            Document document = Jsoup.parse(pageSource, url);
            //根据xpath匹配元素
            Elements results = Xsoup.compile(rule).evaluate(document).getElements();
            content = results.text();
            showContent = results.outerHtml();
        } else if (ruleType == 1) {
            //正则匹配 "<div id=\"main_content\" class=\"js_selection_area\">[\\s\\S]*?</div)"
            Pattern pattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(pageSource);
            if (matcher.find()) {
                String str = matcher.group();
                if (matcher.groupCount() >= 1) {
                    str = matcher.group(1);
                }
                Document document = Jsoup.parse(str, url);
                content = document.body().text();
                showContent = document.body().html();
            }
        }
        return new String[]{content, showContent};
    }
}

package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.plugins.tools.util.MyHtmlUtils;
import com.toptime.webspider.util.MyWhitelist;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网页源码清洗标准化服务
 * Created by bjoso on 2017/6/15.
 */
public class HtmlCleaner {

    private static Logger logger = LoggerFactory.getLogger(AutoFormat.class);

    /**
     * 最大节点数
     */
    private static final int MAX_NODE_NUM = 10000;

    /**
     * 字符数阈值
     */
    private static final int MAX_WORDS = 50 * 10000;

    /**
     * 网页源码标准化
     *
     * @param pageSource 网页源码
     * @param url        网页链接
     * @param isRelaxed  轻文档模式(清除js css等)
     * @return 标准化网页源码
     */
    public String standardizingHtml(String pageSource, String url, boolean isRelaxed) {
        //网页编码解除
        try {
            pageSource = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeHtml4(pageSource));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        //判断DOM结构
        Document doc = Jsoup.parse(pageSource, url);
        int nodeNum = doc.getAllElements().size();
        if (nodeNum < MAX_NODE_NUM) {
            //标签更名
            doc.select("noscript").tagName("div");
            if (!isRelaxed) {
                //标准模式 地址绝对化处理
                pageSource = MyHtmlUtils.absoluteAddress(doc, url);
            } else {//轻文档模式
                pageSource = Jsoup.clean(pageSource, url, Whitelist.relaxed());
                pageSource = Jsoup.parse(pageSource, url).html();
            }
        } else {
            pageSource = "网页节点数超过最大限制";
            logger.error("网页节点数超过最大限制|" + "[url:" + url + "]");
        }
        return pageSource;
    }

    /**
     * 白名单模式标准化网页源码
     *
     * @param pageSource 网页源码
     * @param url        网页链接
     * @param whitelist  白名单(MyWhitelist)
     * @return 标准化网页源码
     */
    public String standardizingHtml(String pageSource, String url, Whitelist whitelist) {
        if (whitelist != null) {
            return StringEscapeUtils.unescapeHtml4(Jsoup.clean(pageSource, url, whitelist));
        } else {
            return StringEscapeUtils.unescapeHtml4(Jsoup.clean(pageSource, url, MyWhitelist.normWhitelist()));
        }
    }

    /**
     * 微信网页源码标准化
     *
     * @param pageSource 网页源码
     * @param url        网页链接
     * @return 标准化网页源码
     */
    public String cleanWeixin(String pageSource, String url) {
        //网页编码解除
        try {
            pageSource = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeHtml4(pageSource));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        //判断DOM结构
        Document doc = Jsoup.parse(pageSource, url);
        doc.select("img").forEach(element -> {
            String srcVal = element.attr("data-src");
            element.attr("src", srcVal);
        });
        //标准模式 地址绝对化处理
        pageSource = MyHtmlUtils.absoluteAddress(doc, url);

        return pageSource;
    }

    /**
     * 网页清洗标准化
     *
     * @param html 网页源码
     * @param url
     * @return
     */
    public Document cleanHtml(String html, String url) {
        if (html.length() > MAX_WORDS) {
            html = "";
        }
        //网页编码解除
        try {
            html = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeHtml4(html));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        //判断DOM结构
        Document doc = Jsoup.parse(html, url);
        int nodeNum = doc.getAllElements().size();
        if (nodeNum < MAX_NODE_NUM) {
            doc.select("img[data-src]").forEach(element -> {
                String srcVal = element.attr("data-src");
                element.attr("src", srcVal);
            });
            //清理冗余标签
            doc.select("script").remove();
            doc.select("link").remove();
            doc.select("style").remove();
            doc.select("textarea").remove();
            //地址绝对化
            MyHtmlUtils.absoluteAddress(doc, url);
        } else {
            doc.body().html("");
        }
        return doc;
    }
}

package com.toptime.webspider.plugins.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * 关键词/摘要提取
 *
 * @author jianghao
 * @since 2017年6月21日
 */
public class KeyWordParser {

    /**
     * 提取关键词和摘要
     *
     * @param html 网页源码
     * @param url  url
     * @return 返回关键词和摘要
     */

    public String[] getKD4Meta(String html, String url) {
        StringBuilder keywords = new StringBuilder();
        StringBuilder description = new StringBuilder();
        Document doc = Jsoup.parse(html, url);
        Elements metas = doc.select("meta");
        metas.forEach(element -> {
            String name = element.attr("name");
            if ("keywords".equals(name) || "Keywords".equals(name)) {
                keywords.append(element.attr("content"));
            }
            if ("description".equals(name) || "Description".equals(name)) {
                description.append(element.attr("content"));
            }
        });
        return new String[]{keywords.toString(), description.toString()};
    }

    /**
     * 提取关键词和摘要
     *
     * @param doc 网页源码
     * @return 返回关键词和摘要
     */
    public String[] getKD4Meta(Document doc) {
        StringBuilder keywords = new StringBuilder();
        StringBuilder description = new StringBuilder();
        Elements metas = doc.select("meta");
        metas.forEach(element -> {
            String name = element.attr("name");
            if ("keywords".equals(name) || "Keywzords".equals(name)) {
                keywords.append(element.attr("content"));
            }
            if ("description".equals(name) || "Description".equals(name)) {
                description.append(element.attr("content"));
            }
        });
        return new String[]{keywords.toString(), description.toString()};
    }
}

package com.toptime.webspider.plugins.tools.util;

import org.jsoup.safety.Whitelist;

/**
 * 自定义网页白名单
 * Created by bjoso on 2017/6/20.
 */
public class MyWhitelist {
    /**
     * 网页白名单
     * @return
     */
    public static Whitelist normWhitelist(){

    Whitelist whitelist=new Whitelist()
            .addTags(
                    "a", "b", "blockquote", "br", "caption", "cite", "code", "col",
                    "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6",
                    "i", "img", "li", "ol", "p", "pre", "q", "small", "span", "strike", "strong",
                    "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u",
                    "ul")

            .addAttributes("a", "href", "title")
            .addAttributes("blockquote", "cite")
            .addAttributes("col", "span", "width")
            .addAttributes("colgroup", "span", "width")
            .addAttributes("img", "align", "alt", "height", "src", "title", "width")
            .addAttributes("ol", "start", "type")
            .addAttributes("q", "cite")
            .addAttributes("table", "summary", "width")
            .addAttributes("td", "abbr", "axis", "colspan", "rowspan", "width")
            .addAttributes(
                    "th", "abbr", "axis", "colspan", "rowspan", "scope",
                    "width")
            .addAttributes("ul", "type")

            .addProtocols("a", "href", "ftp", "http", "https", "mailto")
            .addProtocols("blockquote", "cite", "http", "https")
            .addProtocols("cite", "cite", "http", "https")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("q", "cite", "http", "https")
            ;
    return whitelist;
    }
}

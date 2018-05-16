package com.toptime.webspider.plugins.tools.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提取链接
 */
public class MyHtmlUtils {

    private static Log logger = LogFactory.getLog(MyHtmlUtils.class);

    /**
     * 解析 iframe,frame 的链接
     *
     * @param content          包含有 iframe 或 frame 的网页.
     * @param parentUrl        当前网页的URL
     * @param suspectedUrlsMap 非法的网页链接集合
     * @return
     */
    public static Map<String, String> parserFrameLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        Document doc = Jsoup.parse(content, parentUrl);
        //<iframe src="xxx"></iframe>,<frame src="xxx">
        Elements media = doc.select("[src]");//js,iframe,img,frame
        for (Element src : media) {

            if ("iframe".equals(src.tagName()) || src.tagName().equals("frame")) {
                String link = src.attr("src");
                String absLinks = src.attr("abs:src").replaceAll("\r\n|\n", "");//绝对路径链接...

                boolean valid = checkLinks(link, absLinks, "", parentUrl, "iframe,frame");
                if (!valid) {
                    continue;
                }

                if (link.contains("\\")) {
                    absLinks = resolve(parentUrl, link);//补全绝对路径
                }

                if (!absLinks.toLowerCase().startsWith("http")) {
                    suspectedUrlsMap.put(absLinks, "");
                    continue;
                }

                linksMap.put(absLinks, "");

            }

        }

        return linksMap;

    }

    /**
     * 解析 iframe,frame 的链接
     *
     * @param doc
     * @param parentUrl
     * @return
     * @throws Exception
     */
    public static Map<String, String> parserFrameLinks(Document doc, String parentUrl) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();
        //<iframe src="xxx"></iframe>,<frame src="xxx">
        Elements media = doc.select("[src]");//js,iframe,img,frame
        for (Element src : media) {
            if ("iframe".equals(src.tagName()) || src.tagName().equals("frame")) {
                String link = src.attr("src");
                String absLinks = src.attr("abs:src").replaceAll("\r\n|\n", "");//绝对路径链接...
                boolean valid = checkLinks(link, absLinks, "", parentUrl, "iframe,frame");
                if (!valid) {
                    continue;
                }
                if (link.contains("\\")) {
                    absLinks = resolve(parentUrl, link);//补全绝对路径
                }
                if (!absLinks.toLowerCase().startsWith("http")) {
                    continue;
                }
                linksMap.put(absLinks, "");
            }
        }
        return linksMap;
    }

    /**
     * 提取<a href="xxx">xxx</a>的链接
     *
     * @param content          网页源代码
     * @param parentUrl        父 URL
     * @param suspectedUrlsMap 非法链接
     * @return
     */
    public static Map<String, String> parserHrefLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        Document doc = Jsoup.parse(content, parentUrl);

        //<a href="xxx">xxx</a>
        Elements links = doc.select("a[href]"); //href

        //============================a href==========================================
        //<a href="xxx">yyy</a>
        //<a href="#"  相对路径是#,绝对路径是 url#,不处理.
        //<a href="javascript:xxx" 相对路径是 javascript:xxx 绝对路径为空,不处理.
        for (Element link : links) {
            String url = link.attr("href");//同时处理锚点
            String absUrl = link.attr("abs:href").replaceAll("\r\n|\n", "").replaceAll("#.*", "");//绝对地址
            //链接title属性
            String title = link.attr("title");
            if (StringUtils.isEmpty(title)) {
                //链接alt属性
                title = link.attr("alt");
                if (StringUtils.isEmpty(title)) {
                    //使用链接文本
                    title = link.text();
                    if (StringUtils.isEmpty(title)) {
                        //图片title属性
                        Elements imgs = link.select("img");
                        title = imgs.attr("title");
                        if (StringUtils.isEmpty(title)) {
                            //图片alt属性
                            title = imgs.attr("alt");
                        }
                    }
                }
            }
            title = trim(title, 150).replaceAll("\r\n|\n", "");

            //检查地址是否合法
            boolean valid = checkLinks(url, absUrl, title, parentUrl, "href");
            if (!valid) {
                continue;
            }

            //如果相对地址带有反斜杠,则会造成绝对地址有问题.
            if (url.contains("\\")) {
                absUrl = resolve(parentUrl, url);//补全绝对路径
            }


            if (!absUrl.toLowerCase().startsWith("http")) {
                suspectedUrlsMap.put(absUrl, title);
                continue;
            }

            if (linksMap.containsKey(absUrl)) {
                if (linksMap.get(absUrl).length() < title.length()) {
                    linksMap.put(absUrl, title);
                }
            } else {
                linksMap.put(absUrl, title);
            }

            //打印链接及链接文本
            //logger.info(absLink+"|"+anchorText);

        }

        return linksMap;

    }

    /**
     * 提取<a href="xxx">xxx</a>的链接
     *
     * @param doc
     * @param parentUrl
     * @return
     */
    public static Map<String, String> parserHrefLinks(Document doc, String parentUrl) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        //<a href="xxx">xxx</a>
        Elements links = doc.select("a[href]"); //href

        //============================a href==========================================
        //<a href="xxx">yyy</a>
        //<a href="#"  相对路径是#,绝对路径是 url#,不处理.
        //<a href="javascript:xxx" 相对路径是 javascript:xxx 绝对路径为空,不处理.
        for (Element link : links) {
            String url = link.attr("href");//同时处理锚点
            String absUrl = link.attr("abs:href").replaceAll("\r\n|\n", "").replaceAll("#.*", "");//绝对地址
            //链接title属性
            String title = link.attr("title");
            if (StringUtils.isEmpty(title)) {
                //链接alt属性
                title = link.attr("alt");
                if (StringUtils.isEmpty(title)) {
                    //使用链接文本
                    title = link.text();
                    if (StringUtils.isEmpty(title)) {
                        //图片title属性
                        Elements imgs = link.select("img");
                        title = imgs.attr("title");
                        if (StringUtils.isEmpty(title)) {
                            //图片alt属性
                            title = imgs.attr("alt");
                        }
                    }
                }
            }
            title = trim(title, 150).replaceAll("\r\n|\n", "");
            //检查地址是否合法
            boolean valid = checkLinks(url, absUrl, title, parentUrl, "href");
            if (!valid) {
                continue;
            }
            //如果相对地址带有反斜杠,则会造成绝对地址有问题.
            if (url.contains("\\")) {
                absUrl = resolve(parentUrl, url);//补全绝对路径
            }
            if (!absUrl.toLowerCase().startsWith("http")) {
                continue;
            }
            if (linksMap.containsKey(absUrl)) {
                if (linksMap.get(absUrl).length() < title.length()) {
                    linksMap.put(absUrl, title);
                }
            } else {
                linksMap.put(absUrl, title);
            }
        }

        return linksMap;

    }

    /**
     * 提取图片链接 <img src="xxx"/>
     *
     * @param content
     * @param parentUrl
     * @param suspectedUrlsMap
     * @return
     */
    public static Map<String, Map<String, String>> parserImgLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Map<String, Map<String, String>> linksMap = new LinkedHashMap<>();

        //content = content.replaceAll("(?i)<script[\\S\\s]*?</script>", "");//把 JS 先消除掉,防止 JS 里面有链接导致提取混乱.
        Document doc = Jsoup.parse(content, parentUrl);

        //<script src="xxx"></script>,<iframe src="xxx"></iframe>,<img src="xxx"/>,<frame src="xxx">
        Elements media = doc.select("[src]");//js,iframe,img,frame

        //js,iframe,img
        // iframe src="about:blank" 相对路经为 about:blank 绝对路径为空.
        for (Element src : media) {//.js,.img,iframe,frame
            Map<String, String> attrMap = new HashMap<>(2);
            if (!"img".equals(src.tagName())) {
                continue;
            }
            String link = src.attr("src");
            String absLinks = src.attr("abs:src").replaceAll("\r\n|\n", "");
            String altText = trim(src.attr("alt"), 150).replaceAll("\r\n|\n", "");//img 有这个文本

            boolean valid = checkLinks(link, absLinks, altText, parentUrl, "img");
            if (!valid) {
                continue;
            }

            //http://www.ahhy.gov.cn:81/web/display.aspx?id=163777
            if (link.contains("\\")) {  //file file:///C:\DOCUME~1\ADMINI~1\LOCALS~1\Temp\ksohtml\clip_image1579.png

                absLinks = resolve(parentUrl, link);//补全绝对路径...
            }


            if (!absLinks.toLowerCase().startsWith("http")) {
                suspectedUrlsMap.put(absLinks, altText);
                continue;
            }

            String imgSize = src.attr("width") + "x" + src.attr("height");

            attrMap.put("alt", altText);
            attrMap.put("size", imgSize);

            linksMap.put(absLinks, attrMap);

        }

        return linksMap;

    }

    /**
     * 提取图片链接 <img src="xxx"/>
     *
     * @param element   正文节点
     * @param parentUrl
     * @return
     */
    public static Map<String, Map<String, String>> parserImgLinks(Element element, String parentUrl) {

        Map<String, Map<String, String>> linksMap = new LinkedHashMap<>();

        Elements media = element.select("[src]");//js,iframe,img,frame

        //js,iframe,img
        // iframe src="about:blank" 相对路经为 about:blank 绝对路径为空.
        for (Element src : media) {//.js,.img,iframe,frame
            Map<String, String> attrMap = new HashMap<>(2);
            if (!"img".equals(src.tagName())) {
                continue;
            }
            String link = src.attr("src");
            String absLinks = src.attr("abs:src").replaceAll("\r\n|\n", "");
            String altText = trim(src.attr("alt"), 150).replaceAll("\r\n|\n", "");//img 有这个文本

            boolean valid = checkLinks(link, absLinks, altText, parentUrl, "img");
            if (!valid) {
                continue;
            }
            //http://www.ahhy.gov.cn:81/web/display.aspx?id=163777
            if (link.contains("\\")) {  //file file:///C:\DOCUME~1\ADMINI~1\LOCALS~1\Temp\ksohtml\clip_image1579.png

                absLinks = resolve(parentUrl, link);//补全绝对路径...
            }
            if (!absLinks.toLowerCase().startsWith("http")) {
                continue;
            }
            String imgSize = src.attr("width") + "x" + src.attr("height");

            attrMap.put("alt", altText);
            attrMap.put("size", imgSize);

            linksMap.put(absLinks, attrMap);
        }
        return linksMap;
    }

    /**
     * 提取图片链接 <img src="xxx"/>
     *
     * @param elements  图片所在节点
     * @param parentUrl
     * @return
     */
    public static Map<String, Map<String, String>> parserImgLinks(Elements elements, String parentUrl) {

        Map<String, Map<String, String>> linksMap = new LinkedHashMap<>();

        Elements media = elements.select("[src]");//js,iframe,img,frame

        //js,iframe,img
        // iframe src="about:blank" 相对路经为 about:blank 绝对路径为空.
        for (Element src : media) {//.js,.img,iframe,frame
            Map<String, String> attrMap = new HashMap<>(2);
            if (!"img".equals(src.tagName())) {
                continue;
            }
            String link = src.attr("src");
            String absLinks = src.attr("abs:src").replaceAll("\r\n|\n", "");
            String altText = trim(src.attr("alt"), 150).replaceAll("\r\n|\n", "");//img 有这个文本

            boolean valid = checkLinks(link, absLinks, altText, parentUrl, "img");
            if (!valid) {
                continue;
            }
            //http://www.ahhy.gov.cn:81/web/display.aspx?id=163777
            if (link.contains("\\")) {  //file file:///C:\DOCUME~1\ADMINI~1\LOCALS~1\Temp\ksohtml\clip_image1579.png

                absLinks = resolve(parentUrl, link);//补全绝对路径...
            }
            if (!absLinks.toLowerCase().startsWith("http")) {
                continue;
            }
            String imgSize = src.attr("width") + "x" + src.attr("height");

            attrMap.put("alt", altText);
            attrMap.put("size", imgSize);

            linksMap.put(absLinks, attrMap);
        }
        return linksMap;
    }

    /**
     * 地图链接
     *
     * @param content
     * @param parentUrl
     * @param suspectedUrlsMap
     * @return
     */
    public static Map<String, String> parserAreaLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        Document doc = Jsoup.parse(content, parentUrl);

        //<area href="xxx">
        Elements area = doc.select("area[href]");

        //地图链接,没有链接文本
        for (Element src : area) {
            String link = src.attr("href");
            String absLink = src.attr("abs:href").replaceAll("\r\n", "");
            boolean valid = checkLinks(link, absLink, "", parentUrl, "area");
            if (!valid) {
                continue;
            }
            //针对带有反斜杠的拼接错误问题
            if (link.contains("\\")) {
                absLink = resolve(parentUrl, link);
            }

            linksMap.put(absLink, "");

        }

        return linksMap;

    }

    /**
     * 获得 OPTION 值的绝对链接(一般都是外链,链接外部网址)
     *
     * @param content          网页源代码
     * @param parentUrl        父 URL
     * @param suspectedUrlsMap 不是以 非法链接
     * @return
     */
    public static Map<String, String> parserOptionLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Document doc = Jsoup.parse(content, parentUrl);

        //option只对绝对路径有效果.....
        Elements options = doc.select("select > option");

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        //===============================option,只针对 http ==================
        for (Element option : options) {
            String optionHref = option.attr("value").trim();//链接
            String optionText = option.text();//链接文本

            //待验证.
            if (optionHref.toLowerCase().startsWith("http")) {
                linksMap.put(optionHref, optionText);
            }

        }


        return linksMap;

    }

    /**
     * 解析 JS 中获得的链接  <script src=news.asp?typeid=23&shu=9&time=0&title=29></script>
     *
     * @param content
     * @param parentUrl
     * @param suspectedUrlsMap
     * @return
     */
    public static Map<String, String> parserJSLinks(String content, String parentUrl, Map<String, String> suspectedUrlsMap) {

        Map<String, String> linksMap = new LinkedHashMap<String, String>();

        Document doc = Jsoup.parse(content, parentUrl);

        //option只对绝对路径有效果.
        Elements jsscript = doc.select("script[src]");//如果返回的 contenttype 是 application/javascript 或  application/x-javascript 就不应该要

        for (Element src : jsscript) {

            String link = src.attr("src");//相对路径
            String absLink = src.attr("abs:src").replaceAll("\r\n", "");//绝对路径
            boolean valid = checkLinks(link, absLink, "", parentUrl, "script");
            if (!valid) {
                continue;
            }
            //针对带有反斜杠的拼接错误问题
            if (link.contains("\\")) {
                absLink = resolve(parentUrl, link);
            }

            linksMap.put(absLink, "");

        }

        return linksMap;

    }

    private static String trim(String s, int width) {
        if (s.length() > width) {
            return s.substring(0, width - 1) + ".";
        } else {
            return s;
        }
    }


    /**
     * 检查 URL 的合法性
     *
     * @param link      相对链接
     * @param absLink   绝对链接
     * @param parentUrl 父链接
     * @param type      不合法
     */
    public static boolean checkLinks(String link, String absLink, String title, String parentUrl, String type) {

        boolean valid = true;
        //<a href="#"     (不是死链,一般有 onClick=Javascript)
        //<a href="#" onclick="javascript:window.external.AddFavorite('http://www.lusong.gov.cn','中国·芦淞政府门户网站')" title="收藏中国·芦淞政府门户网站到你的收藏夹">收藏本站</a>
        //<a href="#"   不是 javascript,是 # 标签.这种一般是  OK 的,不带 # 的地址肯定会验证.
        //<a href="#" target="_blank" title="打开中国·芦淞政府门户网站旧页面">旧页面</a>
        //以上不会被认为是死链
        if ("#".equals(link.toLowerCase())) {
            return false;
        }

        if (link.length() == 0 || "about:blank".equals(link.toLowerCase())) {
            return false;
        }


        if (link.toLowerCase().startsWith("mailto:")) {
            return false;
        }

        //<a href="javascript:xxx"></a>,链接是 javascript
        if (absLink.length() == 0) {
            return false;
        }


        if ("<x:infourl></x:infourl>".equals(link.toLowerCase())) {
            return false;
        }

        if (link.toLowerCase().contains("<x:infosubtitle")) {
            return false;
        }

        return valid;

    }

    /**
     * 地址绝对化处理
     *
     * @param document 网页源码
     * @param baseUri  基本链接
     * @return
     */
    public static String absoluteAddress(Document document, String baseUri) {
        //处理含有src属性的的元素(包括JS、IMG、IFRAME、FRAME)
        Elements srcElements = document.select("[src]");
        srcElements.forEach(srcElement -> {
            String absSrc = srcElement.attr("abs:src").replaceAll("\r\n|\n", "");
            String src = srcElement.attr("src");
            //针对带有反斜杠的拼接错误问题
            if (src.contains("\\")) {
                absSrc = MyHtmlUtils.resolve(baseUri, src);//补全绝对路径
            }
            if (absSrc.endsWith("#")) {
                absSrc = absSrc.substring(0, absSrc.length() - 1);
            }
            if (absSrc.startsWith("../")) {
                absSrc = absSrc.replace("../", "");
            }
            srcElement.attr("src", absSrc);
        });

        //处理含有href属性的的元素(包括CSS、a链接)
        Elements hrefElements = document.select("[href]");
        hrefElements.forEach(hrefElement -> {
            String absHref = hrefElement.attr("abs:href").replaceAll("\r\n|\n", "");
            String href = hrefElement.attr("href");
            //针对带有反斜杠的拼接错误问题
            if (href.contains("\\")) {
                //补全绝对路径
                absHref = MyHtmlUtils.resolve(baseUri, href);
            }
            if (absHref.endsWith("#")) {
                absHref = absHref.substring(0, absHref.length() - 1);
            }
            if (absHref.contains("../")) {
                absHref = absHref.replace("../", "");
            }
            hrefElement.attr("href", absHref);
        });

        return document.html();
    }

    /**
     * 地址绝对化处理
     *
     * @param element 节点元素
     * @param baseUri 基本链接
     * @return 绝对地址
     */
    public static String absoluteAddress(Element element, String baseUri) {
        if (element.hasAttr("href")) {
            String absHref = element.attr("abs:href").replaceAll("\r\n|\n", "");
            String href = element.attr("href");
            //针对带有反斜杠的拼接错误问题
            if (href.contains("\\")) {
                absHref = MyHtmlUtils.resolve(baseUri, href);//补全绝对路径
            }
            if (absHref.endsWith("#")) {
                absHref = absHref.substring(0, absHref.length() - 1);
            }
            if (absHref.contains("../")) {
                absHref = absHref.replace("../", "");
            }
            return absHref;
        } else if (element.hasAttr("src")) {
            String absSrc = element.attr("abs:src").replaceAll("\r\n|\n", "");
            String src = element.attr("src");
            //针对带有反斜杠的拼接错误问题
            if (src.contains("\\")) {
                absSrc = MyHtmlUtils.resolve(baseUri, src);//补全绝对路径
            }
            if (absSrc.endsWith("#")) {
                absSrc = absSrc.substring(0, absSrc.length() - 1);
            }
            if (absSrc.contains("../")) {
                absSrc = absSrc.replace("../", "");
            }
            return absSrc;
        } else {
            return "";
        }
    }

    /**
     * 移除网页中的链接
     *
     * @param pageSource 网页源码
     * @param url        网页链接
     * @return 清洗后网页
     */
    public static String removeLink(String pageSource, String url) {
        Document doc = Jsoup.parse(pageSource, url);
        //移除含有src属性的的元素(包括JS、IMG、IFRAME、FRAME)
        doc.select("[src]").remove();
        //移除含有href属性的的元素(包括CSS、a链接)
        doc.select("[href]").remove();
        return doc.html();
    }

    /**
     * 创建相对地址
     *
     * @param base   the existing absolulte base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return the resolved absolute URL
     * @throws MalformedURLException if an error occurred generating the URL
     */
    public static URL resolve(URL base, String relUrl) throws MalformedURLException {
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
        if (relUrl.startsWith("?"))
            relUrl = base.getPath() + relUrl;
        // workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
        if (relUrl.indexOf('.') == 0 && base.getFile().indexOf('/') != 0) {
            base = new URL(base.getProtocol(), base.getHost(), base.getPort(), "/" + base.getFile());
        }
        return new URL(base, relUrl);
    }

    /**
     * 创建相对地址
     *
     * @param baseUrl the existing absolute base URL
     * @param relUrl  the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return an absolute URL if one was able to be generated, or the empty string if not
     */
    public static String resolve(String baseUrl, String relUrl) {
        URL base;

        //注意
        relUrl = relUrl.replaceAll("\\\\\'", "");

        relUrl = relUrl.replaceAll("\\\\", "/");
        relUrl = relUrl.replaceAll("\r\n|\n", "");

        try {
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute/rel may be abs on its own, so try that
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }

    /**
     * 网页源码去除干扰因素
     *
     * @param html 网页源码
     */
    public static String deleteTab(String html) {
        html = html.replaceAll("&nbsp;", "").replaceAll("　", "")
                .replaceAll("<!--[\\s\\S]*?-->", "").replaceAll("<script[\\s\\S]*?</script>", "")
                .replaceAll("【", "").replaceAll("】", "").replaceAll("［", "").replaceAll("］", "")
                .replaceAll("[\r\n]", " ").replaceAll("\t", " ").replaceAll(" ", "")
                .replaceAll("十[一二三四五六七八九十][、]", "").replaceAll("二十、", "").replaceAll("[一二三四五六七八九十][、]", "");

        return html;
    }

    /**
     * 去除两者之间的标签
     *
     * @param between 两者之间的内容
     * @return
     */
    public static String deleteBetween(String between) {
        between = between.replaceAll("　", " ").replaceAll("[\n\r]", " ").replaceAll("<br/>", "")
                .replaceAll("&nbsp;", " ").replaceAll("[\t]", " ").replaceAll("[ ]{2,}", " ")
                .replaceAll("<[\\s\\S]*?>", "").replaceAll("<[\\s\\S]*", "").replaceAll("&mdash;", "—")
                .trim();

        return between;
    }
}

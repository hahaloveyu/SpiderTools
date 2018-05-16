package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.entity.RegexpConf;
import com.toptime.webspider.entity.XpathConf;
import com.toptime.webspider.plugins.tools.util.MyHtmlUtils;
import com.toptime.webspider.util.DomainUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import us.codecraft.xsoup.Xsoup;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * URL提取
 *
 * @author jianghao
 * @since 2017年6月19日
 */
public class UrlParser {

    /**
     * 转向正则
     */
    private static final Pattern TURN_PATTERN = Pattern.compile("window\\.location\\.href=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * 安全狗正则
     */
    private static final List<Pattern> SAFE_DOGS = new ArrayList<>();

    static {
        SAFE_DOGS.add(Pattern.compile("function\\st3_ar_guard\\(\\)[\\s\\S]*href=\"/stream_", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        SAFE_DOGS.add(Pattern.compile("function\\sJumpSelf\\(\\)[\\s\\S]*\"/index.asp\\?WebShieldSessionVerify", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        SAFE_DOGS.add(Pattern.compile("function\\sstringToHex\\(str\\)[\\s\\S]*/default.asp\\?security_verify_data", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        SAFE_DOGS.add(Pattern.compile("function\\sstringToHex\\(str\\)[\\s\\S]*self.location = \"/\\?security_verify_data=", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        SAFE_DOGS.add(Pattern.compile("po\\s\\+=\\s\"\\\\\"\";\\seval\\(\"qo=eval;qo\\(po\\);\"", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
    }

    /**
     * 提取链接和标题
     *
     * @param url          页面链接
     * @param html         页面源码
     * @param isAreaLink   是否提取地图链接,没有链接文本
     * @param isOptionLink 是否提取下拉列表链接
     * @param isImgLink    是否提取图片链接
     * @param isJsDateLink 是否提取js链接
     * @return 返回map key:url,value:title
     */
    
    public LinkedHashMap<String, String> getUrlAndTitle(String url, String html, boolean isAreaLink,
                                                        boolean isOptionLink, boolean isImgLink, boolean isJsDateLink) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        Map<String, String> suspectedUrlsMap = new HashMap<>();
        //href链接
        Map<String, String> hrefLinkMap = MyHtmlUtils.parserHrefLinks(html, url, suspectedUrlsMap);
        if (hrefLinkMap != null && hrefLinkMap.size() > 0) {
            resultMap.putAll(hrefLinkMap);
        }
        //frame 链接
        Map<String, String> frameLinkMap = MyHtmlUtils.parserFrameLinks(html, url, suspectedUrlsMap);
        if (frameLinkMap != null && frameLinkMap.size() > 0) {
            resultMap.putAll(frameLinkMap);
        }
        if (isAreaLink) {
            //地图链接
            Map<String, String> areaLinkMap = MyHtmlUtils.parserAreaLinks(html, url, suspectedUrlsMap);
            if (areaLinkMap != null && areaLinkMap.size() > 0) {
                resultMap.putAll(areaLinkMap);
            }
        }
        if (isOptionLink) {
            //下拉列表链接
            Map<String, String> optionLinkMap = MyHtmlUtils.parserOptionLinks(html, url, suspectedUrlsMap);
            if (optionLinkMap != null && optionLinkMap.size() > 0) {
                resultMap.putAll(optionLinkMap);
            }
        }
        if (isImgLink) {
            //图片链接
            Map<String, Map<String, String>> imgLinkMap = MyHtmlUtils.parserImgLinks(html, url, suspectedUrlsMap);
            if (imgLinkMap != null && imgLinkMap.size() > 0) {
                imgLinkMap.forEach((k, v) -> resultMap.put(k, v.get("alt")));
            }
        }
        if (isJsDateLink) {
            //js时间
            Map<String, String> jsdateLinkMap = MyHtmlUtils.parserJSLinks(html, url, suspectedUrlsMap);
            if (jsdateLinkMap != null && jsdateLinkMap.size() > 0) {
                resultMap.putAll(jsdateLinkMap);
            }
        }
        return resultMap;
    }

    /**
     * xpath提取链接和标题
     *
     * @param url       页面链接
     * @param html      页面源码
     * @param templates 模板规则
     * @return 返回map  key: 0:域外 1:域内,value: url和标题map-key:url value:标题
     */
    public Map<Integer, LinkedHashMap<String, String>> getUrlAndTitleXpath(String url, String html, List<XpathConf> templates) {
        Map<Integer, LinkedHashMap<String, String>> resultMap = new HashMap<>();
        //域内的链接
        LinkedHashMap<String, String> domainMap = new LinkedHashMap<>();
        //域外的链接
        LinkedHashMap<String, String> unDomainMap = new LinkedHashMap<>();
        /*根据父节点模板分组*/
        Map<String, List<XpathConf>> xpathMap = templates.stream().collect(Collectors.groupingBy(XpathConf::getPid));
        Document document = Jsoup.parse(html, url);
        xpathMap.forEach((pid, xpathConfs) -> {
            Elements parents = Xsoup.compile(pid).evaluate(document).getElements();
            parents.forEach(parent -> {
                String link = "";
                String title = "";
                for (XpathConf xpathConf : xpathConfs) {
                    String tempName = xpathConf.getTempName();
                    String xpath = xpathConf.getXpath();
                    Elements children = Xsoup.compile(xpath).evaluate(Jsoup.parse(parent.outerHtml())).getElements();
                    if (children != null && children.size() > 0) {
                        if ("LINK".equals(tempName) || "IMAGE".equals(tempName)) {
                            link = MyHtmlUtils.absoluteAddress(children.get(0), url);
                        } else if ("TITLE".equals(tempName)) {
                            title = children.text();
                        }
                    }
                }
                //提取域名
                String urlDomain = DomainUtils.getDomainSite(url);
                //域内
                if (link.toLowerCase().contains(urlDomain)) {//该 iframe 或 frame 的网址不属于父 URL 的域名.
                    domainMap.put(link, title);
                } else {
                    //域外
                    unDomainMap.put(link, title);
                }
            });
        });
        resultMap.put(0, unDomainMap);
        resultMap.put(1, domainMap);
        return resultMap;
    }

    /**
     * 正则提取链接和标题
     *
     * @param url       页面链接
     * @param html      页面源码
     * @param templates 模板规则
     * @return 返回map  key: 0:域外 1:域内,value: url和标题map-key:url value:标题
     */
    public Map<Integer, LinkedHashMap<String, String>> getUrlAndTitleRegexp(String url, String html, List<RegexpConf> templates) {
        Map<Integer, LinkedHashMap<String, String>> resultMap = new HashMap<>();
        //域内的链接
        LinkedHashMap<String, String> domainMap = new LinkedHashMap<>();
        //域外的链接
        LinkedHashMap<String, String> unDomainMap = new LinkedHashMap<>();
        /*根据父节点模板分组*/
        Map<String, List<RegexpConf>> regexpMap = templates.stream().collect(Collectors.groupingBy(RegexpConf::getPid));

        List<Map<String, String>> results = new ArrayList<>();
        regexpMap.forEach((pid, regexpConfs) -> {
            Pattern pattern = Pattern.compile(pid, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            Matcher matcher = pattern.matcher(html);
            regexpConfs.forEach(xpathConf -> {
                String tempName = xpathConf.getTempName();
                String regexp = xpathConf.getRegex();
                while (matcher.find()) {
                    String link = "";
                    String title = "";
                    String parent = matcher.group();
                    Pattern pat = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    Matcher mat = pat.matcher(parent);
                    String child = "";
                    if (mat.find()) {
                        child = mat.group(1);
                        //链接 图片
                        if ("LINK".equals(tempName) || "IMAGE".equals(tempName)) {
                            if (!child.startsWith("http")) {
                                child = MyHtmlUtils.resolve(url, child);
                            }
                            link = child;
                        } else if ("TITLE".equals(tempName)) {
                            title = child;
                        }
                    }
                    //提取域名
                    String urlDomain = DomainUtils.getDomainSite(url);
                    //域内 //该 iframe 或 frame 的网址不属于父 URL 的域名.
                    if (link.toLowerCase().contains(urlDomain)) {
                        domainMap.put(link, title);
                    } else {
                        //域外
                        unDomainMap.put(link, title);
                    }
                }
            });
        });
        resultMap.put(0, unDomainMap);
        resultMap.put(1, domainMap);
        return resultMap;
    }

    /**
     * 提取链接和标题	只取frame和href链接
     *
     * @param url  页面链接
     * @param html 页面源码
     * @return 返回map key:url,value:title
     */
    public LinkedHashMap<String, String> getUrlAndTitle(String url, String html) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        Map<String, String> suspectedUrlsMap = new HashMap<>();
        //href链接
        Map<String, String> hrefLinkMap = MyHtmlUtils.parserHrefLinks(html, url, suspectedUrlsMap);
        if (hrefLinkMap != null && hrefLinkMap.size() > 0) {
            resultMap.putAll(hrefLinkMap);
        }
        //frame 链接
        Map<String, String> frameLinkMap = MyHtmlUtils.parserFrameLinks(html, url, suspectedUrlsMap);
        if (frameLinkMap != null && frameLinkMap.size() > 0) {
            resultMap.putAll(frameLinkMap);
        }
        return resultMap;
    }

    /**
     * 提取链接和标题	只取frame和href链接
     *
     * @param url 页面链接
     * @param doc 页面源码
     * @return 返回map key:url,value:title
     */
    public LinkedHashMap<String, String> getUrlAndTitle(Document doc, String url) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        //href链接
        Map<String, String> hrefLinkMap = MyHtmlUtils.parserHrefLinks(doc, url);
        if (hrefLinkMap != null && hrefLinkMap.size() > 0) {
            resultMap.putAll(hrefLinkMap);
        }
        //frame 链接
        Map<String, String> frameLinkMap = MyHtmlUtils.parserFrameLinks(doc, url);
        if (frameLinkMap != null && frameLinkMap.size() > 0) {
            resultMap.putAll(frameLinkMap);
        }
        return resultMap;
    }

    /**
     * 提取链接和标题
     *
     * @param url  页面链接
     * @param html 页面源码
     * @param site 0：按域  1：按站
     * @return 返回map key:url,value:title
     */
    public LinkedHashMap<String, String> getUrlAndTitle(String url, String html, int site) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        LinkedHashMap<String, String> linkMap = getUrlAndTitle(url, html);
        //提取域名
        String urlDomain = DomainUtils.getDomainSite(url);
        String urlHost = DomainUtils.getWebSite(url);
        for (String key : linkMap.keySet()) {
            if (site == 0) {//按域名
                if (DomainUtils.getDomainSite(key).equals(urlDomain)) {
                    continue;
                }
            }
            if (site == 1) {//按站点
                if (DomainUtils.getWebSite(key).equals(urlHost)) {
                    continue;
                }
            }
            resultMap.put(key, linkMap.get(key));
        }
        return resultMap;
    }

    /**
     * 提取链接和标题
     *
     * @param url  页面链接
     * @param html 页面源码
     * @return 返回map key: 0:域外 1:域内,value: url和标题map-key:url value:标题
     */
    public Map<Integer, LinkedHashMap<String, String>> getUrlAndTitleByDomain(String url, String html) {
        Map<Integer, LinkedHashMap<String, String>> resultMap = new HashMap<>();
        LinkedHashMap<String, String> linkMap = getUrlAndTitle(url, html);
        //提取域名
        String urlDomain = DomainUtils.getDomainSite(url);
        //域内的链接
        LinkedHashMap<String, String> domainMap = new LinkedHashMap<>();
        //域外的链接
        LinkedHashMap<String, String> unDomainMap = new LinkedHashMap<>();
        for (String key : linkMap.keySet()) {
            //域内
            if (DomainUtils.getDomainSite(key).equals(urlDomain)) {
                domainMap.put(key, linkMap.get(key));
            } else {
                //域外
                unDomainMap.put(key, linkMap.get(key));
            }
        }
        resultMap.put(0, unDomainMap);
        resultMap.put(1, domainMap);
        return resultMap;
    }

    /**
     * 提取链接和标题
     *
     * @param url 页面链接
     * @param doc 页面源码
     * @return 返回map key: 0:域外 1:域内,value: url和标题map-key:url value:标题
     */
    public Map<Integer, LinkedHashMap<String, String>> getUrlAndTitleByDomain(Document doc, String url) {
        Map<Integer, LinkedHashMap<String, String>> resultMap = new HashMap<>();
        LinkedHashMap<String, String> linkMap = getUrlAndTitle(doc, url);
        //提取域名
        String urlDomain = DomainUtils.getDomainSite(url);
        //域内的链接
        LinkedHashMap<String, String> domainMap = new LinkedHashMap<>();
        //域外的链接
        LinkedHashMap<String, String> unDomainMap = new LinkedHashMap<>();
        for (String key : linkMap.keySet()) {
            //域内
            if (DomainUtils.getDomainSite(key).equals(urlDomain)) {
                domainMap.put(key, linkMap.get(key));
            } else {
                //域外
                unDomainMap.put(key, linkMap.get(key));
            }
        }
        resultMap.put(0, unDomainMap);
        resultMap.put(1, domainMap);
        return resultMap;
    }


    /**
     * 提取链接和标题
     *
     * @param url            页面链接
     * @param html           页面源码
     * @param isContains     包含 true  不包含false
     * @param conditionsList 自定义url过滤正则
     * @return 返回map key:url,value:title
     */
    public LinkedHashMap<String, String> getUrlAndTitle(String url, String html, boolean isContains, List<String> conditionsList) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        LinkedHashMap<String, String> linkMap = getUrlAndTitle(url, html);
        for (String key : linkMap.keySet()) {
            if (conditionsList != null && conditionsList.size() > 0) {
                //自定义url过滤正则
                boolean isCheck = checkUrl(key, isContains, conditionsList);
                if (!isCheck) {
                    continue;
                }
            }
            resultMap.put(key, linkMap.get(key));
        }
        return resultMap;
    }

    /**
     * 提取链接和标题
     *
     * @param url            页面链接
     * @param html           页面源码
     * @param site           0：按域  1：按站
     * @param isContains     包含 true  不包含false
     * @param conditionsList 自定义url过滤正则
     * @return 返回map key:url,value:title
     */
    public LinkedHashMap<String, String> getUrlAndTitle(String url, String html, int site, boolean isContains, List<String> conditionsList) {
        LinkedHashMap<String, String> resultMap = new LinkedHashMap<>();
        LinkedHashMap<String, String> linkMap = getUrlAndTitle(url, html);
        //提取域名
        String urlDomain = DomainUtils.getDomainSite(url);
        String urlHost = DomainUtils.getWebSite(url);
        for (String key : linkMap.keySet()) {
            //按域名
            if (site == 0) {
                if (DomainUtils.getDomainSite(key).equals(urlDomain)) {
                    continue;
                }
            }
            //按站点
            if (site == 1) {
                if (DomainUtils.getWebSite(key).equals(urlHost)) {
                    continue;
                }
            }
            if (conditionsList != null && conditionsList.size() > 0) {
                //自定义url过滤正则
                boolean isCheck = checkUrl(key, isContains, conditionsList);
                if (!isCheck) {
                    continue;
                }
            }
            resultMap.put(key, linkMap.get(key));
        }
        return resultMap;
    }

    /**
     * 转向链接提取
     *
     * @param html 网页源码
     * @param url  url
     * @return
     */
    public Set<String> linkTurn(String html, String url) {
        html = StringEscapeUtils.unescapeHtml4(html);
        Set<String> results = new HashSet<>();
        Matcher matcher = TURN_PATTERN.matcher(html);
        while (matcher.find()) {
            String result = matcher.group();
            if (matcher.groupCount() >= 1) {
                result = matcher.group(1);
            }
            if (StringUtils.isNotEmpty(result) && !result.startsWith("http")) {
                result = MyHtmlUtils.resolve(url, result);
            }
            results.add(result);
        }
        return results;
    }

    /**
     * 判断是不是安全狗
     *
     * @param html 网页源码
     * @param url  链接
     * @return
     */
    public boolean isSafeDog(String html, String url) {
        for (Pattern safeDog : SAFE_DOGS) {
            if (safeDog.matcher(html).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否作为 合法的 URL 进入,为了检验 更新时间
     *
     * @param url             页面链接
     * @param contains        包含 true  不包含false
     * @param containsStrList 自定义url过滤正则
     * @return
     */
    private boolean checkUrl(String url, boolean contains, List<String> containsStrList) {
        //包含(列表中的任意一个)
        if (contains) {
            //包含,集合列表中的这些链接字符串,是 OR 的关系.//默认为不包含
            boolean isEnterQueue = false;
            if (containsStrList.size() == 0) {
                //表示可以入队列
                isEnterQueue = true;
            }
            //大小写不敏感
            for (String cstr : containsStrList) {
                //表示有包含
                if (url.toLowerCase().contains(cstr.toLowerCase())) {
                    //表示可以入队列
                    isEnterQueue = true;
                    break;
                }
            }
            //没有碰到一个字符串允许进入队列的
            return isEnterQueue;
        } else { //不包含关系
            //不包含这些字符串 //默认为进入队列.
            boolean isEnterQueue = true;
            if (containsStrList.size() == 0) {
                //表示可以进入队列的.
                isEnterQueue = true;
            }
            for (String cstr : containsStrList) {
                //表示有包含
                if (url.toLowerCase().contains(cstr.toLowerCase())) {
                    //表示不能进入队列.
                    isEnterQueue = false;
                    break;
                }
            }
            //表示不能进入队列
            return isEnterQueue;
        }
    }
}

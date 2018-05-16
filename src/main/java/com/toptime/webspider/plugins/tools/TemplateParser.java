package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.entity.RegexpConf;
import com.toptime.webspider.entity.XpathConf;
import com.toptime.webspider.plugins.tools.util.MyDateUtil;
import com.toptime.webspider.plugins.tools.util.MyHtmlUtils;
import com.toptime.webspider.plugins.tools.util.MyStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import us.codecraft.xsoup.Xsoup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模版提取
 */
public class TemplateParser extends com.toptime.webspider.core.tools.TemplateParser{
	private static Log logger = LogFactory.getLog(TemplateParser.class);
    /**
     * 链接
     */
    private static final String URL = "URL";
    /**
     * 图片
     */
    private static final String IMAGEURL = "IMAGEURL";
    /**
     * 正文
     */
    private static final String CONTENT = "DRECONTENT";
    /**
     * 日期
     */
    private static final String DATE = "DREDATE";
    /**
     * ROOT
     */
    private static final String ROOT = "ROOT";

    /**
     * xpath模板抽取
     *
     * @param html      源码
     * @param url       链接
     * @param templates xpath模板组
     * @return 字段&内容
     */
    public List<Map<String, String>> xpathExtract(String html, String url, List<XpathConf> templates) {
        //网页编码解除
        try {
            html = StringEscapeUtils.unescapeJava(html);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        /**根据父节点模板分组*/
        Map<String, List<XpathConf>> xpathMap = templates.stream().collect(Collectors.groupingBy(XpathConf::getPid));

        List<Map<String, String>> results = new ArrayList<>();
        Document document = Jsoup.parse(html, url);
        xpathMap.forEach((pid, xpathConfs) -> {
            if (ROOT.equals(pid.toUpperCase())) {
                Map<String, String> result = new LinkedHashMap<>();
                xpathConfs.forEach(xpathConf -> {
                        /*if (tempName.equals("DRECONTENT") || tempName.equals("DREDATE") || tempName.equals("DRETITLE")) {
                            return;
                        }*/
                    /**
                     * 需要保证父节点中能查到 样例:
                     * 父节点->/html/body/div[@id='main']/li[1]
                     * 子节点->/html/body/li[1]/h2/a
                     */
                    String tempName = xpathConf.getTempName().toUpperCase();
                    String xpath = xpathConf.getXpath();
                    Elements children = Xsoup.compile(xpath).evaluate(document).getElements();
                    if (children != null && children.size() > 0) {
                        String child = "";
                        switch (tempName) {
                            case URL:
                            case IMAGEURL:
                                child = MyHtmlUtils.absoluteAddress(children.get(0), url);
                                break;
                            case CONTENT:
                                child = MyStringUtils.filter(children.text());
                                break;
                            case DATE:
                                child += MyDateUtil.convergeTime(children.text());
                                break;
                            default:
                                child = children.text();
                        }
                        result.put(tempName, child);
                    } else {
                        result.put(tempName, "");
                    }
                });
                results.add(result);
            } else {
                Elements parents = Xsoup.compile(pid).evaluate(document).getElements();
                parents.forEach(parent -> {
                    Map<String, String> result = new LinkedHashMap<>();
                    xpathConfs.forEach(xpathConf -> {
                            /*if (tempName.equals("DRECONTENT") || tempName.equals("DREDATE") || tempName.equals("DRETITLE")) {
                                return;
                            }*/
                        String tempName = xpathConf.getTempName().toUpperCase();
                        String xpath = xpathConf.getXpath();
                        Elements children = Xsoup.compile(xpath).evaluate(Jsoup.parse(parent.outerHtml())).getElements();
                        if (children != null && children.size() > 0) {
                            String child = "";
                            switch (tempName) {
                                case URL:
                                case IMAGEURL:
                                    child = MyHtmlUtils.absoluteAddress(children.get(0), url);
                                    break;
                                case CONTENT:
                                    child = MyStringUtils.filter(children.text());
                                    break;
                                case DATE:
                                    child += MyDateUtil.convergeTime(children.text());
                                    break;
                                default:
                                    child = children.text();
                            }
                            result.put(tempName, child);
                        } else {
                            result.put(tempName, "");
                        }
                    });
                    results.add(result);
                });
            }
        });
        return results;
    }

    @Test
    public void testString() {
		// TODO Auto-generated method stub
    	String a ="¥2520000";
    	System.out.println("1"+a.replace("¥", ""));
    	System.out.println("2"+a.replaceAll("¥", ""));
	}
    
    /**
     * 正则模板抽取
     *
     * @param html      源码
     * @param url       链接
     * @param templates 正则模版组
     * @return 字段&内容
     */
    public List<Map<String, String>> regexpExtract(String html, String url, List<RegexpConf> templates) {
        //网页编码解除
        try {
            html = StringEscapeUtils.unescapeJava(html);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        /*根据父节点模板分组*/
        Map<String, List<RegexpConf>> regexpMap = templates.stream().collect(Collectors.groupingBy(RegexpConf::getPid));
        Pattern pattern;
        List<Map<String, String>> resultList = new ArrayList<>();
        for (String pid : regexpMap.keySet()) {
            List<RegexpConf> regexpConfs = regexpMap.get(pid);
            //父节点为当前页面
            if (ROOT.equals(pid.toUpperCase())) {
                Map<String, String> result = new LinkedHashMap<>();
                for (RegexpConf regexpConf : regexpConfs) {
                    String tempName = regexpConf.getTempName().toUpperCase();
                    String regexp = regexpConf.getRegex();
                    //获取子节点
                    pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                    Matcher matcher = pattern.matcher(html);
                    String child = "";
                    if (matcher.find()) {
                        child = matcher.group();
                        if (matcher.groupCount() >= 1) {
                            child = matcher.group(1);
                        }
                        switch (tempName) {
                            case URL:
                            	if (!child.startsWith("http")) {
                                    child = MyHtmlUtils.resolve(url, child);
                                }
                            	break;
                            case IMAGEURL:
                                if (!child.startsWith("http")) {
                                    child = MyHtmlUtils.resolve(url, child);
                                }
                                break;
                            case CONTENT:
                                child = MyStringUtils.filter(child);
                                break;
                            case DATE:
                                child = "" + MyDateUtil.convergeTime(child);
                                break;
                            default:
                            	child=child.replaceAll("<[^>]*?>", "").replace("元", "").replace(",", "").replace(":", "").trim();
                            	child = StringEscapeUtils.unescapeJava(child);
                            	child = StringEscapeUtils.unescapeHtml4(child).replace("¥", "").trim();
                            	//logger.info("-----------------------进入child选择----------------"+child);
                            	
                        }
                    }
                    result.put(tempName, child);
                }
                resultList.add(result);
            } else {
                //父节点有规则
                pattern = Pattern.compile(pid, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher matcher = pattern.matcher(html);
                //获取父节点
                while (matcher.find()) {
                    Map<String, String> result = new LinkedHashMap<>();
                    for (RegexpConf regexpConf : regexpConfs) {
                        String tempName = regexpConf.getTempName().toUpperCase();
                        String regexp = regexpConf.getRegex();
                        String parent = matcher.group();
                        //获取子节点
                        pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                        Matcher mat = pattern.matcher(parent);
                        String child = "";
                        if (mat.find()) {
                            child =mat.group();
                            if (mat.groupCount() >= 1) {
                                child = mat.group(1);
                            }
                            switch (tempName) {
                                case URL:
                                	if (!child.startsWith("http")) {
                                        child = MyHtmlUtils.resolve(url, child);
                                    }
                                	break;
                                case IMAGEURL:
                                    if (!child.startsWith("http")) {
                                        child = MyHtmlUtils.resolve(url, child);
                                    }
                                    break;
                                case CONTENT:
                                    child = MyStringUtils.filter(child);
                                    break;
                                case DATE:
                                    child = "" + MyDateUtil.convergeTime(child);
                                    break;
                                default:
                                	child=child.replaceAll("<[^>]*?>", "").replace("元", "").replace(",", "").replace(":", "").trim();
                                	child = StringEscapeUtils.unescapeJava(child);
                                	child = StringEscapeUtils.unescapeHtml4(child).replace("¥", "").trim();
                                	//logger.info("-----------------------进入child选择----------------"+child);
                                	
                            }
                        }
                        result.put(tempName, child);
                    }
                    resultList.add(result);
                }
            }
        }
        return resultList;
    }


}

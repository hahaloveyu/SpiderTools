package com.toptime.webspider.plugins.tools;

import com.alibaba.fastjson.JSONObject;
import com.toptime.webspider.config.ToolsConfig;
import com.toptime.webspider.entity.*;
import com.toptime.webspider.plugins.tools.util.MyDateUtil;
import com.toptime.webspider.plugins.tools.util.MyHtmlUtils;
import com.toptime.webspider.plugins.tools.util.MyStringUtils;
import com.toptime.webspider.plugins.tools.util.SimilarityUtil;
import com.toptime.webspider.util.DomainUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用流程方法
 * Created by bjoso on 2017/10/19.
 */
public class CommonMethod extends com.toptime.webspider.core.tools.CommonMethod {

    private static Log logger = LogFactory.getLog(CommonMethod.class);

    private String path = System.getProperty("java.class.path");

    /**
     * 列表页url正则
     */
    private static Pattern URL_PATTERN = Pattern.compile("index|list|more|page|deforce", Pattern.CASE_INSENSITIVE);
    /**
     * “更多”正则
     */
    private static Pattern MORE_PATTERN = Pattern.compile("更多\\S*[》>.]", Pattern.CASE_INSENSITIVE);

    /**
     * 时间日期提取
     */
    private DatetimeParser datetimeParser = new DatetimeParser();
    /**
     * 正文提取
     */
    private Readability readability = new Readability();
    /**
     * 正文提取
     */
    private HtmlArticleParser articleParser = new HtmlArticleParser();
    /**
     * 标题提取
     */
    private HtmlTitleParser html2Title = new HtmlTitleParser();
    /**
     * 关键词提取
     */
    private KeyWordParser keyWordParser = new KeyWordParser();
    /**
     * 网页清洗
     */
    private HtmlCleaner htmlCleaner = new HtmlCleaner();
    /**
     * 缩略图提取
     */
    private ThumbnailParser thumbnailParser = new ThumbnailParser();
    /**
     * HanLPAnalyze
     */
    private HanLPAnalyze hanLPAnalyze = new HanLPAnalyze();
    /**
     * URL提取
     */
    private UrlParser urlParser = new UrlParser();
    /**
     * 模版提取
     */
    private TemplateParser templateParser = new TemplateParser();
    /**
     * 自动化格式化服务
     */
    private AutoFormat autoFormat = new AutoFormat();
    /**
     * 附件解析
     */
    private AnnexParser annexParser = new AnnexParser();


    /**
     * 获取tools常用数据
     *
     * @param url           网页URL
     * @param html          网页源码
     * @param isShowContent 是否需要showcontent
     * @param isAutoFormat  是否需要自动格式化
     * @param type          网站类别
     * @param linkTitle     链接标题
     * @return 返回tools 常用数据
     */
    public ToolsInfo getToolsInfo(String url, String html, boolean isShowContent, boolean isAutoFormat, String type, String linkTitle) {
        logger.info(path);
        logger.info("plugins tools");
        logger.info("url:[" + url + "].start.");
        ToolsInfo toolsInfo = new ToolsInfo();
        toolsInfo.setUrl(url);
        toolsInfo.setEmotiona(0);
        /*网页清洗*/
        Document doc = htmlCleaner.cleanHtml(html, url);
        /*提取正文*/
        Element article = readability.articleContent(doc.html(), url);
        String content = article.text();
        toolsInfo.setContent(MyStringUtils.filter(content));
        if (isShowContent) {
            String showContent = article.html().replaceAll("[\\r\\n\\t]", "");
            toolsInfo.setShowContent(showContent);
        }
        /*提取日期时间*/
        String text = doc.text();
        List<Long> dateTimeList = datetimeParser.parserDateTimeAuto(text, url);
        toolsInfo.setDateTimeList(dateTimeList);
        /*标题*/
        String title = html2Title.getTitle(doc);
        toolsInfo.setTitle(title);
        /*提取所有链接和标题	key: 0:域外 1:域内,value: url和标题map-key:url value:标题*/
        Map<Integer, LinkedHashMap<String, String>> urlAndTitleMap = urlParser.getUrlAndTitleByDomain(doc, url);
        toolsInfo.setUrlAndTitleMap(urlAndTitleMap);
        /*判断列表页*/
        boolean enabled = true;
        if ("其他".equals(type)) {
            enabled = false;
        }
        boolean isList = isList(html, content, url, enabled, linkTitle);
        toolsInfo.setList(isList);
        //如果不是列表页继续执行
        if (!isList) {
            /*关键词和摘要*/
            String[] strings = keyWordParser.getKD4Meta(doc);
            String keyWords = strings[0];
            String summary = strings[1];
            if (keyWords.isEmpty()) {
                keyWords = hanLPAnalyze.extractKeyword(content, 5);
            }
            if (summary.isEmpty()) {
                summary = hanLPAnalyze.extractSummary(content, 7);
            }
            toolsInfo.setKeyWords(keyWords);
            toolsInfo.setSummary(summary);
            Map<String, String> wordMap = hanLPAnalyze.segment(content);
            toolsInfo.setNr(wordMap.get("nr"));
            toolsInfo.setNs(wordMap.get("ns"));
            toolsInfo.setNt(wordMap.get("nt"));
            /*缩略图提取*/
            List<ImgInfo> imgInfoList = thumbnailParser.getAllImgBase64(article, url);
            toolsInfo.setImgInfoList(imgInfoList);
            //原创判断
            String source = MyStringUtils.regexParse(doc.text(), "来源");
            toolsInfo.setSource(source);
            int original = isOriginal(source, url, content);
            toolsInfo.setOriginal(original);
            /*自动格式化*/
            if (isAutoFormat) {
                Map<Integer, Map<String, String>> autoFormatMap = autoFormat.autoFormat(doc.html(), url);
                toolsInfo.setAutoFormatMap(autoFormatMap);
            }
        }
        logger.info("url:[" + url + "].end.");
        return toolsInfo;
    }

    /**
     * 附件解析
     *
     * @param bytes  字节流
     * @param suffix 后缀
     * @return
     */
    public ToolsInfo getToolsAnnex(byte[] bytes, String suffix) {
        List<Attachment> attachments = annexParser.annexParser(bytes, suffix);
        // 提取正文
        StringBuilder content = new StringBuilder();
        //标题
        StringBuilder title = new StringBuilder();
        //时间
        List<Long> dateList = new ArrayList<>();

        for (Attachment attachment : attachments) {
            content.append(attachment.getContent());
            title.append(attachment.getTitle());
            dateList.add(attachment.getLastmodify());
        }
        //降序排序
        dateList.sort(Collections.reverseOrder());

        ToolsInfo toolsInfo = new ToolsInfo();
        //TODO 情感分析
        toolsInfo.setEmotiona(0);
        toolsInfo.setContent(content.toString());
        toolsInfo.setDateTimeList(dateList);
        toolsInfo.setTitle(title.toString());
        toolsInfo.setKeyWords("");
        toolsInfo.setSummary("");
        toolsInfo.setList(false);
        return toolsInfo;
    }

    /**
     * 获取tools常用数据
     *
     * @param url             网页URL
     * @param html            网页源码
     * @param regexpList      正则模版集合
     * @param needShowContent 是否需要showcontent
     * @param isAutoFormat    是否需要自动格式化
     * @return 返回tools 常用数据
     */
    public ToolsInfo getToolsInfoByRegexp(String url, String html, List<RegexpConf> regexpList, boolean needShowContent, boolean isAutoFormat, String linkTitle) {
        logger.info("url:[" + url + "].start.");
        // 避免页面源码清洗后，html标签变化，提取不准，在html清洗前提取正文
        // 提取正文
        String content = "";
        String showContent = "";
        // 时间日期提取
        List<Long> dateTimeList = null;
        // 标题
        String title = "";
        // 其他字段 	模版格式化
        List<Map<String, String>> templateParserMap = null;

        // 正则提取   正文/时间/标题
        if (regexpList != null && regexpList.size() > 0) {
            for (RegexpConf regexp : regexpList) {
                switch (regexp.getTempName().toUpperCase()) {
                    case "DRECONTENT":
                        // 正则提取正文
                        String[] contents = articleParser.getContent4Template(html, url, regexp.getRegex(), 1);
                        content = contents[0];
                        showContent = contents[1];
                        break;
                    case "DREDATE":
                        // 正则提取时间
                        dateTimeList = datetimeParser.parserDateTimeTemplate(html, url, regexp.getRegex(), 1);
                        break;
                    case "DRETITLE":
                        // 正则提取标题
                        title = html2Title.getTitleTemplate(html, url, regexp.getRegex(), 1);
                        break;
                    default:
                }
            }
            // 其他字段 	模版格式化
            templateParserMap = templateParser.regexpExtract(html, url, regexpList);
        }

        // 网页源码清洗标准化
        html = htmlCleaner.standardizingHtml(html, url, false);
        // 正则没有获取到 正文/时间/标题  , 则走普通方式,试用清洗后源码
        if (StringUtils.isEmpty(content)) {
            Element contents = readability.articleContent(html, url);
            content = contents.text();
            showContent = contents.html();
        }
        if (dateTimeList == null || dateTimeList.size() == 0) {
            // 时间日期提取 首先删除网页中链接防止干扰
            dateTimeList = datetimeParser.parserDateTimeAuto(html, url);
        }
        if (StringUtils.isEmpty(title)) {
            // 普通方式  提取标题
            title = html2Title.getTitle(html, url);
        }

        ToolsInfo toolsInfo = pubToolsInfo(url, html, content, showContent, isAutoFormat);
        //TODO 情感分析
        toolsInfo.setEmotiona(0);
        toolsInfo.setTitle(title);
        toolsInfo.setContent(MyStringUtils.filter(content));
        if (needShowContent) {
            toolsInfo.setShowContent(showContent);
        }
        toolsInfo.setDateTimeList(dateTimeList);
        toolsInfo.setTemplateParserMap(templateParserMap);
        toolsInfo.setList(isList(html, content, url, true, linkTitle));
        logger.info("url:[" + url + "].end.");
        return toolsInfo;
    }

    /**
     * 获取tools常用数据
     *
     * @param url             网页URL
     * @param html            网页源码
     * @param xpathList       xpath模版集合
     * @param needShowContent 是否需要showcontent
     * @param isAutoFormat    是否需要自动格式化
     * @return 返回tools 常用数据
     */
    public ToolsInfo getToolsInfByXpath(String url, String html, List<XpathConf> xpathList, boolean needShowContent, boolean isAutoFormat, String linkTitle) {
        logger.info("url:[" + url + "].start.");
        // 避免页面源码清洗后，html标签变化，提取不准，在html清洗前提取正文
        // 提取正文
        String content = "";
        String showContent = "";
        // 时间日期提取
        List<Long> dateTimeList = null;
        // 标题
        String title = "";
        // 其他字段 	模版格式化
        List<Map<String, String>> templateParserMap = null;

        // xpath模版提取   正文/时间/标题
        if (xpathList != null && xpathList.size() > 0) {
            for (XpathConf xpath : xpathList) {
                switch (xpath.getTempName().toUpperCase()) {
                    case "DRECONTENT":
                        // xpath模版提取正文
                        String[] contents = articleParser.getContent4Template(html, url, xpath.getXpath(), 0);
                        content = contents[0];
                        showContent = contents[1];
                        break;
                    case "DREDATE":
                        // xpath模版提取时间
                        dateTimeList = datetimeParser.parserDateTimeTemplate(html, url, xpath.getXpath(), 0);
                        break;
                    case "DRETITLE":
                        // xpath模版提取标题
                        title = html2Title.getTitleTemplate(html, url, xpath.getXpath(), 0);
                        break;
                    default:
                }
            }
            // 其他字段 	模版格式化
            templateParserMap = templateParser.xpathExtract(html, url, xpathList);
        }

        // 网页源码清洗标准化
        html = htmlCleaner.standardizingHtml(html, url, false);
        // 正则没有获取到 正文/时间/标题  , 则走普通方式,试用清洗后源码
        if (StringUtils.isEmpty(content)) {
            Element contents = readability.articleContent(html, url);
            content = contents.text();
            showContent = contents.html();
        }
        if (dateTimeList == null) {
            // 时间日期提取 首先删除网页中链接防止干扰
            dateTimeList = datetimeParser.parserDateTimeAuto(html, url);
        }
        if (StringUtils.isEmpty(title)) {
            // 普通方式  提取标题
            title = html2Title.getTitle(html, url);
        }

        ToolsInfo toolsInfo = pubToolsInfo(url, html, content, showContent, isAutoFormat);
        //TODO 情感分析
        toolsInfo.setEmotiona(0);
        toolsInfo.setTitle(title);
        toolsInfo.setContent(MyStringUtils.filter(content));
        if (needShowContent) {
            toolsInfo.setShowContent(showContent);
        }
        toolsInfo.setDateTimeList(dateTimeList);
        toolsInfo.setTemplateParserMap(templateParserMap);
        toolsInfo.setList(isList(html, content, url, true, linkTitle));
        logger.info("url:[" + url + "].end.");
        return toolsInfo;
    }

    /**
     * 列表页判断(beta)
     *
     * @param html    网页源码
     * @param content 正文
     * @param enabled 启用
     * @return
     */
    private boolean isList(String html, String content, String url, boolean enabled, String linkTitle) {
        //url分割
        String[] urls = url.toLowerCase().replaceFirst("http.*?//", "").split("[/\\\\]");
        //首页判断http://www.gz.gov.cn/  http://www.gz.gov.cn
        if (urls.length <= 1) {
            return true;
        }
        Matcher matcher;
        if (enabled) {
            String lastUrl = urls[urls.length - 1];
            //规则判断url是否列表页
            matcher = URL_PATTERN.matcher(lastUrl);
            boolean bool = matcher.find();
            if (bool) {
                return true;
            }
        }
        //去掉所有空白字符
        linkTitle = linkTitle.replaceAll("\\s*", "");
        //根据链接标题判断
        boolean flag = ToolsConfig.linkTitles.contains(linkTitle);
        if (flag) {
            return true;
        }

        matcher = MORE_PATTERN.matcher(linkTitle);
        boolean bool = matcher.find();
        if (bool) {
            return true;
        }
//        if (!linkTitle.isEmpty()) {
//            return StringUtils.isNumeric(linkTitle);
//        }

        //如果url判断不出 通过正文判断
        /*Document doc = Jsoup.parse(html);
        doc.select("a").remove();
        //正文比例
        double textProportion = (float) content.length() / (float) doc.text().length();
        return textProportion < 0.6;*/
        return false;
    }

    /**
     * 元搜索列表分析分析
     *
     * @param html      源码
     * @param parentUrl 链接
     */
    public List<MetaInfo> getMetaList(String html, String parentUrl, String meta) {
        MetaConf metaConf = JSONObject.parseObject(meta, MetaConf.class);
        try {
            html = StringEscapeUtils.unescapeJava(html);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        List<MetaInfo> metaInfos = new ArrayList<>();
        String root = metaConf.getRoot();
        //正则提取
        Pattern pattern = Pattern.compile(root, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            MetaInfo metaInfo = new MetaInfo();
            //获取子节点数据
            //提取链接
            String url = MyStringUtils.regexMatch(matcher.group(), metaConf.getUrl());
            if (StringUtils.isNotEmpty(url) && !url.startsWith("http")) {
                url = MyHtmlUtils.resolve(parentUrl, url);
            }
            if (StringUtils.isEmpty(url)) {
                continue;
            }
            metaInfo.setUrl(url);
            //提取时间
            String dateTime = MyStringUtils.regexMatch(matcher.group(), metaConf.getDate());
            metaInfo.setDate(MyDateUtil.convergeTime(dateTime));
            //提取来源 作者标题
            metaInfo.setSource(MyStringUtils.filter(MyStringUtils.regexMatch(matcher.group(), metaConf.getSource())));
            //提取正文
            String showContent = MyStringUtils.regexMatch(matcher.group(), metaConf.getContent());
            String content = MyStringUtils.filter(showContent);
            metaInfo.setContent(content);
            metaInfo.setShowContent(showContent);
            //关键词摘要
            String keyWords = hanLPAnalyze.extractKeyword(content, 5);
            metaInfo.setKeywords(keyWords);
            metaInfo.setDescription(content);
            Map<String, String> wordMap = hanLPAnalyze.segment(content);
            metaInfo.setNr(wordMap.get("nr"));
            metaInfo.setNs(wordMap.get("ns"));
            metaInfo.setNt(wordMap.get("nt"));
            //提取标题
            metaInfo.setTitle(MyStringUtils.filter(MyStringUtils.regexMatch(matcher.group(), metaConf.getTitle())));
            //其他链接
            String otherUrl = MyStringUtils.regexMatch(matcher.group(), metaConf.getOtherUrl());
            if (StringUtils.isNotEmpty(otherUrl) && !otherUrl.startsWith("http")) {
                otherUrl = MyHtmlUtils.resolve(parentUrl, otherUrl);
            }
            metaInfo.setOtherUrl(otherUrl);
            //其他配置
            Map<String, String> els = new HashMap<>();
            metaConf.getOthers().forEach((k, v) -> els.put(k, MyStringUtils.filter(MyStringUtils.regexMatch(matcher.group(), v))));
            metaInfo.setOthers(els);
            metaInfos.add(metaInfo);
        }

        //没有搜索结果
        if (metaInfos.size() == 0) {
            logger.error("Url:[" + parentUrl + "].blocked.没有提取到有效信息");
        }
        return metaInfos;
    }

    /**
     * 元搜索详情分析
     *
     * @param html      源码
     * @param parentUrl 链接
     * @return
     */
    public MetaInfo getMetaDetails(String html, String parentUrl) {
        MetaInfo metaInfo = new MetaInfo();
        /*网页清洗*/
        Document doc = htmlCleaner.cleanHtml(html, parentUrl);
        /*提取正文*/
        Element article = readability.articleContent(doc.html(), parentUrl);
        String content = article.text();
        metaInfo.setContent(MyStringUtils.filter(content));
        String showContent = article.html().replaceAll("[\\r\\n\\t]", "");
        metaInfo.setShowContent(showContent);
        /*提取日期时间*/
        String text = doc.text();
        List<Long> dateTimeList = datetimeParser.parserDateTimeAuto(text, parentUrl);
        if (dateTimeList.size() > 0) {
            metaInfo.setDate(dateTimeList.get(0));
        }
        /*标题*/
        String title = html2Title.getTitle(doc);
        metaInfo.setTitle(title);
        /*关键词和摘要*/
        String[] strings = keyWordParser.getKD4Meta(doc);
        String keyWords = strings[0];
        String summary = strings[1];
        if (keyWords.isEmpty()) {
            keyWords = hanLPAnalyze.extractKeyword(content, 5);
        }
        if (summary.isEmpty()) {
            summary = hanLPAnalyze.extractSummary(content, 7);
        }
        Map<String, String> wordMap = hanLPAnalyze.segment(content);
        metaInfo.setNr(wordMap.get("nr"));
        metaInfo.setNs(wordMap.get("ns"));
        metaInfo.setNt(wordMap.get("nt"));
        metaInfo.setKeywords(keyWords);
        metaInfo.setDescription(summary);
        /*缩略图提取*/
        List<ImgInfo> imgInfoList = thumbnailParser.getAllImgBase64(article, parentUrl);
        metaInfo.setImgInfos(imgInfoList);
        return metaInfo;
    }

    /**
     * 百度贴吧详情页分析
     *
     * @param html 网页源码
     * @param url
     * @return
     */
    public MetaInfo getTiebaDetails(String html, String url) {
        MetaInfo metaInfo = new MetaInfo();
        //标题
        String title = MyStringUtils.regexMatch(html, "<title>(.*?)</title>");
        metaInfo.setTitle(title);
        //有效元素获取
        String[] split = url.split("#", 2);
        String regex = "";
        if (split.length == 2) {
            regex = "(<div id=\"post_content_" + split[1] + "\"[\\s\\S]*?)<ul class=\"p_props_tail props_appraise_wrap\"></ul>";
        }
        String div = MyStringUtils.regexMatch(html, regex);
        //正文
        String content = MyStringUtils.filter(MyStringUtils.regexMatch(div, "<div id=\"post_content.*?>([\\s\\S]*?)</div>"));
        metaInfo.setContent(content);
        metaInfo.setShowContent(div);
        //时间
        String dataStr = MyStringUtils.regexMatch(div, "<span class=\"tail-info\">.*?/span><span class=\"tail-info\">(.*?)</span>");
        metaInfo.setDate(MyDateUtil.convergeTime(dataStr));
        //关键词
        String keyWords = hanLPAnalyze.extractKeyword(content, 5);
        String summary = hanLPAnalyze.extractSummary(content, 7);
        Map<String, String> wordMap = hanLPAnalyze.segment(content);
        metaInfo.setNr(wordMap.get("nr"));
        metaInfo.setNs(wordMap.get("ns"));
        metaInfo.setNt(wordMap.get("nt"));
        metaInfo.setKeywords(keyWords);
        metaInfo.setDescription(summary);
        //缩略图
        List<ImgInfo> imgInfos = thumbnailParser.getAllImgBase64(url, div);
        metaInfo.setImgInfos(imgInfos);
        return metaInfo;
    }

    /**
     * @param url          网页URL
     * @param html         网页源码(清洗后)
     * @param content      正文
     * @param isAutoFormat 是否需要自动格式化
     * @return 返回普通处理的数据
     */
    private ToolsInfo pubToolsInfo(String url, String html, String content, String showContent, boolean isAutoFormat) {
        // 提取所有链接和标题	key: 0:域外 1:域内,value: url和标题map-key:url value:标题
        Map<Integer, LinkedHashMap<String, String>> urlAndTitleMap = urlParser.getUrlAndTitleByDomain(url, html);
        //关键词和摘要
        String[] strings = keyWordParser.getKD4Meta(html, url);
        String keyWords = strings[0];
        String summary = strings[1];
        if (keyWords.isEmpty()) {
            keyWords = hanLPAnalyze.extractKeyword(content, 5);
        }
        if (summary.isEmpty()) {
            summary = hanLPAnalyze.extractSummary(content, 7);
        }
        Map<String, String> wordMap = hanLPAnalyze.segment(content);
        // 所有缩略图提取
        List<ImgInfo> imgInfoList = thumbnailParser.getAllImgBase64(url, showContent);

        ToolsInfo toolsInfo = new ToolsInfo();
        toolsInfo.setUrl(url);
        toolsInfo.setHtml(html);
        toolsInfo.setUrlAndTitleMap(urlAndTitleMap);
        toolsInfo.setKeyWords(keyWords);
        toolsInfo.setSummary(summary);
        toolsInfo.setNr(wordMap.get("nr"));
        toolsInfo.setNs(wordMap.get("ns"));
        toolsInfo.setNt(wordMap.get("nt"));
        toolsInfo.setImgInfoList(imgInfoList);
        if (isAutoFormat) {// 自动格式化
            Map<Integer, Map<String, String>> autoFormatMap = autoFormat.autoFormat(html, url);
            toolsInfo.setAutoFormatMap(autoFormatMap);
        }
        return toolsInfo;
    }

    /**
     * 是否原创
     *
     * @param source 来源
     * @param url    url
     * @return
     */
    public int isOriginal(String source, String url, String content) {

        //1.来源网址的domain比较 确定原创 (抽取来源的a标签链接提取domain)
        //2.直接比较网站名称 相似度
        //3.没有来源的确定(原标题 转载自 ...  【时间 作者 作为兄弟节点 提取误差比较大】)

        String domainSite = DomainUtils.getDomainSite(url);
        String siteName = ToolsConfig.domainSiteNameMap.get(domainSite);

        //来源不为空并且相似度小于50% 视为转载
        if (siteName != null && StringUtils.isNotEmpty(source) && SimilarityUtil.getSimilarityRatio(source, siteName) < 0.5) {
            return 0;
        }

        //文章正文包含 原标题 转载自 视为转载
        if (content.contains("原标题") || content.contains("转载自")) {
            return 0;
        }

        return 1;
    }

    /**
     * 知乎元搜索列表
     * //TODO 知乎API获取
     *
     * @param html 网页源码
     * @param url  url
     * @return
     */
    public List<MetaInfo> zhihuMetaList(String html, String url) {
        List<MetaInfo> list = new ArrayList<>();
        Document document = Jsoup.parse(html, url);
        Elements dataEle = document.select("div#data");
        String dataState = dataEle.attr("data-state");
        //解析json
        JSONObject jo = JSONObject.parseObject(dataState);
        JSONObject answers = jo.getJSONObject("entities").getJSONObject("answers");
        answers.forEach((k, v) -> {
            MetaInfo metaInfo = new MetaInfo();
            JSONObject jno = (JSONObject) v;

            String title = jno.getJSONObject("question").getString("name");
            metaInfo.setTitle(title);

            String description = jno.getString("excerpt");
            metaInfo.setDescription(description);

            String showContent = jno.getString("content");
            String content = MyStringUtils.filter(showContent);
            metaInfo.setContent(content);
            metaInfo.setShowContent(showContent);

            long createdTime = jno.getLongValue("createdTime") * 1000;
            metaInfo.setDate(createdTime);

            String uri = "https://www.zhihu.com/question/" + jno.getJSONObject("question").getString("id") + "/answer/" + k;
            metaInfo.setUrl(uri);

            String keyWords = hanLPAnalyze.extractKeyword(content, 5);
            metaInfo.setKeywords(keyWords);

            Map<String, String> wordMap = hanLPAnalyze.segment(content);
            metaInfo.setNr(wordMap.get("nr"));
            metaInfo.setNs(wordMap.get("ns"));
            metaInfo.setNt(wordMap.get("nt"));

            list.add(metaInfo);
        });
        return list;
    }

    /**
     * 知乎详情分析
     *
     * @param html 源码
     * @param url  链接
     * @return
     */
    public MetaInfo zhihuMetaDetails(String html, String url) {
        MetaInfo metaInfo = new MetaInfo();
        List<MetaInfo> list = new ArrayList<>();
        Document document = Jsoup.parse(html, url);
        Elements dataEle = document.select("div#data");
        String dataState = dataEle.attr("data-state");
        //解析json
        JSONObject jo = JSONObject.parseObject(dataState);

        StringBuffer sb = new StringBuffer();
        JSONObject questions = jo.getJSONObject("entities").getJSONObject("questions");
        questions.forEach((k, v) -> {
            JSONObject jno = (JSONObject) v;

            sb.append(jno.getString("detail"));

            String title = jno.getString("title");
            metaInfo.setTitle(title);

            long updatedTime = jno.getLongValue("updatedTime") * 1000;
            metaInfo.setDate(updatedTime);
        });


        JSONObject answers = jo.getJSONObject("entities").getJSONObject("answers");
        answers.forEach((k, v) -> {
            JSONObject jno = (JSONObject) v;

            sb.append(jno.getString("content"));

        });
        String showContent = sb.toString();
        String content = MyStringUtils.filter(showContent);
        metaInfo.setContent(content);
        metaInfo.setShowContent(showContent);

        String keyWords = hanLPAnalyze.extractKeyword(content, 5);
        metaInfo.setKeywords(keyWords);

        String description = hanLPAnalyze.extractSummary(showContent, 7);
        metaInfo.setDescription(description);

        Map<String, String> wordMap = hanLPAnalyze.segment(showContent);
        metaInfo.setNr(wordMap.get("nr"));
        metaInfo.setNs(wordMap.get("ns"));
        metaInfo.setNt(wordMap.get("nt"));
        return metaInfo;
    }
}

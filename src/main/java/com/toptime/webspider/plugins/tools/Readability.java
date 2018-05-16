package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.util.MyHtmlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Readability正文抽取
 * Created by bjoso on 2017/6/21.
 */
public class Readability {

    private static Logger log = LoggerFactory.getLogger(Readability.class);

    private static final String LINE = System.lineSeparator();
    private static final String UNLIKELY_CANDIDATES_RE = "(?iu)combx|modal|comment|disqus|foot|header|menu|meta|nav|rss|shoutbox|sidebar|sponsor|social|teaserlist|time|tweet|twitter";
    private static final String OK_MAYBE_ITSA_CANDIDATE_RE = "(?ium)and|article|body|column|main|story|entry|^post";
    private static final String POSITIVE_RE = "(?iu)article|body|content|entry|hentry|page|pagination|post|section|chapter|description|main|blog|text";
    private static final String NEGATIVE_RE = "(?iu)combx|comment|contact|foot|footer|footnote|link|media|meta|promo|related|scroll|shoutbox|sponsor|utility|tags|widget";
    private static final String DIV_TOP_ELEMENTS_RE = "(?iu)<(a|blockquote|dl|div|img|ol|p|pre|table|ul)";
    private static final String NORMALIZE_RE = "\\s{2,}";
    private static final String KILL_BREAKS_RE = "(<br\\s*/?>(\\s|&nbsp;?)*){1,}";
    private static final String VIDEO_RE = "(?iu)http:\\/\\/(www\\.)?(youtube|vimeo|youku|tudou|56|yinyuetai)\\.com";
    private static final String ATTRIBUTE_RE = "blog|post|article";


    /**
     * 获取文章可视正文
     */
    public Element articleContent(String html, String url) {
        /*
         * 源码准备 适当清洗
         */
        html = html.replaceAll("<\\s*!\\s*--[\\s\\S]*?--\\s*>", "");
        Document document = Jsoup.parse(html, url);
        document.select("script").remove();
        document.select("option").remove();
        MyHtmlUtils.absoluteAddress(document, url);

        //克隆一份以备后期使用
        Document cache = document.clone();
        Element articleContent = grabArticle(document, false);
        if ("".equals(getInnerText(articleContent, false))) {
            if (cache.body() == null) {
                log.error("No body tag was found.");
                return new Element("div");
            }
            document.body().html(cache.body().html());
            articleContent = grabArticle(document, true);
        }
        return articleContent;
    }

    /**
     * 使用各种指标（内容分数，类名，元素类型），找到内容
     * 最有可能是用户想要阅读的内容。 然后把它包裹起来。
     */
    private Element grabArticle(Document document, boolean preserveUnlikelyCandidates) {

        /*
         * 节点准备,看起来很烂的垃圾节点（如类名“comment”等）,并转换div
         * 为P标签（如在其中不包含其他块级元素的位置）。
         */
        Elements nodes = document.getAllElements();
        for (Element node : nodes) {

            //删除不太可能的候选
            boolean continueFlag = false;
            if (!preserveUnlikelyCandidates) {
                String unlikelyMatchString = node.className() + LINE + node.id();
                if (matcher(unlikelyMatchString, UNLIKELY_CANDIDATES_RE) && !matcher(unlikelyMatchString, OK_MAYBE_ITSA_CANDIDATE_RE) && !"html".equals(node.tagName()) && !"body".equals(node.tagName())) {
                    log.debug("Removing unlikely candidate - " + unlikelyMatchString);
                    node.remove();
                    continueFlag = true;
                }
            }
            // 将没有子节点的所有div转换为p
            if (!continueFlag && "div".equals(node.tagName())) {
                if (!matcher(node.html(), DIV_TOP_ELEMENTS_RE)) {
                    log.debug("Altering div to p");
                    Element newNode = document.createElement("p");
                    newNode.html(node.html());
                    node.replaceWith(newNode);
                } else {
                    // 实验功能
                    node.textNodes().forEach(childNode -> {
                        Node nextNode = childNode.nextSibling();
                        if (nextNode != null && "br".equals(nextNode.nodeName())) {
                            log.debug("replacing text node followed by br with a p tag with the same content.");
                            Element p = document.createElement("p");
                            p.html(childNode.text());
                            nextNode.remove();
                            childNode.replaceWith(p);
                        } else {
                            // 使用span而不是p。 需要更多的测试。
                            log.debug("replacing text node with a span tag with the same content.");
                            Element span = document.createElement("span");
                            span.html(childNode.text());
                            childNode.replaceWith(span);
                        }
                    });
                }
            }
        }

        /*
         * 循环遍历所有段落，并根据内容分配一个分数。
         * 然后将他们的分数添加到其父节点。
         * 得分由逗号，类名等数字决定。或者最终链接密度。
         */
        Elements allParagraphs = document.select("p,pre");
        //临时Map稍后回收
        Map<Element, Double> candidates = new HashMap<>(100);
        allParagraphs.forEach(paragraph -> {
            Element parentNode = paragraph.parent();
            Element grandParentNode = parentNode.parent();
            String innerText = getInnerText(paragraph, true);

            // 如果此段落小于25个字符，跳过。
            if (innerText.length() < 25) {
                return;
            }

            if (!candidates.containsKey(parentNode)) {
                // 初始化父级的可读性数据.
                double parentNodeModule = initializeNode(parentNode);
                candidates.put(parentNode, parentNodeModule);
            }

            if (!candidates.containsKey(grandParentNode)) {
                // 初始化祖父级的可读性数据.
                double grandParentNodeModule = initializeNode(grandParentNode);
                candidates.put(grandParentNode, grandParentNodeModule);
            }

            int score = 0;

            // 为段落本身添加一个点作为基础
            ++score;

            // 为本段内的任何逗号添加点
            score += innerText.replace("，", ",").split(",").length;

            // 对于本段中每100个字符，添加另一个点。 最多3分。
            score += Math.min(Math.floor(innerText.length() / 100), 3);

            // 将得分添加到父级。 祖父级一半。
            candidates.put(parentNode, candidates.get(parentNode) + score);
            candidates.put(grandParentNode, candidates.get(grandParentNode) + (score / 2));
        });

        /*
         * 计算分数后，循环遍历发现的所有可能的候选节点
         * 找到最高分。
         */
        Element topElement = null;
        double max = 0.0;

        for (Element element : candidates.keySet()) {
            /*
             * 根据链路密度来缩放最终的候选人得分。 符合的内容
             * 链接密度应该相对较小（5％以下），大部分不受此操作的影响。
             */
            double score = candidates.get(element);
            score = score * (1 - getLinkDensity(element));

            log.debug("Candidate: " + " (" + element.className() + ":" + element.id() + ") with score " + score);

            if (topElement == null || score > max) {
                topElement = element;
                max = score;
            }
        }
        /*
         * 如果还没有最优的候选，只要用body作为最后的手段。
         * 还必须复制body节点，这是可以修改的
         */
        if (topElement == null || "body".equals(topElement.tagName())) {
            // 如果没有顶级候选 也没有body 抛异常
            if (document.body() == null) {
                log.error("No body tag was found.");
                //无body抛异常
//                throw new Exception("No body tag was found.");
                return new Element("div");
            }

            topElement = document.createElement("div");
            topElement.html(document.body().html());
            document.body().html("");
            document.body().appendChild(topElement);
            max = initializeNode(topElement);
        }

        /*
         * 现在有最优选了，其兄弟节点的内容可能也是相关联的。
         */
        Element articleContent = document.createElement("div");
        articleContent.attr("id", "readability-content");
        double siblingScoreThreshold = Math.max(10, max * 0.2);
        Elements siblingNodes = topElement.parent().children();
        //获取兄弟节点集合
        Map<Element, Double> siblingNodeMaps = new HashMap<>(100);
        siblingNodes.forEach(element -> siblingNodeMaps.put(element, candidates.getOrDefault(element, null)));

        //遍历兄弟集合
        for (Element siblingNode : siblingNodeMaps.keySet()) {
            boolean append = false;
            Double score = siblingNodeMaps.get(siblingNode);
            log.debug("Looking at sibling node: " + " (" + siblingNode.className() + ":" + siblingNode.id() + ')' + ((score != null) ? (" with score " + score) : " "));
            log.debug("Sibling has score " + (score != null ? score : "Unknown"));

            if (siblingNode == topElement) {
                append = true;
            }

            if (score != null && score >= siblingScoreThreshold) {
                append = true;
            }

            if ("p".equals(siblingNode.tagName())) {
                double linkDensity = getLinkDensity(siblingNode);
                String nodeContent = getInnerText(siblingNode, true);
                int nodeLength = nodeContent.length();

                if (nodeLength > 80 && linkDensity < 0.25) {
                    append = true;
                } else if (nodeLength < 80 && linkDensity == 0 && matcher(nodeContent, "\\.( |$)")) {
                    append = true;
                }
            }
            if (append) {
                log.debug("Appending node: (" + siblingNode.id() + ":" + siblingNode.className() + ")");
                articleContent.appendChild(siblingNode);
            }
        }

        /*已经获取到最高的节点开始清洗元素*/
        prepArticle(articleContent);
        return articleContent;
    }

    /**
     * 字符串正则匹配
     */
    private boolean matcher(String s, String regex) {
        return Pattern.compile(regex).matcher(s).find();
    }

    /**
     * 使用可读性对象初始化一个节点。 还检查
     * className / id用于添加到其分数的特殊名称.
     */
    private double initializeNode(Element element) {
        double score = 0;
        switch (element.tagName()) {
            case "article":
                score += 10;
                break;

            case "section":
                score += 8;
                break;

            case "div":
                score += 5;
                break;

            case "pre":
            case "td":
            case "blockquote":
                score += 3;
                break;

            case "address":
            case "ol":
            case "ul":
            case "dl":
            case "dd":
            case "dt":
            case "li":
            case "form":
                score -= 3;
                break;

            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
            case "th":
                score -= 5;
                break;
            default:
        }

        if (element.hasAttr("itemscope")) {
            score += 5;
            if (element.hasAttr("itemtype")
                    && element.attr("itemtype").toLowerCase().matches(ATTRIBUTE_RE)) {
                score += 30;
            }
        }
        score += getClassWeight(element);
        return score;
    }

    /**
     * 获取元素calss/id权重,使用正则表达式来判断
     */
    private int getClassWeight(Element element) {
        int weight = 0;
        //寻找特殊的类名
        if (!element.className().isEmpty()) {
            if (matcher(element.className(), NEGATIVE_RE)) {
                weight -= 25;
            }
            if (matcher(element.className(), POSITIVE_RE)) {
                weight += 25;
            }
        }
        //寻找特殊的ID
        if (!element.id().isEmpty()) {
            if (matcher(element.className(), NEGATIVE_RE)) {
                weight -= 25;
            }
            if (matcher(element.className(), POSITIVE_RE)) {
                weight += 25;
            }
        }
        return weight;
    }

    /**
     * 获取a链接的密度占内容的百分比
     * 链接内的文本量除以节点中的总文本数量。
     */
    private double getLinkDensity(Element element) {
        Elements links = element.select("a");
        double textLength = getInnerText(element, true).length();
        textLength = textLength == 0 ? 1 : textLength;
        double linkLength = 0;
        for (Element link : links) {
            String href = link.attr("href");
            //<h2><a href="#menu"></a></h2> / <h2><a></a></h2>
            if (href.isEmpty() || (href.startsWith("#"))) {
                continue;
            }
            linkLength += getInnerText(link, true).length();
        }
        return linkLength / textLength;
    }

    /**
     * 清理提取后的正文节点
     */
    private void prepArticle(Element articleContent) {
//        cleanStyles(articleContent);
        cleanAttributes(articleContent);
        killBreaks(articleContent);

        /*从文章内容清除垃圾*/
        clean(articleContent, "form");
        clean(articleContent, "object");
        if (articleContent.getElementsByTag("h1").size() == 1) {
            clean(articleContent, "h1");
        }
        /*
         * 如果只有一个h2，他们可能正在使用它
         * 作为标题而不是子标题，所以删除它，因为我们已经有一个标题。
         */
        if (articleContent.getElementsByTag("h2").size() == 1) {
            clean(articleContent, "h2");
        }

        clean(articleContent, "iframe");
        //TODO 待测(可能导致正文提取不到,不清理会导致冗余】)
//        clean(articleContent, "span");

        cleanHeaders(articleContent);

        /*最后删除可能影响的垃圾*/
        cleanConditionally(articleContent, "table");
        cleanConditionally(articleContent, "ul");
        cleanConditionally(articleContent, "div");

        /*删除额外的段落*/
        Elements articleParagraphs = articleContent.getElementsByTag("p");
        for (int i = articleParagraphs.size() - 1; i >= 0; i--) {
            int imgCount = articleParagraphs.get(i).getElementsByTag("img").size();
            int embedCount = articleParagraphs.get(i).getElementsByTag("embed").size();
            int objectCount = articleParagraphs.get(i).getElementsByTag("object").size();

            if (imgCount == 0 && embedCount == 0 && objectCount == 0 && "".equals(getInnerText(articleParagraphs.get(i), false))) {
                articleParagraphs.get(i).remove();
            }
        }

        cleanSingleHeader(articleContent);

        articleContent.html(articleContent.html().replaceAll("<br[^>]*>\\s*<p", "<p"));

        //TODO 是否删除所有a标签
//        articleContent.select("a").remove();
    }

    /**
     * 删除样式属性。
     *
     * @param e
     **/
    private void cleanStyles(Element e) {
        if (e == null) {
            return;
        }
        // 删除根节点style
        if (!"readability-styled".equals(e.className())) {
            e.removeAttr("style");
            e.removeAttr("class");
        }
        // 直到没有更多的子节点
        Element cur = e.children().first();
        while (cur != null) {
            // 删除样式属性
            if (!"readability-styled".equals(cur.className())) {
                cur.removeAttr("style");
                cur.removeAttr("class");
            }
            cleanStyles(cur);
            cur = cur.nextElementSibling();
        }
    }

    /**
     * 删除元素属性。
     **/
    protected void cleanAttributes(Element e) {
        if (e == null) {
            return;
        }
        // 直到没有更多的子节点
        Element cur = e.children().first();
        while (cur != null) {
            for (Attribute attribute : cur.attributes().asList()) {
                String key = attribute.getKey();
                if (!"href".equals(key) && !"src".equals(key) && !"width".equals(key) && !"height".equals(key)) {
                    cur.removeAttr(key);
                }
            }
            cleanAttributes(cur);
            cur = cur.nextElementSibling();
        }
    }

    /**
     * 从节点中删除无关的断点标记。
     */
    private void killBreaks(Element e) {
        e.html(e.html().replaceAll(KILL_BREAKS_RE, "<br />"));
    }

    /**
     * 清理“tag”类型的所有元素的节点。
     **/
    private void clean(Element e, String tag) {
        Elements targetList = e.getElementsByTag(tag);
        boolean isEmbed = ("object".equals(tag) || "embed".equals(tag));
        targetList.forEach(target -> {
            if (isEmbed) {
                if (matcher(target.html(), VIDEO_RE)) {
                    return;
                }
            }
            target.remove();
        });
    }

    /**
     * 清除元素中的伪标题。 检查类名和链接密度。
     *
     * @param e
     */
    private void cleanHeaders(Element e) {
        for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
            Elements headers = e.getElementsByTag("h" + headerIndex);
            for (int i = headers.size() - 1; i >= 0; --i) {
                if (getClassWeight(headers.get(i)) < 0 || getLinkDensity(headers.get(i)) > 0.33) {
                    headers.get(i).remove();
                }
            }
        }
    }

    /**
     * 清理“tag”类型的所有标签的元素，如果它们看起来Fishy。
     * “Fishy”是基于内容长度，类名，链接密度，图像数量和嵌入等的算法。
     */
    private void cleanConditionally(Element e, String tag) {
        Elements tagsList = e.select(tag);
        int curTagsLength = tagsList.size();

        /*
         * 收集嵌入其他典型元素的数据。
         * 向后移动，可以同时删除节点，而不影响遍历。
         */
        for (int i = curTagsLength - 1; i >= 0; i--) {
            Element element = tagsList.get(i);
            int weight = getClassWeight(element);
            if (weight < 0) {
                element.remove();
            } else if (getCharCount(element, ",") < 10) {
                /*
                 * 如果不是很多的逗号和数量
                 * 非段落元素超过段落或其他不祥迹象，删除元素。
                 */
                int p = element.getElementsByTag("p").size();
                int img = element.getElementsByTag("img").size();
                int li = element.getElementsByTag("li").size() - 100;
                int input = element.getElementsByTag("input").size();

                int embedCount = 0;
                Elements embeds = element.getElementsByTag("embed");
                for (Element embed : embeds) {
                    if (embed.hasAttr("src") && !matcher(embed.attr("src"), VIDEO_RE)) {
                        embedCount++;
                    }
                }
                double linkDensity = getLinkDensity(element);
                int contentLength = getInnerText(element, true).length();
                boolean toRemove = false;

                if (img > p && img > 1) {
                    toRemove = true;
                } else if (li > p && !"ul".equals(tag) && !"ol".equals(tag)) {
                    toRemove = true;
                } else if (input > Math.floor(p / 3)) {
                    toRemove = true;
                } else if (contentLength < 25 && (img == 0 || img > 2)) {
                    toRemove = true;
                } else if (weight < 25 && linkDensity > .2) {
                    toRemove = true;
                } else if (weight >= 25 && linkDensity > .5) {
                    toRemove = true;
                } else if ((embedCount == 1 && contentLength < 75) || embedCount > 1) {
                    toRemove = true;
                }

                if (toRemove && element.parent() != null) {
                    element.remove();
                }
            }
        }
    }

    /**
     * 获取字符串出现在节点e中的次数。
     */
    private int getCharCount(Element e, String s) {
        if (s == null) {
            s = "[,，]";
        }
        return getInnerText(e, true).split(s).length;
    }

    /**
     * 获取节点的内部文本 - 跨浏览器兼容性。
     * 此外，还可以删除任何多余的空白空间。
     */
    private String getInnerText(Element e, boolean bol) {
        if (bol) {
            return e.text().trim().replaceAll(NORMALIZE_RE, " ");
        } else {
            return e.text().trim();
        }
    }

    /**
     * 删除没有下一个兄弟的标题.
     */
    private static void cleanSingleHeader(Element e) {
        for (int headerIndex = 1; headerIndex < 7; headerIndex++) {
            Elements headers = e.getElementsByTag("h" + headerIndex);
            for (int i = headers.size() - 1; i >= 0; --i) {
                if (headers.get(i).nextElementSibling() == null) {
                    headers.get(i).remove();
                }
            }
        }
    }
}

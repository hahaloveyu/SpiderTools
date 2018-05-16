package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.config.ToolsConfig;
import com.toptime.webspider.util.MyHtmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 自动化格式化服务
 * Created by bjoso on 2017/6/15.
 */
public class AutoFormat {

    private static Logger logger = LoggerFactory.getLogger(AutoFormat.class);

    /**
     * 自动化格式化
     *
     * @param pageSource 网页源码
     * @param url        网页链接
     * @return 类型, 字段&内容 0:政策 1:办事 -1:不确定
     */
    public Map<Integer, Map<String, String>> autoFormat(String pageSource, String url) {
        Map<Integer, Map<String, String>> integerMap = new HashMap<>();
        Map<String, String> businessMap;
        try {
            businessMap = getFieldMapping(pageSource, ToolsConfig.business);
        } catch (Exception e) {
            businessMap = new LinkedHashMap<>();
            logger.error(e.getMessage());
        }
        Map<String, String> policyMap;
        try {
            policyMap = getFieldMapping(pageSource, ToolsConfig.policys);
        } catch (Exception e) {
            policyMap = new LinkedHashMap<>();
            //发送消息
            logger.error(e.getMessage());
        }
        //判断格式化类型
        if (businessMap.size() >= 5 && businessMap.size() >= policyMap.size()) {
            integerMap.put(1, businessMap);
        } else if (policyMap.size() >= 3 && policyMap.size() > businessMap.size()) {
            integerMap.put(0, businessMap);
        } else {
            integerMap.put(-1, businessMap);
        }
        return integerMap;
    }

    /**
     * 获取字段map
     *
     * @param pageSource 网页源码
     * @param strings    配置
     * @return 字段关系映射
     */
    private Map<String, String> getFieldMapping(String pageSource, Set<String> strings) {
        //删除不需要标签
        pageSource = MyHtmlUtils.deleteTab(pageSource);

        //获取到字段内容
        return parser(pageSource, strings);
    }

    /**
     * 处理字段
     *
     * @param html     网页源码
     * @param fieldSet 所有字段的集合
     * @return
     */
    private Map<String, String> parser(String html, Set<String> fieldSet) {
        Map<String, String> map = new LinkedHashMap<>();
        //String content = "";

        //对网页源码中出现的字段位置进行排序
        Map<Integer, String> ziduanMap = parserText(html, fieldSet);

        //字段位置的集合
        List<Integer> weizhiList = new ArrayList<Integer>();
        for (Map.Entry<Integer, String> entry : ziduanMap.entrySet()) {
            //把各个字段的位置放到集合里面
            weizhiList.add(entry.getKey());
        }

        for (int i = 0; i < weizhiList.size(); i++) {

            //所有的字段(除了最后一个)
            if (i < weizhiList.size() - 1) {
                //当前词位置
                int cur = weizhiList.get(i);
                //当前词
                String curStr = ziduanMap.get(cur);
                //下一个词位置
                int next = weizhiList.get(i + 1);
                //下一个词
                String nextStr = ziduanMap.get(next);

                //两个词之间的内容
                String between = html.substring(cur, next).replaceAll("<a[\\s\\S]*?>", "").replaceFirst("[：|:]", "");
                //这种类型的字段：>[主题分类
//		    	if(curStr.contains("[")){
//		    		curStr = curStr.replace(">", ">\\\\");
//		    	}

                Pattern p = Pattern.compile(curStr + "([\\s\\S]*)");
                Matcher m = p.matcher(between);
                if (m.find()) {
                    between = MyHtmlUtils.deleteBetween(m.group(1));
                    if (between.contains("&mdash;")) {
                        between = between.replaceAll("&mdash;", "—");
                    }
                    //content += "字段是："+curStr + "--->内容是："+ between + "@@@@@";
                    map.put(curStr.replace(">", ""), between);
                }

            } else {
                //最后一个词的位置
                int lastIndex = weizhiList.get(weizhiList.size() - 1);
                //最后一个词
                String lastStr = ziduanMap.get(lastIndex);
                //最后一个词到文章结尾的内容,把冒号过滤掉
                String lastHtml = html.substring(lastIndex).replaceFirst("[：|:]", "");

                //如果匹配到了以</标签
                String endTag = "";
                int endTagIndex = 0;
                //最后一个字段到结束标签之间的内容
                String between = "";
                Pattern p = Pattern.compile("(</)+");
                Matcher m = p.matcher(lastHtml);

                for (int j = 0; j < 3; j++) {
                    if (m.find()) {
                        //第一个结束标签
                        if (m.group(1) != null) {
                            //获取到这一个标签
                            endTag = m.group(1);
                        }
                        //匹配最后一个字段到结束标签之间的内容
                        Pattern betPat = Pattern.compile(lastStr + "([\\s\\S]*?)" + endTag);
                        Matcher betMat = betPat.matcher(lastHtml);
                        if (betMat.find()) {
                            between = betMat.group(1);
                            between = MyHtmlUtils.deleteBetween(between);
                            //>主题词</strong></td>     <tdcolspan=
                            //如果内容为空，则把</strong>删掉
                            if (between.equals("")) {
                                Pattern pat = Pattern.compile("(<[\\s\\S]*?>)");
                                Matcher mat = pat.matcher(lastHtml);
                                if (mat.find()) {
                                    lastHtml = lastHtml.replace(mat.group(1), "");
                                    if (j == 2) {
                                        //System.out.println("最后一个字段是：空!!!!!!!!!" );
                                        //content += "最后一个字段是：" + lastStr + "--->内容是：空!!!!!!";
                                    }
                                } else {
                                    System.out.println("没有找到</>的内容");
                                }
                            } else {
                                if (between.contains("&mdash;")) {
                                    between = between.replaceAll("&mdash;", "—");
                                }
                                //content += "最后一个字段是：" + lastStr + "--->内容是：" + between;
                                map.put(lastStr.replace(">", ""), between);
                                break;
                            }
                        }

                    }
                }

            }

        }
        return map;
    }

    /**
     * 分析字段
     *
     * @param text
     * @param fieldSet
     * @return
     */
    private Map<Integer, String> parserText(String text, Set<String> fieldSet) {
        //还没有排序
        Map<String, Integer> sourceFieldsMap = new HashMap<String, Integer>();
        //引入排序比较器
        ValueComparator valueComparator = new ValueComparator(sourceFieldsMap);
        //临时排序 //要排序的 map
        Map<String, Integer> tempSortedFieldsMap = new TreeMap<String, Integer>(valueComparator);

        for (String exp : fieldSet) {
            //记录位置
            int pt = text.indexOf(exp);
            if (pt > -1) {
                //记录字段位置
                sourceFieldsMap.put(exp, pt);
            }
        }

        tempSortedFieldsMap.putAll(sourceFieldsMap);

        //将来要输出的排序 map
        Map<Integer, String> sortedFielsMap = new LinkedHashMap<Integer, String>();

        for (String s : tempSortedFieldsMap.keySet()) {
            //注意这个 sourceFieldsMap
            sortedFielsMap.put(sourceFieldsMap.get(s), s);
        }

        return sortedFielsMap;
    }


    class ValueComparator implements Comparator<String> {

        Map<String, Integer> base;

        ValueComparator(Map<String, Integer> base) {
            this.base = base;
        }

        @Override
        public int compare(String a, String b) {
            if (base.get(a) >= base.get(b)) {
                // 从小到大  -1 从大到小
                return 1;
            } else {
                return -1;
            } // returning 0 would merge keys
        }
    }

}

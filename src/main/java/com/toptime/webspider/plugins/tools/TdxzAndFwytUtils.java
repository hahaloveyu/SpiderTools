package com.toptime.webspider.plugins.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 土地性质和房屋用途，字段提取工具类
 *
 * @author YangBin
 * @date 2018/5/3
 */
public class TdxzAndFwytUtils {
    private static Logger logger = LoggerFactory.getLogger(TdxzAndFwytUtils.class);

    /**
     * 土地性质字典
     */
    private static final Map<String, String> tdxzDic = new LinkedHashMap<>();

    /**
     * 房屋用途字典
     */
    private static final Map<String, String> fwytDic = new LinkedHashMap<>();

    static {
        // 土地性质字典初始化
        tdxzDic.put("划拨", "划拨");
        tdxzDic.put("国有出让", "出让");
        tdxzDic.put("出让", "出让");
        tdxzDic.put("国家出资入股", "国家出资入股");
        tdxzDic.put("国有", "国有");

        // 房屋用途字典初始化
        fwytDic.put("居住", "居住");
        fwytDic.put("住宅", "居住");
        fwytDic.put("商业", "商业");
        fwytDic.put("办公", "办公");
        fwytDic.put("工业", "工业");
        fwytDic.put("综合", "综合");
        // fwytDic.put("其他", "其他");
    }

    /**
     * 提取土地性质字段值
     *
     * @param text 待处理文本
     * @return 土地性质字段值
     */
    public static String fetchTdxz(String text) {
        String tdxz = "其他";
        if (!isEmpty(text)) {
            try {
                List<String> list = new ArrayList<>();
                for (Map.Entry<String, String> entry : tdxzDic.entrySet()) {
                    if (entry != null && text.contains(entry.getKey())) {
                        list.add(entry.getValue());
                        // 替换掉原始文本中的key值，避免重复计算
                        text = text.replace(entry.getKey(), " ");
                    }
                }

                if (list.size() > 0) {
                    tdxz = list.stream()
                            .filter(s -> !isEmpty(s))
                            .distinct()
                            .collect(Collectors.joining(",", "", ""));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return tdxz;
    }

    /**
     * 提取房屋用途字段值
     *
     * @param text 待处理文本
     * @return 房屋用途字段值
     */
    public static String fetchFwyt(String text) {
        String fwyt = "其他";
        if (!isEmpty(text)) {
            try {
                List<String> list = new ArrayList<>();
                for (Map.Entry<String, String> entry : fwytDic.entrySet()) {
                    if (entry != null && text.contains(entry.getKey())) {
                        list.add(entry.getValue());
                        // 替换掉原始文本中的key值，避免重复计算
                        text = text.replace(entry.getKey(), " ");
                    }
                }
                if (list.size() > 0) {
                    fwyt = list.stream()
                            .filter(s -> !isEmpty(s))
                            .distinct()
                            .collect(Collectors.joining(",", "", ""));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return fwyt;
    }

    /**
     * 判断字符串是否为空<br />
     * 已处理字符串为空格情况
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {

        if (str == null) {
            return true;
        }
        if (str.trim().equals("")) {
            return true;
        }
        return false;

    }
}

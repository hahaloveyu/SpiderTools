package com.toptime.webspider.plugins.tools;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hanlp分析应用
 *
 * @author bjoso
 * @date 2017/11/23
 */
public class HanLPAnalyze {

    /**
     * 分词器
     */
    private Segment segment = HanLP.newSegment().enableAllNamedEntityRecognize(true);

    /**
     * 分词
     *
     * @param text 文本
     * @return 词汇库
     */
    public Map<String, String> segment(String text) {
        Map<String, String> wordMap = new HashMap<>(3);

        List<Term> termList = segment.seg(text);
        //人名(人名 日语人名 音译人名 复姓 蒙古姓名)
        String nr = termList.stream().filter(term -> Nature.nr.equals(term.nature) || Nature.nrj.equals(term.nature) || Nature.nrf.equals(term.nature) || Nature.nr1.equals(term.nature) || Nature.nr2.equals(term.nature)).map(term -> term.word.trim()).distinct().collect(Collectors.joining("@@@@@"));
        //地名
        String ns = termList.stream().filter(term -> Nature.ns.equals(term.nature)).map(term -> term.word.trim()).distinct().collect(Collectors.joining("@@@@@"));
        //机构团体名(公司名 工厂 银行 酒店宾馆 政府机构 大学 中小学 医院)
        String nt = termList.stream().filter(term -> Nature.nt.equals(term.nature) || Nature.ntc.equals(term.nature) || Nature.ntcf.equals(term.nature) || Nature.ntcb.equals(term.nature) || Nature.ntch.equals(term.nature) || Nature.nto.equals(term.nature) || Nature.ntu.equals(term.nature) || Nature.nts.equals(term.nature) || Nature.nth.equals(term.nature)).map(term -> term.word.trim()).distinct().collect(Collectors.joining("@@@@@"));

        wordMap.put("nr", nr);
        wordMap.put("ns", ns);
        wordMap.put("nt", nt);
        return wordMap;
    }

    /**
     * 提取关键词
     *
     * @param text 文本
     * @param size 数量
     * @return 关键词
     */
    public String extractKeyword(String text, int size) {
        return HanLP.extractKeyword(text, size).stream().collect(Collectors.joining(","));
    }

    /**
     * 提取摘要
     *
     * @param text 文本
     * @param size 数量
     * @return 摘要
     */
    public String extractSummary(String text, int size) {
        return HanLP.extractSummary(text, size).stream().collect(Collectors.joining(","));
    }
}

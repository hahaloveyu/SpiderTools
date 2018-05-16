package com.toptime.webspider.plugins.tools;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

  
  
public class WhUtil{ 
	public static Map<String, String> WhOut(String s){
		if (s.trim().replaceAll(
				"([\\u4e00-\\u9fa5\\s]*\\d+\\s+\\d+[^\\d]*)"
				, "").equals("")) {
			s=s.trim().replaceAll(" ", "\'");
		}
		
		
		//广东省人力资源、社会保障厅广东省财政厅广东省地方税务局 粤人社函[2016]2861号
		
		s=s.replaceAll("　*| *", " ");
		if (s.replaceAll("[\\s\\S]+?\\s([\\u4e00-\\u9fa5]{2,6}\\W?\\d+\\W?[\\u4e00-\\u9fa5]*\\W?\\d+\\W?.*?)", "").equals("")
				
				) {
			System.err.println("过滤发布单位");
			Pattern hps = Pattern.compile(".*?([\\u4e00-\\u9fa5]+\\W?\\d+\\W?[\\u4e00-\\u9fa5]*\\W?\\d+\\W?.*?)",2);
			Matcher mts = hps.matcher(s);
			mts.find();
			s=mts.group(1);
		}
		
		//　　政密字第714号令公布
		s=s.replaceAll(" ", "");
		//〔89〕粤税三字第062号
		if (!s.contains("届")) {
			
			s=s.replaceAll("[一|１]", "1").replaceAll("[六|６]", "6")
			.replaceAll("[二|２]", "2").replaceAll("[七|７]", "7")
			.replaceAll("[三|３]", "3").replaceAll("[八|８]", "8")
			.replaceAll("[四|４]", "4").replaceAll("[九|９]", "9")
			.replaceAll("[五|５]", "5").replaceAll("[零|０]", "0")
			.replaceAll("[十|百|千|万]", "");
		}
		if (!s.contains("\\d字")) {
			String[]strs=s.split("字");
			if (strs.length==2) {
				String str=strs[0].substring(strs[0].length()-1,strs[0].length());
				str=str.replaceAll("1", "一").replaceAll("6", "六")
						.replaceAll("2", "二").replaceAll("7", "七")
						.replaceAll("3", "三").replaceAll("8", "八")
						.replaceAll("4", "四").replaceAll("9", "九")
						.replaceAll("5", "五").replaceAll("0", "零");
				s=strs[0].substring(0,strs[0].length()-1)+str+"字"+strs[1];
			}
			
		}

		
		Map<String, String> whmap=new HashMap<String, String>();
		if (s==null) {
			whmap=null;
			return whmap;
		}
		System.out.println(s);
		
		//第11期 总第62期
		if (s.trim().replaceAll(
				"([\\u4e00-\\u9fa5]+\\d+[号|次|期][\\u4e00-\\u9fa5\\s]+\\d+[号|次|期][\\u4e00-\\u9fa5]*)"
				, "").equals("")) {
			System.err.println(7);
			Pattern jtwhp = Pattern.compile("[\\u4e00-\\u9fa5]+(\\d+)[\\u4e00-\\u9fa5\\s]+\\d+[\\u4e00-\\u9fa5]+",2);
			Matcher jtwhm = jtwhp.matcher(s);
			whmap.put("docwz", "");
			whmap.put("docnf", "");
			if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}else	
		
		//财税外字第230号
		if (s.trim().replaceAll(
		"([\\u4e00-\\u9fa5]+\\W?\\d+[号|次|期][^\\d]*)"
		, "").equals("")) {
			System.err.println(1);
			Pattern wzp = Pattern.compile("([\\u4e00-\\u9fa5]+)\\W?\\d+\\W?.*",2);
			Pattern jtwhp = Pattern.compile("[\\u4e00-\\u9fa5]+\\W?(\\d+)\\W?.*",2);
			Matcher wzm = wzp.matcher(s);
			Matcher jtwhm = jtwhp.matcher(s);
	        if (wzm.find()) {whmap.put("docwz", wzm.group(1).replaceAll("第", ""));}
	        				 whmap.put("docnf", "");
	        if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}else
		//国函〔1988〕74号
		if (s.trim().replaceAll(
			"([\\u4e00-\\u9fa5]+\\W?\\d+\\W[\\u4e00-\\u9fa5]*\\W?\\d+\\W?.*)"
			, "").equals("")) {
			System.err.println(2);
			Pattern wzp = Pattern.compile("([\\u4e00-\\u9fa5]+?[告|令|知|字|函|办]?)\\W?\\d+\\W?[\\u4e00-\\u9fa5]*\\W?\\d+\\W?[\\u4e00-\\u9fa5]*",2);
			Pattern nfp = Pattern.compile("[\\u4e00-\\u9fa5]+\\W?(\\d+)\\W?[\\u4e00-\\u9fa5]*\\W?\\d+\\W?[\\u4e00-\\u9fa5]*",2);
			Pattern jtwhp = Pattern.compile("[\\u4e00-\\u9fa5]+\\W?\\d+\\W?[\\u4e00-\\u9fa5]*\\W?(\\d+)\\W?[\\u4e00-\\u9fa5]*",2);
			Matcher wzm = wzp.matcher(s);
			Matcher nfm = nfp.matcher(s);
			Matcher jtwhm = jtwhp.matcher(s);
			if (wzm.find()) {whmap.put("docwz", wzm.group(1));}
			if (nfm.find()) {whmap.put("docnf", nfm.group(1));}
			if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}else	
		//〔62〕财农字第056号
		if (s.trim().replaceAll(
				"(\\W?\\d+\\W?[\\u4e00-\\u9fa5]+\\W?\\d+\\W?[\\u4e00-\\u9fa5]*?)"
				, "").equals("")) {
			System.err.println(3);
			Pattern wzp =Pattern.compile("\\W?\\d+\\W?([\\u4e00-\\u9fa5]+)\\W?\\d+\\W?[\\u4e00-\\u9fa5]*?",2);
			Pattern nfp = Pattern.compile("\\W?(\\d+)\\W?[\\u4e00-\\u9fa5]+\\W?\\d+\\W?[\\u4e00-\\u9fa5]*?",2);
			Pattern jtwhp = Pattern.compile("\\W?\\d+\\W?[\\u4e00-\\u9fa5]+\\W?(\\d+)\\W?[\\u4e00-\\u9fa5]*?",2);
			Matcher wzm = wzp.matcher(s);
			Matcher nfm = nfp.matcher(s);
			Matcher jtwhm = jtwhp.matcher(s);
			
			if (wzm.find()) {
				String wz=wzm.group(1).substring(0,wzm.group(1).length());
				if (wz.contains("第")) {
					whmap.put("docwz", wz.substring(0,wz.length()-1));	
				}else{whmap.put("docwz", wz);}}
			if (nfm.find()) {whmap.put("docnf", nfm.group(1));}
			if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}else	
		//广东省地方税务局公告     一般不要
//		if (s.trim().replaceAll(
//				"([\\u4e00-\\u9fa5]+?)"
//				, "").equals("")) {
//			System.err.println(4);
//			Pattern wzp =Pattern.compile("([\\u4e00-\\u9fa5]+)",2);
//			Matcher wzm = wzp.matcher(s);
//			if (wzm.find()) {whmap.put("docwz", wzm.group(1));}
//			whmap.put("docnf", "");
//			whmap.put("docjtwh", "");
//		}else
		//1951.08.08|1951年0月08日
		if (s.trim().replaceAll(
				"([\\d]{2,4}\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?)"
				, "").equals("")) {
			System.err.println(5);
			Pattern nfp =Pattern.compile("([\\d]{2,4})\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?",2);
			Matcher nfm = nfp.matcher(s);
			whmap.put("docwz", "");
			if (nfm.find()) {whmap.put("docnf", nfm.group(1));}
			whmap.put("docjtwh", "");
		}else
		//1988年8月6日国务院令第11号发布
		if (s.trim().replaceAll(
				"([\\d]{2,4}\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?[\\u4e00-\\u9fa5]+\\d+[\\u4e00-\\u9fa5]+)"
				, "").equals("")) {
			System.err.println(6);
			Pattern wzp =Pattern.compile("[\\d]{2,4}\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?([\\u4e00-\\u9fa5]+)\\d+[\\u4e00-\\u9fa5]+",2);
			Pattern nfp = Pattern.compile("([\\d]{2,4})\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?[\\u4e00-\\u9fa5]+\\d+[\\u4e00-\\u9fa5]+",2);
			Pattern jtwhp = Pattern.compile("[\\d]{2,4}\\W[\\d]{1,2}\\W[\\d]{1,2}\\W?[\\u4e00-\\u9fa5]+(\\d+)[\\u4e00-\\u9fa5]+",2);
			Matcher wzm = wzp.matcher(s);
			Matcher nfm = nfp.matcher(s);
			Matcher jtwhm = jtwhp.matcher(s);
			if (wzm.find()) {whmap.put("docwz", wzm.group(1).substring(0,wzm.group(1).length()-1));}
			if (nfm.find()) {whmap.put("docnf", nfm.group(1));}
			if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}else	
		//其他
		{
			System.err.println("其他");
			Pattern wzp =Pattern.compile("(\\W+)第",2);
			Pattern nfp = Pattern.compile("\\W?(\\d{4})[^号|^次|^期]",2);
			Pattern jtwhp = Pattern.compile("(\\d+)\\s*[号|次|期]\\S*",2);
			Matcher wzm = wzp.matcher(s);
			Matcher nfm = nfp.matcher(s);
			Matcher jtwhm = jtwhp.matcher(s);
			if (wzm.find()) {whmap.put("docwz", wzm.group(1));}
			if (nfm.find()) {whmap.put("docnf", nfm.group(1));}
			if (jtwhm.find()) {whmap.put("docjtwh", jtwhm.group(1));}
		}		
		
		
		return whmap;
	}
	public static void main(String[] args) {
		String s="国函〔1988〕74号";
		
		Map<String, String> whmap=WhOut(s);
		System.out.println("文———种："+StringEscapeUtils.unescapeJava(whmap.get("docwz")));
		System.out.println("年———份："+StringEscapeUtils.unescapeJava(whmap.get("docnf")));
		System.out.println("具体文号："+StringEscapeUtils.unescapeJava(whmap.get("docjtwh")));
	
	}
}  



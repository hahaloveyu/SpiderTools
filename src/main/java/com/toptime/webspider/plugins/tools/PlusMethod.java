package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.entity.AgentPara;
import com.toptime.webspider.entity.AgentResponse;

import javaxt.utils.string;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 增强插件
 *
 * @author bjoso
 * @date 2018/4/17
 */
public class PlusMethod extends com.toptime.webspider.core.tools.PlusMethod{
	 private static Log logger = LogFactory.getLog(PlusMethod.class);
	 private String abc;
    /**
     * 增强方法
     *
     * @param html    网页源码
     * @param url     url
     * @param mapData 源数据
     * @return 修改后的数据
     */
    public Map<String, String> plus(String html, String url, Map<String, String> mapData) {
    	//docno 對其進行判斷如果為非空則對文號進行分割
    	String JMGG ="";//	竞买公告的加工之后的数据
    	if (mapData.getOrDefault("#DREFIELD DOCNO","").length()>0) {
    		String docno = mapData.get("#DREFIELD DOCNO");
    		WhUtil whUtil = new WhUtil();
    		Map<String, String> whmap=WhUtil.WhOut(docno);
    		mapData.put("#DREFIELD docnf",StringEscapeUtils.unescapeJava(whmap.get("docnf")));
    		mapData.put("#DREFIELD C7", StringEscapeUtils.unescapeJava(whmap.get("docnf")));
    		mapData.put("#DREFIELD C5", StringEscapeUtils.unescapeJava(whmap.get("docwz")));
    		mapData.put("#DREFIELD docwz",StringEscapeUtils.unescapeJava(whmap.get("docwz")));
    		mapData.put("#DREFIELD C6", mapData.get("#DREFIELD pubname"));
    		mapData.put("#DREFIELD docjtwh", StringEscapeUtils.unescapeJava(whmap.get("docjtwh")));
		}
    	//对详细的拍卖情况进行分析
    	if (mapData.getOrDefault("#DREFIELD PMMS","").length()>0) {
    		logger.info("进入PMMS方法-----------------------------");
    		String s = "http:"+ mapData.get("#DREFIELD PMMS").trim();
    		String urlGetresponse = UrlGetresponse(s, "拍卖[\\w]?方式：([\\s\\S]*?)[四|五|六|七]", "PMMS");
    		if (urlGetresponse.length()==0) {
    			logger.info("进入PMMS2赋值改造方法-----------------------------");
    			String orDefault = mapData.getOrDefault("#DREFIELD PMMS2","");
        		mapData.put("#DREFIELD PMMS", orDefault);
			}else
    		mapData.put("#DREFIELD PMMS", urlGetresponse);
    		
    	}
    	//对竞买记录进行匹配
    	if (mapData.getOrDefault("#DREFIELD JMJLSJZW","").length()>0) {
    		logger.info("进入JMJLSJZW方法-----------------------------");
    		String s = "http:"+ mapData.get("#DREFIELD JMJLSJZW").trim();
    		String urlGetresponse = UrlGetresponse(s, "date:\"(.*?)\"", "JMJLSJ");
    		String urlGetresponse2 = UrlGetresponse(s, "records:\\[{(.*?)},", "JMJLSJZW");
    		mapData.put("#DREFIELD JMJLSJ", urlGetresponse);
    		mapData.put("#DREFIELD JMJLSJZW", urlGetresponse2);
    		
    	}
    	//对标的名称进行过滤
    	if (mapData.getOrDefault("#DREFIELD BDMC","").length()>0) {
    		logger.info("进入BDMC过滤方法-----------------------------");
    		String orDefault = mapData.getOrDefault("#DREFIELD BDMC","");
    		orDefault = orDefault.replaceAll("-  司法拍卖 - 阿里拍卖", "").trim();
    		mapData.put("#DREFIELD BDMC", orDefault);
    		    
    	}
    	//对评估价进行分析，如果评估价为空，则把市场价的值复制给评估价，并且把C9的值设置为1
    	if (mapData.getOrDefault("#DREFIELD PGJ","").length()==0) {
    		logger.info("进入PGJ修改成市场价方法准备阶段-----------------------------");
    		int scj = mapData.getOrDefault("#DREFIELD SCJ","").length();
    		if (scj>0) {
    			logger.info("进入市场价赋值阶段-----------------------------");
    			String orDefault = mapData.getOrDefault("#DREFIELD SCJ","");
    			mapData.put("#DREFIELD PGJ", orDefault);
    			mapData.put("#DREFIELD C9", "1");
    		}
    		
    	}
    	//获取竞买公告的内容
    	if (mapData.getOrDefault("#DREFIELD JMGG","").length()>0) {
    		logger.info("进入JMGG方法-----------------------------");
    		String ss = "http:"+ mapData.get("#DREFIELD JMGG").trim();
    		 PlusMethod plusMethod2=new PlusMethod();
				String urlGetresponse = plusMethod2.UrlGetresponse(ss);
				String replaceAll = urlGetresponse.replaceAll("<[^>]*?>", "");
				replaceAll = StringEscapeUtils.unescapeJava(replaceAll);
				replaceAll = StringEscapeUtils.unescapeHtml4(replaceAll);
				JMGG = replaceAll;
				mapData.put("#DREFIELD JMGGZW",urlGetresponse);
    	}
   	
    	
        //拿到想要访问的URL 并且对其进行访问。
    	if (mapData.getOrDefault("#DREFIELD BDW","").length()>0) {
    		logger.info("进入BDW方法-----------------------------");
    		 String s = "http:"+ mapData.get("#DREFIELD BDW").trim();
    		 //logger.info("取到的正则表达式的值为---------------------"+s);
    	        PlusMethod plusMethod2=new PlusMethod();
				String urlGetresponse = plusMethod2.UrlGetresponse(s);
				String replaceAll = urlGetresponse.replaceAll("<[^>]*?>", "");
				String replaceAll21 = replaceAll.replaceAll("[\\s]+", "");
				replaceAll = StringEscapeUtils.unescapeJava(replaceAll);
				replaceAll = StringEscapeUtils.unescapeHtml4(replaceAll);
				mapData.put("#DREFIELD BDWZW",urlGetresponse);
    	        //创建集合，存入元素
				   Map<String,String> map = new HashMap<String,String>();
			        map.put("QZQK","权证情况(.*?)</tr>");//权证情况
			        map.put("BDSUR","所有人(.*?)</tr>@@@@@所 有 人([\\s\\S]*?)</tr>@@@@@所有权人([\\s\\S]*?)</tr>");//标的所有人
			        map.put("FWYT","(房屋|规划|设计)[\\W]*?用途[\\W]*?[\\s]+([\\u4e00-\\u9fa5]{1,})[\\s]");//房屋用途
			        map.put("BDXZ","标的物现状(.*?)<strong>@@@@@拍品现状([\\s\\S]*?)权利限制情况@@@@@拍品现状([\\s\\S]*?)权利限制@@@@@标的现状([\\s\\S]*?)权利限制@@@@@标的现状([\\s\\S]*?)</tr>");//标的现状
			        map.put("TDXZ","(土地|规划|地类)(用途及性质|级别为|用途)[\\W]*?[\\s]+([\\u4e00-\\u9fa5]{1,})[\\s]");//土地性质
			        map.put("TDYT","(土地|地类)[\\W]*?(用途|)[\\W]*?[\\s]+([\\u4e00-\\u9fa5]{1,})[\\s]");//土地用途
			        map.put("QLXZQK","权利限制情况(.*?)</tr>");//权利限制情况
			        map.put("BYSX","(用途|性质)[\\W]*?([\\s\\S]*?)(</tr>|\\r|\\n)");//土地用途
			        map.put("HKQK","过户情况(.*?)</tr>@@@@@户口情况(.*?)</tr>");//HKQK户口情况
			        map.put("YS","钥匙<[^>]*?>(.*?)</tr>@@@@@钥&nbsp;&nbsp;匙<[^>]*?>(.*?)</tr>@@@@@钥.*?匙(.*?)</tr>");//YS钥匙情况
			        map.put("ZLQK","租赁情况(.*?)</tr>@@@@@租赁(.*?)</tr>");//ZLQK租赁情况
			        //房屋插件
			        map.put("JZMJ","建筑[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,，]{1,12})(㎡|平|米|m2)@@@@@房屋[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,，]{1,12})(㎡|平|米|m2)@@@@@住房[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,，]{1,12})(㎡|平|米|m2)");//面积
			        //土地插件
			        map.put("SYTDMJ","[土|宗]地[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,，]{1,12})(㎡|平|米|m2|)@@@@@使用权[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,，]{1,12})(㎡|平|米|m2|)");
			        //补充面积
			        map.put("MJ","([0-9.,，]{1,12})(㎡|平方米|m2|平米|米)@@@@@面积[^0-9]{0,5}([0-9.,，]{1,12})(㎡|m2|平|米|\\(㎡\\)|\\(平\\)|\\(m2\\)|\\(米\\))@@@@@面积[^0-9]{0,5}([0-9.,，]{1,12})");
			        map.put("ZDH","宗地号</td><td.*?>(.*?)</td></tr><tr height=\\\"70\\\">@@@@@土地权证号(.*?)，@@@@@土地产权证([\\s\\S]*?)</tr>@@@@@土地使用权证号：([\\s\\S]*?)</p>@@@@@土地使用权证：([\\s\\S]*?)，@@@@@土地使用权证([\\s\\S]*?)</tr>@@@@@土地所有权证([\\s\\S]*?)</tr>@@@@@土地证号为([\\s\\S]*?)，@@@@@宗地号([\\s\\S]*?)</tr>@@@@@地[\\W]{0,2}号[^\\u4e00-\\u9fa5]{1}(.*?)[，|。|,|；]");//宗地号
			        map.put("QZH","房屋产权证([\\s\\S]*?)</tr>@@@@@房屋所有权证([\\s\\S]*?)</tr>@@@@@房产证号：(.*?)[，|。|,]@@@@@证号[：|](.*?)[，|。|,]@@@@@房[\\W]{0,2}产权证[\\W]{0,2}号([\\s\\S]*?)</tr>");//房屋权证号
			        String bdxz ="";
			        //获取map集合中的所有键，存入到Set集合中，
			        Set<Map.Entry<String,String>> entry = map.entrySet();
			        //通过迭代器取出map中的键值关系，迭代器接收的泛型参数应和Set接收的一致
			        Iterator<Map.Entry<String,String>> it = entry.iterator();
			        while (it.hasNext())
			        {
			            //将键值关系取出存入Map.Entry这个映射关系集合接口中
			            Map.Entry<String,String>  me = it.next();
			            PlusMethod plusMethod  =  new PlusMethod();
			            //使用Map.Entry中的方法获取键和值
			            //判断value的值，如果包含@@@@@分隔符则进行循环截取。
			            bdxz = forEatch(urlGetresponse, plusMethod, "BDXZ", map.get("BDXZ"));
			            String key = me.getKey();
			            String value = me.getValue();
			            String abc ="";
			            switch (key) {
						case "JZMJ":
							abc = forEatch(JMGG+replaceAll, plusMethod, key, value).replace("：", "");
							String[] split = abc.split(",");
							   int a =split.length;
							   if (a>1) {
								if (split[a-1].length()<3) {
									abc=abc.substring(0, abc.length()-split[a-1].length()-1).replace(",", "");
								}
							}
							if (abc.equals("2")) {
								abc="";
							}
							break;
						case "SYTDMJ":
							abc = forEatch(JMGG+replaceAll, plusMethod, key, value).replace("：", "");
							String[] split2 = abc.split(",");
							   int a2 =split2.length;
							   if (a2>1) {
								if (split2[a2-1].length()<3) {
									abc=abc.substring(0, abc.length()-split2[a2-1].length()-1).replace(",", "");
								}
							}
							if (abc.equals("2")) {
								abc="";
							}
							break;
							
						case "MJ":
							abc = forEatch(JMGG+replaceAll, plusMethod, key, value).replace("：", "");
							String[] split3 = abc.split(",");
							   int a3 =split3.length;
							   if (a3>1) {
								if (split3[a3-1].length()<3) {
									abc=abc.substring(0, abc.length()-split3[a3-1].length()-1).replace(",", "");
								}
							}
							if (abc.equals("2")) {
								abc="";
							}
							break;
						case "TDXZ":
							 abc = TdxzAndFwytUtils.fetchTdxz(replaceAll21);
							break;
						case "TDYT":
							abc = TdxzAndFwytUtils.fetchFwyt(replaceAll21);
							break;
						case "FWYT":
							abc = TdxzAndFwytUtils.fetchFwyt(replaceAll21);
							break;
						case "ZLQK":
			                abc = forEatch(urlGetresponse, plusMethod, key, value).replace("情况", "");
			                break;
						default:
							abc = forEatch(urlGetresponse, plusMethod, key, value);
							break;
						}
		            //System.out.println("得到的输出字段为"+"#DREFIELD "+" = "+ key + " : " + abc);
		            mapData.put("#DREFIELD "+key, abc);

		        }

}
    	
    	  return mapData;
    }
  
    public  String UrlGetresponse(String url){
    	
    	logger.info("截取到的Url为————————————————————————"+url);
        String html ="";
        AgentPara agentPara = new AgentPara();
        agentPara.setUrl(url);
        agentPara.setFileLength(10 * 1024 * 1024);
        AgentResponse response = null;
        try {
            response = new OkHttpGet().getAgentResponse(agentPara, true);
             html = new String(response.getContent(), response.getCharset());
            //System.out.println(html);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return html;
    }
    public String UrlGetresponse(String url ,String rex ,String name){

        String html ="";
        AgentPara agentPara = new AgentPara();
        agentPara.setUrl(url);
        agentPara.setFileLength(10 * 1024 * 1024);
        AgentResponse response = null;
        try {
            response = new OkHttpGet().getAgentResponse(agentPara, true);
             html = new String(response.getContent(), response.getCharset());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Pattern p = Pattern.compile(rex, 2);
        Matcher m = p.matcher(html);
        String valuestr = "";
        if (m.find()) {
            valuestr = m.group(1).replace("&nbsp;", "").trim();
            valuestr = StringEscapeUtils.unescapeJava(valuestr);
            valuestr = StringEscapeUtils.unescapeHtml4(valuestr);
            valuestr = valuestr.replaceAll("<[^>]*?>", "").trim();

            html=valuestr;
        }else {
            html=valuestr;
        }
        return html;
    }
    /*
     * 根据URL进行正则匹配
     */
    public String Rexgethtml(String html,String rex,String name) {
    	Pattern p = Pattern.compile(rex, 2);
        Matcher m = p.matcher(html);
        String valuestr = "";
        if (m.find()) {
            valuestr = m.group(1).trim();
            
            valuestr = valuestr.replaceAll("<[^>]*?>", " ").replace("&nbsp;", "").replaceAll("\\s+", " ").trim();
            valuestr = StringEscapeUtils.unescapeJava(valuestr);
            valuestr = StringEscapeUtils.unescapeHtml4(valuestr);
            //System.out.println("匹配的正则为-----"+rex+"过滤之后的值为----"+valuestr);
            html=valuestr;
        }else {
            html="";
        }
        return html;
    	
	}
    /*
     * 根据URL进行正则匹配只匹配能取到的括号里面的值（）
     */
    public String Rexgethtml2(String html,String rex,String name) {
    	Pattern p = Pattern.compile(rex, 2);
        Matcher m = p.matcher(html);
        String valuestr = "";
        String valuestr2 = "";
        if (m.find()) {
            valuestr = m.group().trim();
            if (valuestr.contains("&radic;")) {
                System.out.println("进入打钩判断");
                p = Pattern.compile("[\\u4e00-\\u9fa5]{1,}\\W*?&radic;\\W+?");
                m = p.matcher(valuestr);
                while (m.find()) {
                    
                    valuestr2 += m.group().trim();
                }
                valuestr = valuestr2;
            }
            valuestr = valuestr.replaceAll("<[^>]*?>", " ").replaceAll("[\\s]+", " ").replace("&nbsp;", "").trim();
            valuestr = StringEscapeUtils.unescapeJava(valuestr);
            valuestr = StringEscapeUtils.unescapeHtml4(valuestr);
            //System.out.println("匹配的正则为-----"+rex+"过滤之后的值为----"+valuestr);
            html=valuestr;
        }else {
            html="";
        }
        return html;
    	
	}
    /*
     * 根据URL进行正则匹配 循环匹配
     */
    public String Rexgethtmlxh(String html,String rex,String name) {
    	Pattern p = Pattern.compile(rex, 2);
    	Matcher m = p.matcher(html);
    	String valuestr = "";
    	String a ="";
    	while (m.find()) {
    		//System.out.println("选用进入面积的正则表达式~~~"+rex);
    		valuestr = m.group(1).trim();
    		valuestr = valuestr.replaceAll("<[^>]*?>", "").replace("&nbsp;", "").trim();
    		valuestr = StringEscapeUtils.unescapeJava(valuestr);
    		valuestr = StringEscapeUtils.unescapeHtml4(valuestr);
    		if (valuestr.equals("2")) {
				continue;
			}
    		//System.out.println("匹配的正则为-----"+rex+"过滤之后的值为----"+valuestr);
    		a+="@@@"+valuestr;
    	}
    	return a.replaceFirst("@@@", "");
    	
    }
    /*
     * 根据URL进行正则匹配 循环匹配 取全部的属性值
     */
    public String Rexgethtmlxh2(String html,String rex,String name) {
        Pattern p = Pattern.compile(rex, 2);
        Matcher m = p.matcher(html);
        String valuestr = "";
        String a ="";
        while (m.find()) {
            //System.out.println("选用进入面积的正则表达式~~~"+rex);
            valuestr = m.group().trim();
            valuestr = valuestr.replaceAll("<[^>]*?>", "").replace("&nbsp;", "").trim();
            valuestr = StringEscapeUtils.unescapeJava(valuestr);
            valuestr = StringEscapeUtils.unescapeHtml4(valuestr);
            //System.out.println("匹配的正则为-----"+rex+"过滤之后的值为----"+valuestr);
            a+="@@@"+valuestr;
        }
        return a.replaceFirst("@@@", "");
        
    }
	/**
	 * @param urlGetresponse
	 * @param plusMethod
	 * @param key
	 * @param value
	 */
	private static String forEatch(String urlGetresponse, PlusMethod plusMethod, String key, String value) {
		String a ="";
		if (value.contains("@@@@@")) {
			String[] values = value.split("@@@@@");
			for (int i = 0; i < values.length; i++) {
				//System.out.println(values[i]);
				if (key.equals("MJ")) {
					a =plusMethod.Rexgethtmlxh(urlGetresponse, values[i], key);
				}else if (key.equals("TDYT")|key.equals("FWYT")|key.equals("TDXZ")) {
					a =plusMethod.Rexgethtml2(urlGetresponse, values[i], key);
				}else if (key.equals("BYSX")) {
				    a =plusMethod.Rexgethtmlxh2(urlGetresponse, values[i], key);
                }else {
					a = plusMethod.Rexgethtml(urlGetresponse,values[i],key);
				}
				if (a.length()>0) {
					//System.out.println("当前a的值为"+a);
					break;
				}
				
			}
			
		}else {
			if (key.equals("MJ")) {
				a =plusMethod.Rexgethtmlxh(urlGetresponse, value, key);
			}else if (key.equals("TDYT")|key.equals("FWYT")|key.equals("TDXZ")) {
				a =plusMethod.Rexgethtml2(urlGetresponse, value, key);
			}else if (key.equals("BYSX")) {
                a =plusMethod.Rexgethtmlxh2(urlGetresponse, value, key);
            }else {
				a = plusMethod.Rexgethtml(urlGetresponse,value,key);
			}
			
		}
		return a;
	}
    public static void main(String[] args) {
    	String JMGG="";
    	String ss = "http:"+"//sf.taobao.com/json/get_notice_attach.htm?project_id=574580";
		 PlusMethod plusMethod3=new PlusMethod();
			String urlGetresponse2 = plusMethod3.UrlGetresponse(ss);
			String replaceAll2 = urlGetresponse2.replaceAll("<[^>]*?>", "");
			replaceAll2 = StringEscapeUtils.unescapeJava(replaceAll2);
			replaceAll2 = StringEscapeUtils.unescapeHtml4(replaceAll2);
			JMGG = replaceAll2;
    	Map<String, String> mapData =new HashMap<String, String>();
        //拿到想要访问的URL 并且对其进行访问。
        String s = "http:"+"//desc.alicdn.com/i6/560/430/563436234012/TB10Ye0dJHO8KJjSZFt8qwhfXla.desc%7Cvar%5Edesc%3Bsign%5E9ab6c2fa56c3dee48230e535f956cfbc%3Blang%5Egbk%3Bt%5E1514432452";
        //创建集合，存入元素
        PlusMethod plusMethod2 = new PlusMethod();
        String urlGetresponse = plusMethod2.UrlGetresponse(s);
        String replaceAll = urlGetresponse.replaceAll("<[^>]*?>", " ").replace("&nbsp;", " ");
        String replaceAll21 = replaceAll.replaceAll("[\\s]+", "");
        //System.out.println("处理之后的字段的数据~~~~~"+replaceAll);
        String bdxz ="";
        //String bysx ="";
        Map<String,String> map = new HashMap<String,String>();
        map.put("QZQK","权证情况(.*?)</tr>");//权证情况
        map.put("BDSUR","所有人(.*?)</tr>@@@@@所 有 人([\\s\\S]*?)</tr>@@@@@所有权人([\\s\\S]*?)</tr>");//标的所有人
        //过时的方法，之前是匹配标签过滤之后的数据存在匹配不准的情况现在采用匹配标签之前的数据
        //map.put("FWYT","(房屋|规划|设计)[\\W]*?用途[\\W]*?[\\s]+([\\u4e00-\\u9fa5]{1,})[\\s]");//房屋用途
        map.put("FWYT","(房屋|规划|设计|使用)[\\u4e00-\\u9fa5]*?(用途|性质)[\\W]*?([\\s\\S]*?)(</tr>|\\r|\\n)");//房屋用途
        map.put("TDXZ","(土地|规划|地类)(用途及性质|用途|性质|级别)[\\W]*?([\\s\\S]*?)(</tr>|\\r|\\n)]");//土地性质
        map.put("TDYT","(土地|地类|规划)[\\u4e00-\\u9fa5]*?(用途|性质|级别)[\\W]*?([\\s\\S]*?)(</tr>|\\r|\\n)");//土地用途
        //备用用途及性质
        map.put("BYSX","(用途|性质)[\\W]*?([\\s\\S]*?)(</tr>|\\r|\\n)");//土地用途
        
        map.put("BDXZ","标的物现状(.*?)<strong>@@@@@拍品现状([\\s\\S]*?)权利限制情况@@@@@拍品现状([\\s\\S]*?)权利限制@@@@@标的现状([\\s\\S]*?)权利限制@@@@@标的现状([\\s\\S]*?)</tr>@@@@@情况说明</span>([\\s\\S]*?)</tr>");//标的现状
        map.put("QLXZQK","权利限制情况(.*?)</tr>");//权利限制情况
        map.put("HKQK","过户情况(.*?)</tr>@@@@@户口情况(.*?)</tr>");//HKQK户口情况
        map.put("YS","钥匙<[^>]*?>(.*?)</tr>@@@@@钥&nbsp;&nbsp;匙<[^>]*?>(.*?)</tr>@@@@@钥.*?匙(.*?)</tr>");//YS钥匙情况
        map.put("ZLQK","租赁情况(.*?)</tr>@@@@@租赁(.*?)</tr>");//ZLQK租赁情况
        //房屋插件
        map.put("JZMJ","建筑[^0-9]{0,3}面积[^0-9]{0,3}([0-9.]{1,9})(㎡|米|m2)@@@@@房屋[^0-9]{0,3}面积[^0-9]{0,3}([0-9.]{1,9})(㎡|米|m2)@@@@@住房[^0-9]{0,3}面积[^0-9]{0,3}([0-9.]{1,9})(㎡|米|m2)");//面积
        //土地插件
        map.put("SYTDMJ","[土|宗]地[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,]{1,9})(㎡|米|m2|)@@@@@使用权[^0-9]{0,3}面积[^0-9]{0,3}([0-9.,]{1,9})(㎡|米|m2|)");
        //补充面积
        map.put("MJ","([0-9.,]{1,8})(㎡|平方米|m2|平米|米)@@@@@面积[^0-9]{0,5}([0-9.]{1,8})(㎡|m2|平|米|\\(㎡\\)|\\(平\\)|\\(m2\\)|\\(米\\))@@@@@面积(㎡|m2|平|米|\\(㎡\\)|\\(平\\)|\\(m2\\)|\\(米\\))[\\s]+([0-9.]{1,8})@@@@@面积[^0-9]{0,5}([0-9.,]{1,8})");
        map.put("ZDH","宗地号</td><td.*?>(.*?)</td></tr><tr height=\\\"70\\\">@@@@@土地权证号(.*?)，@@@@@土地产权证([\\s\\S]*?)</tr>@@@@@土地使用权证号：([\\s\\S]*?)</p>@@@@@土地使用权证：([\\s\\S]*?)，@@@@@土地使用权证([\\s\\S]*?)</tr>@@@@@土地所有权证([\\s\\S]*?)</tr>@@@@@土地证号为([\\s\\S]*?)，@@@@@宗地号([\\s\\S]*?)</tr>@@@@@地[\\W]{0,2}号[^\\u4e00-\\u9fa5]{1}(.*?)[，|。|,|；]");//宗地号
        map.put("QZH","房屋产权证([\\s\\S]*?)</tr>@@@@@房屋所有权证([\\s\\S]*?)</tr>@@@@@房产证号：(.*?)[，|。|,]@@@@@证号[：|](.*?)[，|。|,]@@@@@房[\\W]{0,2}产权证[\\W]{0,2}号([\\s\\S]*?)</tr>");//房屋权证号

        //获取map集合中的所有键，存入到Set集合中，
        Set<Map.Entry<String,String>> entry = map.entrySet();
        
        //通过迭代器取出map中的键值关系，迭代器接收的泛型参数应和Set接收的一致
        Iterator<Map.Entry<String,String>> it = entry.iterator();
        while (it.hasNext())
        {
            //将键值关系取出存入Map.Entry这个映射关系集合接口中
            Map.Entry<String,String>  me = it.next();
            PlusMethod plusMethod  =  new PlusMethod();
            //使用Map.Entry中的方法获取键和值
            //判断value的值，如果包含@@@@@分隔符则进行循环截取。
            
            bdxz = forEatch(urlGetresponse, plusMethod, "BDXZ", map.get("BDXZ"));
            //bysx = forEatch(urlGetresponse, plusMethod, "BYSX", map.get("BYSX"));
            String key = me.getKey();
            String value = me.getValue();
            String abc ="";
            switch (key) {
			case "JZMJ":
				abc = forEatch(JMGG+replaceAll, plusMethod, key, value);
				abc=abc.replace("：", "");
				if (abc.equals("2")) {
					abc="";
				}
				break;
			case "SYTDMJ":
				abc = forEatch(JMGG+replaceAll, plusMethod, key, value);
				abc=abc.replace("：", "");
				if (abc.equals("2")) {
					abc="";
				}
				break;
			case "MJ":
				abc = forEatch(JMGG+replaceAll, plusMethod, key, value);
				abc=abc.replace("：", "").replace(",", "");
				if (abc.equals("2")) {
					abc="";
				}
				break;
			case "TDXZ":
				 abc = TdxzAndFwytUtils.fetchTdxz(replaceAll21);
				break;
			case "TDYT":
				abc = TdxzAndFwytUtils.fetchFwyt(replaceAll21);
				break;
			case "FWYT":
				abc = TdxzAndFwytUtils.fetchFwyt(replaceAll21);
				break;
			case "ZLQK":
			    abc = forEatch(urlGetresponse, plusMethod, key, value).replace("情况", "");
			    break;
			default:
				abc = forEatch(urlGetresponse, plusMethod, key, value);
				break;
			}
            System.out.println("得到的输出字段为"+"#DREFIELD "+" = "+ key + " : " + abc);


        }


}

   }



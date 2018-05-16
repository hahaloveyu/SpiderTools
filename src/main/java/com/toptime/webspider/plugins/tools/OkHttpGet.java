package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.core.agent.okhttpclient.HostnameNotVerifier;
import com.toptime.webspider.core.agent.okhttpclient.X509TrustManagerImpl;
import com.toptime.webspider.entity.AgentPara;
import com.toptime.webspider.entity.AgentResponse;
import com.zhcode.SinoDetect;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OkHttpGet {

    private static Log logger = LogFactory.getLog(OkHttpGet.class);

    private static Map<String, String> mimeMap = new HashMap<>();

    private static Map<Integer, String> charsetMap = new HashMap<>();

    static {
        mimeMap.put("text/html", ".html");
        mimeMap.put("application/xml", ".xml");
        mimeMap.put("text/xml", ".xml");
        mimeMap.put("text/css", ".css");
        mimeMap.put("application/json", ".json");
        mimeMap.put("application/javascript", ".js");
        mimeMap.put("text/plain", ".txt");
        mimeMap.put("application/octet-stream", ".file");
        mimeMap.put("video/mp4", ".mp4");
        mimeMap.put("video/x-flv", ".flv");
        mimeMap.put("application/x-shockwave-flash", ".flv");
        mimeMap.put("application/pdf", ".pdf");
        mimeMap.put("application/msword", ".doc");
        mimeMap.put("application/vnd.ms-word", ".doc");
        mimeMap.put("application/msexcel", ".xls");
        mimeMap.put("application/vnd.ms-excel", ".xls");
        mimeMap.put("application/mspowerpoint", ".ppt");
        mimeMap.put("application/zip", ".zip");
        mimeMap.put("application/x-zip-compressed", ".zip");
        mimeMap.put("application/x-rar-compressed", ".rar");
        mimeMap.put("image/jpeg", ".jpg");
        mimeMap.put("image/jpg", ".jpg");
        mimeMap.put("image/gif", ".gif");
        mimeMap.put("image/png", ".png");
        mimeMap.put("application/x-download", ".download");
        mimeMap.put("application", ".application");

        charsetMap.put(0, "gbk");
        charsetMap.put(1, "gbk");
        charsetMap.put(2, "gb18030");
        charsetMap.put(3, "gbk");
        charsetMap.put(4, "big5");
        charsetMap.put(5, "cns11643");
        charsetMap.put(6, "utf-8");
    }

    /**
     * 获得网页的属性
     *
     * @param para
     * @return
     */
    public AgentResponse getAgentResponse(AgentPara para, boolean returnContent) throws Exception {

        AgentResponse agentResponse = new AgentResponse();

        String url = para.getUrl();
        agentResponse.setInUrl(url);
        agentResponse.setRealUrl(url);

        ResponseBody body = null;
        Response response = null;
        InputStream inStream = null;
        ByteArrayOutputStream outStream = null;
        byte[] bytes = null;
        try {
            //code=301, message=Moved Permanently,
            response = httpGet(url, para);
            if (response == null) {
                agentResponse.setHttpcode(-9);
                agentResponse.setMyHttpcode(-9);
                return agentResponse;
            }
            //响应时间
            agentResponse.setResponseTime(response.receivedResponseAtMillis() - response.sentRequestAtMillis());
            //返回码
            int statusCode = response.code();
            agentResponse.setHttpcode(statusCode);
            agentResponse.setInUrl(url);
            //重定向循环计次
            int count = 0;
            while (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307) {
                if (count >= 3) {
                    break;
                }
                logger.info(statusCode + ",[" + url + "] connected successfully!");
                //网页转向
                String redirectUrl = response.header("Location");
                if (redirectUrl == null || redirectUrl.trim().length() == 0) {
                    agentResponse.setRealUrl("");
                    //无法获得网页内容
                    return agentResponse;
                }
                //处理  redirectUrl 是否是绝对路径
                //转化为绝对路径
                redirectUrl = resolve(url, redirectUrl);
                response = httpGet(redirectUrl, para);
                agentResponse.setRealUrl(redirectUrl);
                //转向的无法再取得链接
                if (response == null) {
                    return agentResponse;
                }
                agentResponse.setResponseTime(response.receivedResponseAtMillis() - response.sentRequestAtMillis());
                statusCode = response.code();
                agentResponse.setHttpcode(statusCode);
                agentResponse.setMyHttpcode(statusCode);
                agentResponse.setFileType(".html");
                if (statusCode == 200) {
                    break;
                } else if (statusCode == 301 || statusCode == 302) {
                    count++;
                } else if (statusCode == 401) {
                    //WWW Auth
                    logger.info(statusCode + ":[" + url + "]");
                    return agentResponse;
                } else {
                    logger.warn(statusCode + ":[" + url + "]");
                    return agentResponse;
                }
            }

            if (statusCode == 200) {
                logger.info(statusCode + ",[" + url + "] connected successfully!");
            } else if (statusCode == 401) {
                logger.info(statusCode + ",[" + url + "] connected successfully!");
                agentResponse.setMyHttpcode(statusCode);
                agentResponse.setFileType(".html");
                return agentResponse;
            } else {
                logger.warn("Can't meet " + statusCode + ";[" + url + "]");
                agentResponse.setMyHttpcode(statusCode);
                agentResponse.setFileType(".html");
                return agentResponse;
            }

            //获得 Content-Type
            String contentType = response.header("Content-Type");
            if (contentType == null) {
                contentType = "unknown";
            }
            if (contentType.contains(";")) {
                //防止有空格
                contentType = contentType.split(";", 2)[0].trim();
            }

            //是否符合要采集的网站
            if (mimeMap.containsKey(contentType)) {
                agentResponse.setFileType(mimeMap.get(contentType));
            } else {
                //其它类型的文件,对于联通性来说,认为是可以
                agentResponse.setFileType("." + contentType);
                //不获得内容
                return agentResponse;
            }

            //获得网页内容，(下载时间)
            long starttime = System.currentTimeMillis();
            if (returnContent) {
                body = response.body();
                if (body != null) {
                    inStream = body.byteStream();
                    outStream = new ByteArrayOutputStream();
                    byte[] buff = new byte[1024];
                    int len;
                    while ((len = inStream.read(buff)) != -1) {
                        outStream.write(buff, 0, len);
                        //限制文件大小
                        if (outStream.size() > para.getFileLength()) {
                            bytes = "".getBytes();
                            agentResponse.setHttpcode(-5);
                            agentResponse.setMyHttpcode(-5);
                            break;
                        }
                    }
                    if (bytes == null) {
                        bytes = outStream.toByteArray();
                    }
                    agentResponse.setContent(bytes);
                }
            }
            long endtime = System.currentTimeMillis();
            agentResponse.setDownloadTime(endtime - starttime);
            //总时间
            agentResponse.setTotalTime(agentResponse.getDownloadTime() + agentResponse.getResponseTime());
            //解析网页编码
            if (bytes != null && bytes.length > 0) {
                int de = new SinoDetect().detectEncoding(bytes);
                String charset = charsetMap.get(de);
                if (charset != null) {
                    agentResponse.setCharset(charset);
                }
            }
        } finally {
            IOUtils.closeQuietly(outStream);
            IOUtils.closeQuietly(inStream);
            if (body != null) {
                body.close();
            }
            if (response != null) {
                response.close();
            }
        }

        return agentResponse;

    }

    /**
     * Get请求
     *
     * @param url
     * @return
     */
    private Response httpGet(String url, AgentPara para) throws Exception {

        Builder builder = new Builder().url(url);
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        builder.header("Accept-Language", "zh-CN,zh;q=0.8");
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        para.getHeaderMap().forEach(builder::header);
        if (StringUtils.isNotEmpty(para.getUserAgent())) {
            builder.header("User-Agent", para.getUserAgent());
        }
        Request request = builder.build();

        Response response;
        if (para.isIsproxy()) {
            //新建一个
            X509TrustManagerImpl xtm = new X509TrustManagerImpl();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());
            int timeout = MyOkHttpClient.imgOkHttpClient.readTimeoutMillis();
            response = MyOkHttpClient.imgOkHttpClient.newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).followRedirects(false).sslSocketFactory(sslContext.getSocketFactory(), xtm).hostnameVerifier(new HostnameNotVerifier()).proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(para.getProxyIp(), para.getProxyPort()))).build().newCall(request).execute();
        } else {
            response = MyOkHttpClient.imgOkHttpClient.newCall(request).execute();
        }
        return response;
    }

    /**
     * 绝对地址转换
     *
     * @param base
     * @param relUrl
     * @return
     * @throws MalformedURLException
     */
    private URL resolve(URL base, String relUrl) throws MalformedURLException {
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not
        // '//path/file?foo' as desired
        if (relUrl.startsWith("?")) {
            relUrl = base.getPath() + relUrl;
        }
        // workaround: //example.com + ./foo = //example.com/./foo, not
        // //example.com/foo
        if (relUrl.indexOf('.') == 0 && base.getFile().indexOf('/') != 0) {
            base = new URL(base.getProtocol(), base.getHost(), base.getPort(), "/" + base.getFile());
        }
        return new URL(base, relUrl);
    }

    /**
     * 绝对地址转换
     *
     * @param baseUrl
     * @param relUrl
     * @return
     * @throws MalformedURLException
     */
    private String resolve(final String baseUrl, String relUrl) {
        URL base;

        relUrl = relUrl.replaceAll("\\\\", "/");
        relUrl = relUrl.replaceAll("\r\n|\n", "");

        try {
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute/rel may be abs on
                // its own, so try that
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }

    }
}

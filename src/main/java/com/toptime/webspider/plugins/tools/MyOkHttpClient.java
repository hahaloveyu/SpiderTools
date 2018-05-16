package com.toptime.webspider.plugins.tools;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * OkHttpClientConfiguration
 *
 * @author bjoso
 * @date 2017/11/27
 */
public class MyOkHttpClient{

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static OkHttpClient imgOkHttpClient;

    static  {
        X509TrustManagerImpl xtm = new X509TrustManagerImpl();
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{xtm}, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(30000, TimeUnit.MILLISECONDS)
                .readTimeout(30000, TimeUnit.MILLISECONDS)
                .followRedirects(false)
                .hostnameVerifier(new HostnameNotVerifier());
        if (sslContext != null) {
            builder.sslSocketFactory(sslContext.getSocketFactory(), xtm);
        }

        imgOkHttpClient = builder.build();
    }

    static class X509TrustManagerImpl implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // TODO Auto-generated method stub
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] x509Certificates = new X509Certificate[0];
            return x509Certificates;
        }
    }

    static class HostnameNotVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}

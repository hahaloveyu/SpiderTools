package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.entity.ImgInfo;
import com.toptime.webspider.plugins.tools.util.MyHtmlUtils;
import javaxt.io.Image;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 缩略图片处理
 * Created by bjoso on 2017/8/14.
 */
public class ThumbnailParser {

    private static Logger logger = LoggerFactory.getLogger(AutoFormat.class);
    /**
     * 图片阈值
     */
    private static final int IMAGE_SIZE = 3 * 1024 * 1024;
    /**
     * 图片 宽/高 比例阈值
     */
    private static final double WIDTH_HEIGHT_RATIO = 3.3;
    /**
     * 宽高  限值
     */
    private static final int WIDTH_RATIO = 300;
    /**
     * 高度限值
     */
    private static final int HEIGHT_RATIO = 180;
    /**
     * 生成新闻列表页缩略图的宽
     */
    private static final int DEFAULT_NEWS_IMG_WIDTH = 121;
    /**
     * 生成新闻列表页缩略图的高
     */
    private static final int DEFAULT_NEWS_IMG_HEIGHT = 75;
    /**
     * 生成大缩略图的宽
     */
    private static final int DEFAULT_BIG_IMG_WIDTH = 180;
    /**
     * 生成大缩略图的高
     */
    private static final int DEFAULT_BIG_IMG_HEIGHT = 112;
    /**
     * 符合要求的图片计数器
     */
    private static final int COUNT = 3;
    /**
     * 累计不符合要求的图片计数器
     */
    private static final int NOHANDLE = 10;
    /**
     * 获取图片超时 为null的计数器
     */
    private static final int TIMEOUT = 2;

    private static Map<String, String> mimeMap = new HashMap<>();

    static {
        mimeMap.put("image/jpeg", ".jpg");
        mimeMap.put("image/jpg", ".jpg");
        mimeMap.put("image/gif", ".gif");
        mimeMap.put("image/png", ".png");
    }

    /**
     * 获取页面上所有是否符合的图片的base64
     *
     * @param url  页面URL
     * @param html 页面源码
     * @return 图片信息对象l集合
     */

    public List<ImgInfo> getAllImgBase64(String url, String html) {
        int count = 0;
        int nohandle = 0;
        int timeout = 0;
        List<ImgInfo> imgInfoList = new ArrayList<>();
        // 获取页面所有的图片链接
        LinkedHashMap<String, Map<String, String>> linkMap = getAllImgUrl(url, html);
        if (linkMap != null && linkMap.size() > 0) {
            for (String imageUrl : linkMap.keySet()) {
                //只取最多三张图片
                if (count == COUNT || nohandle == NOHANDLE || timeout == TIMEOUT) {
                    break;
                }
                try {
                    ImgInfo imgInfo = new ImgInfo();
                    imgInfo.setUrl(imageUrl);
//                    ImageWrapper image = getImage(imageUrl, imgInfo);
                    Image image = getImageX(imageUrl, imgInfo);
                    // 过滤不合要求的图片
                    if (isQualified(image, linkMap.get(imageUrl))) {
                        // 新闻图片base64
                        BufferedImage newsbi = disposeImage(image, DEFAULT_NEWS_IMG_WIDTH, DEFAULT_NEWS_IMG_HEIGHT);
                        if (newsbi != null) {
                            imgInfo.setNewsThumbnail(getImageBase64(newsbi, imgInfo.getType()));
                        }
                        // 大缩略图base64
                        BufferedImage bi = disposeImage(image, DEFAULT_BIG_IMG_WIDTH, DEFAULT_BIG_IMG_HEIGHT);
                        if (bi != null) {
                            imgInfo.setThumbnail(getImageBase64(bi, imgInfo.getType()));
                        }
                        imgInfoList.add(imgInfo);
                        count++;
                    } else {
                        nohandle++;
                    }
                } catch (RuntimeException e) {
                    logger.error(e + " | url:" + url + " | imgUrl:" + imageUrl);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return imgInfoList;
    }

    /**
     * 获取页面上所有是否符合的图片的base64
     *
     * @param url      页面URL
     * @param elements 正文节点源码
     * @return 图片信息对象集合
     */
    public List<ImgInfo> getAllImgBase64(Element elements, String url) {
        int count = 0;
        int nohandle = 0;
        int timeout = 0;
        List<ImgInfo> imgInfoList = new ArrayList<>();
        // 获取页面所有的图片链接
        Map<String, Map<String, String>> linkMap = MyHtmlUtils.parserImgLinks(elements, url);
        if (linkMap != null && linkMap.size() > 0) {
            for (String imageUrl : linkMap.keySet()) {
                //只取最多三张图片
                if (count == COUNT || nohandle == NOHANDLE) {
                    break;
                }
                try {
                    ImgInfo imgInfo = new ImgInfo();
                    imgInfo.setUrl(imageUrl);
//                    ImageWrapper image = getImage(imageUrl, imgInfo);
                    Image image = getImageX(imageUrl, imgInfo);
                    // 过滤不合要求的图片
                    if (isQualified(image, linkMap.get(imageUrl))) {
                        // 新闻图片base64
                        BufferedImage newsbi = disposeImage(image, DEFAULT_NEWS_IMG_WIDTH, DEFAULT_NEWS_IMG_HEIGHT);
                        if (newsbi != null) {
                            imgInfo.setNewsThumbnail(getImageBase64(newsbi, imgInfo.getType()));
                        }
                        // 大缩略图base64
                        BufferedImage bi = disposeImage(image, DEFAULT_BIG_IMG_WIDTH, DEFAULT_BIG_IMG_HEIGHT);
                        if (bi != null) {
                            imgInfo.setThumbnail(getImageBase64(bi, imgInfo.getType()));
                        }
                        imgInfoList.add(imgInfo);
                        count++;
                    } else {
                        nohandle++;
                    }
                } catch (RuntimeException e) {
                    logger.error(e + " | url:" + url + " | imgUrl:" + imageUrl);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return imgInfoList;
    }


    /**
     * 获取页面上所有是否符合的图片的base64
     *
     * @param url      页面URL
     * @param elements 正文节点源码
     * @return 图片信息对象集合
     */
    public List<ImgInfo> getAllImgBase64(Elements elements, String url) {
        int count = 0;
        int nohandle = 0;
        int timeout = 0;
        List<ImgInfo> imgInfoList = new ArrayList<>();
        // 获取页面所有的图片链接
        Map<String, Map<String, String>> linkMap = MyHtmlUtils.parserImgLinks(elements, url);
        if (linkMap != null && linkMap.size() > 0) {
            for (String imageUrl : linkMap.keySet()) {
                //只取最多三张图片
                if (count == COUNT || nohandle == NOHANDLE) {
                    break;
                }
                try {
                    ImgInfo imgInfo = new ImgInfo();
                    imgInfo.setUrl(imageUrl);
//                    ImageWrapper image = getImage(imageUrl, imgInfo);
                    Image image = getImageX(imageUrl, imgInfo);
                    // 过滤不合要求的图片
                    if (isQualified(image, linkMap.get(imageUrl))) {
                        // 新闻图片base64
                        BufferedImage newsbi = disposeImage(image, DEFAULT_NEWS_IMG_WIDTH, DEFAULT_NEWS_IMG_HEIGHT);
                        if (newsbi != null) {
                            imgInfo.setNewsThumbnail(getImageBase64(newsbi, imgInfo.getType()));
                        }
                        // 大缩略图base64
                        BufferedImage bi = disposeImage(image, DEFAULT_BIG_IMG_WIDTH, DEFAULT_BIG_IMG_HEIGHT);
                        if (bi != null) {
                            imgInfo.setThumbnail(getImageBase64(bi, imgInfo.getType()));
                        }
                        imgInfoList.add(imgInfo);
                        count++;
                    } else {
                        nohandle++;
                    }
                } catch (RuntimeException e) {
                    logger.error(e + " | url:" + url + " | imgUrl:" + imageUrl);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return imgInfoList;
    }

    /**
     * 根据url获取图片信息
     *
     * @param imageUrl 图片URL
     * @return 图片信息对象集合
     */
    public ImgInfo getImageInfo(String imageUrl) {
        ImgInfo imgInfo = new ImgInfo();
        imgInfo.setUrl(imageUrl);
        // 过滤不合要求的图片
        try {
//            ImageWrapper image = getImage(imageUrl, imgInfo);
            Image image = getImageX(imageUrl, imgInfo);
            // 过滤不合要求的图片
            if (isQualified(image, null)) {
                // 新闻图片base64
                BufferedImage newsbi = disposeImage(image, DEFAULT_NEWS_IMG_WIDTH, DEFAULT_NEWS_IMG_HEIGHT);
                if (newsbi != null) {
                    imgInfo.setNewsThumbnail(getImageBase64(newsbi, imgInfo.getType()));
                }
                // 大缩略图base64
                BufferedImage bi = disposeImage(image, DEFAULT_BIG_IMG_WIDTH, DEFAULT_BIG_IMG_HEIGHT);
                if (bi != null) {
                    imgInfo.setThumbnail(getImageBase64(bi, imgInfo.getType()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return imgInfo;
    }

    /**
     * 根据url获取图片信息 不判断图片尺寸
     *
     * @param imageUrl 图片URL
     * @return 图片信息对象集合
     */
    public ImgInfo getImageInfoNot(String imageUrl) {
        ImgInfo imgInfo = new ImgInfo();
        imgInfo.setUrl(imageUrl);
        // 过滤不合要求的图片
        try {
//            ImageWrapper image = getImage(imageUrl, imgInfo);
            Image image = getImageX(imageUrl, imgInfo);
            // 过滤不合要求的图片
            if (image != null) {
                // 新闻图片base64
                BufferedImage newsbi = disposeImage(image, DEFAULT_NEWS_IMG_WIDTH, DEFAULT_NEWS_IMG_HEIGHT);
                if (newsbi != null) {
                    imgInfo.setNewsThumbnail(getImageBase64(newsbi, imgInfo.getType()));
                }
                // 大缩略图base64
                BufferedImage bi = disposeImage(image, DEFAULT_BIG_IMG_WIDTH, DEFAULT_BIG_IMG_HEIGHT);
                if (bi != null) {
                    imgInfo.setThumbnail(getImageBase64(bi, imgInfo.getType()));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return imgInfo;
    }

    /**
     * 获取所有图片链接
     *
     * @param url  页面URL
     * @param html 页面源码
     * @return 返回map key:图片URL  value:标题
     */

    public LinkedHashMap<String, Map<String, String>> getAllImgUrl(String url, String html) {
        LinkedHashMap<String, Map<String, String>> linkMap = new LinkedHashMap<>();
        Map<String, String> suspectedUrlsMap = new HashMap<>();
        //图片链接
        Map<String, Map<String, String>> imgLinkMap = MyHtmlUtils.parserImgLinks(html, url, suspectedUrlsMap);
        if (imgLinkMap != null && imgLinkMap.size() > 0) {
            linkMap.putAll(imgLinkMap);
        }
        return linkMap;
    }

    /**
     * 判断图片是否符合要求
     *
     * @param image 图片
     * @return
     */
    private boolean isQualified(Image image, Map<String, String> attrMap) {
        if (image == null) {
            return false;
        }
        int imageHeight = image.getHeight();
        int imageWidth = image.getWidth();
        if (attrMap != null && attrMap.size() == 2) {
            String size = attrMap.get("size");
            String[] split = size.split("x");
            if (split.length == 2) {
                imageWidth = Integer.valueOf(split[0]);
                imageHeight = Integer.valueOf(split[1]);
            }
        }
        // 原始图片长或宽小于需要的图片大小,不符合要求 // 宽高比超过阈值
        if ((double) imageWidth / (double) imageHeight > WIDTH_HEIGHT_RATIO) {
            return false;
        }
        return imageWidth >= WIDTH_RATIO && imageHeight >= HEIGHT_RATIO;
    }

    /**
     * 获取图片
     *
     * @param imageUrl 图片地址
     * @param imgInfo  图片对象
     * @return
     */
    private Image getImageX(String imageUrl, ImgInfo imgInfo) {
        Image image = null;
        Map<byte[], String> imgMap = imageGet(imageUrl);
        for (byte[] bytes : imgMap.keySet()) {
            if (bytes != null && bytes.length > 0) {
                imgInfo.setType(imgMap.get(bytes));
                imgInfo.setImageLength(bytes.length + "");
                image = new Image(bytes);
                // 图片长*宽
                imgInfo.setImageSize(image.getWidth() + "×" + image.getHeight());
            }
        }
        return image;
    }


    /**
     * 处理图片 裁剪 缩放 加水印等
     *
     * @param image      原始图片
     * @param purpWidth  目标宽度
     * @param purpHeight 目标高度
     * @return
     */
    private BufferedImage disposeImage(Image image, int purpWidth, int purpHeight) {
        Image copy = image.copy();
        BufferedImage bufferedImage = null;
        int imageWidth = copy.getWidth();
        int imageHeight = copy.getHeight();
        if (imageWidth < purpHeight || imageHeight < purpHeight) {
            logger.error("图片实际宽高小于目标宽高,不能进行裁切缩放.");
            return null;
        }
        // 原始图片的宽高比
        double imageRatio = (double) imageWidth / (double) imageHeight;
        // 生成缩略图的宽高比
        double newThumbnailRatio = (double) purpWidth / (double) purpHeight;
        if (imageRatio > newThumbnailRatio) {
            //图片宽高比大于缩略图的宽高比，高不变，宽度进行截取，取中
            int newImageWidth = (int) (newThumbnailRatio * imageHeight);
            //裁切
            copy.crop((imageWidth - newImageWidth) / 2, 0, newImageWidth, imageHeight);
            //缩放到指定宽高
            copy.setWidth(purpWidth);
            copy.setHeight(purpHeight);
            bufferedImage = copy.getBufferedImage();
        } else if (imageRatio < newThumbnailRatio) {
            //图片宽高比小于缩略图的宽高比，宽不变，高度进行截取，取中
            int newImageHeight = (int) (imageWidth / newThumbnailRatio);
            //裁切
            copy.crop(0, (imageHeight - newImageHeight) / 4, imageWidth, newImageHeight);
            //缩放到指定宽高
            copy.setWidth(purpWidth);
            copy.setHeight(purpHeight);
            bufferedImage = copy.getBufferedImage();
        }
        return bufferedImage;
    }

    /**
     * 获取图片base64
     *
     * @param bi 图片对象BufferedImage
     * @return 返回图片base64码
     */
    private String getImageBase64(BufferedImage bi, String suffix) {
        ByteArrayOutputStream output = null;
        try {
            output = new ByteArrayOutputStream();
            ImageIO.write(bi, suffix.replaceFirst("\\.", ""), output);
            byte[] bytes = output.toByteArray();
            // 去除空格回车换行
            return Base64.encodeBase64String(bytes).trim().replaceAll("[\\t\\n\\r]", "");
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return "";
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    /**
     * 获取图片流
     */
    private Map<byte[], String> imageGet(String url) {

        Map<byte[], String> result = new HashMap<>();

        Request.Builder builder = new Request.Builder().url(url);
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
        builder.header("Accept-Language", "zh-CN,zh;q=0.8");
        builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        Request request = builder.build();
        try {
            Response response = MyOkHttpClient.imgOkHttpClient.newCall(request).execute();
            int statusCode = response.code();
            if (statusCode == 200) {
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
                    ResponseBody body = response.body();
                    byte[] bytes = null;
                    if (body != null) {
                        InputStream inStream = body.byteStream();
                        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                        byte[] buff = new byte[1024];
                        int len;
                        while ((len = inStream.read(buff)) != -1) {
                            outStream.write(buff, 0, len);
                            //限制文件大小
                            if (outStream.size() > IMAGE_SIZE) {
                                bytes = "".getBytes();
                                break;
                            }
                        }
                        if (bytes == null) {
                            bytes = outStream.toByteArray();
                            result.put(bytes, mimeMap.get(contentType));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return result;
    }
}

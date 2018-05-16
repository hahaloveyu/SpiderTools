package com.toptime.webspider;

import com.toptime.webspider.util.SecurityUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * 测试类
 *
 * @author bjoso
 * @date 2018/4/17
 */
public class ToolsTest {
    private static final String KEY = "webSpider";

    public static void main(String[] args) throws IOException {

        String autoformat = FileUtils.readFileToString(new File("config/autoformat.json"), "utf-8");

        /*
         * 加密
         */
        System.out.println("加密后的密文是:" + SecurityUtils.aesEncode(KEY, autoformat));

        /*
         * 解密
         */
        System.out.println("解密后的明文是:" + SecurityUtils.aesDecode(KEY, "BLvGxkZV7KwMW0FTmzZajA=="));
    }
}

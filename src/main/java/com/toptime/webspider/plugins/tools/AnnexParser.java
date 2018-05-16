package com.toptime.webspider.plugins.tools;

import com.toptime.webspider.core.tools.DocumentExtractor;
import com.toptime.webspider.entity.Attachment;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 附件解析接口实现
 * Created by bjoso on 2017/7/10.
 */
public class AnnexParser {

    private static Logger logger = Logger.getLogger(AnnexParser.class);

    /**
     * 文档扩展名
     */
    private static final String DOC_EXTENSION_NAME = ".doc.docx.ppt.pptx.xls.xlsx.pdf";
    /**
     * 压缩文档扩展名
     */
    private static final String COMPRESSION_EXTENSION_NAME = ".zip.rar";

    /**
     * 附件解析
     */
    public List<Attachment> annexParser(byte[] bytes, String suffix) {
        List<Attachment> documents = new ArrayList<>();
        try {
            if (DOC_EXTENSION_NAME.contains(suffix)) {
                if (".pdf".equals(suffix)) {
                    documents.add(DocumentExtractor.pdfExtractor(bytes));
                } else {
                    documents.add(DocumentExtractor.officeExtractor(bytes));
                }
            } else if (COMPRESSION_EXTENSION_NAME.contains(suffix)) {

            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return documents;
    }
}

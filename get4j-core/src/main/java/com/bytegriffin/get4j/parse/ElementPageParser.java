package com.bytegriffin.get4j.parse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;

/**
 * 针对页面中的某一个元素的解析器<br>
 * 这个Parser主要用于长期获取网页上的某个值<br>
 * 而不是一堆值，一堆值需要自己写解析类<br>
 * 默认支持Jsoup的css选择器和正则表达式
 */
public class ElementPageParser implements PageParser {

    private static final Logger logger = LogManager.getLogger(ElementPageParser.class);

    private String elementSeletor;

    ElementPageParser(String elementSeletor) {
        this.elementSeletor = elementSeletor;
    }

    @Override
    public void parse(Page page) {
        String text = UrlAnalyzer.selectPageText(page, elementSeletor);
        if (page.isHtmlContent()) {
        	page.setHtmlContent(text);
        } else if (page.isJsonContent()) {
            page.setJsonContent(text);
        } else if (page.isXmlContent()) {
        	page.setXmlContent(text);
        }
        logger.info("线程[" + Thread.currentThread().getName() + "]解析种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }

}

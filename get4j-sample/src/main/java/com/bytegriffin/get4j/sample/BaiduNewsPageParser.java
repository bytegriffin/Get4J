package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class BaiduNewsPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getCharset() + "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://news.baidu.com/##")
                .parser(BaiduNewsPageParser.class)
                .thread(1).start();
    }

}

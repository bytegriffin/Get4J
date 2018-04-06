package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 网易新闻
 */
public class WangyiPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() + "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://www.163.com").parser(WangyiPageParser.class)
                .defaultUserAgent().thread(1).start();
    }

}

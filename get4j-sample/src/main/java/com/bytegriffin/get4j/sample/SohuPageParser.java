package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class SohuPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() + "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://www.sohu.com/").parser(SohuPageParser.class).defaultUserAgent()
                .thread(1).start();
    }

}

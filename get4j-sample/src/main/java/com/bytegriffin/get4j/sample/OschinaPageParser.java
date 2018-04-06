package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 开源中国社区
 */
public class OschinaPageParser  implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://www.oschina.net/action/ajax/get_more_news_list?newsType=project&p={1}").defaultUserAgent()
        				.detailSelector("a.title[href]").parser(OschinaPageParser.class).thread(1).start();
    }

}

package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class ZhaopinPageParser  implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() );
    }

    /**
     * http链接自动跳转到https
     * @param args String
     * @throws Exception Exception
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://fe-api.zhaopin.com/c/i/sou?start=90&pageSize=10&cityId=530").defaultUserAgent()
        	.detailLinkSelector("$.data.results.positionURL").parser(ZhaopinPageParser.class).thread(1).start();
    }

}
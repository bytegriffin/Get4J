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
        Spider.list_detail().fetchUrl("http://sou.zhaopin.com/jobs/searchresult.ashx?jl=北京&p={1}").defaultUserAgent()
        	.detailSelector("td.zwmc > div > a[href]").parser(ZhaopinPageParser.class).thread(1).start();
    }

}
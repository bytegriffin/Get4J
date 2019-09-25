package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class WeiboPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle());
    }

    public static void main(String[] args) throws Exception {
        Spider.cascade().fetchUrl("http://weibo.com/login.php").parser(WeiboPageParser.class)
        		.javascriptSupport(true).defaultUserAgent().thread(1).start();
    }

}

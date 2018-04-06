package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class DoubanPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle() + "  " + page.getUrl());
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://www.douban.com/explore/").detailSelector("div.title>a[href]").parser(DoubanPageParser.class)
                .thread(1).start();
    }

}
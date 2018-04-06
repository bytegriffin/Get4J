package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class CsdnBlogPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle() + "  " + page.getUrl());
    }

    public static void main(String[] args) throws Exception {//blog_list clearfix
        Spider.list_detail().fetchUrl("http://blog.csdn.net/?&page={1}").sleepRange(3, 5)
                .resourceSelector("img.head[src]").totalPages(1).detailSelector("h2.csdn-tracking-statistics>a[href]")
                .parser(CsdnBlogPageParser.class).defaultUserAgent()
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                //.mongodb("mongodb://localhost:27017")
                .thread(1).start();
    }

}

package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * ChinaUnix博客页面解析
 */
public class ChinaunixBlogPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println("博客标题：" + page.getTitle() + " 博客分类：" + page.jsoupText("div.tit0307_1 > p"));
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://blog.chinaunix.net/site/index/page/{1}.html")
                .totalPages(1).detailLinkSelector("div.two_cont2_1>a[href]").parser(ChinaunixBlogPageParser.class).defaultUserAgent()
                .resourceSelector("div.classify_con1")
                //.scp("192.168.1.11", "roo", "/home/roo/桌面", 22)
                //.defaultProbe()
                //.probe("div.classify_middle1", 30)
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(1).start();
    }

}

package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.annotation.Field;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * ChinaUnix博客页面解析
 */
public class ChinaunixBlogPageParser implements PageParser {
	
	@Field("div.Blog_con3")
	private String catalog ;
	@Field("div.Blog_tit4.Blog_tit5 > em")
	private String time;

    @Override
    public void parse(Page page) {
        String title = page.getTitle();
        System.err.println(" 博客标题：" + title + " 博客avatar：" + page.getAvatar());
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://blog.chinaunix.net/site/index/page/{1}.html")
                .totalPages(1).detailSelector("div.two_cont2_1>a[href]").parser(ChinaunixBlogPageParser.class).defaultUserAgent()
                .resourceSelector("div.classify_con1")
                .defaultDownloadDisk()
                //.scp("192.168.1.11", "roo", "/home/roo/桌面", 22)
                //.defaultProbe()
                //.probe("div.classify_middle1", 30)
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                .thread(1).start();
    }

}

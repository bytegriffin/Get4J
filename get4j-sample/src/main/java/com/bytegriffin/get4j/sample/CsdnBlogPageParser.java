package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class CsdnBlogPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	if(page.isListPage()){ //第一次返回的是列表页数据
    		System.err.println(page.getUrl()+ "  " + page.json("$..channel"));
    	} else { //之后返回的是详情页数据
    		System.err.println(page.getUrl() + "  ");
    	}
    }

    public static void main(String[] args) throws Exception {//blog_list clearfix
        Spider.list_detail().fetchUrl("https://blog.csdn.net/api/articles?type=more&category=home&shown_offset=ad1531325255862708")
                .detailLinkSelector("$.articles.url")
                .parser(CsdnBlogPageParser.class).defaultUserAgent()
                .probe("$.articles.id", 10)
                //.jdbc("jdbc:mysql://localhost:3306/get4j?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&user=root&password=root")
                //.mongodb("mongodb://localhost:27017")
                .thread(1).start();
    }

}

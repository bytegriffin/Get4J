package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 36氪
 */
public class SanLiuKr implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://36kr.com/api/info-flow/main_site/posts?b_id=507750{1}&per_page=20")
        		.detailSelector("http://36kr.com/p/$.data.items[*].id")
                .parser(SanLiuKr.class).thread(1).start();
    }

}
package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 当当网
 */
public class DangdangPageParser  implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println("书名："+page.getTitle() + "  详情页：" + page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://bang.dangdang.com/books/bestsellers/1-{1}").detailSelector("div.name > a[href]")
                .parser(DangdangPageParser.class).thread(1).start();
    }

}
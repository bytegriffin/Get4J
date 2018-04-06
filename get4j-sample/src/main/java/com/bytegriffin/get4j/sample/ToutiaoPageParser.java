package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 今日头条爬虫
 */
public class ToutiaoPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println("页面标题："+page.getTitle()+" 页面链接:"+page.getUrl());
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://www.toutiao.com/api/pc/feed/?max_behot_time=1523000220&category=__all__&utm_source=toutiao&widen=1&tadrequire=false&as=A1951A5C273285F&cp=5AC76218657F8E1&_signature=sSIE6xAb6.M-iH1Yb7d--rEiBP")
        	.detailSelector("https://www.toutiao.com/$.data[*].source_url").defaultUserAgent()
        	.parser(ToutiaoPageParser.class).thread(1).start();
    }

}
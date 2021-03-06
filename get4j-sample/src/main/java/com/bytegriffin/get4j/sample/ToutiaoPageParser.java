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
    	if(page.isListPage()){ //第一次返回的是列表页数据
    		System.err.println(page.json("$.data[*].item_id"));
    	} else { //之后返回的是详情页数据
    		System.err.println(page.getTitle() + "  " +page.getUrl());
    	}
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://www.toutiao.com/api/pc/feed/?max_behot_time=1531379657&category=__all__&utm_source=toutiao&widen=1")
        	.detailLinkSelector("https://www.toutiao.com/a$.data[*].group_id")
        	.defaultUserAgent().parser(ToutiaoPageParser.class).thread(1).start();
    }

}
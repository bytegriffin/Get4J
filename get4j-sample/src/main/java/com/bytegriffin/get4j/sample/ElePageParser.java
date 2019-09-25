package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 饿了么 商铺信息
 */
public class ElePageParser  implements PageParser {

    @Override
    public void parse(Page page) {
    	Object eles = page.json("$.name");
    	if(eles == null){
    		return;
    	}
        System.err.println("商铺名称："+eles.toString()+"   "+page.getUrl());
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://mainsite-restapi.ele.me/shopping/restaurants?latitude=40.0036&limit=24&longitude=116.32697&offset=264")
                .detailLinkSelector("https://mainsite-restapi.ele.me/shopping/restaurant/$..id").parser(ElePageParser.class)
                .thread(3).start();
    }

}
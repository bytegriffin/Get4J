package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 点评网商铺信息
 */
public class DianpingShopPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	System.err.println( "商铺名称：" + page.getTitle()+ "  商铺地址：" +page.getUrl());
    }

    /**
     * @param args String[]
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://www.dianping.com/beijing/ch10/p{1}").parser(DianpingShopPageParser.class)
                .detailLinkSelector("a[data-click-name=shop_title_click]").defaultUserAgent().sleep(2).thread(1).start();

    }

}

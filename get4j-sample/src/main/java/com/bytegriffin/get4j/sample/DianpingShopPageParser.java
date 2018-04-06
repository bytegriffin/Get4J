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
    	Object obj = page.json("$.msg.shopInfo.shopName");
    	if(obj != null){
    		String shopName = obj.toString();
    		String address = page.json("$.msg.shopInfo.shopName").toString();
    		String phoneNo = page.json("$.msg.shopInfo.phoneNo").toString();
    		System.err.println( "商铺名称：" + shopName+ "  商铺地址：" + address + "  联系电话："+phoneNo);
    	}
    }

    /**
     * html列表页中包含的json详情页所需id
     * @param args String[]
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://www.dianping.com/search/category/2/10/p{1}").parser(DianpingShopPageParser.class)
                .detailSelector("(http://www.dianping.com/ajax/json/shopfood/wizard/BasicHideInfoAjaxFP?shopId=)a.o-favor.J_o-favor[data-fav-referid]").defaultUserAgent().thread(1).start();

    }

}

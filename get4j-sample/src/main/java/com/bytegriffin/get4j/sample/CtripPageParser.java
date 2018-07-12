package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 携程网 --- 酒店搜索
 */
public class CtripPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println("酒店名称："+page.getTitle() + " 酒店地址： " + page.getUrl());
    }

    /**
     * Json数据中包含html属性
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://hotels.ctrip.com/Domestic/Tool/AjaxHotelList.aspx?cityId=1&page={1}")
                .detailSelector("http://hotels.ctrip.com/$.hotelPositionJSON.url").parser(CtripPageParser.class)
                .thread(1).start();
    }

}

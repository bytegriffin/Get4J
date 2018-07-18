package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 美团
 */
public class MeituanPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://bj.meituan.com/meishi/api/poi/getPoiList?cityName=北京"
        		+ "&cateId=0&areaId=0&sort=&dinnerCountAttrId=&page=1&userId=&uuid=aceb826eb6e5443eb2a9.1531401169.1.0.0"
        		+ "&platform=1&partner=126&originUrl=http%3A%2F%2Fbj.meituan.com%2Fmeishi%2F&riskLevel=1&optimusCode=1"
        		+ "&_token=eJxtT11vgkAQ%2FC%2F3WiJ3gAV8E6%2FUzwCiKDY%2BcMepeFIFThCb%2FveeiX1o0mSTmd2Z2ex%2BgXKUgh6C0I"
        		+ "ZQATUrQQ%2BgDuy8AgWISipdHVmmbeu6bhgKoH9nBuwqgJQRBr2PrmkqtmZuH4O57D%2BQrUEFQQtulV9uWFtFM2Q9XCNpAgch"
        		+ "Lj1VJcdOzjJxTT479JyrkleHTJU3%2FK8Dmc8XMi%2BRPzF5ovjtZ%2FIVuaHK9p%2BSsXFzuhMkmns%2FcJiTrqcDPnd5GO"
        		+ "9nVf%2B0dPCQboxBmF6I7w5vre%2BUN%2FyStfPBYuxXnMUXv24sHzN8OB42JEE5x82wDf3ZPYO6ptbrrrVLMlLzmAX1op1S"
        		+ "Lz%2B%2FjSK64F4M5%2Bcod0hbmvdNFAas0AorOVP%2BTlq2L64TCIsoK7GFMq85vbllfjJdzIJQG6%2FiSYo8NmPC6y9XhT4d1"
        		+ "CajCYkbY3dc2vioadO1GFb0HYsLZLa%2FutWhexUEmXu65svUAN8%2FxMCUqA%3D%3D")
        		.detailSelector("http://www.meituan.com/meishi/$.data.poiInfos.poiId")
        		.parser(MeituanPageParser.class).defaultUserAgent().thread(1).start();
    }

}

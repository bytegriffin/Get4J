package com.bytegriffin.get4j.sample;

import java.util.List;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;
import com.google.common.base.Splitter;

/**
 * 美团
 */
public class MeituanPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	String ids = page.json("$.data.searchresult[*].poiid").replace("[", "").replace("]", "");
    	String titles = page.json("$.data.searchresult[*].name").replace("[", "").replace("]", "");
    	List<String> idList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(ids);
    	List<String> titleList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(titles);
    	for(int n=0; n<idList.size(); n++) {
            System.err.println("标题："+titleList.get(n).replace("\"", "")+"   链接：https://hotel.meituan.com/"+idList.get(n)+".html" );
    	}
    }

    public static void main(String[] args) throws Exception {
        Spider.single().fetchUrl("https://ihotel.meituan.com/hbsearch/HotelSearch?utm_medium=pc&version_name=999.9&cateId=20&attr_28=129"
        		+ "&uuid=F70F41EB5A441F4447F12F063A5E74716D665BD271897405435D7A9B691351B8%401569120759715"
        		+ "&cityId=1&offset=40&limit=10&startDay=20190922&endDay=20190922&q=&sort=defaults"
        		+ "&X-FOR-WITH=dmzz4Tt0X37QSkv0aBrMRdhm25ElRDngPWUlMwOm7eWq5jYjz6iu9uLTAWsalUhnkmTcHhyphn"
        		+ "1WHQxE5CuOqa26XvDQyccqsGHAHwJ8TvlncVkMVdes62jk%2FSx6XGn7GkrezaBPXF4XpecHmT1Khg%3D%3D")
        		//.detailSelector("https://hotel.meituan.com/$.data.searchresult[*].poiid")
        		.parser(MeituanPageParser.class).defaultUserAgent().thread(1).start();
    }

}

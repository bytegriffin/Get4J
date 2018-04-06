package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 京东商品
 */
public class JDPageParser implements PageParser {

	@Override
	public void parse(Page page) {
		System.err.println(page.getTitle()+ "   "+page.getUrl()  );
	}

	public static void main(String[] args) throws Exception {
		// 京东商品列表
//       Spider.list_detail().fetchUrl("https://list.jd.com/list.html?cat=670,671,672&page={1}")
//                .detailSelector("div.p-name > a[href]").parser(JDPageParser.class).thread(1).start();
       
		// 京东搜索列表
       Spider.list_detail().fetchUrl("http://search.jd.com/Search?keyword=T%E6%81%A4&enc=utf-8&qrst=1&rt=1&stop=1&vt=2&page=1&s=52&click=0")
       		.detailSelector("li.gl-item>div.gl-i-wrap>div.p-name.p-name-type-2 > a[href]").parser(JDPageParser.class).thread(1).start();
    }

}

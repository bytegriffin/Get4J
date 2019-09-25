package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 链家网-租房模块
 */
public class LianjiazufangPageParser implements PageParser {

	@Override
	public void parse(Page page) {
	    System.err.println(page.getTitle()+" "+page.getUrl());
	}
	
	 public static void main(String[] args) throws Exception {
	        Spider.list_detail().fetchUrl("https://m.lianjia.com/bj/zufang/pg{1}/")
	                .totalPages("1").detailLinkSelector("a[data-el=jumpDetailEl][href]")
	                .parser(LianjiazufangPageParser.class).defaultUserAgent().thread(1).start();
	    }

}

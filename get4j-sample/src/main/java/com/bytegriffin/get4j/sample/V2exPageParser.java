package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;

public class V2exPageParser {

	 public static void main(String[] args) throws Exception {
	        Spider.list_detail().fetchUrl("https://www.v2ex.com/").detailLinkSelector("span.item_title > a[href]")
	               .parser(page -> System.err.println(page.getTitle()+"   "+page.getUrl())) .thread(1).start();
	 }

}

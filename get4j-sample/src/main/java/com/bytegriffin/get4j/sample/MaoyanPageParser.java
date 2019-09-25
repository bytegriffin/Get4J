package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 猫眼电影
 */
public class MaoyanPageParser implements PageParser {

	@Override
	public void parse(Page page) {
	    System.err.println(page.getTitle()+"  "+page.getUrl());
	}

	 public static void main(String[] args) throws Exception {
	        Spider.list_detail().fetchUrl("http://maoyan.com/films?offset={0}") .totalPages("1")
	        		.detailLinkSelector("div.channel-detail.movie-item-title > a[href]").parser(MaoyanPageParser.class)
	                .defaultUserAgent().thread(1).start();
	    }

}

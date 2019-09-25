package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * YY Live - music
 */
public class YYPageParser implements PageParser {

	@Override
	public void parse(Page page) {
		System.err.println("直播间名称："+page.getTitle()+"直播网页地址:"+page.getUrl());
	}

	public static void main(String[] args) throws Exception {
		Spider.list_detail().fetchUrl("http://www.yy.com/music/").parser(YYPageParser.class).detailLinkSelector("a.box[href]")
				.javascriptSupport(false).defaultUserAgent().thread(1).start();
	}

}
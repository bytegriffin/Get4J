package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 网易云音乐
 */
public class Music163PageParser implements PageParser {

	@Override
	public void parse(Page page) {
		if(page.isListPage()) {
			System.err.println("用户自定义专辑页面title："+page.getTitle()+" 地址："+page.getUrl());
		} else {
			String geming = page.jsoupHtml("span.txt > a");
			System.err.println(geming);
		}
		
	}

	public static void main(String[] args) throws Exception {
		// 此地址是iframe地址，不是外部显示的地址
		Spider.list_detail().fetchUrl("http://music.163.com/discover/playlist/?order=hot&cat=全部&limit=35&offset=0")
				.detailSelector("a.tit.f-thide.s-fc0[href]")
				.parser(Music163PageParser.class).thread(1).start();
	}

}

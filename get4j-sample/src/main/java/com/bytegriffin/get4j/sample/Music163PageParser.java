package com.bytegriffin.get4j.sample;

import org.jsoup.select.Elements;

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
			System.err.print("专辑名称："+page.getTitle());
			Elements eles = page.jsoup("ul.f-hide > li > a[href]");
			eles.forEach(e -> {
				System.err.println("歌曲名称："+e.text() +"  歌曲链接：http://music.163.com"+e.attr("href"));
			});
		}
		
	}

	public static void main(String[] args) throws Exception {
		// 此地址是iframe地址，不是外部显示的地址
		Spider.list_detail().fetchUrl("http://music.163.com/discover/playlist/?order=hot&cat=全部&limit=35&offset=0")
				.detailSelector("a.tit.f-thide.s-fc0[href]")
				.parser(Music163PageParser.class).thread(1).start();
	}

}

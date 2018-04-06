package com.bytegriffin.get4j.sample;

import org.jsoup.nodes.Element;
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
		System.err.println("用户自定义专辑页面title："+page.getTitle()+"---用户头像地址："+page.getUrl());
		Elements eles = page.jsoup("div.u-cover.u-cover-1 > a");
		for(int i=0; i<eles.size(); i++ ){
			Element ele = eles.get(i);
			String geming = ele.select("a[href]").text();
			String url = "http://music.163.com"+ele.attr("href");
			System.out.println("歌名："+geming+" 歌曲地址："+url);
		}
		
	}

	public static void main(String[] args) throws Exception {
		// 此地址是iframe地址，不是外部显示的地址
		Spider.list_detail().fetchUrl("http://music.163.com/discover/playlist/?order=hot&cat=全部&limit=35&offset=0")
				.totalPages("1").detailSelector("a.msk[href]").resourceSelector("div.u-cover.u-cover-1").parser(Music163PageParser.class)
				.thread(1).start();
	}

}

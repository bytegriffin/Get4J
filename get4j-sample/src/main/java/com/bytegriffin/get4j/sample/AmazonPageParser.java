package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 亚马逊---图书销售排行榜
 */
public class AmazonPageParser implements PageParser {

	@Override
	public void parse(Page page) {
		//利用Jsoup解析详情页面中的html元素
		String time = page.jsoupText("span.a-size-medium.a-color-secondary.a-text-normal");
		System.err.println("书名" + page.getTitle() + "  图书地址: " + page.getUrl() + " 出版时间：" + time);
	}

	public static void main(String[] args) throws Exception {
		Spider.list_detail().fetchUrl("https://www.amazon.cn/gp/bestsellers/books/ref=zg_bs_books_pg_1?ie=UTF8&pg={1}")
				.detailSelector("a.a-link-normal[href]").parser(AmazonPageParser.class).thread(1).start();
	}

}

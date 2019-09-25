package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 亚马逊---图书销售排行榜
 * List_Detail页面抓取案例
 */
public class AmazonPageParser implements PageParser {

	/**
     * 解析页面
     *
     * @param Page 详情页对象/第一次返回的是列表页对象
     */
	@Override
	public void parse(Page page) {
		if(page.isListPage()){ //第一次返回的是列表页数据
			System.err.println("列表页面: " + page.getTitle() + " 列表地址: " + page.getUrl());
		} else { //之后返回的是详情页数据
			System.err.println("详情页面书名: " + page.getTitle() + " 图书详情地址: " + page.getUrl());
		}
		
	}

	public static void main(String[] args) throws Exception {
		Spider.list_detail().fetchUrl("https://www.amazon.cn/gp/bestsellers/digital-text/ref=zg_bs_pg?ie=UTF8&pg={1}")
				.totalPages(1).detailLinkSelector("a.a-link-normal[href]").parser(AmazonPageParser.class).thread(2).start();
	}

}

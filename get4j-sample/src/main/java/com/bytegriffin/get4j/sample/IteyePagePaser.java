package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * iteye问答抓取
 */
public class IteyePagePaser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println("问题："+page.getTitle() +"  页面链接： "+page.getUrl());
    }

    /**
     * 自动跳转到一个相对路径的url
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://www.iteye.com/ask")
                .detailSelector("div.summary>h3>a[href]").parser(IteyePagePaser.class)
                .thread(3).start();
    }

}

package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class TudouPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle()+" "+page.getUrl());
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://new.tudou.com/sec/10016?spm=a2h28.8313475.top.dtab")
        	.detailLinkSelector("div.td_pc-card > a[href]").parser(TudouPageParser.class).thread(1).start();
    }

}

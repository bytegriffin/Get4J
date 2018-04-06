package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class GithubPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	String content = page.jsoupText("div.readme");
    	System.err.println("页面title：["+page.getTitle() + "] Get4J页面上的简介: " + content );
    }

    public static void main(String[] args) throws Exception {
        Spider.single().fetchUrl("https://github.com/bytegriffin/Get4J")
            .parser(GithubPageParser.class).thread(1).start();
    }

}
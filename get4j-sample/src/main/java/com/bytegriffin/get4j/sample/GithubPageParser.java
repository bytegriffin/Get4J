package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class GithubPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	String content = page.jsoupText("#readme");
    	System.err.println("Get4J内容："+content);
    }

    public static void main(String[] args) throws Exception {
       Spider.single().fetchUrl("https://github.com/bytegriffin/Get4J")
            .parser(GithubPageParser.class).thread(1).start();
    }

}
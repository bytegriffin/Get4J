package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.annotation.Field;
import com.bytegriffin.get4j.annotation.Single;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

@Single(url="https://github.com/bytegriffin/Get4J",thread=1)
public class GithubAnnotationPagePaser implements PageParser {

	@Field("div.readme")
	private String content;

    @Override
    public void parse(Page page) {
    	System.err.println("Get4J内容："+page.getField("content"));
    }

    public static void main(String[] args) throws Exception {
       Spider.annotation(GithubAnnotationPagePaser.class).start();
    }

}

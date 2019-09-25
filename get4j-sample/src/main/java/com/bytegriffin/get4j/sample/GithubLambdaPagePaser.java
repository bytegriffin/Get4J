package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;

public class GithubLambdaPagePaser {

	public static void main(String[] args) throws Exception {
		Spider.single().fetchUrl("https://github.com/bytegriffin/Get4J")
			.parser(page -> {System.err.println("Get4J内容: " + page.jsoupText("div.readme"));})
			.thread(1).start();
	}

}

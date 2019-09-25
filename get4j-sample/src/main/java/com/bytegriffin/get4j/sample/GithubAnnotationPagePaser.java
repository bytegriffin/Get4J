package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.annotation.Field;
import com.bytegriffin.get4j.annotation.Single;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * Annotation使用方法：<br>
 * 1.显示调用annotation(class)方法。 <br>
 * 2.在类上必须声明@Single或者@ListDetail等注解。 <br>
 * 3.不能单独在字段上调用Field注解，必须配合使用。 <br>
 * @author bytegriffin
 */
@Single(url="https://github.com/bytegriffin/Get4J",thread=1)
public class GithubAnnotationPagePaser implements PageParser {

	@Field("#readme")
	private String content;

    @Override
    public void parse(Page page) {
    	System.err.println("Get4J内容："+page.getField("content"));
    }

    public static void main(String[] args) throws Exception {
       Spider.annotation(GithubAnnotationPagePaser.class).start();
    }

}

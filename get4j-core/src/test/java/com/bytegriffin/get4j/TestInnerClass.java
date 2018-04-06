package com.bytegriffin.get4j;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 测试内部类
 * 内部解析类必须加上public static
 */
public class TestInnerClass {

    @Site(url = "http://www.baidu.com/")
    public static class InnerClass implements PageParser {

        @Override
        public void parse(Page page) {
            System.out.println(page.getAvatar());
        }

    }

    public static void main(String[] args) throws Exception {
        Spider.annotation(InnerClass.class).start();
    }

}

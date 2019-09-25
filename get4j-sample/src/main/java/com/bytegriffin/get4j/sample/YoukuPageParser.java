package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class YoukuPageParser  implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl()+ "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://list.youku.com/category/video/c_0_d_1_s_1_p_{1}.html?spm=a2h1n.8251846.0.0").detailLinkSelector("li.title > a[href]")
               .parser(YoukuPageParser.class).thread(1).start();
    }

}
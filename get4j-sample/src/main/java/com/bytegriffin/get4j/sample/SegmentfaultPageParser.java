package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class SegmentfaultPageParser implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println(page.getTitle() + "   " + page.getUrl()+ "  " );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://segmentfault.com/questions?page={1}").detailSelector("h2.title > a[href]")
               .parser(SegmentfaultPageParser.class).thread(1).start();
    }

}

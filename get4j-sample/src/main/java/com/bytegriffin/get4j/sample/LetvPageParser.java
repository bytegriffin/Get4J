package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 乐视-最新电影
 */
public class LetvPageParser   implements PageParser {
	
    @Override
    public void parse(Page page) {
        System.err.println( page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://rec.letv.com/pcw?action=more&pageid=page_cms1003337280&fragid=8168&num=10")
        		.detailSelector("http://www.le.com/ptv/vplay/$.rec[0].videos.vid")
                .parser(LetvPageParser.class).thread(1).start();
    }

}
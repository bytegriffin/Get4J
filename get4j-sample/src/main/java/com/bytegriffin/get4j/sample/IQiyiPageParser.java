package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 爱奇异---热门电视剧
 */
public class IQiyiPageParser  implements PageParser {

    @Override
    public void parse(Page page) {
       System.err.println("电视剧名称："+page.getTitle() + "   网页链接：" + page.getUrl() );
    }

    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("http://list.iqiyi.com/www/2/-------------11-{1}-1-iqiyi--.html").detailSelector("p.site-piclist_info_title > a[href]")
                .parser(IQiyiPageParser.class).thread(1).start();
    }

}
package com.bytegriffin.get4j.sample;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 虎嗅网
 */
public class HuxiuPageParser implements PageParser {

    @Override
    public void parse(Page page) {
        System.err.println(page.getTitle() + "   " + page.getMethod()+ "  " + page.getUrl() + "  ");
    }

    /**
     * 接口内容：Json内容中包含着Html
     * @param args String[] 
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        Spider.list_detail().fetchUrl("https://www.huxiu.com/v2_action/article_list?page={1}")
        		.parser(HuxiuPageParser.class).defaultUserAgent().post().sleep(3)
                .defaultUserAgent().detailLinkSelector("$.data|a.transition.msubstr-row2[href]")
                .thread(1).start();

    }

}
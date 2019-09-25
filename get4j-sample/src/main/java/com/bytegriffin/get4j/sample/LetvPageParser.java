package com.bytegriffin.get4j.sample;

import java.util.List;

import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;
import com.google.common.base.Splitter;

/**
 * 乐视-最新电影 jsonpath解析案例
 */
public class LetvPageParser implements PageParser {

	@Override
    public void parse(Page page) {
    	String ids = page.json("$.rec[0].videos[*].vid").replace("[", "").replace("]", "");
    	String titles = page.json("$.rec[0].videos[*].vidsubtitle").replace("[", "").replace("]", "");
    	List<String> idList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(ids);
    	List<String> titleList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(titles);
    	for(int n=0; n<idList.size(); n++) {
            System.err.println("标题："+titleList.get(n).replace("\"", "")+"   链接：http://www.le.com/ptv/vplay/"+idList.get(n)+".html" );
    	}
    }

	public static void main(String[] args) throws Exception {
		Spider.single().fetchUrl("http://rec.letv.com/pcw?action=more&pageid=page_cms1003337280&fragid=8168&num=10")
				.parser(LetvPageParser.class).thread(1).start();
	}

}
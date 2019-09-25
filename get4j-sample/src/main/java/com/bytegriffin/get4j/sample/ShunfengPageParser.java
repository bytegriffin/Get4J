package com.bytegriffin.get4j.sample;

import java.util.Iterator;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;


/**
 * 顺丰快递--查询单号
 */
public class ShunfengPageParser implements PageParser {

    @Override
    public void parse(Page page) {
    	JSONArray jsonArray = JSONArray.parseArray( page.json("$.data[*]") );
    	
    	 Iterator<Object> iterator = jsonArray.iterator();
    	while(iterator.hasNext()){
    		JSONObject obj = JSONObject.parseObject(iterator.next().toString());
    		System.err.println("时间："+obj.getString("ftime")+"    地点和跟踪进度："+obj.getString("context"));
    	}
       
    }

    public static void main(String[] args) throws Exception {
    	String orderId = "221728091556";//要查询的单号
        Spider.single().fetchUrl("http://www.kuaidi100.com/query?type=shunfeng&postid=" + orderId)
                .parser(ShunfengPageParser.class).thread(1).start();
    }

}

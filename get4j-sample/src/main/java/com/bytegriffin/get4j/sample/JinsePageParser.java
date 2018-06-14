package com.bytegriffin.get4j.sample;

import java.util.Iterator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

/**
 * 金色财经
 * @author bytegriffin
 */
public class JinsePageParser implements PageParser {
	
	/**
	 * 这里可以需要借助fastjson来解析json字符串
	 */
    @Override
    public void parse(Page page) {
        String str = page.json("$.list[0].lives[*]");//根据jsonpath的查询规则来获取所有json字符串数组
        JSONArray jsonArray = JSONArray.parseArray(str);//之后将json数组解析为fastjson对象
        //最后循环便利每条json数据中的具体内容
        for (Iterator<Object> iterator = jsonArray.iterator(); iterator.hasNext();) { 
            JSONObject jsonObject = (JSONObject) iterator.next(); 
            System.err.println(jsonObject.get("id")+"   "+jsonObject.getString("sort"));
            System.err.println(jsonObject.get("content"));
        } 
    }

    /**
     * 抓取这类站点，需要人工去分析下页面中动态数据的实际地址，找到之后为了方便先设置limit为2条，
     * ps：这个网站访问太多会出现“Too Many Attempts.”字样，爬取时需要隔一定时间抓
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Spider.single().fetchUrl("http://api.jinse.com/v4/live/list?limit=2").defaultUserAgent()
                .parser(JinsePageParser.class).thread(1).start(); 
    }


}

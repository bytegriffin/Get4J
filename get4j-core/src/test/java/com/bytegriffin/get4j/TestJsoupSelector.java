package com.bytegriffin.get4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TestJsoupSelector {

    static String url = "http://music.163.com/playlist?id=2235066484";
    static String selector = "ul.f-hide > li > a";

    public static void main(String[] args) throws Exception {
        Document doc = Jsoup.connect(url).get();
        Elements eles = doc.select(selector);
        System.out.println(eles.attr("href"));
    }

}
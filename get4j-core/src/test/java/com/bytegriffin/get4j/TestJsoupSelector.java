package com.bytegriffin.get4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TestJsoupSelector {

    static String url = "http://blog.chinaunix.net/uid-28815788-id-5763497.html";
    static String selector = "div.Blog_tit4.Blog_tit5";

    public static void main(String[] args) throws Exception {
        Document doc = Jsoup.connect(url).get();
        Elements eles = doc.select(selector);
        System.out.println(eles.text());
    }

}
package com.bytegriffin.get4j.parse;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.net.http.HttpProxy;

/**
 * 免费代理解析
 */
public class FreeProxyPageParser implements PageParser {

    public static final String xicidaili = "xicidaili";

    @Override
    public void parse(Page page) {
        String seedName = page.getSeedName();
        List<HttpProxy> proxys = new ArrayList<>();
        if ("xicidaili".equals(seedName)) {
            Elements eles = page.jsoup("tr.odd");
            for (Element e : eles) {
                String ip = e.select("td:eq(1)").text();
                String port = e.select("td:eq(2)").text();
                HttpProxy proxy = new HttpProxy(ip, port);
                proxys.add(proxy);
            }
            page.putField(xicidaili, proxys);
        }
    }

}

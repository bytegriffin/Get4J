package com.bytegriffin.get4j;

import java.util.HashSet;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.bytegriffin.get4j.fetch.FetchResourceSelector;

/**
 * 测试jsonpath解析
 */
public class TestJsonPathParse {

    public TestJsonPathParse() {
        // TODO Auto-generated constructor stub
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        String jsonpath = "$.data..avatar";
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://angularjs.cn/api/article/latest?p=1");
        HttpResponse response = client.execute(request);
        HashSet<String> urls = FetchResourceSelector.jsonPath(EntityUtils.toString(response.getEntity(), Consts.UTF_8), jsonpath, "");
        System.out.println(urls.size());
    }

}

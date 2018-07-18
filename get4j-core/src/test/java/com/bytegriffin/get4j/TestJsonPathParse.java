package com.bytegriffin.get4j;

import java.io.IOException;
import java.util.HashSet;

import com.bytegriffin.get4j.fetch.FetchResourceSelector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 测试jsonpath解析
 */
public class TestJsonPathParse {

    public static void main(String[] args) throws Exception {
        String jsonpath = "$.articles.url";
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder()
        		.url("https://blog.csdn.net/api/articles?type=more&category=home&shown_offset=ad1531325255862708")
                .get().build();
        try {
            Response response = client.newCall(request).execute();
            HashSet<String> urls = FetchResourceSelector.jsonPath(response.body().string(), jsonpath, "");
            System.out.println(urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

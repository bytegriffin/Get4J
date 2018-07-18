package com.bytegriffin.get4j.net.http;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Page;

/**
 * Http引擎<br>
 * 目前有两种：OKHttpClient 和 Selenium
 */
public interface HttpEngine {

    /**
     * 初始化Http引擎配置
     *
     * @param seed 种子对象
     */
    void init(Seed seed);

    /**
     * 探测最新的页面内容
     * @param page 页面对象
     * @return 探测页面内容
     */
    String probePageContent(Page page);

    /**
     * 测试HttpProxy是否可运行，都不可用程序则退出
     *
     * @param url       url
     * @param httpProxy httpProxy
     * @return 测试可行返回true，否则为false
     */
    boolean testHttpProxy(String url, HttpProxy httpProxy);

    /**
     * 设置页面Content、Cookie
     *
     * @param page 页面对象
     * @return page
     */
    Page getPageContent(Page page);

}
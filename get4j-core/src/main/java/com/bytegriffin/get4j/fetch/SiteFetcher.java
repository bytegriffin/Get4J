package com.bytegriffin.get4j.fetch;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.core.UrlQueue;

/**
 * 整站抓取 <br>
 * 如果这个站点里有含其它站点的url，那么这些url是不会被抓取的，是被过滤的<br>
 * 并且默认不支持向上抓取，只抓取本层以及以下的站点，采用的是宽度优先遍历 <br>
 * 例如抓取的站点是www.aaa.com/bbb/，那么就不会抓取www.aaa.com的内容
 */
public class SiteFetcher implements Process {

    private static final Logger logger = LogManager.getLogger(SiteFetcher.class);
    private HttpEngine http = null;

    @Override
    public void init(Seed seed) {
        // 1.获取相应的http引擎
        http = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName());
        // 2.初始化url选择/过滤器缓存
        FetchResourceSelector.init(seed);
        logger.info("种子[" + seed.getSeedName() + "]的组件SiteFetcher的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        // 1.获取并设置Page的Content属性、Cookies属性
        page = http.getPageContent(page);

        // 2.获取并设置Page的Resource属性
        UrlAnalyzer.custom(page).sniffAndSetResources();

        // 3.设置Page其它属性
        page.setFetchTime(DateUtil.getCurrentDate());

        // 4.嗅探出新访问地址并增加新的访问链接交给爬虫队列
        HashSet<String> links = UrlAnalyzer.custom(page).sniffSiteLinks();
        UrlQueue.addUnVisitedLinks(page.getSeedName(), links);

        logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]整站Url总数是[" +UrlQueue.getUnVisitedUrlCount(page.getSeedName()) + "]个。");
        logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }

}

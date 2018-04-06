package com.bytegriffin.get4j.fetch;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

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
 * 级联页面抓取器<br>
 * 和SiteFetch不同，它提供单个页面上的所有url都要抓取，不管是不是外链 <br>
 */
public class CascadeFetcher implements Process {

    private static final Logger logger = LogManager.getLogger(CascadeFetcher.class);
    private static AtomicInteger count = new AtomicInteger();
    private HttpEngine http = null;

    private void increment() {
        count.incrementAndGet();
    }

    private int getCount() {
        return count.get();
    }

    @Override
    public void init(Seed seed) {
        // 1.获取相应的http引擎
        http = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName());

        // 2.初始化url选择/过滤器缓存
        FetchResourceSelector.init(seed);
        logger.info("种子[" + seed.getSeedName() + "]的组件CascadeFetcher的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        // 1.获取并设置Page的Content属性、Cookies属性
        page = http.getPageContent(page);

        // 2.获取并设置Page的Resource属性
        UrlAnalyzer.custom(page).sniffAndSetResources();

        // 3.设置Page其它属性
        page.setFetchTime(DateUtil.getCurrentDate());

        // 4.嗅探出新访问地址并增加（只增加一次，别的页面的url不管）新的访问链接交给爬虫队列
        if (getCount() == 0) {
            HashSet<String> links = UrlAnalyzer.custom(page).sniffAllLinks();
            UrlQueue.addUnVisitedLinks(page.getSeedName(), links);
            increment();
            logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]级联url总数是[" + UrlQueue.getUnVisitedUrlCount(page.getSeedName())  + "]个。");
        }

        logger.info("线程[" + Thread.currentThread().getName() + "]抓取种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }

}

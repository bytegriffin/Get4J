package com.bytegriffin.get4j.fetch;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.DateUtil;
import com.google.common.base.Strings;

/**
 * List-Detail格式的页面抓取器<br>
 */
public class ListDetailFetcher implements Process {

    private static final Logger logger = LogManager.getLogger(ListDetailFetcher.class);
    private HttpEngine http = null;

    /**
     * 存放详情页面链接与avatar资源的映射关系 key：detail_link value：avatar_link
     */
    private Map<String, String> detailLinkAvatar = new HashMap<>();

    @Override
    public void init(Seed seed) {
        // 1.获取相应的http引擎
        http = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName());

        // 2.初始化detail页面选择器
        String detailSelect = seed.getFetchDetailSelector();
        if (!Strings.isNullOrEmpty(detailSelect)) {
            Globals.FETCH_DETAIL_SELECT_CACHE.put(seed.getSeedName(), detailSelect.replace(" ", ""));
        }

        // 3.初始化资源选择器缓存
        FetchResourceSelector.init(seed);
        logger.info("种子[{}]的组件ListDetailFetcher的初始化完成。",seed.getSeedName() );
    }

    @Override
    public void execute(Page page) {

        if (UrlAnalyzer.isAcessListUrl(page)) {
            // 1.获取并设置列表页Page的Content属性
            page = http.getPageContent(page);

            // 2.设置Page其它属性 （detailSelect要先设置）
            page.setFetchTime(DateUtil.getCurrentDate());

            // 3.获取并设置Page的Resource属性
            FetchResourceSelector resourceselector = Globals.FETCH_RESOURCE_SELECTOR_CACHE.get(page.getSeedName());
            String detailSelect = Globals.FETCH_DETAIL_SELECT_CACHE.get(page.getSeedName());
            // 如果fetch.resource.selector配置了all或者none，或者没有配置fetch.detail.selecotr只配置了fetch.resource.selector，那说明没有detaillink与avatar对应，也就是说只能抓取所有resource
            if (resourceselector == null || resourceselector.isConfigAll() || resourceselector.isConfigNone()
                    || Strings.isNullOrEmpty(detailSelect)) {
                UrlAnalyzer.custom(page).sniffAndSetResources();
            } else { // 如果fetch.resource.selector配置了具体参数（一定是包含detaillink与avatar资源的外部的css选择器或正则，而不只是指定avatar），则表示抓取符合参数的具体资源
                detailLinkAvatar = UrlAnalyzer.custom(page).mappingDetailLinkAndAvatar();
            }

            // 4.设置Page的detailPage属性
            LinkedHashSet<String> links = UrlAnalyzer.custom(page).sniffDetailLinks();
            if (links != null && links.size() > 0) {
                UrlQueue.addUnVisitedLinks(page.getSeedName(), links);
                page.setDetailLinks(links);
                logger.info("线程[{}]抓取种子[{}]url[{}]的详情Url总数是[{}]个。",Thread.currentThread().getName(), page.getSeedName(), page.getUrl() ,UrlQueue.getUnVisitedUrlCount(page.getSeedName()) );
            }

        } else {// 访问的是detail url

            // 1.获取并设置详情页DetailPage的HtmlContent或JsonContent属性
            Page detailPage = new Page(page.getSeedName(), page.getUrl());
            detailPage = http.getPageContent(detailPage);

            // 2.设置Detail Link对应的Avatar资源
            if (!detailLinkAvatar.isEmpty()) {
                if (detailLinkAvatar.containsKey(page.getUrl())) {
                    detailPage.setAvatar(detailLinkAvatar.get(page.getUrl()));
                }
            } else {
                // 3.获取并设置详情页DetailPage的Resource属性
                UrlAnalyzer.custom(detailPage).sniffAndSetResources();
            }

            // 4.将详情页面属性指定为传递对象，当Page类增加新属性后此段代码也需要更新
            page.setTitle(detailPage.getTitle());
            page.setAvatar(detailPage.getAvatar());
            page.setCharset(detailPage.getCharset());
            page.setCookies(detailPage.getCookies());
            page.setFetchTime(DateUtil.getCurrentDate());
            page.setHtmlContent(detailPage.getHtmlContent());
            page.setJsonContent(detailPage.getJsonContent());
            page.setXmlContent(detailPage.getXmlContent());
            page.setResources(detailPage.getResources());
            page.setResourceSavePath(detailPage.getResourceSavePath());
            page.setId(detailPage.getId());
            page.setSeedName(detailPage.getSeedName());
            page.setHost(detailPage.getHost());
            page.setUrl(detailPage.getUrl());
            page.setMethod(detailPage.getMethod());
            page.setListPage(false);

            logger.info("线程[{}]抓取种子[{}]的url[{}]完成。", Thread.currentThread().getName(), page.getSeedName() , page.getUrl() );
        }

    }

}

package com.bytegriffin.get4j.probe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.bytegriffin.get4j.util.Sleep;
import com.bytegriffin.get4j.util.StringUtil;
import com.google.common.base.Strings;

/**
 * 页面变化探测器 ：用于再次爬取页面前事先探查页面的具体情况。<br>
 * 一般探查结果有如下几种情况：<br>
 * 1.页面没有任何改变：继续轮询探测<br>
 * 2.页面框架没变，只是新增了动态数据，此时需要爬取<br>
 * 3.页面改版：需要重新设置爬虫<br>
 * 4.页面url不能再被访问：需要查明情况再定<br>
 * 注意：应尽量避免监控整个页面内容，即：fetchProbeSelector值尽量避免设置为default<br>
 * 因为当每次请求动态页面时，页面上都可能会生成一个随机字符串，这样就导致程序判断每次访问页面都发生了变化。
 */
public class PageChangeProber {

    private static final Logger logger = LogManager.getLogger(PageChangeProber.class);
    private HttpEngine http = null;
    private static volatile boolean isrun = true;
    private ProbePage probePage;
    private String fetchProbeSelector;
    private int fetchProbeSleep;
    private Page page;

    public PageChangeProber(Seed seed) {
        init(seed);
    }

    public void init(Seed seed) {
        // 1.设置page对象
        page = new Page(seed.getSeedName(), UrlAnalyzer.formatListDetailUrl(seed.getFetchUrl()), seed.getFetchHttpMethod());
        fetchProbeSelector = seed.getFetchProbeSelector();

        if (DefaultConfig.default_value.equalsIgnoreCase(seed.getFetchProbeSleep()) ||
                Strings.isNullOrEmpty(seed.getFetchProbeSleep()) ||
                !StringUtil.isNumeric(seed.getFetchProbeSleep()) ||
                Integer.valueOf(seed.getFetchProbeSleep()) <= 0) {
            fetchProbeSleep = DefaultConfig.probe_sleep;
        } else {
            fetchProbeSleep = Integer.valueOf(seed.getFetchProbeSleep());
        }

        // 2.创建probe文件夹
        FileUtil.makeDiskFile(DefaultConfig.probe_page_file);

        // 3.缓存ProbePage文件对象
        if (FileUtil.isExistContont(DefaultConfig.probe_page_file)) {
            probePage = ProbeFileStorage.read(UrlAnalyzer.formatListDetailUrl(seed.getFetchUrl()));
        }

        // 4.获取相应的http引擎
        http = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName());
        logger.info("种子[" + seed.getSeedName() + "]的组件PageChangeProber的初始化完成。");
    }

    private void stop() {
        isrun = false;
    }

    public void start() {
        isrun = true;
        setFinish();
    }

    /**
     * 设置抓取完成状态，表示此次抓取过程完全执行
     */
    public void setFinish(){
    	if(page == null){
    		return;
    	}
    	if(ProbeFileStorage.finished.equalsIgnoreCase(probePage.getStatus())){
    		return;
    	}
    	String content = page.getContent();
        if (!DefaultConfig.default_value.equalsIgnoreCase(fetchProbeSelector)) {
            content = UrlAnalyzer.selectPageContent(page, fetchProbeSelector);
        }
        probePage = ProbeFileStorage.update(page.getUrl(), content, ProbeFileStorage.finished);
    }

    public void run() {
        // 1.第一次启动需要判断probe文件中等于url数据是否为空，如果为空的话则表示要先要抓取一次。
        // 注意：此时还可以添加 判断数据库中是否存在与url相等的记录
        if (probePage == null) {
            String content = http.probePageContent(page);
            if (!DefaultConfig.default_value.equalsIgnoreCase(fetchProbeSelector)) {
                content = UrlAnalyzer.selectPageContent(page, fetchProbeSelector);
            }
            probePage = ProbeFileStorage.append(DefaultConfig.probe_page_file, page.getUrl(), content, ProbeFileStorage.un_finish, true);
            return;
        }

        while (isrun) {
            // 2.获取最新页面内容
            String newContent = http.probePageContent(page);
            // 当页面内容为空时，发送Email通知
            if (Strings.isNullOrEmpty(newContent)) {
                stop();
                String msg = "探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]内容为空。";
                logger.error(msg);
                EmailSender.sendMail(msg);
                ExceptionCatcher.addException(page.getSeedName(), msg);
                break;
            }

            String content;
            if (DefaultConfig.default_value.equals(fetchProbeSelector)) {
                content = newContent;
            } else {
                content = UrlAnalyzer.selectPageContent(page, fetchProbeSelector);
            }

            if (Strings.isNullOrEmpty(content)) {
                stop();
                String msg = "探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]页面选择器[" + content + "]出错或者是页面改版。";
                logger.error(msg);
                EmailSender.sendMail(msg);
                ExceptionCatcher.addException(page.getSeedName(), msg);
                break;
            }

            // 3.比对probe文件中存储url页面内容与新抓取的页面内容，
            // 如果相同并且probe文件finsh状态为已完成，则需要继续轮询监控抓取页面
            // 如果不同或者probe文件finish状态为未完成，需要更新probe文件中相同url的content内容 并且 需要抓取新变化的内容
            if (!probePage.getContent().equalsIgnoreCase(MD5Util.convert(content)) || ProbeFileStorage.un_finish.equalsIgnoreCase(probePage.getStatus())) {
                probePage = ProbeFileStorage.update(page.getUrl(), content, ProbeFileStorage.un_finish);
                logger.error("探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]发现新增内容，准备抓取更新操作。。。");
                stop();
                break;
            }
            Sleep.seconds(fetchProbeSleep);
            logger.info("正在探测种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]的页面变化。。。");
        }

    }

}



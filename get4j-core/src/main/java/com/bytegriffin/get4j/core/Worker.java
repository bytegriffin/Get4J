package com.bytegriffin.get4j.core;

import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.util.Queue;
import com.google.common.base.Strings;
import com.bytegriffin.get4j.core.UrlQueue;

/**
 * 工作线程
 */
public class Worker implements Runnable {

    private static final Logger logger = LogManager.getLogger(Worker.class);

    private String seedName;
    private String method;
    private CountDownLatch latch;

    public Worker(String seedName, String method, CountDownLatch latch) {
        this.seedName = seedName;
        this.method = method;
        this.latch = latch;
    }

    @Override
    public void run() {
        if (Strings.isNullOrEmpty(seedName)) {
            return;
        }
        Chain chain = Globals.CHAIN_CACHE.get(seedName);
        Queue<String> urlQueue = UrlQueue.getUnVisitedLink(seedName);
        logger.info("线程[{}]开始执行任务[{}]。。。",Thread.currentThread().getName(), seedName);
        while (urlQueue != null && !UrlQueue.isEmptyUnVisitedLinks(seedName)) {
            String url = UrlQueue.outFirst(seedName);
            if (Strings.isNullOrEmpty(url)) {
                break;
            }
            chain.execute(new Page(seedName, url, method));
            UrlQueue.newVisitedLink(seedName, url);
        }
        logger.info("线程[{}]完成任务[{}]。。。",Thread.currentThread().getName(), seedName);
        latch.countDown();
    }

}
package com.bytegriffin.get4j.net.sync;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.google.common.collect.Sets;

/**
 * 计划任务：用于按时或按量分批次地同步资源
 */
public class BatchScheduler implements Runnable {

    private Syncer sync;
    private static long last_update_time = System.currentTimeMillis();
    private static AtomicInteger count = new AtomicInteger();
    // 格式： seedname|avatar path 例如：seed1|c:\seed1\abc.jpg  seedname用于在远程创建目录
    public static final String split = "|";
    public static Set<String> resources = Sets.newConcurrentHashSet();

    private static volatile boolean flag = true;

    public static void addResource(String seedName, String path) {
        if (!UrlAnalyzer.isStartHttpUrl(path)) {
            resources.add(seedName + split + path);
        }
    }

    public BatchScheduler(Syncer sync) {
        this.sync = sync;
    }

    public static void start() {
        flag = true;
    }

    public static void stop() {
        flag = false;
    }

    @Override
    public synchronized void run() {
    		  while (flag) {
    	            if (resources.size() >= DefaultConfig.sync_batch_count
    	                    || ( (System.currentTimeMillis() - last_update_time) >= DefaultConfig.sync_batch_time && resources.size()>0)) {
    	                sync.setBatch(resources);
    	                sync.sync();
    	                last_update_time = System.currentTimeMillis();
    	                count.incrementAndGet();
    	                resources.clear();
    	            }
    	        }
    }


}

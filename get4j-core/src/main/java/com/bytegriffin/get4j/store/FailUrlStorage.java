package com.bytegriffin.get4j.store;

import java.util.LinkedList;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Initializer;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.FileUtil;
import com.google.common.collect.Lists;

/**
 * 坏链存储器<br>
 * 负责爬虫在爬取过程中访问不了或者根本不是链接的坏链存储在本地文件中
 */
public final class FailUrlStorage extends Initializer{

	private static final Logger logger = LogManager.getLogger(FailUrlStorage.class);

	@Override
	public  void init() {
		FileUtil.makeDiskFile(DefaultConfig.fail_url_file);
		logger.info("爬虫系统的坏链文件的初始化完成。");
	}

	/**
	 * 负责爬虫爬取完成一次后将全部链接一次性地dump出来
	 */
	public static void dump() {
		Set<String> seedNameKeys = Globals.CHAIN_CACHE.keySet();
		LinkedList<String> allFailUrls = Lists.newLinkedList();
		for (String seedName : seedNameKeys) {
			long count = UrlQueue.getFailVisitedUrlCount(seedName);
			if(count > 0){
				allFailUrls.addAll(UrlQueue.getFailVisitedUrl(seedName));
				logger.info("线程[{}]抓取种子[{}]时一共有[{}]个坏链产生。", Thread.currentThread().getName() , seedName, count);
			}
		}
		FileUtil.append(DefaultConfig.fail_url_file, allFailUrls);
		String content = null;
		for(String failUrl : allFailUrls){
			content += failUrl+" <br> ";
		}
		EmailSender.sendMail("此次爬取总共发现[" + allFailUrls.size() + "]个坏链。分别是：" + content);
		logger.info("线程[{}]dump坏链完成。", Thread.currentThread().getName() );
	}

}

package com.bytegriffin.get4j.core;

import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.alibaba.fastjson.JSONPath;
import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.monitor.HealthChecker;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.net.sync.BatchScheduler;
import com.bytegriffin.get4j.net.sync.RsyncSyncer;
import com.bytegriffin.get4j.net.sync.ScpSyncer;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.store.FailUrlStorage;
import com.bytegriffin.get4j.util.CommandUtil;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.Sleep;
import com.bytegriffin.get4j.util.StringUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Seed加载器：加载每个seed，将seed中url分配给各个worker工作线程
 */
public class Launcher extends TimerTask implements Command {

	private static final Logger logger = LogManager.getLogger(Launcher.class);

	private Seed seed;
	private ExecutorService batch;
	// 当avatar资源文件已经同步到图片服务器后，是否删除本地已经下载的这些资源文件，从而节省磁盘空间
	private boolean isDeleteDownloadFile = false;
	private volatile boolean condition = true;
	private HealthChecker healthChecker;
	private WorkerStatusOpt workerStatusOpt;
	private boolean isProbeMaster;

	public Launcher(Seed seed, WorkerStatusOpt workerStatusOpt,  boolean isProbeMaster) {
		this.seed = seed;
		if(Globals.LAUNCHER_CACHE.get(seed.getSeedName()) != null){
			Globals.LAUNCHER_CACHE.put(seed.getSeedName(), this);
		}
		if(healthChecker != null){
			healthChecker = new HealthChecker();
			healthChecker.register(seed.getSeedName());
		}
		this.workerStatusOpt = workerStatusOpt;
		this.isProbeMaster = isProbeMaster;
	}

	public boolean getCondition(){
		return condition;
	}

	@Override
	public void run() {
		while (!condition) {
			logger.info("种子[{}]已停止工作...", seed.getSeedName() );
			try {
				synchronized (this) {
					this.wait();
				}
			} catch (InterruptedException e) {
				Thread.interrupted();
			}
		}
		// 设置页面变化监测器，集群模式下需要每个节点配置probe选项
		PageChangeProber probe = Globals.FETCH_PROBE_CACHE.get(seed.getSeedName());
		if (probe != null && isProbeMaster) {
			probe.run();
			working();
			probe.start();
			run();
		}
		working();
	}

	private void working() {
		begin();
		// 设置UrlQueue
		setUnVisitedUrlQueue(seed);
		// 设置资源同步
		setSync();
		// 设置执行线程
		ExecutorService executorService;
		CountDownLatch latch;
		int threadNum = seed.getThreadCount();
		if (threadNum <= 1) {
			latch = new CountDownLatch(1);
			executorService = Executors.newSingleThreadExecutor();
			Worker worker = new Worker(seed.getSeedName(),seed.getFetchHttpMethod(), latch);
			executorService.execute(worker);
		} else {
			executorService = Executors.newFixedThreadPool(threadNum);
			latch = new CountDownLatch(threadNum);
			for (int i = 0; i < threadNum; i++) {
				Worker worker = new Worker(seed.getSeedName(), seed.getFetchHttpMethod(), latch);
				executorService.execute(worker);
				Sleep.seconds(3);
			}
		}
		// 等待所有工作线程执行完毕，再将坏链dump出来
		try {
			latch.await();
		} catch (InterruptedException e) {
			logger.info("种子[{}]在爬取工作中出现错误。", seed.getSeedName() , e);
		}
		// dump坏链
		FailUrlStorage.dump();

		// 关闭闲置链接，以便下一次多线程调用，减少服务器tcp链接数量，因此要重新初始化
		HttpEngine he = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName());
		if (he instanceof HttpClientEngine) {
			HttpClientEngine.closeIdleConnection();
		}
		// 关闭资源同步器
		if (DefaultConfig.resource_synchronizer != null) {
			while (BatchScheduler.resources.size() > 0) {
				Sleep.seconds(DefaultConfig.sync_batch_time);
			}
			if (batch != null) {
				BatchScheduler.stop();
				batch.shutdown();
			}
			// 清空下载目录：将页面以及资源文件全部删除，从而节省磁盘空间
			if (isDeleteDownloadFile) {
				for (String seedName : Globals.DOWNLOAD_DISK_DIR_CACHE.keySet()) {
					FileUtil.deleteFile(Globals.DOWNLOAD_DISK_DIR_CACHE.get(seedName));
				}
			}
		}
		// 清空这次抓取访问过的url集合，以方便下次轮训抓取时过滤重复链接
		clearVisitedUrlQueue(seed.getSeedName());
		// 清空异常信息
		ExceptionCatcher.clearExceptions();
		 idle();
	}

	/**
	 * 未开始或已爬取完成
	 */
	@Override
	public void idle() {
		if(workerStatusOpt != null){
			workerStatusOpt.setIdleStatus(seed.getSeedName());
		}
		logger.info("线程[{}]完成种子[{}]的一次爬取工作。", Thread.currentThread().getName(), seed.getSeedName() );
	}

	/**
	 * 正在爬取工作
	 */
	@Override
	public void begin() {
		//设置每次抓取开始时间，以便JMX统计每次抓取的开销时间
		Globals.PER_START_TIME_CACHE.put(seed.getSeedName(), DateUtil.getCurrentDate());
		if(workerStatusOpt != null){
			workerStatusOpt.setRunStatus(seed.getSeedName());
		}
		logger.info("线程[{}]开始种子[{}]的爬取运行。",  Thread.currentThread().getName(), seed.getSeedName() );
	}

	/**
	 * 继续工作
	 */
	@Override
	public void continues() {
		condition = true;
		synchronized (this) {
			this.notify();
		}
		logger.info("种子[{}]继续运行。",  seed.getSeedName() );
	}

	/**
	 * 销毁工作
	 */
	@Override
	public void destory() {
		if (this.cancel()) {
			Globals.LAUNCHER_CACHE.remove(seed.getSeedName());
			logger.info("种子[{}]已被取消。",  seed.getSeedName() );
		}
	}

	/**
	 * 暂停工作
	 */
	@Override
	public void pause() {
		condition = false;
		logger.info("种子[{}]已停止运行。",  seed.getSeedName() );
	}

	/**
	 * 初始化UrlQueue
	 *
	 * @param seed Seed
	 */
	private void setUnVisitedUrlQueue(Seed seed) {
		if (!PageMode.list_detail.equals(seed.getPageMode())) {
			// 添加每个seed对应的未访问url
			UrlQueue.newUnVisitedLink(seed.getSeedName(), seed.getFetchUrl());
		} else {// list_detail 因为分页的问题要特殊处理
			// 1.计算总页数
			String fetchUrl = seed.getFetchUrl();
			String totalPages = seed.getFetchTotalPages();
			if (!Strings.isNullOrEmpty(totalPages) && !StringUtil.isNumeric(totalPages)) {
				Page page = Globals.HTTP_ENGINE_CACHE.get(seed.getSeedName())
						.getPageContent(new Page(seed.getSeedName(), UrlAnalyzer.formatListDetailUrl(fetchUrl), seed.getFetchHttpMethod()));
				if (totalPages.contains(DefaultConfig.json_path_prefix)) {// json格式
					int totalPage = (Integer)JSONPath.read(page.getJsonContent(), totalPages);
					totalPages = String.valueOf(totalPage);// 所以需要再次转换
				} else {// html格式
					Document doc = Jsoup.parse(page.getHtmlContent());
					totalPages = doc.select(totalPages.trim()).text().trim();
					if (Strings.isNullOrEmpty(totalPages)) {
						totalPages = "1";
					}
				}
			}

			// 2.根据输入的列表Url和总页数生成所有页面Url，生成规则就是将大括号中的值自增1，即表示下一个列表页
			// 例如：http://www.aaa.com/bbb?p={1} ==>
			// http://www.aaa.com/bbb?p=1、...、http://www.aaa.com/bbb?p=10
			Pattern p = Pattern
					.compile("\\" + DefaultConfig.fetch_list_url_left + "(.*)" + DefaultConfig.fetch_list_url_right);
			Matcher m = p.matcher(fetchUrl);
			List<String> list = Lists.newArrayList();
			if (m.find()) {
				int pagenum = Integer.valueOf(m.group(1));
				String prefix = fetchUrl.substring(0, fetchUrl.indexOf(DefaultConfig.fetch_list_url_left));
				String suffix = fetchUrl.substring(fetchUrl.indexOf(DefaultConfig.fetch_list_url_right) + 1);
				int totalPage = Integer.valueOf(totalPages);
				for (int i = 0; i < totalPage; i++) {
					int pn = pagenum + i;
					String newurl = prefix + pn + suffix;
					UrlQueue.newUnVisitedLink(seed.getSeedName(), newurl);
					list.add(newurl);
				}
			} else {
				UrlQueue.newUnVisitedLink(seed.getSeedName(), fetchUrl);
				list.add(fetchUrl);
			}
			Globals.LIST_URLS_CACHE.put(seed.getSeedName(), list);
			logger.info("线程[{}]抓取种子[{}]列表Url总数是[{}]个。",Thread.currentThread().getName() , seed.getSeedName(),Globals.LIST_URLS_CACHE.size() );
		}
	}

	/**
	 * 每次抓取完都要清空一次已访问的url集合，以方便下次继续抓取<br>
	 * 否则在下次抓取时程序会判断内存中已经存在抓取过的url就不再去抓取
	 *
	 * @param seed    seed
	 * @see UrlQueue.addUnVisitedLinks()
	 */
	private void clearVisitedUrlQueue(String seedName) {
		UrlQueue.clearVisitedLink(seedName);
		UrlQueue.clearVisitedResource(seedName);
		UrlQueue.clearFailVisitedUrl(seedName);
	}

	/**
	 * 设置资源同步
	 */
	private void setSync() {
		if (DefaultConfig.resource_synchronizer == null) {
			return;
		}
		if ((DefaultConfig.resource_synchronizer instanceof RsyncSyncer
				|| DefaultConfig.resource_synchronizer instanceof ScpSyncer)
				&& System.getProperty("os.name").toLowerCase().contains("windows")) {
			logger.error("Rsync或Scp暂时不支持window系统，因此会强制关闭资源同步。");
			DefaultConfig.sync_open = false;
			return;
		} else if (DefaultConfig.resource_synchronizer instanceof ScpSyncer) {
			// Scp如果想实现增量复制需要先在目标服务器上创建文件夹
			ScpSyncer scp = (ScpSyncer) DefaultConfig.resource_synchronizer;
			CommandUtil.executeShell("ssh " +scp.getUsername() +"@"+ scp.getHost() + " 'mkdir " + scp.getDir() + seed.getSeedName() + "'");
		}
		BatchScheduler.start();
		batch = Executors.newSingleThreadExecutor();
		batch.execute(new BatchScheduler(DefaultConfig.resource_synchronizer));
	}

}

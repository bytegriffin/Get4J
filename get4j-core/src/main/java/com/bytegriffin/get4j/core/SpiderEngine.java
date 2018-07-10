package com.bytegriffin.get4j.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.AbstractConfig;
import com.bytegriffin.get4j.conf.ClusterNode;
import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.DynamicField;
import com.bytegriffin.get4j.conf.ResourceSync;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.download.DiskDownloader;
import com.bytegriffin.get4j.fetch.CascadeFetcher;
import com.bytegriffin.get4j.fetch.DynamicFieldFetcher;
import com.bytegriffin.get4j.fetch.ListDetailFetcher;
import com.bytegriffin.get4j.fetch.SingleFetcher;
import com.bytegriffin.get4j.fetch.SiteFetcher;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.net.http.SeleniumEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.net.sync.FtpSyncer;
import com.bytegriffin.get4j.net.sync.RsyncSyncer;
import com.bytegriffin.get4j.net.sync.ScpSyncer;
import com.bytegriffin.get4j.parse.AutoDelegateParser;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.probe.ProbeMasterChecker;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.store.DBStorage;
import com.bytegriffin.get4j.store.FreeProxyStorage;
import com.bytegriffin.get4j.store.LuceneIndexStorage;
import com.bytegriffin.get4j.store.MongodbStorage;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.Sleep;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * 爬虫配置引擎 <br>
 * 执行前的准备工作：组建工作流程
 */
public final class SpiderEngine {

	private List<Seed> seeds;
	private Configuration configuration;
	private ResourceSync resourceSync;
	private List<DynamicField> dynamicFields;
	private Process hbase;
	private List<Initializer> inits;
	private WorkerStatusOpt workerStatusOpt;
	private ProbeMasterChecker probeMasterChecker;

	private static final Logger logger = LogManager.getLogger(SpiderEngine.class);

	private SpiderEngine() {
		super();
	}

	public static SpiderEngine create() {
		return new SpiderEngine();
	}
	
	/**
	 * 增加HBase下载流程
	 * @param hbase Process
	 * @return SpiderEngine
	 */
	public SpiderEngine addHBase(Process hbase) {
		this.hbase = hbase;
		return this;
	}

	/**
	 * 设置cluster节点初始化信息，在分布式情况下调用
	 * 
	 * @param clusterNode cluster节点
	 * @return SpiderEngine
	 */
	public SpiderEngine setClusterNode(ClusterNode clusterNode) {
		if (clusterNode == null) {
			return this;
		}
		this.inits = clusterNode.getInitializers();
		this.workerStatusOpt = clusterNode.getWorkerStatusOpt();
		this.probeMasterChecker = clusterNode.getProbeMasterChecker();
		this.hbase = clusterNode.getHbase();
		return this;
	}

	/**
	 * 构建爬虫参数
	 */
	public void build() {
		buildDynamicField();
		buildProcess();
		buildResourceSync();
		buildConfiguration();
		buildTimer();
	}

	/**
	 * 设置种子Seed
	 *
	 * @param seed Seed
	 * @return SpiderEngine
	 */
	public SpiderEngine setSeed(Seed seed) {
		List<Seed> seeds = Lists.newArrayList();
		seeds.add(seed);
		this.seeds = seeds;
		return this;
	}

	/**
	 * 设置种子Seed列表
	 *
	 * @param seeds List
	 * @return SpiderEngine
	 */
	public SpiderEngine setSeeds(List<Seed> seeds) {
		this.seeds = seeds;
		return this;
	}

	/**
	 * 设置configuration配置
	 *
	 * @param configuration  Configuration
	 * @return SpiderEngine
	 */
	public SpiderEngine setConfiguration(Configuration configuration) {
		this.configuration = configuration;
		return this;
	}

	/**
	 * 设置资源同步器
	 *
	 * @param resourceSync ResourceSync
	 * @return SpiderEngine
	 */
	public SpiderEngine setResourceSync(ResourceSync resourceSync) {
		this.resourceSync = resourceSync;
		return this;
	}

	/**
	 * 设置动态字段映射
	 * 
	 * @param dynamicField DynamicField
	 * @return SpiderEngine
	 */
	public SpiderEngine setDynamicField(DynamicField dynamicField) {
		this.dynamicFields = Lists.newArrayList();
		this.dynamicFields.add(dynamicField);
		return this;
	}

	/**
	 * 设置动态字段映射列表
	 * 
	 * @param dynamicFields List
	 * @return SpiderEngine
	 */
	public SpiderEngine setDynamicFields(List<DynamicField> dynamicFields) {
		this.dynamicFields = dynamicFields;
		return this;
	}

	/**
	 * 根据配置选择具体的Http引擎<br>
	 * 1.初始化Http引擎的部分参数<br>
	 * 2.测试在具体Http引擎下的代理是否可用<br>
	 *
	 * @param seed  seed
	 */
	private void buildHttpEngine(Seed seed) {
		HttpEngine http;
		if (seed.isFetchJavascriptSupport()) {
			http = new SeleniumEngine();
			logger.info("启用Selenium作为抓取引擎。");
		} else {
			http = new HttpClientEngine();
			logger.info("启用HttpClient作为抓取引擎。");
		}
		// 1.初始化httpclient部分参数
		http.init(seed);
		// 2.测试代理是否可用
		List<HttpProxy> hplist = seed.getFetchHttpProxy();
		if (hplist != null && hplist.size() > 0) {
			LinkedList<HttpProxy> newList = Lists.newLinkedList();
			hplist.forEach(httpProxy -> {
				String furl = UrlAnalyzer.formatListDetailUrl(seed.getFetchUrl());
				boolean isReached = http.testHttpProxy(furl, httpProxy);
				if (isReached) {
					newList.add(httpProxy);
				} else {
					logger.warn("Http代理[{}]测试失效。", httpProxy.toString());
				}
			});
			if (newList.size() == 0) {
				logger.error("启动失败：种子[{}]测试Http代理全部失效，请重新配置。", seed.getSeedName());
				System.exit(1);
			}
		}
		Globals.HTTP_ENGINE_CACHE.put(seed.getSeedName(), http);
	}

	/**
	 * 第一步：映射动态字段
	 */
	private void buildDynamicField() {
		if (dynamicFields == null || dynamicFields.size() == 0) {
			return;
		}
		dynamicFields.stream().filter(p -> !Strings.isNullOrEmpty(p.getSeedName()))
			.filter(p -> p.getFields() != null)
			.filter(p -> !p.getFields().isEmpty())
			.forEach(p -> Globals.DYNAMIC_FIELDS_CACHE.put(p.getSeedName(), p.getFields()));
	}

	/**
	 * 第二步：根据配置文件或api动态地构建爬虫工作流程
	 */
	private void buildProcess() {
		if (seeds == null || seeds.size() == 0) {
			logger.error("启动失败：请先设置种子Seed参数，才能构建爬虫引擎。");
			System.exit(1);
		}
		for (Seed seed : seeds) {
			String seedName = seed.getSeedName();
			Chain chain = new Chain();
			if (Strings.isNullOrEmpty(seed.getFetchUrl())) {
				logger.error("启动失败：种子[{}]-[fetch.url]参数为必填项。", seedName);
				System.exit(1);
			}
			// 1.构建http引擎
			buildHttpEngine(seed);

			// 2.设置流程
			StringBuilder subProcess = new StringBuilder();

			if (!Strings.isNullOrEmpty(seed.getFetchProbeSelector())) {
				PageChangeProber p = new PageChangeProber(seed);
				Globals.FETCH_PROBE_CACHE.put(seed.getSeedName(), p);
				subProcess.append("PageChangeProber-");
			}

			if (PageMode.single.equals(seed.getPageMode())) {
				SingleFetcher fe = new SingleFetcher();
				fe.init(seed);
				chain.addProcess(fe);
				subProcess.append("SingleFetcher");
			} else if (PageMode.cascade.equals(seed.getPageMode())) {
				CascadeFetcher mu = new CascadeFetcher();
				mu.init(seed);
				chain.addProcess(mu);
				subProcess.append("CascadeFetcher");
			} else if (PageMode.site.equals(seed.getPageMode())) {
				SiteFetcher ld = new SiteFetcher();
				ld.init(seed);
				chain.addProcess(ld);
				subProcess.append("SiteFetcher");
			} else if (PageMode.list_detail.equals(seed.getPageMode()) || seed.isListDetailMode()) {// 配置文件设置或者api设置两种判断
				ListDetailFetcher ld = new ListDetailFetcher();
				ld.init(seed);
				chain.addProcess(ld);
				subProcess.append("ListDetailFetcher");
			}

			if (Globals.DYNAMIC_FIELDS_CACHE.get(seedName) != null
					&& !Globals.DYNAMIC_FIELDS_CACHE.get(seedName).isEmpty()) {
				DynamicFieldFetcher df = new DynamicFieldFetcher();
				df.init(seed);
				chain.addProcess(df);
				subProcess.append("-DynamicFieldsFetcher");
			}

			if (!Strings.isNullOrEmpty(seed.getDownloadDisk())) {
				Process p = new DiskDownloader();
				chain.addProcess(p);
				p.init(seed);
				subProcess.append("-DiskDownloader");
			}

			// if (!Strings.isNullOrEmpty(seed.getExtractClassImpl())) {
			// chain.addProcess(new ExtractDispatcher());
			// subProcess.append("-Extract");
			// }

			if (!Strings.isNullOrEmpty(seed.getParseClassImpl())) {
				AutoDelegateParser dp = new AutoDelegateParser();
				chain.addProcess(dp);
				dp.init(seed);
				int index = seed.getParseClassImpl().lastIndexOf(".") + 1;
				subProcess.append("-");
				subProcess.append(seed.getParseClassImpl().substring(index));
			} else if (!Strings.isNullOrEmpty(seed.getParseElementSelector())) {
				AutoDelegateParser dp = new AutoDelegateParser();
				chain.addProcess(dp);
				dp.init(seed);
				subProcess.append("-ElementSelectPageParser");
			} else if (seed.getPageParser() != null) {
				AutoDelegateParser dp = new AutoDelegateParser();
				chain.addProcess(dp);
				dp.init(seed);
				subProcess.append("-LambdaPageParser");
			}

			// 不配置成else if是想系统支持多个数据源
			if (!Strings.isNullOrEmpty(seed.getStoreJdbc())) {
				DBStorage dbstorage = new DBStorage();
				dbstorage.init(seed);
				chain.addProcess(dbstorage);
				subProcess.append("-DBStorage");
			}
			if (!Strings.isNullOrEmpty(seed.getStoreMongodb())) {
				MongodbStorage mongodb = new MongodbStorage();
				mongodb.init(seed);
				chain.addProcess(mongodb);
				subProcess.append("-MongodbStorage");
			}
			if (!Strings.isNullOrEmpty(seed.getStoreLuceneIndex())) {
				LuceneIndexStorage index = new LuceneIndexStorage();
				index.init(seed);
				chain.addProcess(index);
				subProcess.append("-LuceneIndexStorage");
			}
			if (!Strings.isNullOrEmpty(seed.getStoreFreeProxy())) {
				FreeProxyStorage freeProxyStorage = new FreeProxyStorage();
				freeProxyStorage.init(seed);
				chain.addProcess(freeProxyStorage);
				subProcess.append("-FreeProxyStorage");
			}
			if(!Strings.isNullOrEmpty(seed.getStoreHBase()) && hbase != null){
				chain.addProcess(hbase);
				hbase.init(seed);
				subProcess.append("-HBaseDownloader");
			}

			Globals.FETCH_PAGE_MODE_CACHE.put(seedName, seed.getPageMode());

			if (chain.list.size() > 0) {
				// 缓存每个site的工作流程
				Globals.CHAIN_CACHE.put(seed.getSeedName(), chain);
				logger.info("种子[{}]流程[{}]设置完成。", seedName, subProcess.toString());
			} else {
				logger.error("启动失败：种子[{}]流程设置失败，没有任何子流程加入，请重新配置。", seedName);
				System.exit(1);
			}
		}

		Initializer.loads(inits);
	}

	/**
	 * 第三步：创建资源同步
	 */
	private void buildResourceSync() {
		if (resourceSync == null || resourceSync.getSync() == null || resourceSync.getSync().isEmpty()) {
			return;
		}
		String open = resourceSync.getSync().get(AbstractConfig.open_node);
		if ("false".equalsIgnoreCase(open)) {
			return;
		}
		String protocal = resourceSync.getSync().get(AbstractConfig.protocal_node);
		if (AbstractConfig.ftp_node.equals(protocal)) {
			Map<String, String> ftp = resourceSync.getFtp();
			if (ftp == null || ftp.isEmpty()) {
				logger.error("yaml配置文件[{}]中的ftp属性出错，请重新检查。", AbstractConfig.resource_sync_yaml_file);
				System.exit(1);
			}
			String host = ftp.get(AbstractConfig.host_node);
			String username = ftp.get(AbstractConfig.username_node);
			String password = ftp.get(AbstractConfig.password_node);
			String port = Strings.isNullOrEmpty(ftp.get(AbstractConfig.port_node)) ? "21"
					: ftp.get(AbstractConfig.port_node);
			// 只检查了host属性是否为空，因为有的ftp服务没有用户名/密码等
			if (Strings.isNullOrEmpty(host)) {
				logger.error("yaml配置文件[{}]中的host属性为空，请重新检查。", AbstractConfig.resource_sync_yaml_file);
				System.exit(1);
			}
			DefaultConfig.resource_synchronizer = new FtpSyncer(host, port, username, password);
		} else if (AbstractConfig.rsync_node.equals(protocal)) {
			Map<String, String> rsync = resourceSync.getRsync();
			if (rsync == null || rsync.isEmpty()) {
				logger.error("yaml配置文件[{}]中的rsync属性出错，请重新检查。", AbstractConfig.resource_sync_yaml_file);
				System.exit(1);
			}
			String host = rsync.get(AbstractConfig.host_node);
			String username = rsync.get(AbstractConfig.username_node);
			String module = rsync.get(AbstractConfig.module_node);
			String dir = rsync.get(AbstractConfig.dir_node);
			if (!Strings.isNullOrEmpty(module)) {
				DefaultConfig.resource_synchronizer = new RsyncSyncer(host, username, module, true);
			} else if (!Strings.isNullOrEmpty(dir)) {
				DefaultConfig.resource_synchronizer = new RsyncSyncer(host, username, dir, false);
			} else {
				logger.error("yaml配置文件[{}]中的rsync的module或dir属性必须二选一，请重新检查。", AbstractConfig.resource_sync_yaml_file);
				System.exit(1);
			}
		} else if (AbstractConfig.scp_node.equals(protocal)) {
			Map<String, String> scp = resourceSync.getScp();
			if (scp == null || scp.isEmpty()) {
				logger.error("yaml配置文件[{}]中的scp属性出错，请重新检查。", AbstractConfig.resource_sync_yaml_file);
				System.exit(1);
			}
			String host = scp.get(AbstractConfig.host_node);
			String username = scp.get(AbstractConfig.username_node);
			String dir = scp.get(AbstractConfig.dir_node);
			String port = Strings.isNullOrEmpty(scp.get(AbstractConfig.port_node)) ? "22"
					: scp.get(AbstractConfig.port_node);
			DefaultConfig.resource_synchronizer = new ScpSyncer(host, username, dir, port);
		}
		DefaultConfig.sync_open = Boolean.valueOf(open);
		DefaultConfig.sync_batch_count = Integer.valueOf(resourceSync.getSync().get(AbstractConfig.batch_count_node));
		DefaultConfig.sync_batch_time = Integer.valueOf(resourceSync.getSync().get(AbstractConfig.batch_count_node)) * 1000;
	}

	/**
	 * 第四步：创建工作环境
	 */
	private void buildConfiguration() {
		if (configuration == null) {
			return;
		}
		DefaultConfig.download_file_url_naming = !(Strings.isNullOrEmpty(configuration.getDownloadFileNameRule())
				|| DefaultConfig.default_value.equals(configuration.getDownloadFileNameRule()));

		if (!Strings.isNullOrEmpty(configuration.getEmailRecipient())) {
			EmailSender es = new EmailSender(configuration.getEmailRecipient());
			Globals.emailSender = es;
		}
	}

	/**
	 * 第五步：启动定时器，按照配置的时间启动抓取任务
	 */
	private void buildTimer() {
		for (Seed seed : seeds) {
			Globals.SEED_CACHE.put(seed.getSeedName(), seed);
			// 这里需要等待在cluster模式下probemaster的选举
			// 由于zookeeper选举过程是异步执行，因此需要等待一定时间，
			// 暂时还没有好方法，直接获取外部项目的执行结果
			if (probeMasterChecker != null) {
				probeMasterChecker.isActive(seed.getSeedName());
				Sleep.seconds(DefaultConfig.probe_master_selector_timeout);
				startUp(seed, workerStatusOpt, probeMasterChecker.isActive(seed.getSeedName()));
			} else {
				// 单机或集群下没有配置zookeeper，可直接根据每个节点的配置文件来设置probe
				startUp(seed, workerStatusOpt, true);
			}
		}
	}

	/**
	 * 启动一个种子任务
	 * 
	 * @param seed Seed
	 * @param workerStatusOpt 分布式下的工作状态接口
	 * @param isProbeMaster    集群环境下本机是否为probe master
	 */
	public void startUp(Seed seed, WorkerStatusOpt workerStatusOpt, boolean isProbeMaster) {
		String interval = seed.getFetchInterval();
		String starttime = seed.getFetchStart();
		Launcher job = new Launcher(seed, workerStatusOpt, isProbeMaster);
		Timer timer = new Timer();
		logger.info("爬虫准备抓取种子[{}]。。。", seed.getSeedName());
		// 注意：如果配置了probe属性，那么程序不再支持interval功能，而是由probe来接管
		if (!Strings.isNullOrEmpty(seed.getFetchProbeSelector())) {
			if (!Strings.isNullOrEmpty(starttime) ) {
				timer.schedule(job, DateUtil.strToDate(starttime));
		    } else {
				timer.schedule(job, 0L);
			}
		} else {
			if (Strings.isNullOrEmpty(starttime) && (Strings.isNullOrEmpty(interval) || Integer.valueOf(interval) == 0 )) {
				timer.schedule(job, 0L);
			} else if (Strings.isNullOrEmpty(starttime) && !Strings.isNullOrEmpty(interval)) {
				timer.schedule(job, DateUtil.strToDate(DateUtil.getCurrentDate()), Long.valueOf(interval) * 1000);
			} else if (!Strings.isNullOrEmpty(starttime) && (Strings.isNullOrEmpty(interval) || interval.equals("0")) ) {
				timer.schedule(job, DateUtil.strToDate(starttime));
			} else {
				timer.schedule(job, DateUtil.strToDate(starttime), Long.valueOf(interval) * 1000);
			}
		}
	}

}

package com.bytegriffin.get4j.ha;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Initializer;
import com.bytegriffin.get4j.core.SpiderEngine;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.util.NetHelper;
import com.bytegriffin.get4j.util.Sleep;

/**
 * Zookeeper客户端：自动感知各节点的运行状况 <br>
 * 一旦发现某节点有状态变化，比如开始运行了新的抓取任务，<br>
 * 它就会唤醒其他节点共同来完成此任务<br>
 * 1.Interval定时器模式：只需要集群中一个节点配置定时器即可。<br>
 * 2.Probe模式：需要每个节点都要配置probe。<br>
 */
public class ZookeeperClient extends Initializer{

	private static final Logger logger = LogManager.getLogger(ZookeeperClient.class);

	private static final int seesion_timeout = 3000;
	private static final int connection_timeout = 3000;
	private static final String namespace = "get4j";

	public static CuratorFramework client;
	private String address;

	public ZookeeperClient(String address) {
		this.address = address;
	}

	@Override
	public void init() {
		// 1. 初始化连接
		client = CuratorFrameworkFactory.builder().connectString(address).sessionTimeoutMs(seesion_timeout)
				.connectionTimeoutMs(connection_timeout).canBeReadOnly(true)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3)).namespace(namespace).defaultData(null).build();
		client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
			public void stateChanged(CuratorFramework client, ConnectionState state) {
				switch (state) {
				case LOST:
				case SUSPENDED:
					logger.error("连接zookeeper丢失。");
					break;
				case RECONNECTED:
					logger.info("重新连接zookeeper。");
					break;
				default:
					break;
				}
			}
		});
		client.start();
		try {
			if (!client.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
				logger.error("连接zookeeper失败。");
				System.exit(1);
			} else {
				logger.info("连接zookeeper成功。");
			}
		} catch (InterruptedException e) {
			logger.error("连接zookeeper失败{}", e);
		}

		// 2.在zk上创建节点并监控节点状态
		try {
			int size = Globals.CHAIN_CACHE.size();
			if(size > 0){
				for (String seedName : Globals.CHAIN_CACHE.keySet()) {
					Stat stat = client.checkExists().forPath(ZookeeperOpt.getStatusNodeName(seedName));
					if (stat == null) {
						//如果存在子节点，那么父节点只能设置为持久化
						client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
								.withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(ZookeeperOpt.getStatusNodeName(seedName), ZookeeperOpt.seed_idle_status.getBytes());
					}
					watchNodeStatus(seedName);
					ProbeMasterElection.create(client, seedName);
				}
			}
		} catch (Exception e) {
			logger.error("创建zookeeper节点失败：",e);
		}
	}

	/**
	 * 监控zk节点的值是否发生变化，如果变化就会启动爬虫继续工作 
	 * 注意：此种方式最好不要配置timer
	 * 
	 * @param seedName String
	 */
	public static void watchNodeStatus(String seedName) {
		try {
			@SuppressWarnings("resource")
			final TreeCache node = new TreeCache(client, ZookeeperOpt.getStatusNodeName(seedName));
			node.getListenable().addListener(new TreeCacheListener() {
				//如果当前节点是第一次运行，那么本节点状态变化是首先会进入Node_add，然后再进入Node_update，
				//此时就会执行了node_update中的startUp方法，虽然可以通知到其他节点也同时运行startUp方法，
				//但是这样会造成本节点多次运行startUp，为了避免这一情况加入此状态，用于表示本节点是否为第一次运行。
				private AtomicBoolean isFirstRun  = new AtomicBoolean(true);
				@Override
				public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
					String nodePath = "";
					String nodeValue = "";
					if(event.getData() != null){
						nodePath = event.getData().getPath();
						nodeValue = new String(event.getData().getData());
					}
					switch (event.getType()) {
						case NODE_ADDED:
							logger.info("Zookeeper发现一个新的节点[{}]连接进来，其状态为[{}]。", NetHelper.getClusterNodeName(), nodeValue);
							if (!isFirstRun.get() && ZookeeperOpt.seed_run_status.equals(nodeValue) ) {
								logger.info("Zookeeper发现在种子[{}]集群下的路径为[{}]有正在执行的任务，并且将此任务分配给新加入状态为[{}]的节点[{}]。", seedName, nodePath, nodeValue, NetHelper.getClusterNodeName());
								SpiderEngine.create().startUp(Globals.SEED_CACHE.get(seedName), ZookeeperOpt.single(), ProbeMasterElection.checkMaster(seedName));
							}
							break;
						case NODE_UPDATED:
							if (!isFirstRun.get() && ZookeeperOpt.seed_run_status.equals(nodeValue) ) {
								logger.info("Zookeeper发现在种子[{}]集群下的路径为[{}]有正在执行的任务，并且将此任务分配给状态为[{}]的节点[{}]。", seedName,  nodePath, nodeValue, NetHelper.getClusterNodeName());
								SpiderEngine.create().startUp(Globals.SEED_CACHE.get(seedName), ZookeeperOpt.single(), ProbeMasterElection.checkMaster(seedName));
							}
							isFirstRun.set(false);
							logger.info("Zookeeper将种子[{}]集群下的路径为[{}]一个节点[{}]的状态更新为[{}]。", seedName,  nodePath, NetHelper.getClusterNodeName(), nodeValue);
							break;
						case NODE_REMOVED:
						case CONNECTION_LOST:
							logger.warn("Zookeeper发现将种子[{}]集群下的路径为[{}]有一个状态为[{}]节点退出。", seedName,  nodePath, nodeValue, NetHelper.getClusterNodeName());
							PageChangeProber probe = Globals.FETCH_PROBE_CACHE.get(seedName);
							if (probe != null){
								ProbeMasterElection.checkMaster(seedName);
					        	Sleep.seconds(DefaultConfig.probe_master_selector_timeout);
					        	boolean isProbeMaster = ProbeMasterElection.checkMaster(seedName);
					        	if(isProbeMaster){
					        		SpiderEngine.create().startUp(Globals.SEED_CACHE.get(seedName), ZookeeperOpt.single(), isProbeMaster);
									logger.warn("Zookeeper发现将种子[{}]集群下的路径为[{}]有一个状态为[{}]客户端[{}]退出。", seedName,  nodePath, nodeValue, NetHelper.getClusterNodeName());
					        	}
							}
							break;
						default:
							break;
						}
				}
			});
			node.start();
		} catch (Exception e) {
			logger.error("监控种子[{}]集群下zookeeper的节点失败：", seedName, e);
		}
	}

}

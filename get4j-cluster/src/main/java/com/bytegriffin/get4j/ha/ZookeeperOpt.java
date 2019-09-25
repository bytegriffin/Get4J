package com.bytegriffin.get4j.ha;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.WorkerStatusOpt;

/**
 * 为子项目提供回调方法，以便触发zookeeper感知节点状态变化
 */
public class ZookeeperOpt implements WorkerStatusOpt{

	private static final Logger logger = LogManager.getLogger(ZookeeperOpt.class);
	//爬虫运行状态父路径，/namespace/status/seedname1
	private static final String status_path = "status";
	//爬取状态
	static final String seed_run_status = "run";
	//闲置状态：未开始或者已经爬取完成正等待下次爬取
	static final String seed_idle_status = "idle";

	private static final ZookeeperOpt zkopt = new ZookeeperOpt();

	private ZookeeperOpt(){
	}

	public static ZookeeperOpt single(){
		return zkopt;
	}

	/**
	 * 获取某个seed下的节点名称
	 * @param seedName String
	 * @return String
	 */
	public static String getStatusNodeName(String seedName){
		return "/"+status_path+"/"+seedName;
	}

	/**
	 * 设置zk节点状态为闲置状态
	 * 
	 * @param seedName String
	 */
	public  synchronized void setIdleStatus(String seedName) {
		String nodeName = getStatusNodeName(seedName);
		try {
			String status = new String(ZookeeperClient.client.getData().forPath(nodeName));
			if (seed_run_status.equals(status)){
				ZookeeperClient.client.setData().forPath(nodeName, seed_idle_status.getBytes());
			}
		} catch (Exception e) {
			logger.error("修改zookeeper节点[{}]失败{}", nodeName, e);
		}
	}

	/**
	 * 设置zk节点状态为运行状态
	 * 
	 * @param seedName String
	 */
	public  synchronized void setRunStatus(String seedName) {
		String nodeName = getStatusNodeName(seedName);
		try {
			String status = new String(ZookeeperClient.client.getData().forPath(nodeName));
			if (seed_idle_status.equals(status)){
				ZookeeperClient.client.setData().forPath(nodeName, seed_run_status.getBytes());
			}
		} catch (Exception e) {
			logger.error("修改zookeeper节点[{}]失败{}", nodeName, e);
		}
	}

}

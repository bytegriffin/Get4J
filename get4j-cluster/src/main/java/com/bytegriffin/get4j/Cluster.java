package com.bytegriffin.get4j;

import java.lang.annotation.Annotation;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.annotation.Redis;
import com.bytegriffin.get4j.annotation.Zookeeper;
import com.bytegriffin.get4j.conf.ClusterNode;
import com.bytegriffin.get4j.conf.ClusterNodeXmlHandler;
import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.ConfigurationXmlHandler;
import com.bytegriffin.get4j.conf.Context;
import com.bytegriffin.get4j.conf.CoreSeedsXmlHandler;
import com.bytegriffin.get4j.conf.DynamicField;
import com.bytegriffin.get4j.conf.DynamicFieldXmlHandler;
import com.bytegriffin.get4j.conf.ResourceSync;
import com.bytegriffin.get4j.conf.ResourceSyncYamlHandler;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Initializer;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.core.SpiderEngine;
import com.bytegriffin.get4j.core.WorkerStatusOpt;
import com.bytegriffin.get4j.ha.ProbeMasterElection;
import com.bytegriffin.get4j.ha.ZookeeperClient;
import com.bytegriffin.get4j.ha.ZookeeperOpt;
import com.bytegriffin.get4j.probe.ProbeMasterChecker;
import com.bytegriffin.get4j.store.HBaseStorage;
import com.bytegriffin.get4j.store.RedisStorage;
import com.bytegriffin.get4j.util.NetHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class Cluster {

	private static final Logger logger = LogManager.getLogger(Cluster.class);

	private static ClusterNode clusterNode;

	private Cluster() {
	}

	/**
	 * 实例化
	 * @return Cluster
	 */
	public static Cluster create(){
		 clusterNode = new ClusterNode();
		 return new Cluster();
	 }

	/**
	 * 创建一个集群节点实例
	 * @return ClusterNode
	 */
    public ClusterNode build() {
        // 自动生成node name
        if (Strings.isNullOrEmpty(clusterNode.getNodeName())) {
            clusterNode.setNodeName(NetHelper.getClusterNodeName());
        }
        if(Strings.isNullOrEmpty(clusterNode.getRedisAddress()) || Strings.isNullOrEmpty(clusterNode.getRedisMode()) 
        		|| Strings.isNullOrEmpty(clusterNode.getRedisAuth())){
        	logger.error("没有设置redis属性...");
        }
        clusterNode.setInitializers(buildInitializers(clusterNode));
        setZookeeperOpt(clusterNode);
        return clusterNode;
    }

    /**
     * 设置集群节点名称
     * @param nodeName String
     * @return Cluster
     */
    public Cluster nodeName(String nodeName){
    	clusterNode.setNodeName(nodeName);
    	return this;
    }

    /**
     * 设置redis
     * @param mode 部署模式：one(单机)/sharded(分片)/cluster(集群)
     * @param address 地址格式：host1:port1,host2:port2
     * @param auth 密码，没有可设置null
     * @return Cluster
     */
    public Cluster redis(String mode, String address, String auth){
    	clusterNode.setRedisMode(mode);
    	clusterNode.setRedisAddress(address);
    	clusterNode.setRedisAuth(auth);
    	return this;
    }

    /**
     * 设置zookeeper
     * @param address 地址
     * @return Cluster
     */
    public Cluster zookeeper(String address){
    	clusterNode.setZookeeperAddress(address);
    	return this;
    }

    /**
     * 根据Annotation设置redis
     * @param clazz 类
     * @return Cluster
     */
    public Cluster redis(Class<?> clazz) {
        Annotation[] ans = clazz.getDeclaredAnnotations();
        if (ans == null || ans.length == 0) {
            logger.error("类[{}]没有配置任何Annotation。", clazz.getName());
            System.exit(1);
        }
        for (Annotation an : ans) {
            String type = an.annotationType().getSimpleName();
            if ("Redis".equalsIgnoreCase(type)) {
                if (clazz.isAnnotationPresent(Redis.class)) {
                	Redis redis = (Redis) clazz.getAnnotation(Redis.class);
                    this.redis(redis.mode(),redis.address(),redis.auth());
                }
            } else  if ("Zookeeper".equalsIgnoreCase(type)) {
            	if (clazz.isAnnotationPresent(Zookeeper.class)) {
            		Zookeeper zookeeper = (Zookeeper) clazz.getAnnotation(Zookeeper.class);
                    this.zookeeper(zookeeper.value());
                }
            }
        }
        return this;
    }

    /**
     * 开启HBase存储功能
     * @return Cluster
     */
    public Cluster enableHBase(){
    	clusterNode.setHbase(buildHBase());
    	return this;
    }

    /**
     * 构建初始化节点
     * @param node ClusterNode
     * @return List
     */
    private static List<Initializer>  buildInitializers(ClusterNode node){
    	RedisStorage redis = null;
    	List<Initializer> inits = Lists.newArrayList();
    	if(!Strings.isNullOrEmpty(node.getRedisMode()) && !Strings.isNullOrEmpty(node.getRedisAddress()) && !Strings.isNullOrEmpty(node.getRedisAuth())){
    		redis = new RedisStorage(node.getRedisMode(),node.getRedisAddress(),node.getRedisAuth());
    		inits.add(redis);
    	}
    	ZookeeperClient zk = null;
    	if(!Strings.isNullOrEmpty(node.getZookeeperAddress())){
    		zk = new ZookeeperClient(node.getZookeeperAddress());
    		inits.add(zk);
    	}
    	return inits;
    }

    /**
     * 构建zookeeper工作状态改变
     * @return WorkerStatusOpt
     */
    private static WorkerStatusOpt buildWorkerStatusOpt(){
    	return ZookeeperOpt.single();
    }

    /**
     * 判断是否为probe master
     * @return ProbeMasterChecker
     */
    private static ProbeMasterChecker buildProbeMasterChecker(){
    	return ProbeMasterElection.single();
    }

    private static void setZookeeperOpt(ClusterNode clusterNode){
    	if(!Strings.isNullOrEmpty(clusterNode.getZookeeperAddress())){
    		clusterNode.setWorkerStatusOpt(buildWorkerStatusOpt());
    		clusterNode.setProbeMasterChecker(buildProbeMasterChecker());
    	}
    }

    private static Process buildHBase(){
    	return new HBaseStorage();
    }

    /**
     * 配置文件调用入口
     * @param args String[]
     */
	public static void main(String[] args) {
		Context context = new Context(new CoreSeedsXmlHandler());
		List<Seed> seeds = context.load();

		context = new Context(new ResourceSyncYamlHandler());
		ResourceSync resourceSync = context.load();

		context = new Context(new ConfigurationXmlHandler());
		Configuration configuration = context.load();

		context = new Context(new DynamicFieldXmlHandler());
		List<DynamicField> dynamicFields = context.load();

		context = new Context(new ClusterNodeXmlHandler());
		ClusterNode clusterNode = context.load();
		clusterNode.setInitializers(buildInitializers(clusterNode));
		setZookeeperOpt(clusterNode);

		SpiderEngine.create().setClusterNode(clusterNode).addHBase(buildHBase()).setSeeds(seeds).setResourceSync(resourceSync)
				.setConfiguration(configuration).setDynamicFields(dynamicFields).build();
		logger.info("爬虫集群开始启动...");
	}

}

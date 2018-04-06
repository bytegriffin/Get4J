package com.bytegriffin.get4j.conf;

import java.util.List;

import com.bytegriffin.get4j.core.Initializer;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.core.WorkerStatusOpt;
import com.bytegriffin.get4j.probe.ProbeMasterChecker;

/**
 * cluster-node.xml全局配置文件
 * 一个ClusterNode对应于一组Seed
 */
public class ClusterNode {

	private String nodeName;
    private String redisMode;
    private String redisAddress;
    private String redisAuth;
    private String zookeeperAddress;
    private List<Initializer> initializers;
    // 单个实例，但其方法支持多个不同的seed
    private WorkerStatusOpt workerStatusOpt;
    // 单个实例，但其方法支持多个不同的seed
    private ProbeMasterChecker probeMasterChecker;
    private Process hdfs;
    private Process hbase;

    public ClusterNode(){ 	
    }

    public ClusterNode(String nodeName){
    	this.nodeName = nodeName;
    }

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getRedisMode() {
		return redisMode;
	}

	public void setRedisMode(String redisMode) {
		this.redisMode = redisMode;
	}

	public String getRedisAddress() {
		return redisAddress;
	}

	public void setRedisAddress(String redisAddress) {
		this.redisAddress = redisAddress;
	}

	public String getRedisAuth() {
		return redisAuth;
	}

	public void setRedisAuth(String redisAuth) {
		this.redisAuth = redisAuth;
	}

	public String getZookeeperAddress() {
		return zookeeperAddress;
	}

	public void setZookeeperAddress(String zookeeperAddress) {
		this.zookeeperAddress = zookeeperAddress;
	}

	public List<Initializer> getInitializers() {
		return initializers;
	}

	public void setInitializers(List<Initializer> initializers) {
		this.initializers = initializers;
	}

	public WorkerStatusOpt getWorkerStatusOpt() {
		return workerStatusOpt;
	}

	public void setWorkerStatusOpt(WorkerStatusOpt workerStatusOpt) {
		this.workerStatusOpt = workerStatusOpt;
	}

	public ProbeMasterChecker getProbeMasterChecker() {
		return probeMasterChecker;
	}

	public void setProbeMasterChecker(ProbeMasterChecker probeMasterChecker) {
		this.probeMasterChecker = probeMasterChecker;
	}

	public Process getHdfs() {
		return hdfs;
	}

	public void setHdfs(Process hdfs) {
		this.hdfs = hdfs;
	}

	public Process getHbase() {
		return hbase;
	}

	public void setHbase(Process hbase) {
		this.hbase = hbase;
	}

}

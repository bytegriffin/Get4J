package com.bytegriffin.get4j.store;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.Initializer;
import com.bytegriffin.get4j.core.UrlQueue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * Redis负责分布式存储Url
 */
public class RedisStorage extends Initializer {

	private static final Logger logger = LogManager.getLogger(RedisStorage.class);

	private static final String host_split = ",";
	private static final int default_port = 6379;

	private static RedisStorage redisStorage;
	
	public static Jedis jedis;
	public static ShardedJedis sharedJedis;
	public static JedisCluster jedisCluster;

	private String redisMode;
	private String redisAddress;
	private String redisAuth;

	public RedisStorage(String redisMode, String redisAddress, String redisAuth) {
		this.redisMode = redisMode;
		this.redisAddress = redisAddress;
		this.redisAuth = redisAuth;
		redisStorage = this;
	}

	@Override
	public void init() {
		if (Strings.isNullOrEmpty(redisMode) || Strings.isNullOrEmpty(redisAddress)) {
			logger.error("分布式环境下组件RedisStorage在初始化时参数为空。");
			System.exit(1);
		}
		if ("standalone".equalsIgnoreCase(redisMode)) {
			standalone(redisAddress, redisAuth);
		} else if ("sharded".equalsIgnoreCase(redisMode)) {
			sharded(redisAddress, redisAuth);
		} else if ("cluster".equalsIgnoreCase(redisMode)) {
			cluster(redisAddress, redisAuth);
		}
		UrlQueue.registerRedisQueue(new RedisQueue<String>());
		logger.info("分布式环境下组件RedisStorage的初始化完成。");
	}

	private static JedisPoolConfig config() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(1000);
		config.setMaxIdle(5);
		config.setMinIdle(1);
		config.setBlockWhenExhausted(true);
		config.setMaxWaitMillis(60 * 1000);
		config.setTestOnBorrow(true);
		config.setTestOnReturn(true);
		config.setTestWhileIdle(true);
		config.setTimeBetweenEvictionRunsMillis(60 * 1000);
		return config;
	}

	/**
	 * 单机
	 * @param address 地址
	 * @param password 密码，可以为空
	 */
	private void standalone(String address, String password) {
		String firstAddress = Splitter.on(host_split).trimResults().split(address).iterator().next();
		List<String> list = Splitter.on(":").trimResults().splitToList(firstAddress);
		String host = "";
		Integer port = null;
		if (list.size() == 1) {
			host = Strings.isNullOrEmpty(list.get(0)) ? null : list.get(0);
			port = default_port;
		} else if (list.size() == 2) {
			host = Strings.isNullOrEmpty(list.get(0)) ? null : list.get(0);
			port = Strings.isNullOrEmpty(list.get(1)) ? null : Integer.valueOf(list.get(1));
		} else {
			logger.error("分布式环境下组件RedisStorage在standalone模式下初始化时发现redis地址格式有问题：{}", address);
			System.exit(1);
		}
		JedisPool jedisPool = null;
		try {
			if (Strings.isNullOrEmpty(password)) {
				jedisPool = new JedisPool(config(), host, port);
			} else {
				jedisPool = new JedisPool(config(), host, port, 2000, password);
			}
			jedis = jedisPool.getResource();
		} catch (Exception e) {
			logger.error("分布式环境下的组件RedisStorage在standalone模式下初始化链接失败。", e);
			System.exit(1);
		}
	}

	/**
	 * 分片 客户端集群
	 * @param addresses 地址集合，用逗号隔开
	 * @param password 密码，可以为空
	 */
	private void sharded(String addresses, String password) {
		List<String> list = Splitter.on(host_split).trimResults().splitToList(addresses);
		List<JedisShardInfo> shards = Lists.newArrayList();
		for (String addr : list) {
			List<String> hostAndPort = Splitter.on(":").trimResults().splitToList(addr);
			if (hostAndPort.size() == 1) {
				JedisShardInfo node = new JedisShardInfo(hostAndPort.get(0), default_port);
				if (!Strings.isNullOrEmpty(password)) {
					node.setPassword(password);
				}
				shards.add(node);
			} else if (hostAndPort.size() == 2) {
				JedisShardInfo node = new JedisShardInfo(hostAndPort.get(0), Integer.valueOf(hostAndPort.get(1)));
				if (!Strings.isNullOrEmpty(password)) {
					node.setPassword(password);
				}
				shards.add(node);
			}
		}
		if (shards == null || shards.isEmpty()) {
			logger.error("分布式环境下组件RedisStorage在Sharded模式下初始化时发现redis地址格式有问题。");
			System.exit(1);
		}
		ShardedJedisPool shardedJedisPool = null;
		try {
			shardedJedisPool = new ShardedJedisPool(config(), shards);
			sharedJedis = shardedJedisPool.getResource();
		} catch (Exception e) {
			logger.error("分布式环境下组件RedisStorage在Sharded模式下初始化链接失败。", e);
			System.exit(1);
		}
	}

	/**
	 * 集群 redis 3以上支持cluster
	 * @param addresses 地址集合，用逗号隔开
	 * @param password 密码，可以为空
	 */
	private void cluster(String addresses, String password) {
		List<String> list = Splitter.on(host_split).trimResults().splitToList(addresses);
		Set<HostAndPort> clusterNodes = Sets.newHashSet();
		for (String addr : list) {
			List<String> hostAndPort = Splitter.on(":").trimResults().splitToList(addr);
			if (hostAndPort.size() == 1) {
				clusterNodes.add(new HostAndPort(hostAndPort.get(0), default_port));
			} else if (hostAndPort.size() == 2) {
				clusterNodes.add(new HostAndPort(hostAndPort.get(0), Integer.valueOf(hostAndPort.get(1))));
			}
		}
		if (clusterNodes == null || clusterNodes.isEmpty()) {
			logger.error("分布式环境下组件RedisStorage在Cluster模式下初始化时发现redis地址格式有问题。");
			System.exit(1);
		}

		try {
			if (Strings.isNullOrEmpty(redisStorage.redisAuth)) {
				jedisCluster = new JedisCluster(clusterNodes, 2000, 2000, 5, config());
			} else {
				jedisCluster = new JedisCluster(clusterNodes, 2000, 2000, 5, redisStorage.redisAuth,
						config());
			}
		} catch (Exception e) {
			logger.error("分布式环境下组件RedisStorage在Cluster模式下初始化链接失败。", e);
			System.exit(1);
		} 
	}

}

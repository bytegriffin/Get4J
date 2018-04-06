package com.bytegriffin.get4j.store;

import java.io.IOException;
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
import redis.clients.jedis.JedisCommands;
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
	private JedisPool jedisPool;
	private ShardedJedisPool shardedJedisPool;
	private Set<HostAndPort> clusterNodes;

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
		if (Strings.isNullOrEmpty(redisMode) || Strings.isNullOrEmpty(redisAddress)
				|| Strings.isNullOrEmpty(redisAuth)) {
			logger.error("Cluster模式下的组件RedisStorage在初始化时参数为空。");
			System.exit(1);
		}
		if ("one".equalsIgnoreCase(redisMode)) {
			standalone(redisAddress, redisAuth);
		} else if ("sharded".equalsIgnoreCase(redisMode)) {
			sharded(redisAddress, redisAuth);
		} else if ("cluster".equalsIgnoreCase(redisMode)) {
			cluster(redisAddress, redisAuth);
		}
		UrlQueue.registerRedisQueue(new RedisQueue<String>());
		logger.info("Cluster模式下的组件RedisStorage的初始化完成。");
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
	 * jedis线程不安全，只能从pool中获取
	 * @return JedisCommands
	 */
	public  static JedisCommands newJedis() {
		return getConnection();
	}
	
	/**
	 * 关闭链接
	 * @param jedis JedisCommands
	 */
	public static void close(JedisCommands jedis) {
		if ("standalone".equalsIgnoreCase(redisStorage.redisMode)) {
			Jedis one = (Jedis) jedis;
			if(one != null){
				one.close();
			}
		} else if ("sharded".equalsIgnoreCase(redisStorage.redisMode)) {
			ShardedJedis sharedJedis = (ShardedJedis)jedis;
			if(sharedJedis != null){
				sharedJedis.close();
			}
		} else if ("cluster".equalsIgnoreCase(redisStorage.redisMode)) {
			try {
				JedisCluster  jedisCluster = (JedisCluster) jedis;
				if(jedisCluster != null){
					jedisCluster.close();
				}
			} catch (IOException e) {
				logger.error("Cluster模式下的组件RedisStorage关闭链接失败。", e);
			}
		}
	}

	private synchronized static JedisCommands getConnection() {
		JedisCommands jedis = null;
		if ("standalone".equalsIgnoreCase(redisStorage.redisMode)) {
			Jedis one = null;
			try {
				one = redisStorage.jedisPool.getResource();
				jedis = one;
			} catch (Exception e) {
				logger.error("Cluster模式下的组件RedisStorage初始化链接失败。", e);
				System.exit(1);
			}
		} else if ("sharded".equalsIgnoreCase(redisStorage.redisMode)) {
			ShardedJedis sharedJedis = null;
			try {
				sharedJedis = redisStorage.shardedJedisPool.getResource();
				jedis = sharedJedis;
			} catch (Exception e) {
				logger.error("Cluster模式下的组件RedisStorage初始化链接失败。", e);
				System.exit(1);
			}
		} else if ("cluster".equalsIgnoreCase(redisStorage.redisMode)) {
			JedisCluster  jedisCluster = null;
			try {
				if (Strings.isNullOrEmpty(redisStorage.redisAuth)) {
					jedisCluster = new JedisCluster(redisStorage.clusterNodes, 2000, 2000, 5, config());
				} else {
					jedisCluster = new JedisCluster(redisStorage.clusterNodes, 2000, 2000, 5, redisStorage.redisAuth,
							config());
				}
				jedis = jedisCluster;
			} catch (Exception e) {
				logger.error("Cluster模式下的组件RedisStorage初始化链接失败。", e);
				System.exit(1);
			} 
		}
		return jedis;
	}

	/**
	 * 单机
	 * @param address 地址
	 * @param password 密码，可以为空
	 */
	public void standalone(String address, String password) {
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
			logger.error("Cluster模式下的组件RedisStorage在初始化时发现redis地址格式有问题：{}", address);
			System.exit(1);
		}
		if (Strings.isNullOrEmpty(password)) {
			jedisPool = new JedisPool(config(), host, port);
		} else {
			jedisPool = new JedisPool(config(), host, port, 2000, password);
		}
	}

	/**
	 * 分片 客户端集群
	 * @param addresses 地址集合，用逗号隔开
	 * @param password 密码，可以为空
	 */
	public void sharded(String addresses, String password) {
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
			logger.error("Cluster模式下的组件RedisStorage在初始化时发现redis地址格式有问题。");
			System.exit(1);
		}
		shardedJedisPool = new ShardedJedisPool(config(), shards);

	}

	/**
	 * 集群 redis 3以上支持cluster
	 * @param addresses 地址集合，用逗号隔开
	 * @param password 密码，可以为空
	 */
	public void cluster(String addresses, String password) {
		List<String> list = Splitter.on(host_split).trimResults().splitToList(addresses);
		clusterNodes = Sets.newHashSet();
		for (String addr : list) {
			List<String> hostAndPort = Splitter.on(":").trimResults().splitToList(addr);
			if (hostAndPort.size() == 1) {
				clusterNodes.add(new HostAndPort(hostAndPort.get(0), default_port));
			} else if (hostAndPort.size() == 2) {
				clusterNodes.add(new HostAndPort(hostAndPort.get(0), Integer.valueOf(hostAndPort.get(1))));
			}
		}
		if (clusterNodes == null || clusterNodes.isEmpty()) {
			logger.error("Cluster模式下的组件RedisStorage在初始化时发现redis地址格式有问题。");
			System.exit(1);
		}
	}

}

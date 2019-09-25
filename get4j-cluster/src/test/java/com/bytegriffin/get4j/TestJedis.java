package com.bytegriffin.get4j;

import com.bytegriffin.get4j.store.RedisStorage;

public class TestJedis {

	@SuppressWarnings("static-access")
	public static void main(String[] args){
		RedisStorage redis = new RedisStorage("cluster","192.168.1.102:6374,192.168.1.102:6375,192.168.1.102:6376,"
				+ "192.168.1.102:6377,192.168.1.102:6378,192.168.1.102:6379","");
		redis.init();
		//获取总数
		System.out.println(redis.jedisCluster.zcard("UN_VISITED_LINKS_seed123"));
		//获取第一个
		System.out.println(redis.jedisCluster.zrange("UN_VISITED_LINKS_seed123", 0, 0));
		//删除第一个
		System.out.println(redis.jedisCluster.zremrangeByRank("UN_VISITED_LINKS_seed123", 8, 9));
		//增加一个
		//System.out.println(redis.jedisCluster.zadd("UN_VISITED_LINKS_seed123", 0, "http://www.aaa.com"));
		// 判断元素是否存在于某集合内
		System.out.println(redis.jedisCluster.zrank("UN_VISITED_LINKS_seed123", "http://www.baidu.com/index2.jsp"));
		// 返回集合
		System.out.println(redis.jedisCluster.zrange("UN_VISITED_LINKS_seed123", 0, -1));
		
	}
	
}

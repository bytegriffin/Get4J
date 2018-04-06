package com.bytegriffin.get4j;

import com.bytegriffin.get4j.store.RedisStorage;

public class TestJedis {

	@SuppressWarnings("static-access")
	public static void main(String[] args){
		RedisStorage redis = new RedisStorage("one","192.168.1.13:6379","foobared");
		redis.init();
		//获取总数
		System.out.println(redis.newJedis().zcard("UN_VISITED_2LINKS_seed123"));
		//获取第一个
		System.out.println(redis.newJedis().zrange("UN_VISITED_LINKS_seed123", 0, 0));
		//删除第一个
		System.out.println(redis.newJedis().zremrangeByRank("UN_VISITED_LINKS_seed123", 8, 9));
		//增加一个
		//System.out.println(redis.jedis.zadd("UN_VISITED_LINKS_seed123", 0, "http://www.aaa.com"));
		// 判断元素是否存在于某集合内
		System.out.println(redis.newJedis().zrank("UN_VISITED_LINKS_seed123", "http://www.baidu.com/index2.jsp"));
		// 返回集合
		System.out.println(redis.newJedis().zrange("UN_VISITED_LINKS_seed123", 0, -1));
	}
	
}

package com.bytegriffin.get4j.store;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.bytegriffin.get4j.util.Queue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ShardedJedis;

public class RedisQueue<E> implements Queue<E> {

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLock.readLock();

	private final Lock writeLock = readWriteLock.writeLock();

	private LinkedList<E> list = Lists.newLinkedList();
	// key：redis_key value：score count
	private final Map<String, Integer> score_map = Maps.newHashMap();
	
	@Override
	public void add(E e) {
		writeLock.lock();
		try {
			if (!contains(e)) {
				list.add(e);
			}
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void add(String key, E e) {
		writeLock.lock();
		try {
			if (!contains(e)) {
				int count = 0;
				if (score_map.containsKey(key)) {
					count = score_map.get(key) + 1;
				}
				if(RedisStorage.jedisCluster != null) {
					JedisCluster jedisCluster = RedisStorage.jedisCluster;
					jedisCluster.zadd(key, count, (String) e);
				} else if(RedisStorage.sharedJedis != null) {
					ShardedJedis sharedJedis = RedisStorage.sharedJedis;
					sharedJedis.zadd(key, count, (String) e);
				} else {
					Jedis jedis = RedisStorage.jedis;
					jedis.zadd(key, count, (String) e);
				}
				score_map.put(key, count);
			}
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public E get(int index) {
		readLock.lock();
		try {
			return list.get(index);
		} finally {
			readLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(String queueName, int index) {
		readLock.lock();
		try {
			Set<String> set = null;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				set = jedisCluster.zrange(queueName, 0, index);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				set = sharedJedis.zrange(queueName, 0, index);
			} else {
				Jedis jedis = RedisStorage.jedis;
				set = jedis.zrange(queueName, 0, index);
			}
			if (set == null || set.isEmpty()) {
				return null;
			}
			return (E) set.iterator().next();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public long size() {
		readLock.lock();
		try {
			return list.size();
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public long size(String queueName) {
		readLock.lock();
		try {
			Long size = 0l;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				size = jedisCluster.zcard(queueName) == null? 0l : jedisCluster.zcard(queueName);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				size = sharedJedis.zcard(queueName) == null? 0l : sharedJedis.zcard(queueName);
			} else {
				Jedis jedis = RedisStorage.jedis;
				size = jedis.zcard(queueName) == null? 0l : jedis.zcard(queueName);
			}
			return size;
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public void clear() {
		writeLock.lock();
		try {
			list.clear();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void clear(String queueName) {
		writeLock.lock();
		try {
			list.clear();
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				jedisCluster.zremrangeByRank(queueName, 0, -1);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				sharedJedis.zremrangeByRank(queueName, 0, -1);
			} else {
				Jedis jedis = RedisStorage.jedis;
				jedis.zremrangeByRank(queueName, 0, -1);
			}
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		writeLock.lock();
		try {
			return list.isEmpty();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean isEmpty(String queueName) {
		writeLock.lock();
		boolean isExist = false;
		try {
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				isExist = !jedisCluster.exists(queueName);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				isExist = !sharedJedis.exists(queueName);
			} else {
				Jedis jedis = RedisStorage.jedis;
				isExist = !jedis.exists(queueName);
			}
			return isExist;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public E outFirst() {
		writeLock.lock();
		try {
			if (!list.isEmpty()) {
				return list.removeFirst();
			}
			return null;
		} finally {
			writeLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public E outFirst(String queueName) {
		writeLock.lock();
		try {
			long count = 0;
			Set<String> url = null;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				url = jedisCluster.zrange(queueName, 0, 0);
				if (url != null && url.size() > 0) {
					count = jedisCluster.zremrangeByRank(queueName, 0, 0);
				}
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				url = sharedJedis.zrange(queueName, 0, 0);
				if (url != null && url.size() > 0) {
					count = sharedJedis.zremrangeByRank(queueName, 0, 0);
				}
			} else {
				Jedis jedis = RedisStorage.jedis;
				url = jedis.zrange(queueName, 0, 0);
				if (url != null && url.size() > 0) {
					count = jedis.zremrangeByRank(queueName, 0, 0);
				}
			}
			if (count == 1) {
				return (E) url.iterator().next();
			}
			return null;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public boolean contains(E e) {
		readLock.lock();
		try {
			return list.contains(e);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public boolean contains(String queueName, E e) {
		readLock.lock();
		try {
			Long count = 0l;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				count = jedisCluster.zrank(queueName, (String) e) == null ? 0l: jedisCluster.zrank(queueName, (String) e);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				count = sharedJedis.zrank(queueName, (String) e) == null ? 0l: sharedJedis.zrank(queueName, (String) e);
			} else {
				Jedis jedis = RedisStorage.jedis;
				count = jedis.zrank(queueName, (String) e) == null ? 0l: jedis.zrank(queueName, (String) e);
			}
			if (count > 0) {
				return true;
			}
			return false;
		} finally {
			readLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Queue<E> getQueue(String queueName) {
		writeLock.lock();
		try {
			Set<String> set = null;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				set = jedisCluster.zrange(queueName, 0, -1);
				
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				set = sharedJedis.zrange(queueName, 0, -1);
			} else {
				Jedis jedis = RedisStorage.jedis;
				set = jedis.zrange(queueName, 0, -1);
			}
			RedisQueue<E> queue = new RedisQueue<E>();
			queue.list.clear();
			for (String str : set) {
				queue.list.add((E) str);
			}
			return queue;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public LinkedList<E> getAll() {
		readLock.lock();
		try {
			return list;
		} finally {
			readLock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<E> getAll(String queueName) {
		readLock.lock();
		try {
			Set<E> set = null;
			if(RedisStorage.jedisCluster != null) {
				JedisCluster jedisCluster = RedisStorage.jedisCluster;
				set = (Set<E>) jedisCluster.zrange(queueName, 0, -1);
			} else if(RedisStorage.sharedJedis != null) {
				ShardedJedis sharedJedis = RedisStorage.sharedJedis;
				set = (Set<E>) sharedJedis.zrange(queueName, 0, -1);
			} else {
				Jedis jedis = RedisStorage.jedis;
				set = (Set<E>) jedis.zrange(queueName, 0, -1);
			}
			return set;
		} finally {
			readLock.unlock();
		}
	}

}

package com.bytegriffin.get4j.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.util.ConcurrentQueue;
import com.bytegriffin.get4j.util.Queue;

/**
 * Url队列 <br>
 * 负责爬虫中全部的url处理 <br>
 */
public final class UrlQueue {

	// key: seed name value: 该seed所涉及的所有已访问过的链接
	private static ConcurrentHashMap<String, Queue<String>> VISITED_LINKS = new ConcurrentHashMap<>();
	// key: seed name value: 该seed所涉及的所有未访问过的链接
	private static ConcurrentHashMap<String, Queue<String>> UN_VISITED_LINKS = new ConcurrentHashMap<>();
	// key: seed name value: 该seed所涉及的所有访问失败的所有url
	private static ConcurrentHashMap<String, Queue<String>> FAIL_VISITED_URLS = new ConcurrentHashMap<>();
	// key: seed name value: 该seed所涉及的所有已访问过的资源文件 css/js等
	private static ConcurrentHashMap<String, Queue<String>> VISITED_RESOURCES = new ConcurrentHashMap<>();
	// key: seed name value: 该seed所涉及的所有未访问过的资源文件 css/js等
	private static ConcurrentHashMap<String, Queue<String>> UN_VISITED_RESOURCES = new ConcurrentHashMap<>();
	// for redis prefix key name
	private static String visitedLinks_prefix = "visited_links_";
	private static String unVisitedLinks_prefix = "un_visited_links_";
	private static String failVisitedUrls_prefix = "fail_visited_urls_";
	private static String visitedResources_prefix = "visited_resources_";
	private static String unVisitedResources_prefix = "un_visited_resources_";

	private static Queue<String> redis_queue;
	// 是否加载本地Queue，否则加载RedisQueue
	private static boolean isLocalQueue = true;

	public static void registerRedisQueue(Queue<String> redisQueue) {
		redis_queue = redisQueue;
		isLocalQueue = false;
	}

	public static long getVisitedUrlCount(String seedName) {
		if (isLocalQueue) {
			return getSize(VISITED_LINKS.get(seedName));
		} else {
			return redis_queue.size(visitedLinks_prefix + seedName);
		}
	}

	public static long getUnVisitedUrlCount(String seedName) {
		if (isLocalQueue) {
			return getSize(UN_VISITED_LINKS.get(seedName));
		} else {
			return redis_queue.size(unVisitedLinks_prefix + seedName);
		}
	}

	public static long getFailVisitedUrlCount(String seedName) {
		if (isLocalQueue) {
			return getSize(FAIL_VISITED_URLS.get(seedName));
		} else {
			return redis_queue.size(failVisitedUrls_prefix + seedName);
		}
	}

	private static long getSize(Queue<String> queues) {
		if (queues == null || queues.isEmpty()) {
			return 0;
		}
		return queues.size();
	}

	/**	
	 * 追加未访问的links队列<br>
	 * 首先判断新抓取的link是否在已访问的队列中，<br>
	 * 然后判断是否在未抓取的队列中<br>
	 * 如果都不在的话则将其加进未访问的队列中<br>
	 *
	 * @param seedName    String
	 * @param links  HashSet
	 */
	public synchronized static void addUnVisitedLinks(String seedName, HashSet<String> links) {
		if (links == null || links.size() == 0) {
			return;
		}
		if (isLocalQueue) {
			for (String link : links) {
				Queue<String> queues = UN_VISITED_LINKS.get(seedName);
				if (queues == null) {
					queues = new ConcurrentQueue<>();
					UN_VISITED_LINKS.put(seedName, queues);
				}
				Queue<String> visitedLinks = VISITED_LINKS.get(seedName);
				if (visitedLinks == null || !visitedLinks.contains(link)) {
					queues.add(link);
				}
			}
		} else {
				for (String link : links) {
					if(!redis_queue.contains(visitedLinks_prefix + seedName, link)){
						redis_queue.add(unVisitedLinks_prefix + seedName, link);
					}
				}
		}
	}

	/**
	 * 新增未访问的link到队列中
	 *
	 * @param seedName    String
	 * @param link  String
	 */
	public synchronized static void newUnVisitedLink(String seedName, String link) {
		link = UrlAnalyzer.filterUrlPound(link);
		Queue<String> queues = null;
		if (isLocalQueue) {
			queues = UN_VISITED_LINKS.get(seedName);
			if (queues == null) {
				queues = new ConcurrentQueue<>();
				UN_VISITED_LINKS.put(seedName, queues);
			}
			Queue<String> visitedLinks = VISITED_LINKS.get(seedName);
			if (visitedLinks == null || !visitedLinks.contains(link)) {
				queues.add(link);
			}
		} else {
			if(!redis_queue.contains(visitedLinks_prefix + seedName, link)){
				redis_queue.add(unVisitedLinks_prefix + seedName, link);
			}
		}
	}

	/**
	 * 删除头元素
	 * @param seedName String
	 * @return String
	 */
	public static String outFirst(String seedName) {
		if (isLocalQueue) {
			return UN_VISITED_LINKS.get(seedName).outFirst();
		} else {
			return redis_queue.outFirst(unVisitedLinks_prefix + seedName);
		}
	}
	
	/**
	 * 获取头元素
	 * @param seedName String
	 * @return String
	 */
	public static String getFirst(String seedName) {
		if (isLocalQueue) {
			return UN_VISITED_LINKS.get(seedName).get(0);
		} else {
			return redis_queue.get(unVisitedLinks_prefix + seedName, 0);
		}
	}

	/**
	 * 判断未访问链接是否为空
	 * @param seedName String
	 * @return boolean
	 */
	public static boolean isEmptyUnVisitedLinks(String seedName) {
		if (isLocalQueue) {
			return UN_VISITED_LINKS.get(seedName).isEmpty();
		} else {
			return redis_queue.isEmpty(unVisitedLinks_prefix + seedName);
		}
	}

	/**
	 * 新增未访问的resource到队列中
	 *
	 * @param seedName   String
	 * @param resourceLink   String
	 */
	public static void newUnVisitedResource(String seedName, String resourceLink) {
		Queue<String> queues = null;
		if (isLocalQueue) {
			queues = UN_VISITED_RESOURCES.get(seedName);
			if (queues == null) {
				queues = new ConcurrentQueue<>();
				UN_VISITED_RESOURCES.put(seedName, queues);
			}
			Queue<String> visitedResources = VISITED_RESOURCES.get(seedName);
			if (visitedResources == null || !visitedResources.contains(resourceLink)) {
				queues.add(resourceLink);
			}
		} else {
			if(!redis_queue.contains(visitedResources_prefix + seedName, resourceLink)){
				redis_queue.add(unVisitedResources_prefix + seedName, resourceLink);
			}
		}
	}

	/**
	 * 新增已访问的link到队列中
	 *
	 * @param seedName    String
	 * @param link   String
	 */
	public static void newVisitedLink(String seedName, String link) {
		Queue<String> queues = null;
		if (isLocalQueue) {
			queues = VISITED_LINKS.get(seedName);
			if (queues == null) {
				queues = new ConcurrentQueue<>();
				VISITED_LINKS.put(seedName, queues);
			}
			Queue<String> visitedLinks = VISITED_LINKS.get(seedName);
			if (visitedLinks == null || !visitedLinks.contains(link)) {
				queues.add(link);
			}
		} else {
			if(!redis_queue.contains(visitedLinks_prefix + seedName, link)){
				redis_queue.add(visitedLinks_prefix + seedName, link);
			}
		}
	}

	/**
	 * 新增已访问的resource到队列中
	 *
	 * @param seedName   String
	 * @param resource   String
	 */
	public static void newVisitedResource(String seedName, String resource) {
		Queue<String> queues = null;
		if (isLocalQueue) {
			queues = VISITED_RESOURCES.get(seedName);
			if (queues == null) {
				queues = new ConcurrentQueue<>();
				VISITED_RESOURCES.put(seedName, queues);
			}
			Queue<String> visitedResources = VISITED_RESOURCES.get(seedName);
			if (visitedResources == null || !visitedResources.contains(resource)) {
				queues.add(resource);
			}
		} else {
			if(!redis_queue.contains(visitedResources_prefix + seedName, resource)){
				redis_queue.add(visitedResources_prefix + seedName, resource);
			}
		}
	}

	/**
	 * 增加已访问失败的url（包括link、资源文件）
	 *
	 * @param seedName     String
	 * @param failurl  String
	 */
	public static void newFailVisitedUrl(String seedName, String failurl) {
		Queue<String> queues = null;
		if (isLocalQueue) {
			queues = FAIL_VISITED_URLS.get(seedName);
			if (queues == null) {
				queues = new ConcurrentQueue<>();
				FAIL_VISITED_URLS.put(seedName, queues);
			}
			Queue<String> failVisitedUrls = FAIL_VISITED_URLS.get(seedName);
			if (failVisitedUrls == null || !failVisitedUrls.contains(failurl)) {
				queues.add(failurl);
			}
		} else {
			if(!redis_queue.contains(failVisitedUrls_prefix + seedName, failurl)){
				redis_queue.add(failVisitedUrls_prefix + seedName, failurl);
			}
		}
	}

	/**
	 * 获取未访问link队列
	 *
	 * @param seedName  String
	 * @return ConcurrentQueue
	 */
	public static Queue<String> getUnVisitedLink(String seedName) {
		if (isLocalQueue) {
			return UN_VISITED_LINKS.get(seedName);
		} else {
			return redis_queue.getQueue(unVisitedLinks_prefix + seedName);
		}
	}

	/**
	 * 获取未访问resource队列
	 *
	 * @param seedName     String
	 * @return ConcurrentQueue
	 */
	public static Queue<String> getUnVisitedResource(String seedName) {
		if (isLocalQueue) {
			return UN_VISITED_RESOURCES.get(seedName);
		} else {
			return redis_queue.getQueue(unVisitedResources_prefix + seedName);
		}
	}

	/**
	 * 获取已访问link队列
	 *
	 * @param seedName  String
	 * @return ConcurrentQueue
	 */
	public static Queue<String> getVisitedLink(String seedName) {
		if (isLocalQueue) {
			return VISITED_LINKS.get(seedName);
		} else {
			return redis_queue.getQueue(visitedLinks_prefix + seedName);
		}
	}

	/**
	 * 清空已访问link队列
	 * 
	 * @param seedName String
	 */
	public static void clearVisitedLink(String seedName) {
		if (isLocalQueue) {
			Queue<String> visitedlink = getVisitedLink(seedName);
			if (visitedlink != null && !visitedlink.isEmpty()) {
				visitedlink.clear();
			}
		} else {
			 redis_queue.clear(visitedLinks_prefix + seedName);
		}
	}
	
	/**
	 * 清空已访问失败url队列
	 * 
	 * @param seedName String
	 */
	public static void clearFailVisitedUrl(String seedName) {
		if (isLocalQueue) {
			Queue<String> failVisitedUrl = FAIL_VISITED_URLS.get(seedName);
			if (failVisitedUrl != null && !failVisitedUrl.isEmpty()) {
				failVisitedUrl.clear();
			}
		} else {
			 redis_queue.clear(failVisitedUrls_prefix + seedName);
		}
	}

	/**
	 * 获取已访问resource队列
	 * 
	 * @param seedName  String
	 * @return ConcurrentQueue
	 */
	public static Queue<String> getVisitedResource(String seedName) {
		if (isLocalQueue) {
			return VISITED_RESOURCES.get(seedName);
		} else {
			return redis_queue.getQueue(visitedResources_prefix + seedName);
		}
	}

	/**
	 * 清空已访问resource队列
	 * 
	 * @param seedName String
	 */
	public static void clearVisitedResource(String seedName) {
		if (isLocalQueue) {
			Queue<String> visitedresource = getVisitedResource(seedName);
			if (visitedresource != null && !visitedresource.isEmpty()) {
				visitedresource.clear();
			}
		} else {
			 redis_queue.clear(visitedResources_prefix + seedName);
		}
	}

	/**
	 * 获取已访问失败的url队列
	 *
	 * @param seedName  String
	 * @return ConcurrentQueue
	 */
	public static Collection<String> getFailVisitedUrl(String seedName) {
		if (isLocalQueue) {
			return FAIL_VISITED_URLS.get(seedName).getAll();
		} else {
			return redis_queue.getAll(failVisitedUrls_prefix + seedName);
		}
	}

}

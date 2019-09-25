package com.bytegriffin.get4j.core;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.lucene.index.IndexWriter;
import org.bson.Document;
import org.openqa.selenium.WebDriver;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxySelector;
import com.bytegriffin.get4j.net.http.SleepRandomSelector;
import com.bytegriffin.get4j.net.http.UserAgentSelector;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.probe.PageChangeProber;
import com.bytegriffin.get4j.send.EmailSender;
import com.google.common.collect.Maps;
import com.mongodb.client.MongoCollection;

import okhttp3.OkHttpClient;

/**
 * 全局变量：缓存供全局访问的变量
 */
public final class Globals {
	
	/**
	 * 种子seed缓存，key : seed name key value : seed 
	 */
	public static Map<String, Seed> SEED_CACHE = Maps.newHashMap();

    /**
     * 当PageMode存放list列表url key：seed_name value：该seed下所有的列表url
     */
    public static Map<String, List<String>> LIST_URLS_CACHE = Maps.newHashMap();

    /**
     * 全局chain工作流缓存 key:seed_name value: site
     */
    public static final Map<String, Chain> CHAIN_CACHE = Maps.newHashMap();

    /**
     * 全局http_proxy缓存 key:seed_name value: HttpProxySelector
     */
    public static final Map<String, HttpProxySelector> HTTP_PROXY_CACHE = Maps.newHashMap();

    /**
     * 全局user_agent缓存 key:seed_name value: UserAgentSelector
     */
    public static final Map<String, UserAgentSelector> USER_AGENT_CACHE = Maps.newHashMap();

    /**
     * 全局http请求间隔缓存 key:seed_name value: fetch.sleep
     */
    public static final Map<String, Integer> FETCH_SLEEP_CACHE = Maps.newHashMap();

    /**
     * 抓取页面变化探测器缓存 key:seed_name value: PageChangeProber
     */
    public static final Map<String, PageChangeProber> FETCH_PROBE_CACHE = Maps.newHashMap();

    /**
     * 全局sleep_selector缓存 key:seed_name value: SleepRangeSelector
     */
    public static final Map<String, SleepRandomSelector> FETCH_SLEEP_RANGE_CACHE = Maps.newHashMap();

    /**
     * 全局okhttpclientbuilder缓存 key:seed_name value: HttpClientBuilder
     */
    public static final Map<String, OkHttpClient.Builder> OK_HTTP_CLIENT_BUILDER_CACHE = Maps.newHashMap();

    /**
     * 全局download dir缓存 key:seed_name value: download dir
     */
    public static final Map<String, String> DOWNLOAD_DISK_DIR_CACHE = Maps.newHashMap();
    
    /**
     * key:seed_name  value: hdfs://address:port/seedname/
     */
    public static final Map<String, String> DOWNLOAD_HDFS_DIR_CACHE = Maps.newHashMap();

    /**
     * 全局lucene index dir缓存 key:seed_name value: lucene index dir
     */
    public static final Map<String, String> LUCENE_INDEX_DIR_CACHE = Maps.newHashMap();

    /**
     * 全局PageMode缓存 key:seed_name value: PageMode
     */
    public static final Map<String, PageMode> FETCH_PAGE_MODE_CACHE = Maps.newHashMap();

    /**
     * LIST_DETAIL模式下的详情页面url选择
     */
    public static final Map<String, String> FETCH_DETAIL_SELECT_CACHE = Maps.newHashMap();

    /**
     * 全局FetchFilter缓存 key:seed_name value: FetchResourceUrl
     */
    public static final Map<String, FetchResourceSelector> FETCH_RESOURCE_SELECTOR_CACHE = Maps.newHashMap();

    /**
     * 自定义的page_parser解析器缓存
     */
    public static final Map<String, PageParser> PAGE_PARSER_CACHE = Maps.newHashMap();

    /**
     * key : seedName value: datasource
     */
    public static Map<String, DataSource> DATASOURCE_CACHE = Maps.newHashMap();

    /**
     * key : seedName value: MongoCollection
     */
    public static Map<String, MongoCollection<Document>> MONGO_COLLECTION_CACHE = Maps.newHashMap();

    /**
     * key : seedName value: IndexWriter
     */
    public static Map<String, IndexWriter> INDEX_WRITER_CACHE = Maps.newHashMap();

    /**
     * 全局WebDriver缓存 key:seed_name value: WebDriver
     */
    public static final Map<String, WebDriver> WEBDRIVER_CACHE = Maps.newHashMap();
    
    /**
     * 全局Http探针缓存 key:seed_name value: HttpEngine
     */
    public static final Map<String, HttpEngine> HTTP_ENGINE_CACHE = Maps.newHashMap();

    /**
     * 全局Email发送器，如果配置了email.recipient就表示当系统出现异常发送邮件提醒
     */
    public static EmailSender emailSender;

    /**
     * key:seed_name | value:每次开始抓取时间
     */
    public static Map<String, String> PER_START_TIME_CACHE = Maps.newHashMap();

    /**
     * 当PageMode存放list列表url key：seed_name value：该seed对应的launcher
     */
    public static Map<String, Launcher> LAUNCHER_CACHE = Maps.newHashMap();

    /**
     * key：seedName value:field_map (key: name value: selector)
     */
    public static Map<String, Map<String,String>> DYNAMIC_FIELDS_CACHE = Maps.newHashMap();

}

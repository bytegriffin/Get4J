package com.bytegriffin.get4j;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.annotation.Cascade;
import com.bytegriffin.get4j.annotation.Config;
import com.bytegriffin.get4j.annotation.Field;
import com.bytegriffin.get4j.annotation.ListDetail;
import com.bytegriffin.get4j.annotation.Single;
import com.bytegriffin.get4j.annotation.Site;
import com.bytegriffin.get4j.annotation.Sync;
import com.bytegriffin.get4j.conf.AbstractConfig;
import com.bytegriffin.get4j.conf.ClusterNode;
import com.bytegriffin.get4j.conf.Configuration;
import com.bytegriffin.get4j.conf.ConfigurationXmlHandler;
import com.bytegriffin.get4j.conf.Context;
import com.bytegriffin.get4j.conf.CoreSeedsXmlHandler;
import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.DynamicField;
import com.bytegriffin.get4j.conf.DynamicFieldXmlHandler;
import com.bytegriffin.get4j.conf.ResourceSync;
import com.bytegriffin.get4j.conf.ResourceSyncYamlHandler;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.core.SpiderEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.parse.PageParser;
import com.bytegriffin.get4j.util.FileUtil;
import com.bytegriffin.get4j.util.MD5Util;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 爬虫入口类兼API<br>
 * 主要负责对内配置文件加载运行 和 对外的API调用
 */
public class Spider {

    private static final Logger logger = LogManager.getLogger(Spider.class);

    private static Spider me;

    private static Seed seed;

    private static ResourceSync resourceSync;
    
    private static Configuration configuration;
    
    private static DynamicField dynamicField;
    
    private Map<String, String> dynamicFieldMap;

    private Spider() {
    	DefaultConfig.closeHttpClientLog();
    	dynamicFieldMap = Maps.newHashMap();
    	seed = new Seed();
        resourceSync = new ResourceSync();
        configuration = new Configuration();
        dynamicField = new DynamicField();
    }

    private Spider(PageMode pageMode) {
    	DefaultConfig.closeHttpClientLog();
    	dynamicFieldMap = Maps.newHashMap();
    	seed = new Seed();
        resourceSync = new ResourceSync();
        configuration = new Configuration();
        dynamicField = new DynamicField();
        pageMode(pageMode);
    }

    /**
     * 设置种子名称<br>
     * 每个种子名称要唯一<br>
     *
     * @param seedName 种子名称
     * @return Spider
     */
    public Spider seedName(String seedName) {
        seed.setSeedName(seedName);
        return this;
    }

    /**
     * 抓取的页面模型<br>
     * 必填项。list_detail（列表-详情页面）、single（单个页面)、site（单个站点）、cascade（单页面上的所有链接）
     * 默认值是single。
     *
     * @param pageMode 页面模型
     * @return Spider
     */
    private Spider pageMode(PageMode pageMode) {
        seed.setPageMode(pageMode);
        return this;
    }

    /**
     * 设置抓取url <br>
     * 必填项。表示要抓取的Url，如果抓取模式pageMode为list_detail，该值为列表Url，
     * 其中可变的页数PageNum需要用大括号{}括起来
     *
     * @param fetchUrl 抓取url
     * @return Spider
     */
    public Spider fetchUrl(String fetchUrl) {
        seed.setFetchUrl(fetchUrl);
        return this;
    }
    
    /**
     * http请求方法<br>
     * 非必填项。抓取时Http的请求方法，默认值是get，可以配置为post，暂且只支持这两种。
     * @return Spider
     */
    public Spider post() {
    	return method("post");
    }

    private Spider method(String method) {
    	seed.setFetchHttpMethod(method);
    	return this;
    }

    /**
     * 设置页面变化探测器，用于探测抓取页面的变化，如果有变化就立刻抓取，没有变化则继续探测。
     * 默认是探测整个页面是否发生改变。
     *
     * @return Spider
     */
    public Spider defaultProbe() {
        seed.setFetchProbeSelector(DefaultConfig.default_value);
        seed.setFetchProbeSleep(DefaultConfig.default_value);
        return this;
    }

    /**
     * 设置页面变化探测器，用于探测抓取页面的变化，如果有变化就立刻抓取，没有变化则继续探测。
     * 此项值的格式支持Jsoup（针对html或xml）或者Jsonpath（针对json）文件
     *
     * @param selector     页面selector选择器
     * @param sleepSeconds 监控频率，单位：秒
     * @return Spider
     */
    public Spider probe(String selector, int sleepSeconds) {
        seed.setFetchProbeSelector(selector);
        seed.setFetchProbeSleep(sleepSeconds + "");
        return this;
    }

    /**
     * 爬虫工作线程数量，非必填项。默认值是1。
     *
     * @param count 爬虫线程数
     * @return Spider
     */
    public Spider thread(int count) {
        seed.setThreadCount(count);
        return this;
    }

    /**
     * 抓取的详情页面链接选择器 <br>
     * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，支持Jsoup原生的选择器（html内容）或Jsonpath（json内容）。 <br>
     * 当此内容为JsonPath字符串的时候，如果list页面的json中提供的detail的链接是相对路径，那么此时这个值的格式为：链接前缀+jsonpath，
     * 例如：http://www.aaa.com/bbb$.data.url；当此内容为空时，说明抓取的页面格式是普通页面，不存在详情页面。<br>
     * 注意：有种特殊情况，当Json属性中的内容是Html格式，并且Html里包含着详情页的链接时候，此时需要先写Jsonpath
     * 再写Jsoup选择器字符串，中间用竖杠隔开，例如： $.data.*|a.class[href]。
     *
     * @param detailSelector 详情页面选择器
     * @return Spider
     */
    public Spider detailSelector(String detailSelector) {
        seed.setFetchDetailSelector(detailSelector);
        return this;
    }

    /**
     * 抓取的列表总页数 <br>
     * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，动态获取页面中显示的总页数。
     * 支持Jsoup原生的选择器（html内容）或Jsonpath（json内容），默认值是1。
     *
     * @param totalPageSelector 总页数选择器
     * @return Spider
     */
    public Spider totalPages(String totalPageSelector) {
        seed.setFetchTotalPages(totalPageSelector);
        return this;
    }

    /**
     * 抓取的列表总页数 <br>
     * 非必填项，当抓取的页面格式属于【列表-详情】页时使用，直接定义抓取页数，默认值是1。
     *
     * @param totalPageNum 总页数
     * @return Spider
     */
    public Spider totalPages(int totalPageNum) {
        seed.setFetchTotalPages(String.valueOf(totalPageNum));
        return this;
    }

    /**
     * 抓取延迟策略<br>
     * 非必填项。每两次http请求之间的间隔时间(单位：秒)，以防止频繁访问站点抓取不到内容，默认值为0
     *
     * @param seconds 每次请求延迟，单位秒
     * @return Spider
     */
    public Spider sleep(int seconds) {
        seed.setFetchSleep(seconds);
        return this;
    }

    /**
     * 抓取随机延迟策略<br>
     * 非必填项。格式：2-7 表示2秒到7秒直接随机延迟，其中范围中间用横杠连接时间的上下限<br>
     * 每两次http请求之间的间隔时间(单位：秒)范围，每次请求时间间隔都是这个范围内的随机值，
     * 主要用于某些网站防爬策略检测出固定时间间隔访问网站的请求，默认值为0
     *
     * @param lower 时间波动下限
     * @param upper 时间波动上限
     * @return Spider
     */
    public Spider sleepRange(int lower, int upper) {
        seed.setFetchSleepRange(lower + "-" + upper);
        return this;
    }

    /**
     * 抓取随机延迟策略<br>
     *
     * @param timerange 格式：2-7
     * @return Spider
     */
    private Spider sleepRange(String timerange) {
        seed.setFetchSleepRange(timerange);
        return this;
    }

    /**
     * 抓取启动器<br>
     * startTime表示爬虫抓取的开始时间，格式为：2001-10-10 23:29:02，如果startTime已经过时，爬虫会立刻执行
     *
     * @param startTime 抓取的开始时间
     * @return Spider
     */
    public Spider timer(String startTime) {
        seed.setFetchStart(startTime);
        return this;
    }

    /**
     * 抓取启动器<br>
     * firstTime表示爬虫第一次的抓取时间，格式为：2001-10-10 23:29:02，如果firstTime已经过时，爬虫会立刻执行 <br>
     * interval表示爬虫重复抓取的时间间隔，单位是秒
     *
     * @param firstTime 第一次的抓取时间
     * @param interval  下次抓取的时间间隔，单位是秒
     * @return Spider
     */
    public Spider timer(String firstTime, int interval) {
        seed.setFetchStart(firstTime);
        seed.setFetchInterval(String.valueOf(interval));
        return this;
    }

    /**
     * 资源选择器，配置时必须要包含在detail_link与avatar资源的外部资源选择器。<br>
     * 支持Jsoup原生的选择器（html内容）或Jsonpath（json内容）<br>
     * 当此内容为JsonPath字符串的时候，如果json中提供的detail的链接是相对路径，那么此时这个值<br>
     * 的格式为：链接前缀+jsonpath。如果需要抓取多种资源可以用逗号","隔开，默认不填是全抓取。<br>
     * 注意：当启用list_detail模式时，资源特指的是avatar资源，即：与Detail_Link一一对应的资源，<br>
     * 当启用非list_detail模式时，可抓取多种资源，每个选择器中间用逗号隔开。
     *
     * @param resourceSelector Jsoup选择器支持的字符串
     * @return Spider
     */
    public Spider resourceSelector(String resourceSelector) {
        seed.setFetchResourceSelectors(resourceSelector);
        return this;
    }

    /**
     * 是否支持Javascript，有些网站需要等待javascript来生成结果，此时可以将此属性设为true，
     * 默认值是false，慎用：抓取效率会变慢
     *
     * @param ifSupport 是否支持
     * @return Spider
     */
    public Spider javascriptSupport(boolean ifSupport) {
        seed.setFetchJavascriptSupport(ifSupport);
        return this;
    }

    /**
     * 设置代理<br>
     * 爬虫会自动检测，如果代理不能用，会立刻停止
     *
     * @param ip   代理ip
     * @param port 代理端口
     * @return Spider
     */
    public Spider proxy(String ip, Integer port) {
        HttpProxy hp = new HttpProxy(ip, port);
        seed.setFetchHttpProxy(Lists.newArrayList(hp));
        return this;
    }

    /**
     * 设置一组代理<br>
     * 爬虫会自动检测，如果代理不能用，会立刻停止
     *
     * @param list 代理列表
     * @return Spider
     */
    public Spider proxys(List<HttpProxy> list) {
        seed.setFetchHttpProxy(list);
        return this;
    }

    /**
     * 加载系统自带的http_proxy文件
     * 注意：调用此方法之前请确保conf/http_proxy文件中有可用代理
     *
     * @return Spider
     */
    public Spider defaultProxy() {
        seed.setFetchHttpProxyFile(DefaultConfig.http_proxy);
        return this;
    }

    /**
     * 伪造一个UserAgent
     *
     * @param userAgent 自定义useragent字符串
     * @return Spider
     */
    public Spider userAgent(String userAgent) {
        seed.setFetchUserAgent(Lists.newArrayList(userAgent));
        return this;
    }

    /**
     * 伪造一个列表的UserAgent
     *
     * @param userAgents 自定义useragent字符串
     * @return Spider
     */
    public Spider userAgents(List<String> userAgents) {
        seed.setFetchUserAgent(userAgents);
        return this;
    }

    /**
     * 默认加载系统自带的UserAgent
     *
     * @return Spider
     */
    public Spider defaultUserAgent() {
        seed.setFetchUserAgentFile(DefaultConfig.user_agent);
        return this;
    }

    /**
     * 下载本地根路径，默认地址为$path/data/download/
     * 子目录是${seedName}，用来表示每个seed对应不同的下载子目录
     *
     * @param disk 磁盘路径
     * @return Spider
     */
    public Spider downloadDisk(String disk) {
        seed.setDownloadDisk(disk);
        return this;
    }

    /**
     * 默认的下载本地路径
     * 默认地址：$path/data/download/${seedname}
     *
     * @return Spider
     */
    public Spider defaultDownloadDisk() {
        seed.setDownloadDisk(DefaultConfig.default_value);
        return this;
    }

    /**
     * 下载到hdfs路径
     *
     * @param hdfs hdfs地址
     * @return Spider
     */
    public Spider downloadHdfs(String hdfs) {
        seed.setDownloadHdfs(hdfs);
        return this;
    }

    /**
     * 自定义页面解析类
     *
     * @param parser 自定义解析类
     * @return Spider
     */
    public Spider parser(Class<?> parser) {
        seed.setParseClassImpl(parser.getName());
        return this;
    }

    /**
     * 自定义页面解析接口，用于Lambda表达式
     * @param parser
     * @return
     */
    public Spider parser(PageParser parser) {
    	seed.setPageParser(parser);
        return this;
    }

    /**
     * 单个页面元素解析内部类，设置了此项就不能设置自定义的解析类了
     *
     * @param elementSelector Jsoup支持的选择器字符串
     * @return Spider
     */
    public Spider elementSelectParser(String elementSelector) {
        seed.setParseElementSelector(elementSelector);
        return this;
    }

    /**
     * 将解析的结果保存到Mysql中<br>
     * jdbc格式：<a>jdbc:mysql://localhost:3306/spider?useSSL=false&amp;serverTimezone=UTC&amp;characterEncoding=UTF-8&amp;user=root&amp;password=root</a>
     *
     * @param jdbc 需要把用户名与密码也写到url的参数中
     * @return Spider
     */
    public Spider jdbc(String jdbc) {
        seed.setStoreJdbc(jdbc);
        return this;
    }

    /**
     * 将解析的结果保存到MongoDB中<br>
     * <a>mongodb://localhost:27017</a> 或者加密 
     * <a>mongodb://user1:pwd1@host1/?authSource=db1&amp;ssl=true</a> 
     * 或者多个<a>mongodb://host1:27017,host2:27017</a>
     *
     * @param mongodburl String
     * @return Spider
     */
    public Spider mongodb(String mongodburl) {
        seed.setStoreMongodb(mongodburl);
        return this;
    }

    /**
     * 将解析结果进行Lucene索引并且保存
     * 子目录是${seedName}，用来表示每个seed对应不同的下载子目录
     *
     * @param indexPath Lucene索引存储的磁盘根路径
     * @return Spider
     */
    public Spider lucene(String indexPath) {
        seed.setStoreLuceneIndex(indexPath);
        return this;
    }

    /**
     * 将解析结果索引保存到本系统/data/index目录下
     *
     * @return Spider
     */
    public Spider defaultLucene() {
        seed.setStoreLuceneIndex(DefaultConfig.default_value);
        return this;
    }

    /**
     * 将解析结构保存到hbase数据库中
     *
     * @param address hbase地址
     * @return Spider
     */
    public Spider hbase(String address) {
        seed.setStoreHBase(address);
        return this;
    }

    /**
     * 下载文件命名规则，一般为default或url两种类型
     * url表示文件名中包含url，default表示不包含
     * @param isContainUrl boolean
     * @return Spider
     */
    public Spider downloadFilenameRule(boolean isContainUrl){
    	if(isContainUrl){
    		configuration.setDownloadFileNameRule("url");
    	} else {
    		configuration.setDownloadFileNameRule(DefaultConfig.default_value);
    	}
    	return this;
    }

    /**
     * 当系统发生异常，可将相关信息发送给指定接收人
     * @param recipient 一个或多个指定人
     * @return Spider
     */
    public Spider email(String... recipient){
    	StringBuilder sb = new StringBuilder();
    	for(int i=0; i<recipient.length; i++){
    		sb.append(recipient[i]);
    		if(i < recipient.length-1){
    			sb.append(DefaultConfig.email_recipient_split);
    		}
    	}
    	configuration.setEmailRecipient(sb.toString());
    	return this;
    }

    /**
     * 设置一个动态字段
     * @param name 自定义字段名称
     * @param selector 对应字段的页面选择器
     * @return Spider
     */
    public Spider field(String name, String selector){
    	dynamicField.setSeedName(seed.getSeedName());
    	dynamicFieldMap.put(name, selector);
    	dynamicField.setFields(dynamicFieldMap);
    	return this;
    }

    /**
     * 设置多个动态字段<br>
     * 
     * @param fields 动态字段[字段名称，字段值]
     * @return Spider
     */
    public Spider fields(Map<String, String> fields){
    	dynamicField.setSeedName(seed.getSeedName());
    	dynamicField.setFields(fields);
    	return this;
    }

    /**
     * 设置ftp为资源同步方式
     *
     * @param host     服务器地址
     * @param port     端口号，默认为21
     * @param username 用户名（可以为空）
     * @param password 密码（可以为空）
     * @return Spider
     */
    public Spider ftp(String host, int port, String username, String password) {
        Map<String, String> ftp = Maps.newHashMap();
        ftp.put(AbstractConfig.host_node, host);
        ftp.put(AbstractConfig.port_node, String.valueOf(port));
        ftp.put(AbstractConfig.username_node, username);
        ftp.put(AbstractConfig.password_node, password);
        resourceSync.setFtp(ftp);
        Map<String, String> sync = Maps.newHashMap();
        sync.put(AbstractConfig.open_node, "true");
        sync.put(AbstractConfig.batch_count_node, DefaultConfig.sync_batch_count + "");
        sync.put(AbstractConfig.batch_time_node, DefaultConfig.sync_batch_time + "");
        sync.put(AbstractConfig.protocal_node, AbstractConfig.ftp_node);
        resourceSync.setSync(sync);
        return this;
    }

    /**
     * 设置rsync为资源同步方式 <br>
     * 注意：暂时不支持windows
     *
     * @param host                 服务器地址
     * @param username       用户名
     * @param isModule         是否为module模式，是为true，不是则代表远程目录为false
     * @param moduleOrDir  module模式或者远程dir目录，如果是module模式，密码需
     *                    要在服务器端配置；如果是远程dir，需要ssh-keygen配置无密码登陆
     * @return Spider
     */
    public Spider rsync(String host, String username, boolean isModule, String moduleOrDir) {
        Map<String, String> rsync = Maps.newHashMap();
        rsync.put(AbstractConfig.host_node, host);
        rsync.put(AbstractConfig.username_node, username);
        if (isModule) {
            rsync.put(AbstractConfig.module_node, moduleOrDir);
        } else {
            rsync.put(AbstractConfig.dir_node, moduleOrDir);
        }
        resourceSync.setRsync(rsync);
        Map<String, String> sync = Maps.newHashMap();
        sync.put(AbstractConfig.open_node, "true");
        sync.put(AbstractConfig.batch_count_node, DefaultConfig.sync_batch_count + "");
        sync.put(AbstractConfig.batch_time_node, DefaultConfig.sync_batch_time + "");
        sync.put(AbstractConfig.protocal_node, AbstractConfig.rsync_node);
        resourceSync.setSync(sync);
        return this;
    }

    /**
     * 设置scp为资源同步方式 <br>
     * 需要ssh-keygen配置无密码登陆
     *
     * @param host     服务器地址
     * @param username 登陆的用户名
     * @param dir      服务器端目录
     * @param port     scp端口号，默认为22
     * @return Spider
     */
    public Spider scp(String host, String username, String dir, Integer port) {
        Map<String, String> scp = Maps.newHashMap();
        scp.put(AbstractConfig.host_node, host);
        scp.put(AbstractConfig.username_node, username);
        scp.put(AbstractConfig.dir_node, dir);
        port = port == null ? 22 : port;
        scp.put(AbstractConfig.port_node, String.valueOf(port));
        resourceSync.setScp(scp);
        Map<String, String> sync = Maps.newHashMap();
        sync.put(AbstractConfig.open_node, "true");
        sync.put(AbstractConfig.batch_count_node, DefaultConfig.sync_batch_count + "");
        sync.put(AbstractConfig.batch_time_node, DefaultConfig.sync_batch_time + "");
        sync.put(AbstractConfig.protocal_node, AbstractConfig.scp_node);
        resourceSync.setSync(sync);
        return this;
    }

    /**
     * 通过反射的方式设置一个类的多个动态字段
     * @param clazz 类
     * @return Spider
     */
    private void setDynamicField(Class<? extends PageParser> clazz){        
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
        if(fields == null || fields.length == 0){
        	return;
        }
        for(java.lang.reflect.Field field : fields){
        	String name = field.getName();
        	Field column = field.getAnnotation(Field.class);
            String selector = column.value();
            this.field(name, selector);
        }
    }

    /**
     * annotation入口，如果不想一项一项设置Api，也可以写一个annotation
     * annotation类：ListDetail（列表-详情页面）、Single（单个页面，不抓取页面上链接)、Cascade（单个页面，包括所有链接）、Site（单个站点）
     *
     * @param clazz 自定义Annotation类
     * @return Spider
     * @throws Exception 异常
     */
    public static Spider annotation(Class<? extends PageParser> clazz) throws Exception {
    	DefaultConfig.closeHttpClientLog();
        me = new Spider();
        return me.getAnnotation(clazz);
    }

    /**
     * 动态判断Annotation
     *
     * @param clazz clazz
     * @return Spider
     */
    private Spider getAnnotation(Class<? extends PageParser> clazz) {
        Annotation[] ans = clazz.getDeclaredAnnotations();
        if (ans == null || ans.length == 0) {
            logger.error("类[{}]没有配置任何Annotation。",clazz.getName());
            System.exit(1);
        }
        for (Annotation an : ans) {
            String type = an.annotationType().getSimpleName();
            if ("ListDetail".equalsIgnoreCase(type) && clazz.isAnnotationPresent(ListDetail.class)) {
                ListDetail seed = (ListDetail) clazz.getAnnotation(ListDetail.class);
                this.pageMode(PageMode.list_detail);
                this.fetchUrl(seed.url());
                this.method(seed.method());
                this.probe(seed.probeSelector(), seed.probeSleep());
                this.detailSelector(seed.detailSelector());
                this.totalPages(seed.totolPages());
                this.thread(seed.thread());
                this.timer(seed.startTime(), seed.interval());
                this.sleep(seed.sleep());
                this.sleepRange(seed.sleepRange());
                HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                if (hp != null) {
                    this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                }
                this.userAgent(seed.userAgent());
                this.resourceSelector(seed.resourceSelector());
                this.downloadDisk(seed.downloadDisk());
                this.downloadHdfs(seed.downloadHdfs());
                this.javascriptSupport(seed.javascriptSupport());
                this.jdbc(seed.jdbc());
                this.lucene(seed.lucene());
                this.hbase(seed.hbase());
                this.parser(clazz);
                setDynamicField(clazz);
            } else if ("Site".equalsIgnoreCase(type) && clazz.isAnnotationPresent(Site.class)) {
                Site seed = (Site) clazz.getAnnotation(Site.class);
                this.pageMode(PageMode.site);
                this.fetchUrl(seed.url());
                this.method(seed.method());
                this.probe(seed.probeSelector(), seed.probeSleep());
                this.thread(seed.thread());
                this.timer(seed.startTime(), seed.interval());
                this.sleep(seed.sleep());
                this.sleepRange(seed.sleepRange());
                HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                if (hp != null) {
                    this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                }
                this.userAgent(seed.userAgent());
                this.resourceSelector(seed.resourceSelector());
                this.downloadDisk(seed.downloadDisk());
                this.downloadHdfs(seed.downloadHdfs());
                this.javascriptSupport(seed.javascriptSupport());
                this.jdbc(seed.jdbc());
                this.lucene(seed.lucene());
                this.hbase(seed.hbase());
                this.parser(clazz);
                setDynamicField(clazz);
            } else if ("Single".equalsIgnoreCase(type) && clazz.isAnnotationPresent(Single.class)) {
                Single seed = (Single) clazz.getAnnotation(Single.class);
                this.pageMode(PageMode.single);
                this.fetchUrl(seed.url());
                this.method(seed.method());
                this.probe(seed.probeSelector(), seed.probeSleep());
                this.thread(seed.thread());
                this.timer(seed.startTime(), seed.interval());
                this.sleep(seed.sleep());
                this.sleepRange(seed.sleepRange());
                HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                if (hp != null) {
                    this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                }
                this.userAgent(seed.userAgent());
                this.resourceSelector(seed.resourceSelector());
                this.downloadDisk(seed.downloadDisk());
                this.downloadHdfs(seed.downloadHdfs());
                this.javascriptSupport(seed.javascriptSupport());
                this.jdbc(seed.jdbc());
                this.lucene(seed.lucene());
                this.hbase(seed.hbase());
                this.parser(clazz);
                setDynamicField(clazz);
            } else if ("Cascade".equalsIgnoreCase(type) && clazz.isAnnotationPresent(Cascade.class)) {
                Cascade seed = (Cascade) clazz.getAnnotation(Cascade.class);
                this.pageMode(PageMode.cascade);
                this.fetchUrl(seed.url());
                this.method(seed.method());
                this.probe(seed.probeSelector(), seed.probeSleep());
                this.thread(seed.thread());
                this.timer(seed.startTime(), seed.interval());
                this.sleep(seed.sleep());
                this.sleepRange(seed.sleepRange());
                HttpProxy hp = FileUtil.formatProxy(seed.proxy());
                if (hp != null) {
                    this.proxy(hp.getIp(), Integer.valueOf(hp.getPort()));
                }
                this.userAgent(seed.userAgent());
                this.resourceSelector(seed.resourceSelector());
                this.downloadDisk(seed.downloadDisk());
                this.downloadHdfs(seed.downloadHdfs());
                this.javascriptSupport(seed.javascriptSupport());
                this.jdbc(seed.jdbc());
                this.lucene(seed.lucene());
                this.hbase(seed.hbase());
                this.parser(clazz);
                setDynamicField(clazz);
            } else if ("Sync".equalsIgnoreCase(type) && clazz.isAnnotationPresent(Sync.class)) {
                Sync sync = (Sync) clazz.getAnnotation(Sync.class);
                if (AbstractConfig.ftp_node.equals(sync.protocal())) {
                    this.ftp(sync.host(), sync.port(), sync.username(), sync.password());
                } else if (AbstractConfig.rsync_node.equals(sync.protocal())) {
                    this.rsync(sync.host(), sync.username(), sync.isModule(), sync.module());
                } else if (AbstractConfig.scp_node.equals(sync.protocal())) {
                    this.scp(sync.host(), sync.username(), sync.dir(), sync.port());
                }
            } else if ("Config".equalsIgnoreCase(type) && clazz.isAnnotationPresent(Sync.class)) {
            	 Config config = (Config) clazz.getAnnotation(Config.class);
            	 if("url".equalsIgnoreCase(config.downloadFilenameRule())){
            		 this.downloadFilenameRule(true);
            	 } else {
            		 this.downloadFilenameRule(false);
            	 }
                 this.email(config.email());
            }
        }
        return this;
    }

    /**
     * 创建针对列表-详情页面格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider list_detail() {
    	return new Spider(PageMode.list_detail);
    }

    /**
     * 创建针对单个页面（不抓取页面上的链接）的爬虫<br>
     *
     * @return Spider
     */
    public static Spider single() {
        return new Spider(PageMode.single);
    }

    /**
     * 创建针对单页面上（包括所有链接）格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider cascade() {
        return new Spider(PageMode.cascade);
    }

    /**
     * 创建针对整站（不包括外链）格式的爬虫<br>
     *
     * @return Spider
     */
    public static Spider site() {
        return new Spider(PageMode.site);
    }

    private ClusterNode clusterNode;
    public Spider cluster(ClusterNode node){
    	this.clusterNode = node;
    	return this;
    }

    /**
     * 爬虫开启运行
     * 检查Api设置是否设置正确，否则启动失败
     */
    public void start() {
        if (Strings.isNullOrEmpty(seed.getFetchUrl())) {
            logger.error("种子[" + seed.getSeedName() + "]没有配置要抓取的url。");
            System.exit(1);
        }
        // 自动生成seed name
        if (Strings.isNullOrEmpty(seed.getSeedName())) {
            seed.setSeedName(MD5Util.generateSeedName(seed.getFetchUrl()));
        }
        if(dynamicField.getFields() != null && !dynamicField.getFields().isEmpty()){
        	dynamicField.setSeedName(seed.getSeedName());
        }
        SpiderEngine.create().setClusterNode(clusterNode).setSeed(seed).setResourceSync(resourceSync).setConfiguration(configuration).setDynamicField(dynamicField).build();
    }

    /**
     * 获取互联网上免费代理<br>
     * 并自动将可用的代理保存到本地http_proxy文件中<br>
     * 此方法一般在启动爬虫之前使用，下次在启动爬虫需要代理时，<br>
     * 可以直接调用defaultProxy方法即可
     */
    public static void initFreeProxy() {
    	DefaultConfig.closeHttpClientLog();
        Seed xicidaili = new Seed("xicidaili");
        xicidaili.setPageMode(PageMode.list_detail);
        xicidaili.setFetchUrl("http://www.xicidaili.com/nn/{1}");
        xicidaili.setThreadCount(1);
        xicidaili.setFetchTotalPages("5");
        xicidaili.setParseClassImpl("com.bytegriffin.get4j.parse.FreeProxyPageParser");
        xicidaili.setStoreFreeProxy(DefaultConfig.http_proxy);
        xicidaili.setFetchUserAgentFile(DefaultConfig.user_agent);
        SpiderEngine.create().setSeeds(Lists.newArrayList(xicidaili)).build();
    }

    /**
     * 通过配置文件启动爬虫的入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String... args) {
    	DefaultConfig.closeHttpClientLog();
        Context context = new Context(new CoreSeedsXmlHandler());
        List<Seed> seeds = context.load();

        context = new Context(new ResourceSyncYamlHandler());
        ResourceSync synchronizer = context.load();

        context = new Context(new ConfigurationXmlHandler());
        Configuration configuration = context.load();

        context = new Context(new DynamicFieldXmlHandler());
        List<DynamicField> dynamicFields = context.load();

        SpiderEngine.create().setSeeds(seeds).setResourceSync(synchronizer).setConfiguration(configuration).setDynamicFields(dynamicFields).build();
        logger.info("爬虫开始启动...");
    }

}

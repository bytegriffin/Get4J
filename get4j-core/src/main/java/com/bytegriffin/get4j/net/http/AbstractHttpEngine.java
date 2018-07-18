package com.bytegriffin.get4j.net.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.download.DownloadFile;
import com.bytegriffin.get4j.net.http.interceptor.GzipInterceptor;
import com.bytegriffin.get4j.net.http.interceptor.LoggingInterceptor;
import com.bytegriffin.get4j.net.http.interceptor.RedirectInterceptor;
import com.bytegriffin.get4j.net.http.interceptor.RetryInterceptor;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.Sleep;
import com.bytegriffin.get4j.util.StringUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import okhttp3.CacheControl;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HttpEngine共有属性方法
 */
public abstract class AbstractHttpEngine {

	/**
	 * 大文件判断标准（默认是超过10M就算）
	 */
	static final long big_file_max_size = 10485760;// 10M

	/**
	 * 出现防止刷新页面的关键词， 当然有种可能是页面中的内容包含这些关键词，而网站并没有屏蔽频繁刷新的情况
	 */
	private static final Pattern KEY_WORDS = Pattern.compile(".*(\\.(刷新太过频繁|刷新太频繁|刷新频繁|频繁访问|访问频繁|访问太频繁|访问过于频繁))$");

	/**
	 * 记录站点防止频繁抓取的页面链接<br>
	 * 处理某些站点避免频繁请求而作出的页面警告，当然这些警告原本就是页面内容，不管如何都先记录下来<br>
	 * 有的站点需要等待一段时间就可以访问正常；有的需要人工填写验证码，有的直接禁止ip访问等 <br>
	 * 出现这种问题，爬虫会先记录下来，如果出现这类日志，爬虫使用者可以设置Http代理和UserAgent重新抓取
	 *
	 * @param seedName  seedName
	 * @param url    url
	 * @param content   content
	 * @param logger    logger
	 */
	void frequentAccesslog(String seedName, String url, String content, Logger logger) {
		if (KEY_WORDS.matcher(content).find()) {
			logger.error("线程[{}]种子[{}]访问[{}]时太过频繁。",Thread.currentThread().getName() ,seedName, url);
		}
	}
	
	// 连接超时，单位秒
    private final static int conn_timeout = 30;
    // 写超时，单位秒
    private final static int write_timeout = 5;
    // 读超时，单位秒
    private final static int read_timeout = 5;
    
	 /**
     * 初始化OKHttpClientBuilder
     * @param seedName
     */
    OkHttpClient.Builder initOkHttpClientBuilder(String seedName) {
    	OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(conn_timeout, TimeUnit.SECONDS)
            .writeTimeout(write_timeout, TimeUnit.SECONDS)
            .readTimeout(read_timeout, TimeUnit.SECONDS)
            .followRedirects(false) //禁止okhttp的重定向操作，自己处理重定向
            .followSslRedirects(false)
            .cookieJar(new CustomCookieJar()) //设置自动携带Cookie
            .retryOnConnectionFailure(true)  //允许失败重试
            .addInterceptor(new RetryInterceptor())
            .addInterceptor(new GzipInterceptor())
            .addInterceptor(new RedirectInterceptor())
            .addInterceptor(new LoggingInterceptor())
            .cookieJar(new CustomCookieJar())
            .sslSocketFactory(createSSLSocketFactory(),new TrustAllManager())
            .hostnameVerifier(new TrustAllHostnameVerifier());
    	Globals.OK_HTTP_CLIENT_BUILDER_CACHE.put(seedName, builder);
    	return builder;
    }
    
    /**
     * CookieJar是用于保存Cookie的
     */
    private class CustomCookieJar implements CookieJar {
	    // cookie 保存  key:host value:List<Cookie>
	    private final LinkedHashMap<String, List<Cookie>> cookieStore = Maps.newLinkedHashMap();

		@Override public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
            cookieStore.put(httpUrl.host(), list);
        }

        @Override public List<Cookie> loadForRequest(HttpUrl httpUrl) {
            List<Cookie> cookies = cookieStore.get(httpUrl.host());
            return cookies != null ? cookies : new ArrayList<Cookie>();
        }
	}

    /**
     * 实现一个X509TrustManager接口，用于绕过验证，不用修改里面的方法
     */
    private static class TrustAllManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override public X509Certificate[] getAcceptedIssuers() {
        	return new X509Certificate[0];
        }
    }

    /**
     * 创建SSL工厂
     *
     * @return SSLContext
     */
    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory sslSocketFactory = null;
        try {
        	SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            sslSocketFactory = ctx.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sslSocketFactory;
    }

    /**
     * 信任所有验证
     */
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /**
     * 事先将大文件缓存起来
     * @param seedName 种子名称
     * @param url url
     * @param contentLength 网页内容长度
     * @return boolean
     */
    static boolean cacheBigFile(String seedName, String url, long contentLength){
    	if (contentLength <= big_file_max_size) {//10M
            return false;
        }
    	DownloadFile dfile = new DownloadFile();
    	dfile.setSeedName(seedName).setUrl(url).setContentLength(contentLength);
    	DownloadFile.add(seedName, dfile);
    	return true;
    }

	/**
     * 获取资源文件后缀名
     * @param subType String
     * @return String
     */
    static String getResourceSuffix(String subType) {
    	String fileSuffix = "";
    	switch(subType){
        case "icon":
        	fileSuffix = "ico";
            break;
        case "javascript":
        	fileSuffix = "js";
            break;
        case "excel":
        	fileSuffix = "xls";
            break;
        case "powerpoint":
        	fileSuffix = "ppt";
            break;
        case "word":
        	fileSuffix = "doc";
            break;  
        case "flash":
        	fileSuffix = "swf";
            break;    
        default:
        	fileSuffix = subType;
            break;
    	}
        return fileSuffix;
    }

	//本方法暂时没用
	//对url进行解码，否则就是类似这种格式：http://news.baidu.com/n?cmd=6&loc=0&name=%B1%B1%BE%A9
	@Deprecated
	protected String decodeUrl(String url, String charset) {
		try {
			url = URLDecoder.decode(url, charset);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return url;
	}

	/**
	 * 初始化Http引擎配置参数：HttpProxy、UserAgent、Sleep、SleepRange
	 *
	 * @param seed    seed
	 * @param logger    logger
	 */
	void initParams(Seed seed, Logger logger) {
		// 1.初始化Http Proxy
		List<HttpProxy> httpProxys = seed.getFetchHttpProxy();
		if (httpProxys != null && httpProxys.size() > 0) {
			HttpProxySelector hplooper = new HttpProxySelector();
			hplooper.setQueue(httpProxys);
			Globals.HTTP_PROXY_CACHE.put(seed.getSeedName(), hplooper);
		}

		// 2.初始化Http UserAgent
		List<String> userAgents = seed.getFetchUserAgent();
		if (userAgents != null && userAgents.size() > 0) {
			UserAgentSelector ualooper = new UserAgentSelector();
			ualooper.setQueue(userAgents);
			Globals.USER_AGENT_CACHE.put(seed.getSeedName(), ualooper);
		}

		// 3.设置HttpClient请求的间隔时间，
		if (seed.getFetchSleep() != 0) { // 首先判断sleep参数，然后才判断sleep.range参数
			Globals.FETCH_SLEEP_CACHE.put(seed.getSeedName(), seed.getFetchSleep());
		} else {
			setSleepRange(seed, logger);
		}
	}

	/**
	 * 设置Sleep Range参数
	 *
	 * @param seed    seed
	 * @param logger  logger
	 */
	private static void setSleepRange(Seed seed, Logger logger) {
		String sleepRange = seed.getFetchSleepRange();
		if (Strings.isNullOrEmpty(sleepRange)) {
			return;
		}
		String split = sleepRange.contains("-") ? "-" : "－";
		String start = sleepRange.substring(0, sleepRange.indexOf(split));
		String end = sleepRange.substring(sleepRange.lastIndexOf(split) + 1, sleepRange.length());
		if (!StringUtil.isNumeric(start) || !StringUtil.isNumeric(end)) {
			logger.error("线程[{}]检查种子[{}]的间隔范围配置中[{}]不能出现字符串。",Thread.currentThread().getName(),seed.getSeedName(),sleepRange);
			System.exit(1);
		}
		int min = Integer.valueOf(start);
		int max = Integer.valueOf(end);
		if (max == min) {
			Globals.FETCH_SLEEP_CACHE.put(seed.getSeedName(), min);
		} else if (min > max) {
			int temp = max;
			max = min;
			min = temp;
		}
		List<Integer> list = Lists.newArrayList();
		for (Integer i = min; i <= max; i++) {
			list.add(i);
		}
		SleepRandomSelector sleeprandom = new SleepRandomSelector();
		sleeprandom.setQueue(list);
		Globals.FETCH_SLEEP_RANGE_CACHE.put(seed.getSeedName(), sleeprandom);
	}

	/**
	 * 设计Http请求间隔时间
	 *
	 * @param seedName   种子名称
	 * @param logger  logger
	 */
	protected static void sleep(String seedName, Logger logger) {
		Integer millis = Globals.FETCH_SLEEP_CACHE.get(seedName);
		if (millis == null) {
			SleepRandomSelector random = Globals.FETCH_SLEEP_RANGE_CACHE.get(seedName);
			if (random == null) {
				return;
			}
			millis = random.choice();
		}
		Sleep.seconds(millis);
	}

	/**
	 * 判断是否为Json文件
	 * @param mediaType MediaType
	 * @return boolean
	 */
	static boolean isJsonPage(MediaType mediaType, String content) {
		return (mediaType.subtype().contains("json") || content.startsWith("{") || content.startsWith("["));
	}

	/**
	 * 判断是否为普通页面
	 *
	 * @param contentType    contentType
	 * @return boolean
	 */
	static boolean isHtmlPage(MediaType mediaType) {
		return (mediaType.subtype().contains("html") || mediaType.toString().contains("text/plain"));
	}

	/**
	 * 判断是否为xml文件
	 * 特殊情况：
	 * 1.有的xml文件返回的ContentType也是text/html，但是根节点是<?xml ...>的xml内容文件
	 * 2.如果rss的话，那么返回的值是rss+xml；有的返回wlwmanifest+xml
	 * @param contentType String
	 * @param content String
	 * @return boolean
	 */
	static boolean isXmlPage(MediaType mediaType, String content) {
		return (mediaType.subtype().contains("xml") || content.contains("<?xml") || content.contains("<rss")
				|| content.contains("<feed"));
	}

	/**
	 * 获取页面编码<br>
	 * 1.如果Response的Header中有 Content-Type:text/html; charset=utf-8直接获取<br>
	 * 2.但是有时Response的Header中只有
	 * Content-Type:text/html;没有charset，此时需要去html页面中寻找meta标签， 例如：[meta
	 * http-equiv=Content-Type content="text/html;charset=gb2312"]<br>
	 * 3.有时html页面中是这种形式：[meta charset="gb2312"]<br>
	 * 4.如果都没有那只能返回utf-8
	 *
	 * @param MediaType  mediaType
	 * @param content   转码前的content，有可能是乱码
	 * @return String
	 */
	String getCharset(MediaType mediaType, String content) {
		String charset = "";
		if (mediaType != null && mediaType.charset() != null) {// 如果Response的Header中有 Content-Type:text/html;charset=utf-8直接获取
			charset = mediaType.charset().name();
		} else {// 但是有时Response的Header中只有 Content-Type:text/html;没有charset
			if (isXmlPage(mediaType, content)) { // 首先判断是不是xml文件
				charset = getXmlCharset(content);
			} else if (isHtmlPage(mediaType)) {// 如果是html，可以用jsoup解析html页面上的meta元素
				charset = getHtmlCharset(content);
			} else if (isJsonPage(mediaType, content)) { // 如果是json，那么给他设置默认编码
				charset = getJsonCharset();
			}
		}
		return charset;
	}

	String getXmlCharset(String content){
		Document doc = Jsoup.parse(content, "", Parser.xmlParser());
		Node root = doc.root();
		Node node = root.childNode(0);
		return node.attr("encoding");
	}

	String getJsonCharset(){
		return Charset.defaultCharset().name();
	}

	String getHtmlCharset(String content){
		String charset;
		Document doc = Jsoup.parse(content);
		Elements eles1 = doc.select("meta[http-equiv=Content-Type]");
		Elements eles2 = doc.select("meta[charset]");
		if (!eles1.isEmpty() && eles1.get(0) != null) {
			String meta = eles1.get(0).attr("content");
			charset = meta.split("charset=")[1];
		} else if (!eles2.isEmpty() && eles2.get(0) != null) {// 也可以是这种类型：
			charset = eles2.get(0).attr("charset");
		} else {// 如果html页面内也没有含Content-Type的meta标签，那就默认为utf-8
			charset = Charset.defaultCharset().name();
		}
		return charset;
	}

	/**
	 * 根据ContentType设置page内容
	 *
	 * @param contentType   contentType
	 * @param content   转码后的content
	 * @param page   page
	 */
	void setContent(MediaType mediaType, String content, Page page) {
		if (isJsonPage(mediaType, content)) {
			page.setJsonContent(content);
		} else if (isXmlPage(mediaType, content)) {
			page.setXmlContent(content);
			// json文件中一般不好嗅探title属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else if (isHtmlPage(mediaType)) {
			page.setHtmlContent(content);
			// json文件中一般不好嗅探title属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else { // 不是html也不是json，那么只能是resource的链接了，xml也是
			page.getResources().add(page.getUrl());
		}
	}

    /**
     * 设置Http Header缓存机制
     * @return CacheControl
     */
    private static CacheControl buildCacheControl(){
    	final CacheControl.Builder builder = new CacheControl.Builder();
    	//builder.onlyIfCached();//只使用缓存
    	return builder.noCache() //不使用缓存
        	   .noStore() //不存储服务端response缓存
        	   .noTransform() //禁止转码
        	   .maxAge(10, TimeUnit.MILLISECONDS)  //指示客户机可以接收生存期不大于指定时间的响应
        	   .maxStale(10, TimeUnit.SECONDS)  //指示客户机可以接收超出超时期间的响应消息
        	   .minFresh(10, TimeUnit.SECONDS).build();  //指示客户机可以接收响应时间小于当前时间加上指定时间的响应
    }
    
    /**
	 * 设置页面host 可以将它当作request中header的host属性使用
	 * 
	 * @param Request.Builder builder
	 * @param page     Page
	 * @param logger  Logger
	 */
	void setHost(Request.Builder builder, Page page, Logger logger) {
		String host = "";
		try {
			URI uri = new URI(page.getUrl());
			host = uri.getAuthority();
		} catch (URISyntaxException e) {
			logger.error("线程[{}]设置种子[{}]url[{}]的Host属性时错误：{}",Thread.currentThread().getName(),page.getSeedName(), page.getUrl(), e);
		}
		page.setHost(host);
		if(builder != null) {
			builder.addHeader("Host", host);
		}
	}

	/**
	 * 设置http请求url和header
	 * @param builder Request.Builder
	 * @param page 页面
	 * @return Request.Builder
	 */
	public static Request.Builder setUrlAndHeader(Request.Builder builder, String url){
    	return builder.url(url).cacheControl(buildCacheControl()).addHeader("Accept", DefaultConfig.http_header_accept);
	}

	/**
	 * 是否要继续访问 根据response返回的状态码判断是否继续访问，true：是；false：否
	 * 
	 * @param httpClient
	 * @param page
	 * @param request
	 * @param response
	 * @param logger
	 * @return
	 * @throws IOException
	 */
	static boolean isVisit(OkHttpClient httpClient, Page page, Request request, Response response, Logger logger)
			throws IOException {
		String url = page.getUrl();
		String seedName = page.getSeedName();
		int statusCode = response.code();
		if (response.isRedirect()) {// 状态码为3xx：此类url是跳转链接，访问连接后获取Response中头信息的Location属性才是真实地址
			String newUrl = request.url().toString();//RedirectInterceptor已经自动设置好新url了
			if (Strings.isNullOrEmpty(newUrl)) {
				logger.error("线程[{}]访问种子[{}]的url[{}]时发生[{}]错误并且跳转到新的url为空链接，即：header中的Location值为空值。", 
						Thread.currentThread().getName(), seedName, url, statusCode);	
				return false;
			} else {
				//newUrl = UrlAnalyzer.custom(page).getAbsoluteURL(url, newUrl); //有可能跳转的Url是相对路径，例如 /abc/123.html
				page.setUrl(newUrl);//设置需要跳转的url
				page.setMethod(request.method());//设置method方法
				
				logger.warn("线程[{}]访问种子[{}]的url[{}]时发生[{}]错误并且跳转到新的url[{}]上。", 
						Thread.currentThread().getName(), seedName, url, statusCode, newUrl);
			}
			
		} else if (!response.isSuccessful()) { // 状态码为4xx---5xx
			UrlQueue.newFailVisitedUrl(page.getSeedName(), page.getUrl());
			String msg = "线程["+Thread.currentThread().getName()+"]访问种子["+seedName+"]的url["+page.getUrl()+"]请求发送["+statusCode+"]错误。";
			Preconditions.checkArgument(false, msg);
			logger.error("线程[{}]访问种子[{}]的url[{}]请求发送[{}]错误。", Thread.currentThread().getName(), seedName, page.getUrl(),statusCode);
			EmailSender.sendMail(msg);
            ExceptionCatcher.addException(seedName, msg);
			return false;
		}
		return true;
	}
	
}

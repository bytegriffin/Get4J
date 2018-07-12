package com.bytegriffin.get4j.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.Sleep;
import com.bytegriffin.get4j.util.StringUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

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
	 * 是否在页面中发现此内容
	 *
	 * @param content    页面内容
	 * @return boolean
	 */
	private boolean isFind(String content) {
		return KEY_WORDS.matcher(content).find();
	}

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
		if (isFind(content)) {
			logger.warn("线程[" + Thread.currentThread().getName() + "]种子[" + seedName + "]访问[" + url + "]时太过频繁。");
		}
	}

	/**
	 * 转换页面内容： 将inputstream转换成string类型
	 *
	 * @param is   页面内容输入流
	 * @param charset       编码
	 * @return String
	 * @throws IOException          ioException
	 */
	String getContentAsString(InputStream is, String charset) throws IOException {
		return new String(ByteStreams.toByteArray(is), charset);
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
			logger.error("线程[" + Thread.currentThread().getName() + "]检查种子[" + seed.getSeedName() + "]的间隔范围配置中["
					+ sleepRange + "]不能出现字符串。");
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
	 * 判断HttpClient下载是否为Json文件
	 *
	 * @param contentType    contentType
	 * @return boolean
	 */
	static boolean isJsonPage(String contentType) {
		return (contentType.contains("json") || contentType.contains("JSON") || contentType.contains("Json"));
	}

	/**
	 * 判断是否为普通页面
	 *
	 * @param contentType       contentType
	 * @return boolean
	 */
	static boolean isHtmlPage(String contentType) {
		return (contentType.contains("text/html") || contentType.contains("text/plain"));
	}

	/**
	 * 判断是否为xml文件 有的xml文件返回的ContentType也是text/html，但是根节点是<?xml ...>
	 *
	 * @param contentType    contentType
	 * @param content  content
	 * @return boolean
	 */
	static boolean isXmlPage(String contentType, String content) {
		return (contentType.contains("xml") || content.contains("<?xml") || content.contains("<rss")
				|| content.contains("<feed"));
	}

	/**
	 * 设置页面host 可以将它当作request中header的host属性使用
	 *
	 * @param page     Page
	 * @param logger  Logger
	 */
	void setHost(Page page, Logger logger) {
		String host = "";
		try {
			URI uri = new URI(page.getUrl());
			host = uri.getAuthority();
		} catch (URISyntaxException e) {
			logger.error("线程[" + Thread.currentThread().getName() + "]设置种子[" + page.getSeedName() + "]url["
					+ page.getUrl() + "]的HOST属性时错误。", e);
		}
		page.setHost(host);
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
	 * @param contentType  contentType
	 * @param content           转码前的content，有可能是乱码
	 * @return String
	 */
	String getCharset(String contentType, String content) {
		String charset = "";
		if (contentType.contains("charset=")) {// 如果Response的Header中有 Content-Type:text/html;charset=utf-8直接获取
			charset = contentType.split("charset=")[1];
		} else {// 但是有时Response的Header中只有 Content-Type:text/html;没有charset
			if (isXmlPage(contentType, content)) { // 首先判断是不是xml文件
				charset = getXmlCharset(content);
			} else if (isHtmlPage(contentType)) {// 如果是html，可以用jsoup解析html页面上的meta元素
				charset = getHtmlCharset(content);
			} else if (isJsonPage(contentType)) { // 如果是json，那么给他设置默认编码
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
	void setContent(String contentType, String content, Page page) {
		if (isHtmlPage(contentType)) {
			// 注意：有两种特殊情况
			// 1.有时text/plain这种文本格式里面放的是json字符串，并且是这个json字符串里的属性值却是html
			// 2.有时text/html反应出来的是rss的xml格式
			if (isXmlPage(contentType, content)) {
				page.setXmlContent(content);
			} else {
				page.setHtmlContent(content);
			}
			// json文件中一般不好嗅探title属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else if (isJsonPage(contentType)) {
			page.setJsonContent(content);
		} else if (isXmlPage(contentType, content)) {
			page.setXmlContent(content);
			// json文件中一般不好嗅探title属性
			page.setTitle(UrlAnalyzer.getTitle(content));
		} else { // 不是html也不是json，那么只能是resource的链接了，xml也是
			page.getResources().add(page.getUrl());
		}
	}

	/**
	 * 是否要继续访问 根据response返回的状态码判断是否继续访问，true：是；false：否
	 *
	 * @param statusCode  返回状态码
	 * @param page   page对象
	 * @param logger   logger日志
	 * @param url    url
	 * @return boolean
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@SuppressWarnings("resource")
	static boolean isVisit(CloseableHttpClient httpClient, Page page, Object request, Object response, Logger logger)
			throws ClientProtocolException, IOException {
		String url = page.getUrl();
		String seedName = page.getSeedName();
		int statusCode = 200;
		if (response instanceof HttpResponse) {
			HttpResponse newresponse = (HttpResponse) response;
			statusCode = newresponse.getStatusLine().getStatusCode();
		} 
//		else if (response instanceof WebResponse) {
//			WebResponse newresponse = (WebResponse) response;
//			statusCode = newresponse.getStatusCode();
//		}
		// 404/403/500/503 ：此类url不会重复请求，直接把url记录下来以便查明情况
		if (HttpStatus.SC_NOT_FOUND == statusCode || HttpStatus.SC_FORBIDDEN == statusCode
				|| HttpStatus.SC_INTERNAL_SERVER_ERROR == statusCode
				|| HttpStatus.SC_SERVICE_UNAVAILABLE == statusCode) {
			UrlQueue.newFailVisitedUrl(page.getSeedName(), page.getUrl());
			String msg = "线程["+Thread.currentThread().getName()+"]访问种子["+seedName+"]的url["+page.getUrl()+"]请求发送["+statusCode+"]错误。";
			Preconditions.checkArgument(false, msg);
			logger.error("线程[{}]访问种子[{}]的url[{}]请求发送[{}]错误。", Thread.currentThread().getName(), seedName, page.getUrl(),statusCode);
			EmailSender.sendMail(msg);
            ExceptionCatcher.addException(seedName, msg);
			return false;
		} else if (HttpStatus.SC_MOVED_PERMANENTLY == statusCode || HttpStatus.SC_MOVED_TEMPORARILY == statusCode
				|| HttpStatus.SC_SEE_OTHER == statusCode || HttpStatus.SC_TEMPORARY_REDIRECT == statusCode) {
			// 301/302/303/307：此类url是跳转链接，访问连接后获取Response中头信息的Location属性才是真实地址
			if (response instanceof HttpResponse) {
				HttpResponse newresponse = (HttpResponse) response;
				Header responseHeader = newresponse.getFirstHeader("Location");
				if (responseHeader != null && !Strings.isNullOrEmpty(responseHeader.getValue())) {
					String newurl = responseHeader.getValue();
					if (request instanceof HttpGet) {
						HttpGet get = (HttpGet) request;
						get.releaseConnection();
						get = new HttpGet(newurl);
						response = httpClient.execute(get);
					} else if (request instanceof HttpPost) {
						HttpPost post = (HttpPost) request;
						post.releaseConnection();
						post = new HttpPost(newurl);
						response = httpClient.execute(post);
					}
					page.setUrl(newurl);
				}
				logger.warn("线程[{}]访问种子[{}]的url[{}]时发生[{}]错误并且跳转到新的url[{}]上。", Thread.currentThread().getName(), seedName, url,statusCode, page.getUrl());
			}
		} else if (HttpStatus.SC_OK != statusCode) {
			logger.warn("线程[{}]访问种子[{}]的url[{}]时发生[{}]错误。", Thread.currentThread().getName(), seedName, url,statusCode);
		}
		return true;
	}

}

package com.bytegriffin.get4j.net.http;

import java.io.File;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.send.EmailSender;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * 专门处理Javascript效果的html网页 <br>
 * chromedriver需要放到bin文件目录下才能运行程序 <br>
 * 下载地址：https://npm.taobao.org/mirrors/chromedriver/
 */
public class SeleniumEngine extends AbstractHttpEngine implements HttpEngine {

	private static final Logger logger = LogManager.getLogger(SeleniumEngine.class);
	//private DesiredCapabilities capabilities;
	private static final int page_load_timeout = 10;
	private static final int script_timeout = 10;
	private static final int implicitly_wait = 10;// 隐式等待

    @Override public void init(Seed seed) {
        // 1.初始化OKHttpClientBuilder
    	initOkHttpClientBuilder(seed.getSeedName());
        
        // 2.初始化配置参数
        initParams(seed, logger);
        logger.info("种子[{}]的Http引擎SeleniumEngine的初始化完成。", seed.getSeedName());
    }

	/**
	 * 设置Chrome Driver参数
	 * @return
	 */
	private ChromeOptions newChromeOptions(){
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.setAcceptInsecureCerts(true);
    	chromeOptions.setHeadless(true);
    	chromeOptions.addArguments("--start-maximized");
    	chromeOptions.addArguments("--disable-images");
    	chromeOptions.addArguments("--disable-gpu");
    	chromeOptions.addArguments("--disable-infobars");
    	chromeOptions.addArguments("--disable-notifications");
    	chromeOptions.addArguments("--lang=" + "zh-CN");
    	chromeOptions.addArguments("--test-type", "--ignore-certificate-errors");
    	//chromeOptions.addArguments("--disable-images"); //禁用图像
    	//chromeOptions.addArguments("--incognito"); //启动进入隐身模式
    	//chromeOptions.addArguments("----disk-cache-size="); //自定义缓存大小
    	//chromeOptions.addArguments("----disable-popup-blocking"); //禁用弹出拦截
    	//chromeOptions.addArguments("--window-size=1920,1200");
    	//chromeOptions.addArguments("--no-sandbox"); //启动无沙盒模式运行
    	//chromeOptions.addArguments("--user-data-dir=C:/Users/user_name/AppData/Local/Google/Chrome/User Data");
    	chromeOptions.setCapability(CapabilityType.BROWSER_NAME, "Google Chrome");
    	chromeOptions.setCapability("noSign", true);
    	chromeOptions.setCapability("unicodeKeyboard", true);
    	chromeOptions.setCapability("newCommandTimeout", "30");
    	chromeOptions.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);//支持ssl证书
    	Map<String, Object> prefs = Maps.newHashMap();
	    prefs.put("profile.default_content_setting_values.notifications", 2);
	    chromeOptions.setExperimentalOption("prefs", prefs);
	    chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
		return chromeOptions;
	}

	private WebDriver newWebDriver(ChromeOptions chromeOptions){
		String chromedriver_path = DefaultConfig.linux_chromedriver;
		String osname = System.getProperties().getProperty("os.name");
		if (osname.toLowerCase().contains("win")) {
			chromedriver_path = DefaultConfig.win_chromedriver;
		}
		File chromedriverfile = Paths.get(chromedriver_path).toFile();
		if(!chromedriverfile.exists()) {
			logger.error("文件[{}]不存在。",chromedriver_path);
			System.exit(1);
		}
		if(!chromedriverfile.canExecute()) {
			logger.error("文件[{}]不可以执行。",chromedriver_path);
			System.exit(1);
		}
		System.setProperty("webdriver.chrome.driver",  chromedriver_path);
		ChromeDriverService service = new ChromeDriverService.Builder()
				.withSilent(true)
				.withLogFile(Paths.get(DefaultConfig.chromedriver_log).toFile())
                .usingDriverExecutable(chromedriverfile)
                .usingAnyFreePort()
                .build();
		WebDriver driver = new ChromeDriver(service, chromeOptions);
		driver.manage().timeouts().implicitlyWait(implicitly_wait, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(page_load_timeout, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(script_timeout, TimeUnit.SECONDS);
	    driver.manage().window().maximize();
		return driver;
	}
	
	private WebDriver getWebDriver(Page page) {
		sleep(page.getSeedName(), logger);
    	WebDriver webDriver = Globals.WEBDRIVER_CACHE.get(page.getSeedName()) ;
    	if(webDriver == null){
    		ChromeOptions chromeOptions = newChromeOptions();
            setHttpProxy(page.getSeedName(), chromeOptions);
            setUserAgent(page.getSeedName(), chromeOptions);
            setHost(null, page, logger);
            chromeOptions.setCapability("Host", page.getHost());
            chromeOptions.setCapability("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            webDriver = newWebDriver(chromeOptions);
            Globals.WEBDRIVER_CACHE.put(page.getSeedName(), webDriver);
    	}
    	return webDriver;
	}

	/**
	 * 设置代理
	 * @param httpProxy
	 * @return
	 */
	private static Proxy newProxy(HttpProxy httpProxy){
		Proxy proxy = new Proxy();
		proxy.setProxyType(Proxy.ProxyType.MANUAL);
		proxy.setAutodetect(false);
		String hostAndPort = httpProxy.getIp() + ":" + httpProxy.getPort();
		if (httpProxy.getProxy() != null) {
			proxy.setHttpProxy(hostAndPort).setSslProxy(hostAndPort);
			proxy.setNoProxy("localhost");
		}else if(httpProxy.getProxyAuthenticator() != null){
			proxy.setSocksProxy(hostAndPort);
			proxy.setSocksUsername(httpProxy.getUsername());
			proxy.setSocksPassword(httpProxy.getPassword());
		}
		return proxy;
	}

	/**
	 * 检查Http Proxy代理是否可运行
	 *
	 * @return boolean
	 */
	@Override
	public boolean testHttpProxy(String url, HttpProxy httpProxy) {
		ChromeOptions chromeOptions = newChromeOptions();
		chromeOptions.setCapability(CapabilityType.PROXY, newProxy(httpProxy));
		WebDriver driver = newWebDriver(chromeOptions);
		try {
			driver.get(url);
			logger.info("Http代理[" + httpProxy.toString() + "]测试成功。");
			return true;
		} catch (Exception e) {
			logger.error("Http代理[" + httpProxy.toString() + "]测试出错，请重新检查。");
			return false;
		} finally {
			driver.close();
		}
	}

    /**
     * 设置请求中的Http代理
     *
     * @param seedName  seedName
     * @param ChromeOptions chromeOptions
     */
    private static void setHttpProxy(String seedName, ChromeOptions chromeOptions) {
        HttpProxySelector hpl = Globals.HTTP_PROXY_CACHE.get(seedName);
        if (hpl == null) {
            return;
        }
        HttpProxy httpproxy = hpl.choice();
        chromeOptions.setCapability(CapabilityType.PROXY, newProxy(httpproxy));
    }

    /**
     * 设置User_Agent
     * 
     * @param seedName  seedName
     * @param ChromeOptions chromeOptions 
     */
    private static void setUserAgent(String seedName, ChromeOptions chromeOptions) {
        UserAgentSelector ual = Globals.USER_AGENT_CACHE.get(seedName);
        if (ual == null) {
            return;
        }
        String userAgent = ual.choice();
        if (!Strings.isNullOrEmpty(userAgent)) {
        	chromeOptions.addArguments("--user-agent='"+userAgent+"'");
        }
    }

    private boolean isJsonContent(String content){
    	if(Strings.isNullOrEmpty(content)){
    		return false;
    	}
    	return content.trim().startsWith("[") || content.trim().startsWith("{");
    }

    private boolean isHtmlContent(String content){
    	if(Strings.isNullOrEmpty(content)){
    		return false;
    	}
    	return content.contains("<html>") || content.contains("<head>") || content.contains("<body>") ; 
    }

    private boolean isXmlContent(String content){
    	if(Strings.isNullOrEmpty(content)){
    		return false;
    	}
    	return content.trim().startsWith("<xml") || content.trim().startsWith("<?xml") ;
    }

    /**
     * 获取url的内容，与HttpClientProbe的getAndSetContent方法实现完全一致，
     * 只是调用了HtmlUnit的API而已。
     * 注意：WebDriver获取不到Response
     * @param page page
     * @return Page
     */
    public Page getPageContent(Page page) {
    	WebDriver webDriver = getWebDriver(page);
        try {
            webDriver.get(page.getUrl());
//          (new WebDriverWait(webDriver, 10)).until(new ExpectedCondition<Boolean>() {
//    			public Boolean apply(WebDriver d) {
//    				return Strings.isNullOrEmpty(d.getPageSource());
//    			}
//    		});
            String content = webDriver.getPageSource();
            long contentlength = content.length();
            if (contentlength > big_file_max_size) {//大于10M
                if (cacheBigFile(page.getSeedName(), page.getUrl(), contentlength)) {
                    return page;
                }
            }

            if(isJsonContent(content)){
            	page.setCharset(getJsonCharset());
            	page.setJsonContent(content);
            } else if(isHtmlContent(content)){
            	page.setCharset(getHtmlCharset(content));
            	page.setHtmlContent(content);
            	page.setTitle(UrlAnalyzer.getTitle(content));
            } else if(isXmlContent(content)){
            	page.setCharset(getXmlCharset(content));
            	page.setXmlContent(content);
            	page.setTitle(UrlAnalyzer.getTitle(content));
            } else { // 如果是资源文件的话
            	page.getResources().add(page.getUrl());
                return page;
            }

            // 重新设置url编码
            // page.setUrl(decodeUrl(page.getUrl(), page.getCharset()));

            // 记录站点防止频繁抓取的页面链接
            //  frequentAccesslog(page.getSeedName(), url, content, logger);

            //设置Response Cookie
            Set<Cookie> cookies = webDriver.manage().getCookies();
            String cookiesString = Joiner.on(";").join(cookies.toArray());
            page.setSetCookies(cookiesString);
        } catch (Exception e) {
            UrlQueue.newUnVisitedLink(page.getSeedName(), page.getUrl());
            logger.error("线程[{}]种子[{}]获取链接[{}]内容失败。",  Thread.currentThread().getName() ,  page.getSeedName(), page.getUrl(),  e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        } finally {
        	//webDriver.close();
        }
        return page;
    }

	@Override
	public String probePageContent(Page page) {
		WebDriver webDriver = getWebDriver(page);
        try {
            webDriver.get(page.getUrl());
            String content = webDriver.getPageSource();
 
            long contentlength = content.length();
            if (contentlength > big_file_max_size) {//大于10m
            	 logger.warn("线程[{}]探测种子[{}]url[{}]页面内容太大。",Thread.currentThread().getName(), page.getSeedName(), page.getUrl());
            }

            if (Strings.isNullOrEmpty(content)) {
                logger.error("线程[{}]探测种子[{}]url[{}]页面内容为空。", Thread.currentThread().getName(), page.getSeedName(), page.getUrl());
                return null;
            }

            if(isJsonContent(content)){
            	page.setCharset(getJsonCharset());
            	page.setJsonContent(content);
            } else if(isHtmlContent(content)){
            	page.setCharset(getHtmlCharset(content));
            	page.setHtmlContent(content);
            } else if(isXmlContent(content)){
            	page.setCharset(getXmlCharset(content));
            	page.setXmlContent(content);
            }

            return content;
        } catch (Exception e) {
            logger.error("线程[{}]探测种子[{}]url[{}]内容失败：{}", Thread.currentThread().getName(), page.getSeedName(), page.getUrl(), e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        }
        return null;
	}


}

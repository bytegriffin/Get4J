package com.bytegriffin.get4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;

import com.bytegriffin.get4j.conf.DefaultConfig;


/**
 * 测试HttpClient与HttpUnit在实际状态下的抓取结果
 */
public class TestHttpEngine {

	private static String url = "http://tousu.baidu.com/news/add";

	/**
	 * 注意：Jsoup在选择多个class时，中间的空格用点替代
	 *
	 * @param cotent
	 */
	public static void parse(String cotent) {
		Document doc = Jsoup.parse(cotent);
		Elements eles = doc.select("div._5pcb");
		System.err.println(eles.html());
		for (Element e : eles) {
			String link = e.attr("href");
			System.err.println(link);
		}
	}

	public static void httpclient() throws ClientProtocolException, IOException {
		long start = System.currentTimeMillis();
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();// 标准Cookie策略

		CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
		// 发送get请求
		HttpGet request = new HttpGet(url);
		// List <NameValuePair> params = new ArrayList<NameValuePair>();
		// params.add(new BasicNameValuePair("pageindex", "1"));
		// params.add(new BasicNameValuePair("pagesize", "5"));
		// params.add(new BasicNameValuePair("RepaymentTypeId", "0"));
		// params.add(new BasicNameValuePair("type", "6"));
		// params.add(new BasicNameValuePair("status", "1"));
		// params.add(new BasicNameValuePair("orderby", "0"));
		// params.add(new BasicNameValuePair("beginDeadLine","0"));
		// params.add(new BasicNameValuePair("endDeadLine","0"));
		// params.add(new BasicNameValuePair("rate","0"));
		// params.add(new BasicNameValuePair("beginRate","0"));
		// params.add(new BasicNameValuePair("endRate","0"));
		// params.add(new BasicNameValuePair("orderby","0"));
		// params.add(new BasicNameValuePair("Cmd","GetInvest_List"));
		//
		// request.setEntity(new UrlEncodedFormEntity(params));
		HttpResponse response = client.execute(request);
		long end = System.currentTimeMillis();
		long aaa = end - start;
		String content = EntityUtils.toString(response.getEntity(), Consts.UTF_8);
		parse(content);
		System.err.println(aaa + " " + response.getStatusLine().getStatusCode());
	}

	public static void selenium() throws Exception {
		System.setProperty("webdriver.chrome.driver",  "/opt/workspace/Get4J/get4j-sample/bin/chromedriver");
		
		ChromeDriverService service = new ChromeDriverService.Builder()
				.withSilent(true)
				.withLogFile(Paths.get(DefaultConfig.chromedriver_log).toFile())
                .usingDriverExecutable(new File("/opt/workspace/Get4J/get4j-sample/bin/chromedriver"))
                .usingAnyFreePort()
                .build();
		// Proxy proxy = new Proxy();
	    // proxy.setHttpProxy("myhttpproxy:3337");
	    // options.setCapability("proxy", proxy);
		ChromeOptions chromeOptions = new ChromeOptions();
    	chromeOptions.addArguments("--headless");
    	chromeOptions.addArguments("--start-maximized");
    	chromeOptions.addArguments("--lang=" + "zh-CN");
    	chromeOptions.addArguments("--test-type", "--ignore-certificate-errors");
    	//chromeOptions.addArguments("user-data-dir=C:/Users/user_name/AppData/Local/Google/Chrome/User Data");
    	chromeOptions.setCapability(CapabilityType.BROWSER_NAME, "Google Chrome");
    	//chromeOptions.setCapability("noSign", true);
    	//chromeOptions.setCapability("unicodeKeyboard", true);
    	chromeOptions.setCapability("newCommandTimeout", "60");
    	
    	WebDriver driver = new ChromeDriver(service, chromeOptions);
    	
		driver.manage().timeouts().implicitlyWait(50, TimeUnit.SECONDS);
		driver.get("https://www.facebook.com/groups/CardanoCommunity");	
		//显示等待控制对象
//	    WebDriverWait webDriverWait=new WebDriverWait(driver,10);
//	    //等待输入框可用后输入账号密码
//	    webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("loginName"))).sendKeys("");
//	    webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("loginPassword"))).sendKeys("");
//	    //点击登录
//	    driver.findElement(By.id("loginAction")).click();
//	    //等待2秒用于页面加载，保证Cookie响应全部获取。
//	    Thread.sleep(2000);
//	    //获取Cookie并打印
//	    Set<Cookie> cookies=driver.manage().getCookies();
//	    Iterator iterator=cookies.iterator();
//	    while (iterator.hasNext()){
//	        System.out.println(iterator.next().toString());
//	    }
		

		
		String content = driver.getPageSource();
		parse(content);
		System.out.println("======================: " +content);
		driver.quit();    
	}

	public static void main(String... args) throws Exception {
		// testunit();
		// httpclient();
		selenium();
	}
}

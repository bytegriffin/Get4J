package com.bytegriffin.get4j.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.send.EmailSender;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;

/**
 * 文件工具类
 */
public final class FileUtil {

	private static Logger logger = LogManager.getLogger(FileUtil.class);

	/**
	 * 读取User Agent文件到内存中
	 *
	 * @param userAgentFile UserAgent文件
	 * @return List
	 */
	public static List<String> readUserAgentFile(String userAgentFile) {
		return readFileLines(userAgentFile);
	}

	/**
	 * 获取系统绝对路径 <br>
	 * 将配置文件转换为爬虫系统的绝对路径，将classpath:/conf/user_agent转换成/opt/work/xxx/conf/user_agent
	 *
	 * @param classpath   String
	 * @return String
	 */
	public static String getSystemAbsolutePath(String classpath) {
		String newpath;
		if (classpath.contains("classpath:")) {
			newpath = System.getProperty("user.dir") + File.separator;
			classpath = classpath.replace("classpath:", "");
		} else if (classpath.contains("classpath：")) {
			newpath = System.getProperty("user.dir") + File.separator;
			classpath = classpath.replace("classpath：", "");
		} else {// 此时为绝对路径，不用转换
			return classpath;
		}
		if (classpath.substring(0, 1).equals("/")) {
			classpath = classpath.replaceFirst("/", "");
		}
		classpath = classpath.replace("/", File.separator);
		return newpath + classpath;
	}

	/**
	 * 读取http代理文件转换为HttpProxy对象到内存中
	 * http_proxy文件的格式是ip:port或者ip:port@username:password
	 *
	 * @param httpProxyFile  String
	 * @return List
	 */
	public static List<HttpProxy> readHttpProxyFile(String httpProxyFile) {
		List<String> list = readFileLines(httpProxyFile);
		if (list == null || list.size() == 0) {
			return null;
		}
		List<HttpProxy> newList = new ArrayList<>();
		for (String str : list) {
			if (Strings.isNullOrEmpty(str)) {
				continue;
			}
			HttpProxy hp;
			if (str.contains("@")) {
				String[] array = str.split("@");
				if (array.length > 0) {
					String[] front = array[0].split(":");
					String[] end = array[1].split(":");
					hp = new HttpProxy(front[0], Integer.valueOf(front[1]), end[0], end[1]);
				} else {
					if (str.contains(":")) {
						String[] front = str.split(":");
						hp = new HttpProxy(front[0], Integer.valueOf(front[1]));
					} else {
						hp = new HttpProxy(str);
					}
				}
			} else if (str.contains(":")) {
				String[] front = str.split(":");
				hp = new HttpProxy(front[0], Integer.valueOf(front[1]));
			} else {
				hp = new HttpProxy(str);
			}
			newList.add(hp);
		}
		return newList;
	}

	/**
	 * 将Proxy字符串解析成代理对象
	 *
	 * @param proxyString  代理字符串
	 * @return HttpProxy
	 */
	public static HttpProxy formatProxy(String proxyString) {
		if (Strings.isNullOrEmpty(proxyString)) {
			return null;
		}
		HttpProxy hp;
		if (proxyString.contains("@")) {
			String[] array = proxyString.split("@");
			String[] front = array[0].split(":");
			String[] end = array[1].split(":");
			hp = new HttpProxy(front[0], Integer.valueOf(front[1]), end[0], end[1]);
		} else if (proxyString.contains(":")) {
			String[] front = proxyString.split(":");
			hp = new HttpProxy(front[0], Integer.valueOf(front[1]));
		} else {
			hp = new HttpProxy(proxyString);
		}
		return hp;
	}

	/**
	 * 读取文件的每行数据将其放回到一个ArrayList中
	 *
	 * @param configFile  String
	 * @return List
	 */
	private static List<String> readFileLines(String configFile) {
		configFile = getSystemAbsolutePath(configFile);
		File file = new File(configFile);
		if (!file.exists()) {
			return null;
		}
		boolean fc = isExistContont(configFile);
		if (!fc) {
			return null;
		}
		List<String> result = Lists.newArrayList();
		try {
			List<String> list = Files.readLines(file, Charset.defaultCharset());
			for (String line : list) {
				// 去除每行开头为#的说明
				if (line.startsWith("#") || Strings.isNullOrEmpty(line)) {
					continue;
				}
				// 如果＃出现在每行中间，那么只将后面的说明去除
				if (line.contains("#")) {
					line = line.split("#")[0];
				}
				result.add(line);
			}
		} catch (IOException e) {
			logger.error("读取文件行时出错。", e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
		return result;
	}

	/**
	 * 判断文件是否为空，是否有内容
	 *
	 * @param filename  String
	 * @return boolean true：存在 false：不存在
	 */
	public static boolean isExistContont(String filename) {
		File file = new File(filename);
		return !(file.length() == 0);
	}

	/**
	 * 在磁盘上创建下载、索引文件夹
	 *
	 * @param diskDir  String
	 * @return String
	 */
	public static String makeDiskDir(String diskDir) {
		diskDir = getSystemAbsolutePath(diskDir);
		File homedir = new File(diskDir);
		if (!homedir.exists()) {
			try {
				homedir.mkdirs();
			} catch (Exception ex) {
				logger.warn("不能创建文件夹[{}]，它可能包含一些特殊字符串。", diskDir, ex);
			}
		}
		return diskDir.endsWith(File.separator) ? diskDir : diskDir + File.separator;
	}

	/**
	 * 生成文件夹以及以下的文件
	 * 
	 * @param filename   String
	 */
	public static void makeDiskFile(String filename) {
		File file = new File(filename);
		try {
			Files.createParentDirs(file);
			Files.touch(file);
		} catch (IOException e) {
			logger.error("创建文件时出错。", e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
	}

	/**
	 * 按行追加内容
	 *
	 * @param fileName   String
	 * @param contents   Collection
	 */
	public static synchronized void append(String fileName, Collection<String> contents) {
		if (contents == null || contents.isEmpty()) {
			return;
		}
		contents.stream().filter(str -> !Strings.isNullOrEmpty(str)).forEach(str -> {
			try {
				Files.asCharSink(new File(fileName), Charset.defaultCharset(), FileWriteMode.APPEND).write(str + System.getProperty("line.separator"));
			} catch (IOException e) {
				logger.error("追加文件时出错。", e);
				EmailSender.sendMail(e);
				ExceptionCatcher.addException(e);
			}
		});
	}

	/**
	 * 删除文件中的某行内容
	 *
	 * @param file    String
	 * @param content   String
	 */
	public static void removeLine(String file, String content) {
		try {
			file = FileUtil.getSystemAbsolutePath(file);
			File inFile = new File(file);
			// 临时文件用于转储，append时会自动创建，不用事先创建
			File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
			List<String> list = Files.readLines(inFile, Charset.defaultCharset());
			for (String line : list) {
				if (line.trim().equals(content)) {
					continue;
				}
				Files.asCharSink(tempFile, Charset.defaultCharset(), FileWriteMode.APPEND).write(line.trim() + System.getProperty("line.separator"));
			}
			Files.move(tempFile, inFile);
		} catch (IOException ex) {
			logger.error("删除文件时出错。", ex );
			EmailSender.sendMail(ex);
			ExceptionCatcher.addException(ex);
		}
	}

	/**
	 * 在本地磁盘生成页面
	 *
	 * @param page Page
	 */
	public static void downloadPagesToDisk(Page page) {
		String folderName = Globals.DOWNLOAD_DISK_DIR_CACHE.get(page.getSeedName());
		String fileName = folderName + File.separator;
		if (page.isJsonContent()) {
			fileName += generatePageFileName(page.getUrl(), DefaultConfig.json_page_suffix);
		} else if (page.isHtmlContent()) {
			fileName += generatePageFileName(page.getUrl(), DefaultConfig.html_page_suffix);
		} else if (page.isXmlContent()) {
			fileName += generatePageFileName(page.getUrl(), DefaultConfig.xml_page_suffix);
		} else {// 这种情况为资源文件，直接返回
			return;
		}
		byte[] content = null;
		try {
			if (page.isHtmlContent()) {
				content = page.getHtmlContent().getBytes(page.getCharset());
			} else if (page.isJsonContent()) {
				content = page.getJsonContent().getBytes(page.getCharset());
			} else if (page.isXmlContent()) {
				content = page.getXmlContent().getBytes(page.getCharset());
			}
		} catch (UnsupportedEncodingException e) {
			logger.error("Seed[{}]通过线程[{}]往硬盘上写入名为[{}]时出错。", page.getSeedName(), Thread.currentThread().getName(), fileName, e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
		FileUtil.writeFileToDisk(fileName, content);
	}

	/**
	 * 往硬盘上写文件
	 *
	 * @param fileName  String
	 * @param content    byte[]
	 */
	public static void writeFileToDisk(String fileName, byte[] content) {
		if (Strings.isNullOrEmpty(fileName) || content == null) {
			return;
		}
		File file = new File(fileName);
		if (file.exists() && content.length == file.length()) {
			//logger.warn("线程[{}]在往硬盘上写入名为[{}]时发现此文件已存在。",Thread.currentThread().getName(), fileName);
			return;
		}
		try {
			Files.write(content, file);
		} catch (IOException e) {
			logger.error("线程[{}]往硬盘上写入页面时出错。",Thread.currentThread().getName() , e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
	}

	/**
	 * 删除url的schema <br>
	 * 例如：http://www.website.com === www.website.com
	 * 
	 * @param url  String
	 * @return
	 */
	private static String deleteUrlSchema(String url) {
		if (Strings.isNullOrEmpty(url)) {
			return url;
		}
		return url.replaceAll("http://", "").replaceAll("https://", "");
	}

	/**
	 * 下载的网页文件名规则 <br>
	 * 此规则是在生成文件名时将url进行编码，然后去除其中的一些操作系统 <br>
	 * 不支持的特殊字符：尖括号、斜杠、竖杠、分号、星号、问号，否则生成的文件名会出错 <br>
	 * 默认生成的文件名是带url的，如果不想带可以将其过滤掉会更简洁，但是<br>
	 * 人为发现不了某个资源（js或css等）文件是属于哪个（php或jsp）页面的
	 *
	 * @param url String
	 * @param suffix  文件后缀名
	 * @return String
	 */
	public static String generatePageFileName(String url, String suffix) {
		String newUrl = deleteUrlSchema(url);
		String laststr = newUrl.substring(newUrl.length() - 1, newUrl.length());
		if (laststr.equals("/")) {
			newUrl = newUrl.substring(0, newUrl.length() - 1);
		}

		// 此时newUrl的格式为 www.aaa.com/path 或者 www.aaa.com
		if (!DefaultConfig.download_file_url_naming) {
			newUrl = newUrl.substring(newUrl.lastIndexOf("/") + 1, newUrl.length());
		}

		// 去除url中的参数，得到的newUrl可能为空：www.aaa.com
		if (newUrl.contains("?")) {
			newUrl = newUrl.substring(0, newUrl.indexOf("?"));
		}

		// 如果是newUrl为空或者www.aaa.com，那么直接将它改名成index.html
		if (Strings.isNullOrEmpty(newUrl) ) {
			newUrl = DefaultConfig.home_page_name;
		}

		// 判断动态页面url中没有后缀名的自动加上相应的后缀名
		if (!isFindPage(newUrl) ) {
			newUrl += "." + suffix;
		}

		// 过滤特殊字符串
		if (!newUrl.equals(DefaultConfig.home_page_name)) {
			newUrl = replaceSpecialChar(newUrl);
		}
		return newUrl;
	}

	/**
	 * 下载的资源命名规则：与页面命名类似，不同的是url后跟的参数全部删除掉，
	 * 因为大多数页面都是动态，后面加上参数会代表不同的页面，而资源文件则不同，根本不需要
	 * 注意：有些资源文件是一个页面经过跳转后的资源文件，比如www.aa.com/cc.php===www.aa.com/img.jpg
	 *
	 * @param url String
	 * @param suffix String
	 * @return String
	 */
	public static String generateResourceName(String url, String suffix) {
		String newUrl = deleteUrlSchema(url);
		String laststr = newUrl.substring(newUrl.length() - 1, newUrl.length());
		if (laststr.equals("/")) {
			newUrl = newUrl.substring(0, newUrl.length() - 1);
		}

		// 此时newUrl的格式为 www.aaa.com/path 或者 www.aaa.com
		if (!DefaultConfig.download_file_url_naming) {
			newUrl = newUrl.substring(newUrl.lastIndexOf("/") + 1, newUrl.length());
		}

		// 去除url中的参数，得到的newUrl可能为空：www.aaa.com
		if (newUrl.contains("?")) {
			newUrl = newUrl.substring(0, newUrl.indexOf("?"));
		}

		// 判断动态url中没有后缀名的自动加上相应的后缀名，有的资源文件没有后缀名，比如css不用写后缀照样也能引用
		// 注意：资源文件link[href]有时候是xml文件，例如：<link type="application/rss+xml" href="rss"/>
		// <link type="application/wlwmanifest+xml"
		// href="wlwmanifest.xml"/>，所以要判断xml后缀
		if (!FetchResourceSelector.isFindResources(newUrl) && !Strings.isNullOrEmpty(suffix)) {
			newUrl += "." + suffix;
		}

		return replaceSpecialChar(newUrl);
	}

	/**
	 * 默认用下划线取代url中的特殊字符，以方便生成合法的文件名
	 * 
	 * @param url String
	 * @return String
	 */
	private static String replaceSpecialChar(String url) {
		return url.replace("*", "_").replace("<", "_").replace(">", "_").replace("/", "_").replace("\\", "_")
				.replace("|", "_").replace(":", "_").replace("\"", "_").replace("?", "_");
		// 当然也可以对url中的特殊字符进行编码
		// try {
		// newUrl = URLEncoder.encode(newUrl,"UTF-8");
		// newUrl = newUrl.replace("*", "");
		// } catch (UnsupportedEncodingException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * 根据url的后缀名（.htm/.jsp/.asp等）来判断是否为一个页面
	 *
	 * @param url String
	 * @return boolean
	 */
	private static boolean isFindPage(String url) {
		return (url.contains(".htm") || url.contains(".jsp") || url.contains(".asp") || url.contains(".action")
				|| url.contains(".php") || url.contains(".xhtml") || url.contains(".shtm") || url.contains(".do")
				|| url.contains(".cgi") || url.contains(".xml") || url.contains(".perl") || url.contains(".ftm")
				|| url.contains(".vm") || url.contains(".thymes") || url.contains(".tml") || url.contains(".xjsp")
				|| url.contains(".jsf") || url.contains(".ftl"));
	}

	/**
	 * 事先创建指定大小的空内容的文件
	 *
	 * @param fileName  String
	 * @param fileSize   long
	 */
	public static void makeDiskFile(String fileName, long fileSize) {
		File newFile = new File(fileName);
		try (RandomAccessFile raf = new RandomAccessFile(newFile, "rw")) {
			raf.setLength(fileSize);
		} catch (Exception e) {
			logger.error("线程[{}]在硬盘上创建文件时出错。",Thread.currentThread().getName(),  e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
	}

	/**
	 * 判断文件是否已经存在磁盘上，有的话就无需下载
	 * @param fileName String
	 * @param fileSize long
	 * @return boolean
	 */
	public static boolean isExistsDiskFile(String fileName, long fileSize){
		File newFile = new File(fileName);
		if (newFile.exists() && newFile.length() == fileSize){
			return true;
		}
		return false;
	}

	/**
	 * 下载大文件到磁盘上
	 *
	 * @param fileName  String
	 * @param contentLength     Long
	 * @param content  InputStream
	 */
	public static void writeBigFileToDisk(String fileName, Long contentLength, InputStream content) {
		File newFile = new File(fileName);
		try (BufferedInputStream bis = new BufferedInputStream(content);
				RandomAccessFile raf = new RandomAccessFile(newFile, "rw")) {
			int unit = (int) (contentLength / 100);// 将文件大小分成100份
			int unitProgress = 0; // 用于保存当前进度(1~100%)
			int bytesRead;
			long offset = 0;
			byte[] buff = new byte[1024];
			while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
				raf.seek(offset);
				raf.write(buff, 0, bytesRead);
				offset = offset + bytesRead;
				int temp = (int) (offset / unit); // 计算当前百分比进度
				if (temp >= 1 && temp > unitProgress) {// 如果下载过程出现百分比变化
					unitProgress = temp;
					if (unitProgress % 20 == 0) {
						logger.info("线程[{}]下载文件[{}]的进度为[{}%]。",  Thread.currentThread().getName() ,fileName, unitProgress);
					}
				}
			}
		} catch (Exception e) {
			logger.error("线程[{}]下载文件到磁盘时出错。", Thread.currentThread().getName(), e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
	}

	/**
	 * 递归删除文件夹以及下面所有的文件
	 *
	 * @param path String
	 */
	public static void deleteFile(String path) {
		try {
			java.nio.file.Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					java.nio.file.Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					java.nio.file.Files.delete(dir);
					return super.postVisitDirectory(dir, exc);
				}
			});
		} catch (IOException e) {
			logger.error("线程[{}]递归删除文件夹[{}]时出错。", Thread.currentThread().getName() , path, e);
			EmailSender.sendMail(e);
			ExceptionCatcher.addException(e);
		}
	}

}

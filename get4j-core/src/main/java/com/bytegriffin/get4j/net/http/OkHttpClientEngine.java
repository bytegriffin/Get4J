package com.bytegriffin.get4j.net.http;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.UrlQueue;
import com.bytegriffin.get4j.download.DownloadFile;
import com.bytegriffin.get4j.send.EmailSender;
import com.bytegriffin.get4j.util.DateUtil;
import com.bytegriffin.get4j.util.FileUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class OkHttpClientEngine extends AbstractHttpEngine implements HttpEngine {

    private static final Logger logger = LogManager.getLogger(OkHttpClientEngine.class);

    private static OkHttpClient.Builder builder;

    @Override public void init(Seed seed) {
        // 1.初始化OKHttpClientBuilder
    	initOkHttpClientBuilder(seed.getSeedName());
    	
        // 2.初始化配置参数
        initParams(seed, logger);
        logger.info("种子[{}]的Http引擎OkHttpClientEngine的初始化完成。", seed.getSeedName());
    }

    /**
     * 检查Http Proxy代理是否可运行
     *
     * @return boolean
     */
    @Override public boolean testHttpProxy(String url, HttpProxy httpProxy) {
    	Request request = null;
    	Response response = null;
    	OkHttpClient okHttpClient = null;
        try {
        	OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        	if(httpProxy.getProxyAuthenticator() != null) {
        		clientBuilder.proxyAuthenticator(httpProxy.getProxyAuthenticator());
        	}
        	okHttpClient = clientBuilder.proxy(httpProxy.getProxy()).build();
        	request = new Request.Builder().url(url).build();
        	response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()){
                logger.info("Http代理[{}]测试成功。", httpProxy.toString() );
                return true;
            } else {
                logger.error("Http代理[{}]测试失败。", httpProxy.toString());
                return false;
            }
        } catch (IOException e) {
        	logger.error("Http代理[{}]测试失败。失败消息：[{}]", httpProxy.toString(),e);
            return false;
		} finally {
			closeConnection(okHttpClient, request, response);
        }
    }

    /**
     * 关闭和清理失效链接和过长链接
     */
    public static void closeConnection(OkHttpClient client, Request request, Response response) {
        if (client != null) {
            client.connectionPool().evictAll();
            client = null;
        }
        if(request != null) {
        	request = null;
        }
        if(response != null) {
        	if(response.body() != null) {
        		response.body().close();
        	}
        	response.close();
        	response = null;
        }
    }

    /**
     * 设置请求中的Http代理
     *
     * @param seedName seedName
     */
    protected static void setHttpProxy(String seedName) {
        HttpProxySelector hpl = Globals.HTTP_PROXY_CACHE.get(seedName);
        if (hpl == null) {
            return;
        }
        HttpProxy proxy = hpl.choice();
        if (proxy.getProxy() != null) {
        	builder.proxy(proxy.getProxy());
        }
        if (proxy.getProxyAuthenticator() != null) {
        	builder.proxyAuthenticator(proxy.getProxyAuthenticator());
        }
    }

    /**
     * 设置User_Agent
     * @param seedName 种子名称
     */
    protected static void setUserAgent(String seedName, okhttp3.Request.Builder requestBuilder) {
        UserAgentSelector ual = Globals.USER_AGENT_CACHE.get(seedName);
        if (ual == null) {
            return;
        }
        String userAgent = ual.choice();
        if (!Strings.isNullOrEmpty(userAgent)) {
        	requestBuilder.addHeader("User-Agent", userAgent);
        }
    }

    /**
     * 获取并设置page的页面内容（包含Html、Json、Xml）
     * 注意：有些网站会检查header中的Referer是否合法
     * 
     * @param page page
     * @return Page
     */
    public Page getPageContent(Page page) {
        OkHttpClient httpClient = null;
        Request request = null;
        Response response = null;
        ResponseBody responseBody = null;
        try {
            sleep(page.getSeedName(), logger);
            httpClient =  Globals.OK_HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
            Request.Builder requestBuilder = new Request.Builder();
        	if(page.isPost()){//post请求，默认是get请求
        		FormBody.Builder formBuilder = new FormBody.Builder();
        		if(page.getParams() != null && !page.getParams().isEmpty()) {
        			for(String key: page.getParams().keySet()) {
            			formBuilder.add(key, page.getParams().get(key));
            		}
        		}
        		requestBuilder.post(formBuilder.build());
        	}
            setHttpProxy(page.getSeedName());
            setUserAgent(page.getSeedName(), requestBuilder);
            setHost(requestBuilder, page, logger);
            request = setUrlAndHeader(requestBuilder, page.getUrl()).build();
            response = httpClient.newCall(request).execute();

            boolean isvisit = isVisit(httpClient, page, request, response, logger);
            if (!isvisit) {
                return page;
            }
            // 设置Response Cookie
            if (!Strings.isNullOrEmpty(request.header("Set-Cookie"))) {
                page.setSetCookies(request.header("Set-Cookie"));
            }
            
            responseBody = response.body();
            if (cacheBigFile(page.getSeedName(), page.getUrl(), responseBody.contentLength())) {
                return page;
            }
            
            String content = responseBody.string();
            if (Strings.isNullOrEmpty(content)) {
                UrlQueue.newFailVisitedUrl(page.getSeedName(), page.getUrl());
                logger.warn("线程[{}]获取种子[{}]url[{}]页面内容为空。",Thread.currentThread().getName(),  page.getSeedName() , page.getUrl());
                return page;
            }

            // 设置页面编码
            MediaType mediaType = responseBody.contentType();
            page.setCharset(getCharset(mediaType, content));

            // 重新设置url编码
            // page.setUrl(decodeUrl(page.getUrl(), page.getCharset()));

            // 记录站点防止频繁抓取的页面链接
            // frequentAccesslog(page.getSeedName(), page.getUrl(), content, logger);

            // 根据不同编码设置page内容
            setContent(mediaType, content, page);

        } catch (Exception e) {
            UrlQueue.newUnVisitedLink(page.getSeedName(), page.getUrl());
            logger.error("线程[{}]获取种子[{}]url[{}]页面内容失败。",Thread.currentThread().getName(),  page.getSeedName() , page.getUrl(), e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        } finally {
        	closeConnection(httpClient, request, response);
        }
        return page;
    }

    /**
     * 下载大文件，默认设置超过10M大小的文件算是大文件
     * 文件太大会抛异常，所以特此添加一个下载大文件的方法
     *
     * @param seedName 种子名称
     * @param url 文件链接
     * @param contentLength 网页内容长度
     * @return boolean
     */
    public static boolean downloadBigFile(String seedName, String url, long contentLength) {
        String start = DateUtil.getCurrentDate();
        String fileName = FileUtil.generateResourceName(url, "");
        fileName = Globals.DOWNLOAD_DISK_DIR_CACHE.get(seedName) + fileName;
        if(FileUtil.isExistsDiskFile(fileName, contentLength)){
        	logger.warn("线程[{}]下载种子[{}]的大文件[{}]时发现磁盘上已经存在此文件[{}]。"
        			,Thread.currentThread().getName(),  seedName, url, fileName);
        	return true;
        }
        FileUtil.makeDiskFile(fileName, contentLength);
        OkHttpClient httpClient = null;
        Request request = null;
        Response response = null;
        ResponseBody responseBody = null;
        try {
        	httpClient =  Globals.OK_HTTP_CLIENT_BUILDER_CACHE.get(seedName).build();
        	Request.Builder requestBuilder = setUrlAndHeader(new Request.Builder(), url);
            setHttpProxy(seedName);
            setUserAgent(seedName, requestBuilder);
            //断点续传的话需要设置Range属性，当然前提是服务器支持
            //requestBuilder.addHeader("Range", "bytes=" + offset + "-" + (this.offset + this.length - 1));
            request = requestBuilder.build();
            response = httpClient.newCall(request).execute();
            responseBody = response.body();
            FileUtil.writeBigFileToDisk(fileName, contentLength, responseBody.byteStream());
            String timeLog = DateUtil.getCostDate(start);
            logger.info("线程[{}]下载大小为[{}]MB的文件[{}]总共花费时间为[{}]。",Thread.currentThread().getName() ,
            		contentLength / (1024 * 1024),fileName, timeLog);
        } catch (Exception e) {
            logger.error("线程[{}]下载种子[{}]的大文件[{}]时失败。", Thread.currentThread().getName() ,seedName, url, e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(seedName, e);
        } finally {
        	closeConnection(httpClient, request, response);
        }
        return true;
    }

    /**
     * 下载网页中的资源文件（JS/CSS/JPG等）
     * @param page 页面对象
     * @param folderName 文件夹名称
     * @return List
     */
    public static List<DownloadFile> downloadResources(Page page,String folderName) {
        if (page.getResources() == null || page.getResources().size() == 0) {
            return null;
        }
        sleep(page.getSeedName(), logger);
        setHttpProxy(page.getSeedName());
        OkHttpClient httpClient =  Globals.OK_HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
        List<DownloadFile> list = Lists.newArrayList();
        LinkedHashSet<String> resources = page.getResources();
        for (String url : resources) {
        	Request request = null;
        	Response response = null;
        	ResponseBody responseBody = null;
            try {
            	Request.Builder requestBuilder = setUrlAndHeader(new Request.Builder(), page.getUrl());
                setUserAgent(page.getSeedName(), requestBuilder);
                request = requestBuilder.build();
                response = httpClient.newCall(request).execute();

                boolean isvisit = isVisit(httpClient, page, request, response, logger);
                if (!isvisit) {
                    continue;
                }

                responseBody = response.body();
                boolean isdone = cacheBigFile(page.getSeedName(), page.getUrl(), responseBody.contentLength());
                if (isdone) {
                	continue;
                }

                // 设置页面编码
                MediaType mediaType = responseBody.contentType();
                
                DownloadFile dfile = new DownloadFile();
                dfile.setContent(responseBody.bytes());
                String content = new String(dfile.getContent());//不能再次使用ResponseBody.string()，会提示已经关闭
                
                String fileSuffix = "";//文件名后缀
                String resourceName = "";//完整的路径文件名
                if (mediaType == null || Strings.isNullOrEmpty(mediaType.toString())) {
                    resourceName = folderName + FileUtil.generateResourceName(url, fileSuffix);
                    dfile.setFileName(resourceName);
                    list.add(dfile);
                    continue;
                } else {
                	String subType = mediaType.subtype(); // 默认为文件后缀名与ContentType保持一致
                    if (isJsonPage(mediaType, content) || isHtmlPage(mediaType) || isXmlPage(mediaType, content)) {
                        continue;// 如果是页面就直接过滤掉
                    }
                    fileSuffix = getResourceSuffix(subType);
                }
                resourceName = folderName + FileUtil.generateResourceName(url, fileSuffix);
                dfile.setFileName(resourceName);
                list.add(dfile);
            } catch (Exception e) {
                UrlQueue.newFailVisitedUrl(page.getSeedName(), url);
                logger.error("线程[{}]下载种子[{}]的url[{}]资源失败。", Thread.currentThread().getName(), page.getSeedName() ,url, e);
                EmailSender.sendMail(e);
                ExceptionCatcher.addException(page.getSeedName(), e);
            } finally {
            	closeConnection(null, request, response);
            }
        }
    	closeConnection(httpClient, null, null);
        return list;
    }

    /**
     * 下载avatar资源文件
     * @param page 页面对象
     * @param folderName 文件夹名称
     * @return DownloadFile
     */
    public static DownloadFile downloadAvatar(Page page, String folderName) {
        String url = page.getAvatar();
        if (Strings.isNullOrEmpty(url)) {
            return null;
        }
        Request request = null;
        Response response = null;
        ResponseBody responseBody = null;
        OkHttpClient httpClient = null;
        try {
            sleep(page.getSeedName(), logger);
            setHttpProxy(page.getSeedName());
            httpClient =  Globals.OK_HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
        	Request.Builder requestBuilder = setUrlAndHeader(new Request.Builder(), page.getUrl());
            setUserAgent(page.getSeedName(), requestBuilder);
            request = requestBuilder.build();
            response = httpClient.newCall(request).execute();
            
            boolean isvisit = isVisit(httpClient, page, request, response, logger);
            if (!isvisit) {
                return null;
            }
            
            responseBody = response.body();
            boolean isdone = cacheBigFile(page.getSeedName(), page.getUrl(), responseBody.contentLength());
            if (isdone) {
                return null;
            }

            // 设置页面编码
            MediaType mediaType = responseBody.contentType();
            
            DownloadFile dfile = new DownloadFile();
            dfile.setContent(responseBody.bytes());
            String content = new String(dfile.getContent());//不能再次使用ResponseBody.string()，会提示已经关闭
            
            String fileSuffix = "";//文件名后缀
            String resourceName = "";//完整的路径文件名
            if (mediaType == null || Strings.isNullOrEmpty(mediaType.toString())) {
                resourceName = folderName + FileUtil.generateResourceName(url, fileSuffix);
                dfile.setFileName(resourceName);
                return dfile;
            } else {
            	String subType = mediaType.subtype(); // 默认为文件后缀名与ContentType保持一致
                if (isJsonPage(mediaType, content) || isHtmlPage(mediaType) || isXmlPage(mediaType, content)) {
                	return null;// 如果是页面就直接过滤掉
                }
                fileSuffix = getResourceSuffix(subType);
                resourceName = folderName + FileUtil.generateResourceName(url, fileSuffix);
                dfile.setFileName(resourceName);
                page.setAvatar(resourceName);
                return dfile;
            }
        } catch (Exception e) {
            UrlQueue.newFailVisitedUrl(page.getSeedName(), url);
            logger.error("线程[{}]下载种子[{}]的url[{}]资源失败。",Thread.currentThread().getName() , page.getSeedName(), url, e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        } finally {
        	closeConnection(httpClient, request, response);
        }
        return null;
    }

    /**
     * 探测页面内容 <br>
     * 针对于getPageContent方法有些裁剪
     * @param page 内容对象
     * @return String
     */
    @Override public String probePageContent(Page page) {
    	Request request = null;
    	Response response = null;
    	ResponseBody responseBody = null;
    	OkHttpClient httpClient = null;
    	try {
            setHttpProxy(page.getSeedName());
            httpClient = Globals.OK_HTTP_CLIENT_BUILDER_CACHE.get(page.getSeedName()).build();
        	Request.Builder requestBuilder = setUrlAndHeader(new Request.Builder(), page.getUrl());
            setUserAgent(page.getSeedName(), requestBuilder);
            request = requestBuilder.build();
            response = httpClient.newCall(request).execute();
            
            boolean isvisit = isVisit(httpClient, page, request, response, logger);
            if (!isvisit) {
                return null;
            }
            
            responseBody = response.body();

            String content = responseBody.string();
            if (Strings.isNullOrEmpty(content)) {
                UrlQueue.newFailVisitedUrl(page.getSeedName(), page.getUrl());
                logger.warn("线程[{}]获取种子[{}]url[{}]页面内容为空。",Thread.currentThread().getName(),  page.getSeedName() , page.getUrl());
                return null;
            }

            // 设置页面编码
            MediaType mediaType = response.body().contentType();
            page.setCharset(getCharset(mediaType, content));

            // 根据不同编码设置page内容
            setContent(mediaType, content, page);
            
            return content;
        } catch (Exception e) {
            logger.error("线程[{}]探测种子[{}]的url[{}]内容失败。",Thread.currentThread().getName() ,page.getSeedName() , page.getUrl(), e);
            EmailSender.sendMail(e);
            ExceptionCatcher.addException(page.getSeedName(), e);
        } finally {
        	closeConnection(httpClient, request, response);
        }
        return null;
    }

}

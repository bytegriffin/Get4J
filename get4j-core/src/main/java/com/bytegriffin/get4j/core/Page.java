package com.bytegriffin.get4j.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.bytegriffin.get4j.fetch.FetchResourceSelector;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jayway.jsonpath.JsonPath;

/**
 * Page对象
 */
public class Page {
    /**
     * 数据库中主键
     */
    private String id;
    /**
     * 抓取url所属的网站
     */
    private String host;
    /**
     * 当前seed名称：对应于Seed，如果要抓取多个种子，请保证此名称唯一
     */
    private String seedName;
    /**
     * 当前页面title信息
     */
    private String title;
    /**
     * 当前页面的html内容
     */
    private String htmlContent;
    /**
     * 当前页面的json内容
     */
    private String jsonContent;
    /**
     * 当前页面是xml内容
     */
    private String xmlContent;
    /**
     * 当前页面的url
     */
    private String url;
    /**
     * Http请求方法
     */
    private String method = "get";
    /**
     * post请求参数
     */
    private Map<String, String> params;
    /**
     * 页面编码
     */
    private String charset;
    /**
     * Response的cookie字符串
     */
    private String cookies;

    /**
     * 当前页面中的资源文件：js、jpg、css等文件
     */
    private HashSet<String> resources = Sets.newHashSet();
    /**
     * 当前资源文件存储路径
     */
    private String resourceSavePath;
    /**
     * 抓取时间
     */
    private String fetchTime;
    /**
     * 当启动list_detail抓取模式时，每个详情页对应的avatar资源
     */
    private String avatar;
    /**
     * 当启动list_detail抓取模式时，每个列表所对应的详情页
     */
    private HashSet<String> detailLinks = Sets.newHashSet();
    /**
     * 自定义动态字段
     */
    private Map<String, Object> fields = Maps.newHashMap();

    public Page() {
    }

    public Page(String seedName, String url) {
        this.seedName = seedName;
        this.url = url;
    }
    
    public Page(String seedName, String url, String method) {
        this.seedName = seedName;
        this.url = url;
        this.method = method;
    }
    
    /**
     * 是否是post方法
     * @return boolean
     */
    public boolean isPost(){
    	if("post".equalsIgnoreCase(this.method)){
    		return true;
    	}
    	return false;
    }

    /**
     * 页面内容是否为Json格式
     *
     * @return boolean
     */
    public boolean isJsonContent() {
        return !Strings.isNullOrEmpty(this.jsonContent);
    }

    /**
     * 页面内容是否为Html格式
     *
     * @return boolean
     */
    public boolean isHtmlContent() {
        return !Strings.isNullOrEmpty(this.htmlContent);
    }

    /**
     * 页面内容是否为Xml格式
     *
     * @return boolean
     */
    public boolean isXmlContent() {
        return !Strings.isNullOrEmpty(this.xmlContent);
    }

    /**
     * 保证同一个SeedName下detailPages的唯一性
     */
    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * 保证同一个SeedName下detailPages的唯一性
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Page p = (Page) obj;
        if (!this.seedName.equals(p.seedName)) {
            return false;
        }
        if (!this.url.equals(p.url)) {
            return false;
        }
        return true;
    }

    /**
     * 根据JsonPath解析JsonContent
     * 注意:有些Http Response返回的Content-Type是text/html而不是json
     * 
     * @param jsonPath   Jsonpath字符串
     * @return Object
     */
    public String json(String jsonPath) {
        if (Strings.isNullOrEmpty(jsonPath)  || this.isXmlContent()) {
            return null;
        }
        try{
        	return  JsonPath.read(this.getContent(), jsonPath).toString();
        }catch(com.jayway.jsonpath.PathNotFoundException e){
        	return null;
        }
        
    }

    /**
     * 根据Jsoup原生支持的cssSelect或正则表达式解析Html
     *
     * @param jsoupSelect jsoup支持的select字符串
     * @return String
     */
    public String jsoupText(String jsoupSelect) {
    	if(Strings.isNullOrEmpty(this.htmlContent)){
    		return null;
    	}
        Document doc = Jsoup.parse(this.htmlContent);
        Elements eles = doc.select(jsoupSelect);
        return eles.text();
    }

    /**
     * 根据Jsoup原生支持的cssSelect或正则表达式解析Html
     *
     * @param jsoupSelect jsoup支持的select字符串
     * @return Elements
     */
    public Elements jsoup(String jsoupSelect) {
    	if(Strings.isNullOrEmpty(this.htmlContent)){
    		return null;
    	}
        Document doc = Jsoup.parse(this.htmlContent);
        return doc.select(jsoupSelect);
    }

    public String getContent(){
        String content = "";
        if (this.isHtmlContent()) {
            content = this.getHtmlContent();
        } else if (this.isJsonContent()) {
            content = this.getJsonContent();
        } else if (this.isXmlContent()) {
            content = this.getXmlContent();
        }
        return content;
    }

    /**
     * 根据Jsoup原生支持的cssSelect或正则表达式解析Xml
     *
     * @param jsoupSelect jsoup支持的select字符串
     * @return List
     */
    public List<String> jsoupXml(String jsoupSelect) {
    	if(Strings.isNullOrEmpty(this.xmlContent)){
    		return null;
    	}
        return FetchResourceSelector.xmlSelect2List(this.xmlContent, jsoupSelect);
    }

    /**
     * 是否需要更新数据库中的page数据
     * 注意：每次请求返回的Cookie都不一样，页面内容确实相同，这种情况下是不是可以不需要此方法，直接全部更新呢？
     *
     * @param dbPage 数据库中出来的page对象
     * @return boolean
     */
    public boolean isRequireUpdate(Page dbPage) {
        if (null == dbPage) {
            return false;
        }
        boolean flag = false;
        try {
            // title中也可能带有单引号之类的特殊字符，所以需要转义处理
            if (!(this.getTitle() == null ? dbPage.getTitle() == null
                    : URLEncoder.encode(this.getTitle(), this.charset).equals(dbPage.getTitle()))) {
                flag = true;
            }
            // html页面首先要解码，因为之前存进去的时候是编码的（为了过滤某些单引号之类的字符串）
            if (!(this.getHtmlContent() == null ? dbPage.getHtmlContent() == null
                    : URLEncoder.encode(this.getHtmlContent(), this.charset).equals(dbPage.getHtmlContent()))) {
                flag = true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (!(this.getJsonContent() == null ? dbPage.getJsonContent() == null
                : this.getJsonContent().equals(dbPage.getJsonContent()))) {
            flag = true;
        }

        if (!(this.getXmlContent() == null ? dbPage.getXmlContent() == null
                : this.getXmlContent().equals(dbPage.getXmlContent()))) {
            flag = true;
        }

        if (!(this.getCookies() == null ? dbPage.getCookies() == null
                : this.getCookies().equals(dbPage.getCookies()))) {
            flag = true;
        }

        if (!(this.getAvatar() == null ? dbPage.getAvatar() == null : this.getAvatar().equals(dbPage.getAvatar()))) {
            flag = true;
        }
        return flag;
    }

    /**
     * 之所以要另开一个方法是因为mongodb不用encode文本内容
     *
     * @param dbPage 从数据库中查出的页面对象
     * @return boolean
     */
    public boolean isRequireUpdateNoEncoding(Page dbPage) {
        if (null == dbPage) {
            return false;
        }
        boolean flag = false;
        // title中也可能带有单引号之类的特殊字符，所以需要转义处理
        if (!(this.getTitle() == null ? dbPage.getTitle() == null
                : this.getTitle().equals(dbPage.getTitle()))) {
            flag = true;
        }
        // html页面首先要解码，因为之前存进去的时候是编码的（为了过滤某些单引号之类的字符串）
        if (!(this.getHtmlContent() == null ? dbPage.getHtmlContent() == null
                : this.getHtmlContent().equals(dbPage.getHtmlContent()))) {
            flag = true;
        }
        if (!(this.getJsonContent() == null ? dbPage.getJsonContent() == null
                : this.getJsonContent().equals(dbPage.getJsonContent()))) {
            flag = true;
        }

        if (!(this.getXmlContent() == null ? dbPage.getXmlContent() == null
                : this.getXmlContent().equals(dbPage.getXmlContent()))) {
            flag = true;
        }

        if (!(this.getCookies() == null ? dbPage.getCookies() == null
                : this.getCookies().equals(dbPage.getCookies()))) {
            flag = true;
        }

        if (!(this.getAvatar() == null ? dbPage.getAvatar() == null
                : this.getAvatar().equals(dbPage.getAvatar()))) {
            flag = true;
        }
        return flag;
    }


    public Map<String, Object> getFields() {
		return fields;
	}

	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}
	
	public void putField(String name, Object value){
		this.fields.put(name, value);
	}

	public Object getField(String name){
		return this.fields.get(name);
	}

	public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public void setXmlContent(String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public String getSeedName() {
        return seedName;
    }

    public void setSeedName(String seedName) {
        this.seedName = seedName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public HashSet<String> getResources() {
        return resources;
    }

    public void setResources(HashSet<String> resources) {
        this.resources = resources;
    }

    public HashSet<String> getDetailLinks() {
        return detailLinks;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setDetailLinks(HashSet<String> detailLinks) {
        this.detailLinks = detailLinks;
    }

    public String getFetchTime() {
        return fetchTime;
    }

    public void setFetchTime(String fetchTime) {
        this.fetchTime = fetchTime;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getResourceSavePath() {
        return resourceSavePath;
    }

    public void setResourceSavePath(String resourceSavePath) {
        this.resourceSavePath = resourceSavePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

}

![Image text](https://raw.githubusercontent.com/bytegriffin/get4j/master/logo.png)
===========================
  Get4J是一款较为通用的爬虫服务器，它包含了抓取、下载、解析、存储、转发等多个子流程，每个子流程可以根据配置或API调用来自由组合。

# 特性
* 支持抓取不同的页面模型：列表-详情页、单页、级联页、整站
* 支持抓取不同的页面格式：Html、Xml、Json、混合页
* 支持配置使用或API调用，且API的风格是使用Fluent API（流式接口）
* 支持单机、分布式两种部署模式
* 支持定时、即时、监控页面变化的抓取模式
* 支持JSoup、Jsonpath原生语法解析
* 支持抓取页面中的Ajax、Javascript生成的数据
* 可配置User-Agent、代理，并随机选取进行访问
* 支持将抓取的内容保存到不同的引擎中：Disk、Mysql、MongoDB、Lucene、HBase等
* 支持JMX监控，实时检查各个爬虫节点/进程的健康状况，一旦发现问题会自动发Email通知管理员

# 安装
请查看get4j-core/INSTALL.txt或get4j-cluster/INSTALL.txt文件

## Maven Repository
单机版:

```xml
<dependency>
  <groupId>com.github.bytegriffin</groupId>
  <artifactId>get4j-core</artifactId>
  <version>1.0.1</version>
</dependency>
```
集群版:

```xml
<dependency>
  <groupId>com.github.bytegriffin</groupId>
  <artifactId>get4j-cluster</artifactId>
  <version>1.0.1</version>
</dependency>
```

# 编程实例
抓取本页面上的内容简介：

```java
import com.bytegriffin.get4j.Spider;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class GithubPageParser implements PageParser {
    @Override
    public void parse(Page page) {
    	String content = page.jsoupText("div.readme");
    	System.err.println("页面title：["+page.getTitle() + "] Get4J页面上的简介: " + content );
    }

    public static void main(String[] args) throws Exception {
	Spider.single().fetchUrl("https://github.com/bytegriffin/Get4J")
	    .parser(GithubPageParser.class).thread(1).start();
    }
}
```

更多的抓取例子详见get4j-sample

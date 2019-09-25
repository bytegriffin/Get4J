package com.bytegriffin.get4j;

import com.bytegriffin.get4j.conf.ClusterNode;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.parse.PageParser;

public class TestCluster implements PageParser{

	@Override
	public void parse(Page page) {
		System.err.println("书名："+page.getTitle() + "  详情页：" + page.getUrl() );
	}

	public static void main(String[] args) throws Exception {
		ClusterNode node = Cluster.create()
				//.redis("cluster", "192.168.1.102:6374,192.168.1.102:6375,"
				//+ "192.168.1.102:6376,192.168.1.102:6377,192.168.1.102:6378,192.168.1.102:6379","")
				.zookeeper("192.168.1.102:2181,192.168.1.102:2182,192.168.1.102:2183")
				.build();
		Spider.list_detail().cluster(node).fetchUrl("http://bang.dangdang.com/books/bestsellers/1-{1}")
			.parser(TestCluster.class).detailLinkSelector("div.name > a[href]").start();
	}


}

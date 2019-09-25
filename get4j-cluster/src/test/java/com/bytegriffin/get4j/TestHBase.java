package com.bytegriffin.get4j;

import com.bytegriffin.get4j.conf.ClusterNode;

public class TestHBase {

	public static void main(String[] args) throws Exception {
		ClusterNode node = Cluster.create().enableHBase().build();//只有cluster端才能开启hbase功能
		Spider.single().cluster(node).fetchUrl("http://www.baidu.com/").hbase("192.168.1.102:2181").start();
	}

}

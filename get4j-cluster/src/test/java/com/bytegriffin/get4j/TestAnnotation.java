package com.bytegriffin.get4j;

import com.bytegriffin.get4j.annotation.Redis;
import com.bytegriffin.get4j.annotation.Zookeeper;
import com.bytegriffin.get4j.conf.ClusterNode;

@Redis(mode="one",address="192.168.1.13:6379",auth="foobared")
@Zookeeper("192.168.1.14:2181,192.168.1.14:2182,192.168.1.14:2183")
public class TestAnnotation {

	public static void main(String[] args) {
		ClusterNode node = Cluster.create().redis(TestAnnotation.class).build();
		Spider.single().cluster(node).fetchUrl("http://www.baidu.com").start();
	}

}

package com.bytegriffin.get4j.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

public final class NetHelper {
	
	private static final Logger logger = LogManager.getLogger(NetHelper.class);
	
	/**
	 * 获取集群节点名称 格式：ip@pid
	 * @return String
	 */
	public static String getClusterNodeName(){
		try{
			return  ip() +"@" + pid();
		}catch(Exception e){
			logger.error("获取集群节点名称出现问题："+e);
		}
		return MD5Util.generateSeedName();
	}

	/**
	 * 获取本地ip地址
	 * 
	 * @return String
	 * @throws SocketException 异常
	 * @throws UnknownHostException 异常
	 */
	public static String ip() throws SocketException, UnknownHostException {
		String localIp = null;
		Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
		while (enumeration.hasMoreElements()) {
			NetworkInterface networkInterface = enumeration.nextElement();
			if (networkInterface.isUp()) {
				Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
				while (addressEnumeration.hasMoreElements()) {
					String ip = addressEnumeration.nextElement().getHostAddress();
					final String REGX_IP = "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)";
					if (ip.matches(REGX_IP) && !ip.equals("127.0.0.1")) {
						localIp = ip;
					}
				}
			}
		}
		if (Strings.isNullOrEmpty(localIp)) {
			localIp = InetAddress.getLocalHost().getHostName();
		}
		return localIp;
	}

	/**
	 * 获取进程id
	 * @return String
	 */
	private static String pid() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		return name.split("@")[0];
	}

}

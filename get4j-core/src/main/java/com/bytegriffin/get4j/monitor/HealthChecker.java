package com.bytegriffin.get4j.monitor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.google.common.collect.Maps;

/**
 * 系统健康检查入口
 */
public class HealthChecker {

	private static final String jmx_server_name = "Get4J";
	private static final Logger logger = LogManager.getLogger(HealthChecker.class);
	private static Map<String, HealthStatus> healthStatusMap = Maps.newHashMap();

	public void register(String seedName) {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		try {
			HealthStatus healthStatus = new HealthStatus(seedName);
			ObjectName objectName = new ObjectName(jmx_server_name + ":name=" + seedName);
			if(!server.isRegistered(objectName) || healthStatusMap.get(seedName) == null){
				server.registerMBean(healthStatus, objectName);
				healthStatusMap.put(seedName, healthStatus);
				logger.info("种子[{}]成功注册Jmx服务。",seedName);
			} else {
				logger.info("种子[{}]已经注册过Jmx服务，无需再次注册。",seedName);
			}
			// CounterMonitor monitor = new CounterMonitor();
			// monitor.addObservedObject(objectName);
			// monitor.setDifferenceMode(true);
			// monitor.setGranularityPeriod(100);
			// monitor.setObservedAttribute("unVisitUrlCount");
			// monitor.start();
		} catch (Exception e) {
			logger.error("种子[{}]注册health失败。", seedName, e);
		}
	}

	public HealthStatus snapshot(String seedName){
		return healthStatusMap.get(seedName);
	}

	public String getOS() {
		OperatingSystemMXBean operateSystemMBean = ManagementFactory.getOperatingSystemMXBean();
		String operateName = operateSystemMBean.getName();
		int processListCount = operateSystemMBean.getAvailableProcessors();
		String archName = operateSystemMBean.getArch();
		String version = operateSystemMBean.getVersion();
		return operateName + " " + archName + " " + version + " " + processListCount;
	}

	public MemoryUsage getHeapMemory() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getHeapMemoryUsage();
	}

	public MemoryUsage getNonHeapMemory() {
		MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
		return memory.getNonHeapMemoryUsage();
	}
	
	public ThreadInfo[] getThreads() {
		ThreadMXBean tm = (ThreadMXBean) ManagementFactory.getThreadMXBean();
		long[] ids = tm.getAllThreadIds();
		return tm.getThreadInfo(ids);
	}

	public ThreadInfo[] getDeadLockThreads() {
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		long[] deadlockedIds = thread.findDeadlockedThreads();
		if (deadlockedIds != null && deadlockedIds.length > 0) {
			return thread.getThreadInfo(deadlockedIds);
		}
		return null;
	}

	public static List<String> getAllException(){
		return ExceptionCatcher.getAllExceptions();
	}

}

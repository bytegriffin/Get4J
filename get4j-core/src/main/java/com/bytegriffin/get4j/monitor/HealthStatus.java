package com.bytegriffin.get4j.monitor;

import java.util.List;

import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Launcher;
import com.bytegriffin.get4j.util.DateUtil;

import com.google.common.base.Strings;
import com.bytegriffin.get4j.core.UrlQueue;

/**
 * 健康状态：可用jconsole查询
 */
public class HealthStatus implements HealthStatusMXBean {

	private String seedName;

	public HealthStatus(String seedName) {
		this.seedName = seedName;
	}

	@Override
	public long getVisitedUrlCount() {
		return UrlQueue.getVisitedUrlCount(seedName);
	}

	@Override
	public long getUnVisitUrlCount() {
		return UrlQueue.getUnVisitedUrlCount(seedName);
	}

	@Override
	public long getFailedUrlCount() {
		return UrlQueue.getFailVisitedUrlCount(seedName);
	}

	@Override
	public List<String> getExceptions() {
		return ExceptionCatcher.getExceptions(seedName);
	}

	@Override
	public String getCostTime() {
		String str = Globals.PER_START_TIME_CACHE.get(seedName);
		if(Strings.isNullOrEmpty(str)){
			str = DateUtil.getCurrentDate();
		}
		return DateUtil.getCostDate(str);
	}

	@Override
	public String getSpiderStatus() {
		if (!Globals.LAUNCHER_CACHE.containsKey(seedName)) {
			return "已销毁";
		}
		Launcher launcher = Globals.LAUNCHER_CACHE.get(seedName);
		if (launcher.getCondition()) {
			return "运行中";
		} else {
			return "已暂停";
		}
	}

}

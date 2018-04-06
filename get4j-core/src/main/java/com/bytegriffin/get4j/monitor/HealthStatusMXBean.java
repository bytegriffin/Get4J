package com.bytegriffin.get4j.monitor;

import java.util.List;

public interface HealthStatusMXBean {

	public long getVisitedUrlCount();

	public long getUnVisitUrlCount();

	public long getFailedUrlCount();

	public List<String> getExceptions();

	public String getCostTime();

	public String getSpiderStatus();

}

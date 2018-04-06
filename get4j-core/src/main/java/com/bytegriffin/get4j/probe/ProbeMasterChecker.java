package com.bytegriffin.get4j.probe;

public interface ProbeMasterChecker {

	/**
	 * 是否处于Active状态
	 * @param seedName String
	 * @return boolean
	 */
	boolean isActive(String seedName);

}

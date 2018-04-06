package com.bytegriffin.get4j.conf;

import java.util.Map;

public class DynamicField {

	// 种子名称
	private String seedName;
	// key: 字段名称  value: 字段值
	private Map<String,String> fields;

	public String getSeedName() {
		return seedName;
	}
	public void setSeedName(String seedName) {
		this.seedName = seedName;
	}
	public Map<String, String> getFields() {
		return fields;
	}
	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}

}

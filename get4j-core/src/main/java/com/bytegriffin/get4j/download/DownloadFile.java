package com.bytegriffin.get4j.download;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DownloadFile {

	private String fileName;
	private byte[] content;
	private long contentLength;
	private String seedName;
	private String url;
	// 待下载的大文件列表 key:seedName value: download file list
	private static Map<String, List<DownloadFile>> download_big_file_list = Maps.newHashMap();

	/**
	 * 先将要下载的大文件地址加入到待下载列表中
	 * 
	 * @param seedName 种子名称
	 * @param downloadFile 下载文件对象
	 */
	public static void add(String seedName, DownloadFile downloadFile) {
		List<DownloadFile> list = download_big_file_list.get(seedName);
		if (list != null && list.isEmpty()) {
			list.add(downloadFile);
		} else {
			list = Lists.newArrayList();
			list.add(downloadFile);
			download_big_file_list.put(seedName, list);
		}
	}

	/**
	 * 是否有大文件需要下载
	 * 
	 * @param seedName 种子名称
	 * @return 如果有大文件需要下载返回true，否则返回false
	 */
	public static boolean isExist(String seedName) {
		List<DownloadFile> list = download_big_file_list.get(seedName);
		if (list == null || list.isEmpty()) {
			return false;
		}
		return true;
	}

	public static void clear(String seedName) {
		download_big_file_list.get(seedName).clear();
	}

	public static List<DownloadFile> get(String seedName) {
		return download_big_file_list.get(seedName);
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public String getSeedName() {
		return seedName;
	}

	public void setSeedName(String seedName) {
		this.seedName = seedName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}

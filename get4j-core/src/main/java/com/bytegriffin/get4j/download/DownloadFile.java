package com.bytegriffin.get4j.download;

import java.util.List;
import java.util.Map;

import com.bytegriffin.get4j.net.http.OkHttpClientEngine;
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

	/**
	 * 下载大文件
	 * @param seedName String
	 */
	public static void downloadBigFile(String seedName) {
		if (isExist(seedName)) {
			List<DownloadFile> downlist = download_big_file_list.get(seedName);
			for (DownloadFile file : downlist) {
				OkHttpClientEngine.downloadBigFile(seedName, file.getUrl(), file.getContentLength());
			}
			download_big_file_list.get(seedName).clear();
		}
	}

	public String getFileName() {
		return fileName;
	}

	public DownloadFile setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public byte[] getContent() {
		return content;
	}

	public DownloadFile setContent(byte[] content) {
		this.content = content;
		return this;
	}

	public long getContentLength() {
		return contentLength;
	}

	public DownloadFile setContentLength(long contentLength) {
		this.contentLength = contentLength;
		return this;
	}

	public DownloadFile setSeedName(String seedName) {
		this.seedName = seedName;
		return this;
	}

	public String getSeedName() {
		return seedName;
	}

	public String getUrl() {
		return url;
	}

	public DownloadFile setUrl(String url) {
		this.url = url;
		return this;
	}

}

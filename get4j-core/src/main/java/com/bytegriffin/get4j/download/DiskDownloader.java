package com.bytegriffin.get4j.download;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.PageMode;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpClientEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;
import com.bytegriffin.get4j.net.sync.BatchScheduler;
import com.bytegriffin.get4j.util.FileUtil;
import com.google.common.base.Strings;

/**
 * 磁盘下载器，负责下载页面以及页面上的资源文件，它的功能是避免了开发者在PageParser中手工写下载页面或资源文件的代码。<br>
 * 注意：<br>
 * 当启用List_detail模式并且detail页面存在的情况下，它特指的是avatar资源，即：与Detail_Link一一对应的资源，
 * 而且fetch.resource.selector配置的是包含detail_link与avatar的css选择器或正则表达式，
 * 如果在List_Detail模式下有特殊的资源下载方式，可以重新创建一种新的downloader或者 在PageParser中自己实现下载代码。<br>
 */
public class DiskDownloader implements Process {

    private static final Logger logger = LogManager.getLogger(DiskDownloader.class);
    // 静态资源服务器地址 如：http://static.site.com/
    private static String staticServer = "";
    // 默认avatar存储路径，当配置静态服务器时系统会自动调用它暂存图片
    private static String defaultAvatarPath = "";

    // 创建每个site的文件夹
    public void init(Seed seed) {
        String diskpath = seed.getDownloadDisk();
        String folderName;
        if (UrlAnalyzer.isStartHttpUrl(diskpath)) {
            defaultAvatarPath = DefaultConfig.getDownloadDisk(seed.getSeedName());
            staticServer = diskpath.endsWith("/") ? diskpath : diskpath + "/";// 静态资源服务器地址
            folderName = FileUtil.makeDiskDir(defaultAvatarPath);// 获取默认的服务器磁盘地址
        } else {
            if (DefaultConfig.default_value.equalsIgnoreCase(diskpath)) {
                diskpath = DefaultConfig.getDownloadDisk(seed.getSeedName());
            } else if (diskpath.contains(File.separator) || diskpath.contains(":")) {
                if (!diskpath.contains(seed.getSeedName())) {
                    diskpath = diskpath + File.separator + seed.getSeedName();
                }
            } else {
                logger.error("下载文件夹[" + diskpath + "]配置出错，请重新检查。");
                System.exit(1);
            }
            folderName = FileUtil.makeDiskDir(diskpath);// 获取用户配置的磁盘地址
        }
        Globals.DOWNLOAD_DISK_DIR_CACHE.put(seed.getSeedName(), folderName);
        logger.info("种子[" + seed.getSeedName() + "]的组件DiskDownloader的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        // 1.在磁盘上生成页面
        PageMode fm = Globals.FETCH_PAGE_MODE_CACHE.get(page.getSeedName());
        if (!PageMode.list_detail.equals(fm)) {// 当启动list_detail模式，默认不会下载页面的
            FileUtil.downloadPagesToDisk(page);
        }

        // 2.下载页面中的资源文件
        String folderName =  Globals.DOWNLOAD_DISK_DIR_CACHE.get(page.getSeedName());
        List<DownloadFile> list = HttpClientEngine.downloadResources(page, folderName);
        if(list != null && !list.isEmpty()) {
        	list.stream().filter(Objects::nonNull).forEach(file -> FileUtil.writeFileToDisk(file.getFileName(), file.getContent()));
        }
        //下载资源文件中的大文件
        downloadBigFile(page.getSeedName());

        // 3.判断是否包含avatar资源，有的话就下载
        if (!Strings.isNullOrEmpty(page.getAvatar())) {
        	DownloadFile downloadFile = HttpClientEngine.downloadAvatar(page, folderName);// 下载avatar资源
        	if(downloadFile != null){
        		FileUtil.writeFileToDisk(downloadFile.getFileName(), downloadFile.getContent());
        		 //下载大文件
        		downloadBigFile(page.getSeedName());
        		// 另开一个线程专门负责启用脚本同步avatar资源文件
                if (DefaultConfig.sync_open) {
                    BatchScheduler.addResource(page.getSeedName(), page.getAvatar());
                }
                String avatar = page.getAvatar();
                if (!Strings.isNullOrEmpty(staticServer)) {
                    avatar = avatar.replace(defaultAvatarPath, staticServer + page.getSeedName() + "/");
                    page.setAvatar(avatar);// 将本地avatar资源文件的路径修改为静态服务器地址
                }
        	}
        }

        // 4.设置page的资源保存路径属性
        page.setResourceSavePath(Globals.DOWNLOAD_DISK_DIR_CACHE.get(page.getSeedName()));
        logger.info("线程[" + Thread.currentThread().getName() + "]下载种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }
    
	/**
	 * 下载大文件
	 * @param seedName
	 */
	private void downloadBigFile(String seedName) {
		if (DownloadFile.isExist(seedName)) {
			List<DownloadFile> downlist = DownloadFile.get(seedName);
			for (DownloadFile file : downlist) {
				HttpClientEngine.downloadBigFile(seedName, file.getUrl(), file.getContentLength());
			}
			DownloadFile.clear(seedName);
		}
	}

}

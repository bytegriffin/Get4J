package com.bytegriffin.get4j.store;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.HttpEngine;
import com.bytegriffin.get4j.net.http.HttpProxy;
import com.bytegriffin.get4j.parse.FreeProxyPageParser;
import com.bytegriffin.get4j.util.FileUtil;

/**
 * 免费代理存储器：将免费代理保存到http_proxy文件中
 */
public class FreeProxyStorage implements Process {

    private static final Logger logger = LogManager.getLogger(FreeProxyStorage.class);

    private static String http_proxy = "";

    @Override
    public void init(Seed seed) {
    	http_proxy = seed.getStoreFreeProxy();
        FileUtil.makeDiskFile(http_proxy);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(Page page) {
        String http_proxy_file = FileUtil.getSystemAbsolutePath(http_proxy);
        List<HttpProxy> proxys = (List<HttpProxy>) page.getField(FreeProxyPageParser.xicidaili);

        // 1.验证抓取的代理
        HashSet<String> validates = new HashSet<>();
        HttpEngine http = Globals.HTTP_ENGINE_CACHE.get(page.getSeedName());
        for (HttpProxy proxy : proxys) {
            boolean isReached = http.testHttpProxy(page.getUrl(), proxy);
            if (isReached) {
                validates.add(proxy.toString());
            }
        }

        // 2.验证http_proxy文件中存在的代理
        List<HttpProxy> existProxys = FileUtil.readHttpProxyFile(http_proxy);
        if (existProxys == null || existProxys.size() == 0) {
            return;
        }
        Iterator<HttpProxy> it = existProxys.iterator();
        while (it.hasNext()) {
            HttpProxy proxy = it.next();
            if (existProxys.contains(proxy)) {
                logger.error("http_proxy文件中的代理[" + proxy.toString() + "]已存在，无需添加。");
                continue;
            }
            boolean isReached = http.testHttpProxy(page.getUrl(), proxy);
            if (isReached) {
                validates.add(proxy.toString());
            } else {
                FileUtil.removeLine(http_proxy, proxy.toString());
                logger.error("http_proxy文件中的代理[" + proxy.toString() + "]已失效。");
            }
        }

        // 3.将可用的代理追加到文件末尾
        int count = validates.size();
        FileUtil.append(http_proxy_file, validates);
        if (count == 0) {
            logger.error("更新http代理失败，请隔一段时间再更新。");
        } else {
            logger.info("更新http代理完成，总共具有[" + count + "]个成功的代理。");
        }
    }

}

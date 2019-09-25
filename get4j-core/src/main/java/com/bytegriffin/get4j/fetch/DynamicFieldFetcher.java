package com.bytegriffin.get4j.fetch;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;

/**
 * 自定义动态字段抓取器：为了避免在PageParse接口中写冗长的解析代码，<br>
 * 程序可通过配置动态地对页面上的数据一一解析成结构化字段数据。
 */
public class DynamicFieldFetcher implements Process{

	private static final Logger logger = LogManager.getLogger(DynamicFieldFetcher.class);

	@Override
	public void init(Seed seed) {
		logger.info("种子[" + seed.getSeedName() + "]的组件DynamicFieldsFetcher的初始化完成。");
	}

	@Override
	public void execute(Page page) {
		Map<String, String> fields = Globals.DYNAMIC_FIELDS_CACHE.get(page.getSeedName());
		for(String name : fields.keySet()){
			String text = UrlAnalyzer.selectPageText(page, fields.get(name));
			page.putField(name, text);
		}
        logger.info("线程[" + Thread.currentThread().getName() + "]解析种子[" + page.getSeedName() + "]url[" + page.getUrl() + "]中的动态字段完成。");
	}

}

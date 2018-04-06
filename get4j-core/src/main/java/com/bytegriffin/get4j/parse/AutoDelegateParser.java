package com.bytegriffin.get4j.parse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.conf.Seed;
import com.bytegriffin.get4j.core.Globals;
import com.bytegriffin.get4j.core.Page;
import com.bytegriffin.get4j.core.Process;

import com.google.common.base.Strings;

/**
 * 代理解析入口：负责代理所有内置以及自定义的解析类
 */
public class AutoDelegateParser implements Process {

    private static final Logger logger = LogManager.getLogger(AutoDelegateParser.class);

    @Override
    public void init(Seed seed) {
        PageParser pp = null;
        String clazz = seed.getParseClassImpl();
        if (!Strings.isNullOrEmpty(seed.getParseElementSelector())) {// new内置ElementSelectPageParser
            pp = new ElementPageParser(seed.getParseElementSelector());
        } else { // 自定义
            try {
                pp = (PageParser) Class.forName(clazz).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("种子[" + seed.getSeedName() + "]初始化页面解析类[" + clazz + "]时出现问题，", e);
                System.exit(1);
            }
        }
        Globals.PAGE_PARSER_CACHE.put(seed.getSeedName(), pp);
        logger.info("种子[" + seed.getSeedName() + "]的组件DelegateParser的初始化完成。");
    }

    @Override
    public void execute(Page page) {
        PageParser pp = Globals.PAGE_PARSER_CACHE.get(page.getSeedName());
        pp.parse(page);
        logger.info("线程[" + Thread.currentThread().getName() + "]解析种子[" + page.getSeedName() + "]的url[" + page.getUrl() + "]完成。");
    }

}

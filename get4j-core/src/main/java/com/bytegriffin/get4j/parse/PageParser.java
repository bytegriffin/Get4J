package com.bytegriffin.get4j.parse;

import com.bytegriffin.get4j.core.Page;

/**
 * 自定义页面解析类必须要实现的接口，否则程序会找不到解析类
 */
public interface PageParser {

    void parse(Page page);

}

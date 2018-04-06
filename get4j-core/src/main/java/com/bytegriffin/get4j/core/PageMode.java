package com.bytegriffin.get4j.core;

/**
 * 页面模型<br>
 * 包括四种：list_detail（列表-详情页面格式）、single（单个页面）、cascade（单页面上关联的所有链接）、site整站（不包括外链）<br>
 * 根据页面模型的不同，程序会自动启动不同的抓取器
 */
public enum PageMode {

    list_detail("list_detail"), single("single"), cascade("cascade"), site("site");

    private final String value;

    PageMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
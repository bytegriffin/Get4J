package com.bytegriffin.get4j.net.http;

import java.util.List;

/**
 * 随机获取Http Proxy，更好地模拟人为操作
 */
public class HttpProxySelector extends RandomSelector<HttpProxy> {


    private List<HttpProxy> queue;

    @Override
    protected List<HttpProxy> getQueue() {
        return queue;
    }


    @Override
    public void setQueue(List<HttpProxy> queue) {
        this.queue = queue;
    }

}

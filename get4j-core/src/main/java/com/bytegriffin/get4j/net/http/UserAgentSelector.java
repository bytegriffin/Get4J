package com.bytegriffin.get4j.net.http;

import java.util.List;

/**
 * 随机获取User Agent，更好地模拟人为操作
 */
public class UserAgentSelector extends RandomSelector<String> {

    private List<String> queue;

    @Override
    protected List<String> getQueue() {
        return queue;
    }

    @Override
    public void setQueue(List<String> queue) {
        this.queue = queue;
    }

}

package com.bytegriffin.get4j.net.http;

import java.util.List;

/**
 * 每次请求的时间间隔随机选择器
 */
public class SleepRandomSelector extends RandomSelector<Integer> {

    private List<Integer> queue;

    @Override
    protected List<Integer> getQueue() {
        return queue;
    }

    @Override
    public void setQueue(List<Integer> queue) {
        this.queue = queue;
    }

}

package com.bytegriffin.get4j.net.http;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 随机选择器<br>
 * 为User Agent，Http Proxy和SleepRange提供一种随机选择
 */
public abstract class RandomSelector<E> {

    private Random random = new Random();

    private static final ReentrantLock lock = new ReentrantLock(true);

    // 设置queue
    public abstract void setQueue(List<E> queue);

    // 获取queue
    protected abstract List<E> getQueue();


    /**
     * 随机选择一个list中的数据
     *
     * @return E
     */
    protected E choice() {
        lock.lock();
        try {
            return getQueue().get(random.nextInt(getQueue().size()));
        } finally {
            lock.unlock();
        }

    }

}

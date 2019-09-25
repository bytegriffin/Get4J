package com.bytegriffin.get4j.util;

import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.Lists;

public class ConcurrentQueue<E> implements Queue<E>{

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

	private final Lock readLock = readWriteLock.readLock();

	private final Lock writeLock = readWriteLock.writeLock();

	private final LinkedList<E> list = Lists.newLinkedList();

    @Override
    public void add(E e) {
        writeLock.lock();
        try {
            if (!contains(e)) {
                list.add(e);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E get(int index) {
        readLock.lock();
        try {
            return list.get(index);
        } finally {
            readLock.unlock();
        }
    }
    
	@Override
	public E get(String queueName, int index) {
		readLock.lock();
        try {
            return list.get(index);
        } finally {
            readLock.unlock();
        }
	}

    @Override
    public long size() {
        readLock.lock();
        try {
            return list.size();
        } finally {
            readLock.unlock();
        }
    }
    
	@Override
	public long size(String queueName) {
		return size() ;
	}

    @Override
    public void clear() {
        writeLock.lock();
        try {
            list.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        writeLock.lock();
        try {
            return list.isEmpty();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E outFirst() {
        writeLock.lock();
        try {
            if (!list.isEmpty()) {
                return list.removeFirst();
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

	@Override
	public E outFirst(String queueName) {
		return null;
	}

    @Override
    public boolean contains(E e) {
        return list.contains(e);
    }

	@Override
	public boolean contains(String queueName, E e) {
		return list.contains(e);
	}

	@Override
	public Queue<E> getQueue(String queueName) {
		return null;
	}

	@Override
	public void add(String key, E e) {
		add(e);
	}

	@Override
	public LinkedList<E> getAll() {
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<E> getAll(String queueName) {
		return (Set<E>) list;
	}

	@Override
	public void clear(String key) {
		 clear();
	}

	@Override
	public boolean isEmpty(String queueName) {
		return isEmpty();
	}


}

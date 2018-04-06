package com.bytegriffin.get4j.core;

import java.util.List;

import com.bytegriffin.get4j.store.FailUrlStorage;

public abstract class Initializer {

	public abstract void init();

	/**
	 * 加载本项目下初始化类
	 */
	private  static void loads(){
		 // 添加坏链接存储功能
		new FailUrlStorage().init();
	}

	/**
	 * 加载本地或外部所有项目的初始化类
	 * @param initializers 初始化类列表
	 */
	public  static void loads(List<Initializer> initializers){
		loads();
		if(initializers == null || initializers.size() == 0){
			return;
		}
		for(Initializer init : initializers){
			if(init == null){
				continue;
			}
			init.init();
		}
	}

}

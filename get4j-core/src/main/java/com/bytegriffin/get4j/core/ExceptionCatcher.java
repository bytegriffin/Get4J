package com.bytegriffin.get4j.core;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.util.Throwables;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * 异常捕捉
 */
public final class ExceptionCatcher {

    /**
     * 与种子抓取相关的异常信息
     * 当 key：seed_name value：exception information
     */
    private static Map<String, List<String>> seed_exception_info = Maps.newHashMap();

    /**
     * 其他类型的异常信息
     */
    private static List<String> exception_infos = Lists.newArrayList();

    public static List<String> getAllExceptions(){
    	return exception_infos;
    }

    public static List<String> getExceptions(String seedName){
    	return seed_exception_info.get(seedName);
    }
    
    /**
     * 每次抓取完成后都要清空一次异常缓存
     */
    public static void clearExceptions(){
    	seed_exception_info.clear();
    }

	/**
	 * 获取完整的堆栈信息
	 * @param t Throwable
	 * @return String
	 */
	public static String getStackTrace(Throwable t) {
        List<String> ls = Throwables.toStringList(t);
        return Joiner.on(System.lineSeparator()).join(ls);  
    }

	/**
	 * 增加异常信息 调用入口一
	 * @param seedName 种子名称
	 * @param t Throwable
	 */
    public static void addException(String seedName, Throwable t){
    	String exception = getStackTrace(t);
    	addException(seedName, exception);
    }

    /**
     * 增加异常信息 调用入口二
     * @param seedName 种子名称
     * @param exception 异常
     */
    public static void addException(String seedName, String exception){
    	List<String> list = seed_exception_info.get(seedName);
        if (list == null || list.size() == 0) {
        	list = Lists.newArrayList();
        	seed_exception_info.put(seedName, list);
        }
        if(!list.contains(exception)){
        	list.add(exception);
        }
    }

    /**
     * 增加异常信息 调用入口三
     * @param t Throwable
     */
    public static void addException(Throwable t){
    	String exception = getStackTrace(t);
    	addException(exception);
    }

    /**
     * 增加异常信息 调用入口四
     * @param exception 异常信息
     */
    public static void addException(String exception){
    	if(!exception_infos.contains(exception)){
    		exception_infos.add(exception);
    	}
    }
}

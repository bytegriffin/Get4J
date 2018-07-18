package com.bytegriffin.get4j.net.http.interceptor;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class LoggingInterceptor implements Interceptor {

	private static final Logger logger = LogManager.getLogger(LoggingInterceptor.class);
	private static boolean is_open = false;

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();
		long startTime = System.nanoTime();
		if(is_open) {
	        logger.info(String.format("发送请求 [%s] on %s%n%s",request.url(), chain.connection(), request.headers()));
		}
        Response response =  chain.proceed(request);
        if(is_open) {
            long endTime = System.nanoTime();
            logger.info(String.format("接收响应 [%s] in %.1fms%n%s",response.request().url(), 
            		(endTime - startTime) / 1e6d, response.headers()));

        }
        return response;
	}

}

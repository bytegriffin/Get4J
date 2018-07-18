package com.bytegriffin.get4j.net.http.interceptor;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
 
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.util.Sleep;
 
/**
 * OKHttp请求重试
 */
public class RetryInterceptor implements Interceptor {

	private static final Logger logger = LogManager.getLogger(RetryInterceptor.class);

    // 最大重试次数
    private final static int max_retry_count = 3;

    //重试的时间间隔 单位：秒
    private final static long next_interval = 1;

	@Override @SuppressWarnings("resource") 
	public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = doRequest(chain, request);
        int retryNum = 0;
        while ((response == null || (!response.isSuccessful() && !response.isRedirect())) && 
        		retryNum < max_retry_count) {
        	logger.warn("链接[{}]第[{}]次请求重试，请等待[{}]秒",request.url(), retryNum,next_interval);
            Sleep.seconds(1);
            response = doRequest(chain, request);
            retryNum++;
        }
        return response;
    }

    private Response doRequest(Chain chain, Request request) {
        Response response = null;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return response;
    }
 
}

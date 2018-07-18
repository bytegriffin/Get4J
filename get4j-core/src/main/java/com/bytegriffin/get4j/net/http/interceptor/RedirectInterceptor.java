package com.bytegriffin.get4j.net.http.interceptor;

import java.io.IOException;

import com.bytegriffin.get4j.net.http.AbstractHttpEngine;
import com.bytegriffin.get4j.net.http.UrlAnalyzer;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 自定义跳转拦截器<br >
 * 处理当发生3XX错误时，自动关闭response.body并且重新定义一个新的Response
 * 注意：Interceptor里的参数传递只能作用在url参数或者form表单中处理，
 * 		如果需要设置page等方法参数会比较麻烦，因此放弃处理这些业务逻辑方法
 */
public class RedirectInterceptor implements Interceptor {
	
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        String newUrl = response.header("Location");
        //有可能跳转的Url是相对路径，例如 /abc/123.html
		newUrl = UrlAnalyzer.custom(null).getAbsoluteURL(request.url().toString(), newUrl);
        if (response.isRedirect()) {
			response.body().close();
			response = null;
			Request newRequest = AbstractHttpEngine.setUrlAndHeader(request.newBuilder(), newUrl).build();
			response = chain.proceed(newRequest);
        }
        return response;
    }
    
}
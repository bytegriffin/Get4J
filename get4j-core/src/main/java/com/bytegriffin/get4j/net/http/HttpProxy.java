package com.bytegriffin.get4j.net.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import javax.annotation.Nullable;

import com.google.common.base.Strings;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Http代理
 */
public class HttpProxy {

    private String ip;
    private Integer port;
    private @Nullable String username;
    private @Nullable String password;
    private Proxy proxy;
    private Authenticator proxyAuthenticator;

    /**
     * 有的Http服务器配置了客户端认证，否则会在Http Header中显示 407 proxy unauthorized
     * 例如：可以在Http Header的请求中配置“Authorization: Basic jdhaHY0=”
     * 的参数，表示登录用户名/密码
     * @return
     */
    private Authenticator newAuthenticator(String username, String password) {
    	return new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder()
                        .header("Authorization", credential).build();
            }
        };
    }

    private void newProxy() {
    	this.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(this.ip, this.port));
    }

    private void newProxyAuthenticate() {
    	this.proxyAuthenticator = newAuthenticator(this.username, this.password);
    }

    /**
     * 默认端口号为80
     * @param ip String
     */
    public HttpProxy(String ip) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        this.port = 80;
        newProxy();
    }

    public HttpProxy(String ip, Integer port) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (port != null) {
            this.port = port;
        }
        newProxy();
    }

    public HttpProxy(String ip, Integer port, String username, String password) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (port != null) {
            this.port = port;
        }
        this.username = username.trim();
        if (Strings.isNullOrEmpty(password)) {
            this.password = "";
        } else {
            this.password = password.trim();
        }
        newProxy();
        newProxyAuthenticate();
    }

    public String toString() {
        String str = this.ip + ":" + (this.port==null ? 80 : this.port);
        if (!Strings.isNullOrEmpty(this.username)) {
            str += "@" + this.username;
        }
        if (!Strings.isNullOrEmpty(this.password)) {
            str += ":" + this.password;
        }
        return str;
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HttpProxy p = (HttpProxy) obj;
        if (!this.ip.equals(p.ip)) {
            return false;
        }
        if (!this.port.equals(p.port)) {
            return false;
        }
        return true;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Proxy getProxy() {
        return proxy;
    }

	public Authenticator getProxyAuthenticator() {
		return proxyAuthenticator;
	}

}

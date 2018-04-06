package com.bytegriffin.get4j.net.http;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import com.google.common.base.Strings;

/**
 * Http代理
 */
public class HttpProxy {

    private String ip;
    private String port;
    private String username;
    private String password;
    private HttpHost httpHost;
    private CredentialsProvider credsProvider;

    public HttpProxy(String ip) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        this.port = "80";
        this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
    }

    public HttpProxy(String ip, String port) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (!Strings.isNullOrEmpty(port)) {
            this.port = port.trim();
        }
        this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
    }

    public HttpProxy(String ip, Integer port) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (port != null) {
            this.port = String.valueOf(port);
        }
        this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
    }

    public HttpProxy(String ip, String port, String schema) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (!Strings.isNullOrEmpty(port)) {
            this.port = port.trim();
        }
        this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port), schema);
    }

    public HttpProxy(String ip, String port, String username, String password) {
        if (!Strings.isNullOrEmpty(ip)) {
            this.ip = ip.trim();
        }
        if (!Strings.isNullOrEmpty(port)) {
            this.port = port.trim();
        }
        this.username = username.trim();
        if (Strings.isNullOrEmpty(password)) {
            this.password = "";
        } else {
            this.password = password.trim();
        }
        this.httpHost = new HttpHost(this.ip, Integer.valueOf(this.port));
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(this.ip, Integer.valueOf(this.port)), new UsernamePasswordCredentials(this.username, this.password));
        this.credsProvider = credsProvider;
    }

    public String toString() {
        String str = this.ip + ":" + this.port;
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

    public String getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public HttpHost getHttpHost() {
        return httpHost;
    }

    public CredentialsProvider getCredsProvider() {
        return credsProvider;
    }

}

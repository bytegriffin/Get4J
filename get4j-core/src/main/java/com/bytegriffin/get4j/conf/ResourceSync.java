package com.bytegriffin.get4j.conf;

import java.util.Map;

/**
 * resource-sync.yaml资源同步配置文件
 */
public class ResourceSync {

    private Map<String, String> sync;
    private Map<String, String> rsync;
    private Map<String, String> ftp;
    private Map<String, String> scp;

    public Map<String, String> getSync() {
        return sync;
    }

    public void setSync(Map<String, String> sync) {
        this.sync = sync;
    }

    public Map<String, String> getRsync() {
        return rsync;
    }

    public void setRsync(Map<String, String> rsync) {
        this.rsync = rsync;
    }

    public Map<String, String> getFtp() {
        return ftp;
    }

    public void setFtp(Map<String, String> ftp) {
        this.ftp = ftp;
    }

    public Map<String, String> getScp() {
        return scp;
    }

    public void setScp(Map<String, String> scp) {
        this.scp = scp;
    }


}

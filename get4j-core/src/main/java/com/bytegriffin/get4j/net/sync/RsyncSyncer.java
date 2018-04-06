package com.bytegriffin.get4j.net.sync;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.bytegriffin.get4j.util.CommandUtil;
import com.bytegriffin.get4j.util.Sleep;
import com.google.common.collect.Sets;

/**
 * Rsync同步器：用于同步下载的资源文件到资源服务器上，比如：avatar文件同步到图片服务器
 * 可以只配置单向复制就可以，以后本地的资源还可以删除。当启用module模式时，密码需要配置到服务器端；
 * 当启用dir模式时，需要配置ssh-keygen配置无密码通道。
 * 由于rsync本身支持增量复制，因此系统会直接同步目录，而不是一个一个文件
 * 目前只支持Unix，不支持windows
 */
public class RsyncSyncer implements Syncer {

    private String host;
    private String username;
    // module或者dir模式，如果是module模式，需要服务器端配置module模块，它是同步根目录，
    // 子目录名是seedname，密码需要在服务器端配置，如果是dir模式需要配置ssh-keygen无密码
    private String dirOrModule;
    private boolean isModule;
    private HashSet<String> commands = Sets.newHashSet();

    public RsyncSyncer(String host, String username, String dirOrModule, boolean isModule) {
        this.host = host;
        this.username = username;
        this.dirOrModule = dirOrModule;
        this.isModule = isModule;
    }

    public void setBatch(Set<String> avatars) {
        // 判断是否为Module，
        String suffix = isModule ? ":" + dirOrModule : dirOrModule;
        for (String resource : avatars) {
            String localdir = resource.substring(resource.indexOf(BatchScheduler.split) + 1, resource.lastIndexOf(File.separator));
            String command = "rsync -az " + localdir + " " + username + "@" + host + ":" + suffix;
            commands.add(command);
        }

    }

    @Override
    public void sync() {
        for (String command : commands) {
            CommandUtil.executeShell(command);
            //如果不同的seed太多，可以减慢同步速度
            Sleep.seconds(1);
        }

    }


}
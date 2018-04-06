package com.bytegriffin.get4j.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bytegriffin.get4j.core.ExceptionCatcher;
import com.bytegriffin.get4j.send.EmailSender;

public final class CommandUtil {

    private static final Logger logger = LogManager.getLogger(CommandUtil.class);

    public static void executeShell(String shellCommand) {
        try {
            String[] cmd = {"/bin/sh", "-c", shellCommand};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process p = pb.inheritIO().start();
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                logger.info("执行Shell命令[" + shellCommand + "]时发生异常：", e);
            }
            logger.info("Shell命令[" + shellCommand + "]执行完毕");
        } catch (Exception ioe) {
            logger.info("执行Shell命令[" + shellCommand + "]时发生异常：", ioe);
            EmailSender.sendMail(ioe);
            ExceptionCatcher.addException(ioe);
        }
    }


}
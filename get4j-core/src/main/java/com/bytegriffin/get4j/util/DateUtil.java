package com.bytegriffin.get4j.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DateUtil {

	private static final Logger logger = LogManager.getLogger(DateUtil.class);
    public static final String yyyyMMddHHmmss = "yyyy-MM-dd HH:mm:ss";

    public static Date strToDate(String str) {
    	DateTimeFormatter format = DateTimeFormatter.ofPattern(yyyyMMddHHmmss);
        try {
        	LocalDateTime ldt = LocalDateTime.parse(str, format);
        	//通过instant做中转换，转换成Date
            Instant instant =  ldt.atZone(ZoneId.systemDefault()).toInstant();
            return Date.from(instant);
        } catch (DateTimeParseException e) {
            logger.error("时间格式出错，正确格式为[yyyy-MM-dd HH:mm:ss]：", e);
            System.exit(1);
        }
        return null;
    }

    /*
     * 当前时间
     */
    public static String getCurrentDate() {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyyMMddHHmmss);
    	return LocalDateTime.now().format(formatter);
    }

    /**
     * 时间开销
     *
     * @param startTime String
     * @return String
     */
    public static String getCostDate(String startTime) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyyMMddHHmmss);
        LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);
        LocalDateTime endDateTime = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(startDateTime, endDateTime);
        return seconds+"秒";
    }

}

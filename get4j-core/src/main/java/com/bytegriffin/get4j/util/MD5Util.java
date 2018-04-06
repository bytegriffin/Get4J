package com.bytegriffin.get4j.util;

import java.security.MessageDigest;
import java.util.UUID;

import com.bytegriffin.get4j.conf.DefaultConfig;
import com.google.common.base.Strings;

/**
 * MD5生成器
 */
public class MD5Util {

    private static String[] chars = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
            "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8",
            "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0)
            n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return chars[d1] + chars[d2];
    }

    private static String byteArrayToSubHexString(byte[] bytes) {
        StringBuilder resultSb = new StringBuilder();
        for (byte b : bytes) {
            resultSb.append(byteToHexString(b));
        }
        return resultSb.toString().substring(0, 8);
    }

    /**
     * 8位短uuid
     * 只要salt一样，那么每次生成的值也一样
     *
     * @param salt 一般值为fetchUrl
     * @return String
     */
    public synchronized static String generateSeedName(String salt) {
        // 为了保证list-detail模式下输入不同页数也生成同一个seedName，所以需要对url进行截断
        if (salt.contains(DefaultConfig.fetch_list_url_left) && salt.contains(DefaultConfig.fetch_list_url_right)) {
            salt = salt.substring(0, salt.lastIndexOf(DefaultConfig.fetch_list_url_left));
        }
        java.security.MessageDigest md;
        String pwd = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] b = salt.getBytes("UTF-8");
            byte[] hash = md.digest(b);
            pwd = byteArrayToSubHexString(hash);
        } catch (Exception e) {
        }

        return pwd;
    }

    /**
     * 8位短uuid
     * 每次生成的值不一样
     *
     * @return String
     */
    public synchronized static String generateSeedName() {
        StringBuilder sb = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int x = Integer.parseInt(str, 16);
            sb.append(chars[x % 0x3E]);
        }
        return sb.toString();
    }

    /**
     * 用于生成数据库主键
     *
     * @return String
     */
    public synchronized static String uuid() {
        return UUID.randomUUID().toString();
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder resultSb = new StringBuilder();
        for (byte b : bytes) {
            resultSb.append(byteToHexString(b));
        }
        return resultSb.toString();
    }

    /**
     * 将普通页面内容转换成MD5，以便持久化
     *
     * @param salt String 
     * @return String
     */
    public synchronized static String convert(String salt) {
        if (Strings.isNullOrEmpty(salt)) {
            return null;
        }
        java.security.MessageDigest md;
        String pwd = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] b = salt.getBytes("UTF-8");
            byte[] hash = md.digest(b);
            pwd = byteArrayToHexString(hash);
        } catch (Exception e) {
        }

        return pwd;
    }

}

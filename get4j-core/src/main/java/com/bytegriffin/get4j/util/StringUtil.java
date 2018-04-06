package com.bytegriffin.get4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class StringUtil {

    /**
     * 是否是数字
     *
     * @param str String
     * @return boolean
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

}

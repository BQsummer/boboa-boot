package com.bqsummer.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final String COMMON_DATE_PATTERN = "yyyy-MM-dd";

    public static String getTodayDateStr(String format) {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(format));
    }

    public static String getTodayDateStr() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(COMMON_DATE_PATTERN));
    }
}

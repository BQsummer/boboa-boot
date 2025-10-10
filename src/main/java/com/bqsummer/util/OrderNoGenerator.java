package com.bqsummer.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNoGenerator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String newOrderNo(String prefix) {
        String ts = LocalDateTime.now().format(FMT);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return (prefix == null ? "" : prefix) + ts + rand;

    }
}


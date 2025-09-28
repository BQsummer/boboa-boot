package com.bqsummer.framework;

public class ThreadLocalPool {
    private static final ThreadLocal<String> user = new ThreadLocal<>();

    public static String getUser() {
        return user.get();
    }

    public static void setUser(String userName) {
        user.set(userName);
    }
}

package com.bqsummer.exception;

/**
 * 路由异常
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public class RoutingException extends RuntimeException {
    
    public RoutingException(String message) {
        super(message);
    }
    
    public RoutingException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.bqsummer.exception;

/**
 * 模型未找到异常
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public class ModelNotFoundException extends RuntimeException {
    
    public ModelNotFoundException(String message) {
        super(message);
    }
    
    public ModelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ModelNotFoundException(Long modelId) {
        super("模型未找到: ID=" + modelId);
    }
}

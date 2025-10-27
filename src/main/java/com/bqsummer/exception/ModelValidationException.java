package com.bqsummer.exception;

/**
 * 模型验证异常
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
public class ModelValidationException extends RuntimeException {
    
    public ModelValidationException(String message) {
        super(message);
    }
    
    public ModelValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

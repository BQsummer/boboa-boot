package com.bqsummer.service.prompt;

/**
 * 模板渲染异常
 *
 * @author Boboa Boot Team
 * @date 2025-11-27
 */
public class TemplateRenderException extends RuntimeException {

    /**
     * 错误发生的行号
     */
    private final int line;

    public TemplateRenderException(String message) {
        super(message);
        this.line = -1;
    }

    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
        this.line = -1;
    }

    public TemplateRenderException(String message, int line, Throwable cause) {
        super(message, cause);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}

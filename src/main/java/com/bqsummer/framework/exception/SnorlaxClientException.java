package com.bqsummer.framework.exception;

import lombok.Getter;

public class SnorlaxClientException extends RuntimeException {

    @Getter
    private int code;

    @Getter
    private String developMessage;

    public SnorlaxClientException(String message) {
        super(message);
    }

    public SnorlaxClientException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SnorlaxClientException(int code, String message, String developMessage) {
        super(message);
        this.code = code;
        this.developMessage = developMessage;
    }
}

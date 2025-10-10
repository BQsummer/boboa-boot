package com.bqsummer.framework.exception;

import lombok.Getter;

public class SnorlaxServerException extends RuntimeException {

    @Getter
    private int code;

    @Getter
    private String developMessage;

    public SnorlaxServerException(String message) {
        super(message);
    }

    public SnorlaxServerException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SnorlaxServerException(int code, String message, String developMessage) {
        super(message);
        this.code = code;
        this.developMessage = developMessage;
    }
}

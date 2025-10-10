package com.bqsummer.framework.log;

import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.logbook.Precorrelation;

import java.io.IOException;

public class CustomHttpLogWriter implements HttpLogWriter {
    private static final Logger log = LoggerFactory.getLogger(CustomHttpLogWriter.class);
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void write(Precorrelation precorrelation, String request) throws IOException {
        log.info(request.replaceAll("\n", " ").replaceAll("\r", " "));
    }

    @Override
    public void write(Correlation correlation, String response) throws IOException {
        log.info(response.replaceAll("\n", " ").replaceAll("\r", " "));
    }
}


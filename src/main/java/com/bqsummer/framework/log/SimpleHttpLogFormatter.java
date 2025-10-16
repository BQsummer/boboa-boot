package com.bqsummer.framework.log;

import org.zalando.logbook.*;

public class SimpleHttpLogFormatter implements HttpLogFormatter {

    private boolean isTextual(String contentType) {
        if (contentType == null) return false;
        String ct = contentType.toLowerCase();
        return ct.contains("json") || ct.contains("text") || ct.contains("xml") || ct.contains("x-www-form-urlencoded");
    }

    private String sanitize(String s) {
        if (s == null) return "";
        return s.replace('\n', ' ').replace('\r', ' ');
    }

    @Override
    public String format(Precorrelation precorrelation, HttpRequest request) {
        String ua = request.getHeaders().getFirst("User-Agent");
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        String ip = xff != null ? xff : request.getRemote();
        String ct = request.getHeaders().getFirst("Content-Type");
        StringBuilder sb = new StringBuilder();
//        sb.append("[correlation:").append(precorrelation.getId()).append("] ")
          sb.append(request.getMethod()).append(' ').append(request.getRequestUri())
          .append(" IP:").append(ip)
          .append(" UA:").append(ua);
        if (isTextual(ct)) {
            try {
                String body = request.getBodyAsString();
                if (body != null && !body.isEmpty()) {
                    sb.append(" reqBody=").append(sanitize(body));
                }
            } catch (Exception ignore) {
                // ignore body on error
            }
        }
        return sb.toString();
    }

    @Override
    public String format(Correlation correlation, HttpResponse response) {
        String ct = response.getHeaders().getFirst("Content-Type");
        StringBuilder sb = new StringBuilder();
//        sb.append("[correlation:").append(correlation.getId()).append("] ")
        sb.append("httpStatus=").append(response.getStatus());
        if (isTextual(ct)) {
            try {
                String body = response.getBodyAsString();
                if (body != null && !body.isEmpty()) {
                    sb.append(" respBody=").append(sanitize(body));
                }
            } catch (Exception ignore) {
                // ignore body on error
            }
        }
        return sb.toString();
    }
}

package com.bqsummer.framework.exception;

import com.bqsummer.common.vo.Response;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;

import static com.bqsummer.framework.exception.GlobalErrorResponseConstants.*;


@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ExceptionResolver {

    @ExceptionHandler(SnorlaxClientException.class)
    public ResponseEntity<Response<?>> handleClientException(SnorlaxClientException e) {
        log.warn("Client error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(SnorlaxServerException.class)
    public ResponseEntity<Response<?>> handleServerException(SnorlaxServerException e) {
        log.error("Server error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.error(e.getCode(), e.getMessage()));
    }

    // 常见 Servlet 层 4xx 异常 -> 400
    @ExceptionHandler({
            NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            MissingPathVariableException.class,
            MissingServletRequestParameterException.class,
            TypeMismatchException.class,
            HttpMessageNotReadableException.class,
            HttpMessageNotWritableException.class,
            BindException.class,
            MethodArgumentNotValidException.class,
            HttpMediaTypeNotAcceptableException.class,
            ServletRequestBindingException.class,
            ConversionNotSupportedException.class,
            MissingServletRequestPartException.class,
            ValidationException.class
    })
    public ResponseEntity<Response<?>> handleServletException(Exception e) {
        log.warn("Bad request: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.fail(COMMON_SERVER_ERROR_CODE, COMMON_SERVER_ERROR_MESSAGE, e.getMessage()));
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Response<?>> handleAsyncTimeout(AsyncRequestTimeoutException e) {
        log.info("Request timeout: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(Response.fail(COMMON_CLIENT_ERROR_CODE, REQUEST_ERROR_MESSAGE, "request timeout"));
    }

    // 认证失败 -> 401，并打印日志
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Response<?>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Response.fail(AUTHENTICATION_FAILED_ERROR_CODE, AUTHENTICATION_FAILED_ERROR_MESSAGE, e.getMessage()));
    }

    // 权限不足 -> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<?>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Response.fail(PERMISSION_DENIED_ERROR_CODE, PERMISSION_DENIED_ERROR_MESSAGE, e.getMessage()));
    }

    // 数据访问/SQL 异常 -> 500，并打印堆栈
    @ExceptionHandler({
            DataAccessException.class,
            PersistenceException.class,
            SQLException.class
    })
    public ResponseEntity<Response<?>> handleDataAccessException(Exception e) {
        String msg = e.getMessage();
        Throwable root = e.getCause();
        if (root != null && root.getMessage() != null) {
            msg = root.getMessage();
        }
        log.error("Database error: {}", msg, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(COMMON_SERVER_ERROR_CODE, COMMON_SERVER_ERROR_MESSAGE, msg));
    }

    // 兜底异常 -> 500，并打印堆栈
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Response<?>> handleAny(Throwable e) {
        log.error("Unhandled error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.fail(COMMON_SERVER_ERROR_CODE, COMMON_SERVER_ERROR_MESSAGE, e.getMessage()));
    }
}

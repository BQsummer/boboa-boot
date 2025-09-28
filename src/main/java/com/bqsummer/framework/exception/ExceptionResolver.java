package com.bqsummer.framework.exception;

import com.bqsummer.common.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
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

import java.util.HashMap;
import java.util.Map;

import static com.bqsummer.framework.exception.GlobalErrorResponseConstants.*;

@RestControllerAdvice
@Slf4j
public class ExceptionResolver {

    @ExceptionHandler(value = SnorlaxClientException.class)
    public Response handleClientException(SnorlaxClientException e) {
        log.error(e.getMessage(), e);
        return Response.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(value = SnorlaxServerException.class)
    public Response handleServerException(SnorlaxServerException e) {
        log.error(e.getMessage(), e);
        return Response.builder().errCode(e.getCode()).message(e.getMessage()).developerMessage(e.getDevelopMessage()).build();
    }

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
            AsyncRequestTimeoutException.class
    })
    public Response handleServletException(Exception e) {
        log.error(e.getMessage(), e);
        return Response.fail(COMMON_SERVER_ERROR_CODE, COMMON_SERVER_ERROR_MESSAGE, e.getMessage());
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public Response handleAuthenticationException(AuthenticationException e) {
        return Response.fail(AUTHENTICATION_FAILED_ERROR_CODE, AUTHENTICATION_FAILED_ERROR_MESSAGE, e.getMessage());
    }

    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Response handleAccessDeniedException(AccessDeniedException e) {
        return Response.fail(PERMISSION_DENIED_ERROR_CODE, PERMISSION_DENIED_ERROR_MESSAGE, e.getMessage());
    }
}
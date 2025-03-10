package com.ideafly.aop;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public R<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("http请求的方法不正确:【{}】", e.getMessage());
        return R.error(ErrorCode.PARAM_ERROR.getCode(),"http请求的方法不正确");
    }

    /**
     * 请求参数不全
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public R<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("请求参数不全:【{}】", e.getMessage());
        return R.error(ErrorCode.PARAM_ERROR.getCode(),"请求参数不全");
    }
    @ResponseBody
    @ExceptionHandler({ConstraintViolationException.class})
    public R<?> constraintViolationException(Exception e) {
        Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) e).getConstraintViolations();
        StringBuilder errorMsg = new StringBuilder();
        for (ConstraintViolation<?> error : violations) {
            errorMsg.append(error.getMessage()).append(",");
        }
        errorMsg.delete(errorMsg.length() - 1, errorMsg.length());
        return R.error(ErrorCode.PARAM_ERROR.getCode(), errorMsg.toString());
    }
    @ResponseBody
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public R<?> jsonFormatExceptionHandler(Exception e) {
        log.error("参数JSON格式错误,URL:{}", getCurrentRequestUrl(), e);
        return R.error(ErrorCode.PARAM_ERROR.getCode(), "参数JSON格式错误");
    }
    @ResponseBody
    @ExceptionHandler({BadSqlGrammarException.class})
    public R<?> badSqlGrammarException(Exception e) {
        log.error("执行sql异常,URL:{}", getCurrentRequestUrl(), e);
        return R.error(ErrorCode.EXECUTE_SQL_ERROR.getCode(), e.getMessage());
    }
    /**
     * json 格式错误 缺少请求体
     */
    @ResponseBody
    @ExceptionHandler({TypeMismatchException.class, BindException.class})
    public R<?> paramExceptionHandler(Exception e) {
        if (e instanceof BindException) {
            if (e instanceof MethodArgumentNotValidException) {
                List<FieldError> fieldErrors = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors();
                List<String> msgList = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
                return R.error(ErrorCode.PARAM_ERROR.getCode(), String.join(",", msgList));
            }
            List<FieldError> fieldErrors = ((BindException) e).getFieldErrors();
            List<String> error = fieldErrors.stream().map(field -> field.getField() + ":" + field.getRejectedValue()).collect(Collectors.toList());
            String errorMsg = ErrorCode.PARAM_ERROR.getMsg() + ":" + error;
            return R.error(ErrorCode.PARAM_ERROR.getCode(), errorMsg);
        }
        return R.error(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMsg());
    }

   /* *//**
     * 其他全部异常
     *
     * @param e 全局异常
     * @return 错误结果
     *//*
    @ResponseBody
    @ExceptionHandler(Throwable.class)
    public R<?> errorHandler(Throwable e) {
        log.error("捕获全局异常,URL:{}", getCurrentRequestUrl(), e);
        return R.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }*/
    @ResponseBody
    @ExceptionHandler(Throwable.class)
    public  R<?> errorHandler(Exception e) {
        log.error("捕获全局异常,URL:{}", getCurrentRequestUrl(), e);
        // 你可以根据不同的异常类型返回不同的 ApiResponse
        return R.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }
    /**
     * 获取当前请求url
     */
    private String getCurrentRequestUrl() {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (null == request) {
            return null;
        }
        ServletRequestAttributes servletRequest = (ServletRequestAttributes) request;
        return servletRequest.getRequest().getRequestURI();
    }
}

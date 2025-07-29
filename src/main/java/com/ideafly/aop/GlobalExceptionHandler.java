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
    // 常见错误的路径前缀，这些错误只记录简单信息，不记录堆栈跟踪
    private static final String[] COMMON_ERROR_PATHS = {
        "/api/jobs/list",
        "/api/users/get",
        "/api/common"
    };
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public R<?> httpRequestMethodNotSupportedExceptionHandler(HttpRequestMethodNotSupportedException e) {
        log.warn("http请求的方法不正确: {}", e.getMessage());
        return R.error(ErrorCode.PARAM_ERROR.getCode(),"http请求的方法不正确");
    }

    /**
     * 请求参数不全
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public R<?> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        log.warn("请求参数不全: {}", e.getMessage());
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
        log.warn("参数验证失败: {}", errorMsg);
        return R.error(ErrorCode.PARAM_ERROR.getCode(), errorMsg.toString());
    }
    
    @ResponseBody
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public R<?> jsonFormatExceptionHandler(HttpMessageNotReadableException e) {
        String url = getCurrentRequestUrl();
        boolean isCommonError = isCommonErrorPath(url);
        
        // 增强日志，记录更多异常信息
        String errorMessage = e.getMessage();
        Throwable rootCause = e.getRootCause();
        String rootCauseMessage = rootCause != null ? rootCause.getMessage() : "无根本原因";
        
        // 如果是 /user/follow/toggle 接口，则记录详细信息，帮助调试
        if (url.contains("/user/follow/toggle")) {
            log.error("关注接口参数JSON解析错误: URL={}, 异常消息={}, 根本原因={}", url, errorMessage, rootCauseMessage, e);
            
            try {
                // 尝试记录请求体内容
                RequestAttributes request = RequestContextHolder.getRequestAttributes();
                if (request != null) {
                    ServletRequestAttributes servletRequest = (ServletRequestAttributes) request;
                    String contentType = servletRequest.getRequest().getContentType();
                    String method = servletRequest.getRequest().getMethod();
                    log.error("关注接口请求详情: Method={}, ContentType={}", method, contentType);
                }
            } catch (Exception ex) {
                log.error("尝试记录请求详情时出错", ex);
            }
            
            return R.error(ErrorCode.PARAM_ERROR.getCode(), "关注用户参数格式错误: " + rootCauseMessage);
        }
        
        // 原来的日志处理逻辑
        if (isCommonError) {
            log.warn("参数JSON格式错误, URL: {}", url);
        } else {
            log.error("参数JSON格式错误, URL: {}, 异常: {}, 根本原因: {}", url, errorMessage, rootCauseMessage, e);
        }
        return R.error(ErrorCode.PARAM_ERROR.getCode(), "参数JSON格式错误");
    }
    
    @ResponseBody
    @ExceptionHandler({BadSqlGrammarException.class})
    public R<?> badSqlGrammarException(Exception e) {
        String url = getCurrentRequestUrl();
        // SQL错误总是记录详细信息，但可以简化消息
        log.error("SQL执行异常, URL: {}, 错误: {}", url, e.getMessage());
        return R.error(ErrorCode.EXECUTE_SQL_ERROR.getCode(), "数据库操作异常");
    }
    
    /**
     * 处理参数类型不匹配和参数绑定异常（如表单校验失败）
     */
    @ResponseBody // 返回值会自动序列化为JSON响应给前端
    @ExceptionHandler({TypeMismatchException.class, BindException.class}) // 捕获参数类型不匹配和参数绑定异常
    public R<?> paramExceptionHandler(Exception e) {
        // 如果异常是参数绑定异常（如表单校验失败）
        if (e instanceof BindException) {
            // 如果异常是方法参数校验失败（如 @Valid 注解校验失败）
            if (e instanceof MethodArgumentNotValidException) {
                // 获取所有字段的校验错误信息
                List<FieldError> fieldErrors = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors();
                // 提取每个字段的默认错误消息，组成一个字符串列表
                List<String> msgList = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
                // 将所有错误消息用逗号拼接成一个字符串
                String errorMessage = String.join(",", msgList);
                // 打印警告日志，内容为所有校验失败的提示
                log.warn("参数验证失败: {}", errorMessage);
                // 返回一个带有错误码和错误消息的响应对象
                return R.error(ErrorCode.PARAM_ERROR.getCode(), errorMessage);
            }
            List<FieldError> fieldErrors = ((BindException) e).getFieldErrors();
            List<String> error = fieldErrors.stream().map(field -> field.getField() + ":" + field.getRejectedValue()).collect(Collectors.toList());
            String errorMsg = "参数错误: " + String.join(", ", error);
            log.warn(errorMsg);
            return R.error(ErrorCode.PARAM_ERROR.getCode(), errorMsg);
        }
        log.warn("参数类型不匹配: {}", e.getMessage());
        return R.error(ErrorCode.PARAM_ERROR.getCode(), ErrorCode.PARAM_ERROR.getMsg());
    }

    /**
     * 全局异常处理
     */
    @ResponseBody
    @ExceptionHandler(Throwable.class)
    public R<?> errorHandler(Exception e) {
        String url = getCurrentRequestUrl();
        boolean isCommonError = isCommonErrorPath(url);
        
        if (isCommonError) {
            // 对于高频API的错误，只记录简单日志，不记录堆栈跟踪
            log.error("API错误, URL: {}, 异常类型: {}, 消息: {}", url, e.getClass().getSimpleName(), e.getMessage());
        } else {
            // 对于其他错误，记录完整堆栈跟踪
            log.error("系统异常, URL: {}", url, e);
        }
        
        return R.error(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMsg());
    }
    
    /**
     * 获取当前请求url
     */
    private String getCurrentRequestUrl() {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (null == request) {
            return "unknown";
        }
        ServletRequestAttributes servletRequest = (ServletRequestAttributes) request;
        return servletRequest.getRequest().getRequestURI();
    }
    
    /**
     * 判断是否为高频错误API路径
     */
    private boolean isCommonErrorPath(String url) {
        if (url == null) {
            return false;
        }
        for (String path : COMMON_ERROR_PATHS) {
            if (url.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}

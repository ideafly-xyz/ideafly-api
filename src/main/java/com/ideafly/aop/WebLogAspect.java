package com.ideafly.aop;

import cn.hutool.core.date.StopWatch;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Aspect
@Component
@Slf4j
public class WebLogAspect {
    // 静态资源路径模式
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(".*(css|js|png|ico|jpg|jpeg|gif|html|xml)$");
    
    // 不需要记录详细日志的URL路径
    private static final String[] IGNORE_LOGGING_PATHS = {
        "/api/jobs/list", // 频繁访问的列表接口
        "/swagger", 
        "/v3/api-docs",
        "/webjars",
        "/csrf"
    };
    
    // 不记录请求参数的接口
    private static final String[] NO_PARAM_LOGGING_PATHS = {
        "/api/comments/list",
        "/api/jobs/list"
    };
    
    // 不记录响应内容的接口
    private static final String[] NO_RESPONSE_LOGGING_PATHS = {
        "/api/jobs/list"
    };
    
    public WebLogAspect() {
    }

    @Pointcut("execution(* com.ideafly.controller.*Controller.*(..)) || execution(* com.ideafly.controller.h5.*Controller.*(..))")
    public void webLogPointcut() {
    }

    @Around("webLogPointcut()")
    public Object Around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求信息
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String uri = request.getRequestURI();
        
        // 忽略静态资源和swagger等不需要日志记录的URL
        if (RESOURCE_PATTERN.matcher(uri).matches() || shouldIgnoreLogging(uri)) {
            return joinPoint.proceed();
        }
        
        // 开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // 打印请求参数 (如果配置允许)
        String param = "";
        if (!shouldIgnoreParamLogging(uri)) {
            //如果参数有HttpRequest,ServletResponse，直接移除，不打印这些
            param = JSONUtil.toJsonStr(Stream.of(joinPoint.getArgs())
                .filter(args -> !(args instanceof ServletRequest))
                .filter(args -> !(args instanceof ServletResponse))
                .filter(args -> !(args instanceof MultipartFile))
                .collect(Collectors.toList()));
        }
        
        // 执行实际方法
        Object result = joinPoint.proceed();
        stopWatch.stop();
        long cost = stopWatch.getTotalTimeMillis();
        
        // 如果执行时间过长则记录警告日志
        if (cost > 500) { // 500ms阈值可调整
            log.warn("[SLOW API] [url:{}] - 执行耗时:{}ms", uri, cost);
        }
        
        // 记录日志 (如果不是高频API)
        if (!shouldIgnoreResponseLogging(uri)) {
            // 只在调试模式下记录详细请求和响应
            if (log.isDebugEnabled()) {
                log.debug("[API] [url:{}] [param:{}] [response:{}] [cost:{}ms]", 
                    uri, param, JSONUtil.toJsonStr(result), cost);
            } else {
                // 正常模式下只记录URL和耗时
                log.info("[API] [url:{}] [cost:{}ms]", uri, cost);
            }
        }
        
        return result;
    }
    
    // 判断是否忽略日志记录
    private boolean shouldIgnoreLogging(String uri) {
        return Arrays.stream(IGNORE_LOGGING_PATHS)
                .anyMatch(uri::startsWith);
    }
    
    // 判断是否忽略请求参数日志
    private boolean shouldIgnoreParamLogging(String uri) {
        return Arrays.stream(NO_PARAM_LOGGING_PATHS)
                .anyMatch(uri::startsWith);
    }
    
    // 判断是否忽略响应内容日志
    private boolean shouldIgnoreResponseLogging(String uri) {
        return Arrays.stream(NO_RESPONSE_LOGGING_PATHS)
                .anyMatch(uri::startsWith);
    }
}
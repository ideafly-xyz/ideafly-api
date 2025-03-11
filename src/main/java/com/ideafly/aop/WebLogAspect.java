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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Aspect
@Component
@Slf4j
public class WebLogAspect {
    public WebLogAspect() {
    }

    @Pointcut("execution(* com.ideafly.controller.*Controller.*(..)) || execution(* com.ideafly.controller.h5.*Controller.*(..))")
    public void webLogPointcut() {
    }

    @Around("webLogPointcut()")
    public Object Around(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String uri = request.getRequestURI();
        //如果参数有HttpRequest,ServletResponse，直接移除，不打印这些
        String param = JSONUtil.toJsonStr(Stream.of(joinPoint.getArgs())
                .filter(args -> !(args instanceof ServletRequest))
                .filter(args -> !(args instanceof ServletResponse))
                .collect(Collectors.toList()));
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object result = joinPoint.proceed();
        stopWatch.stop();
        long cost = stopWatch.getTotalTimeMillis();
        log.info("[requestUrl:{}][param:{}][response:{}][cost:{}ms][operator:{}]", uri, param, JSONUtil.toJsonStr(result), cost, "system");
        return result;
    }

    private Object getArgs(ProceedingJoinPoint pjp) {
        if (pjp == null) {
            return null;
        }
        return Stream.of(pjp.getArgs()).filter(a -> !(a instanceof HttpServletResponse)
                        && !(a instanceof HttpServletRequest) && !(a instanceof MultipartFile))
                .collect(Collectors.toList());
    }
}
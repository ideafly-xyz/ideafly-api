package com.ideafly.aop;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Component
public class TokenInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是HandlerMethod，直接放行 (例如：静态资源)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 检查方法上是否有 @NoAuth 注解
        NoAuth noAuthAnnotation = handlerMethod.getMethodAnnotation(NoAuth.class);

        // 如果有 @NoAuth 注解，直接放行，不需要Token验证
        if (Objects.nonNull(noAuthAnnotation)) {
            return true;
        }

        // 从请求头中获取 Authorization
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7); // 去除 "Bearer " 前缀
        }
        // 如果token为空或者无效，返回 401 Unauthorized
        if (token == null || !jwtUtil.isTokenValid(token, jwtUtil.extractPhoneNumber(token))) { //  更严谨的验证方式，同时验证token和phoneNumber
            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write(JSONUtil.toJsonStr(R.error(ErrorCode.NO_AUTH))); // 返回 JSON 错误信息
            return false; // 拦截请求
        }
        // Token 验证通过，放行请求
        return true;
    }
}
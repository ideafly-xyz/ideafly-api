package com.ideafly.aop;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UserDto;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
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
    @Autowired
    private UsersService usersService;

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
        try { // Add try-catch block
            if (token == null || !jwtUtil.isTokenValid(token, jwtUtil.extractPhoneNumber(token))) { //  更严谨的验证方式，同时验证token和phoneNumber
                response.setStatus(HttpStatus.OK.value());
                response.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSONUtil.toJsonStr(R.error(ErrorCode.NO_AUTH))); // 返回 JSON 错误信息
                return false; // 拦截请求
            }
            String phoneNumber = jwtUtil.extractPhoneNumber(token);
            UserDto userDto= BeanUtil.copyProperties(usersService.getUserByMobile(phoneNumber),UserDto.class);
            UserContextHolder.setUser(userDto); //  将 UserDTO 放入 ThreadLocal
            // Token 验证通过，放行请求
            return true;

        } catch (ExpiredJwtException e) { // Catch ExpiredJwtException specifically
            response.setStatus(HttpStatus.OK.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(R.error(ErrorCode.TOKEN_EXPIRED))); // Return specific TOKEN_EXPIRED error
            return false; // 拦截请求
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // 处理格式错误的JWT令牌
            response.setStatus(HttpStatus.OK.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(R.error(ErrorCode.INVALID_TOKEN)));
            return false;
        } catch (Exception e) {
            // 处理其他JWT验证异常
            response.setStatus(HttpStatus.OK.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(R.error(ErrorCode.NO_AUTH)));
            return false;
        }
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 【新增代码】 请求完成后，清除 ThreadLocal 中的用户信息，防止内存泄漏
        UserContextHolder.removeUser();
    }
}
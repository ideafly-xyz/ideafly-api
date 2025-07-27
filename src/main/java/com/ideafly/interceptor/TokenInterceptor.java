package com.ideafly.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.auth.LoginUser;
import com.ideafly.dto.user.UserDto;
import com.ideafly.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 静态资源路径正则
    private static final Pattern RESOURCE_PATTERN = Pattern.compile(".*\\.(js|css|ico|png|jpg|jpeg|gif|svg|woff|woff2|ttf|eot)$");
    
    // 不需要验证token的路径
    private static final String[] WHITE_LIST = {
            "/api/user/telegram",    // Telegram登录相关
            "/api/user/refreshToken", // Token刷新
            "/api/sms/sendSms",      // 短信验证码
            "/api/sms/login",        // 短信登录
            "/swagger",
            "/v2/api-docs",
            "/v3/api-docs",
            "/webjars/",
            "/doc.html"
    };
    
    // 允许匿名访问但尝试提取token的路径
    private static final String[] PUBLIC_PATHS = {
            "/api/jobs/list",         // 职位列表（公开，但尝试获取用户信息）
            "/api/comments/list",     // 评论列表游标分页（公开，允许未登录用户查看）
            "/api/comments/count",    // 评论数量（公开，允许未登录用户查看）
            "/api/comments/loadMoreChildren", // 加载更多子评论（公开，允许未登录用户查看）
            "/api/comments/childrenCount",    // 子评论数量（公开，允许未登录用户查看）
            "/api/user/profile/",     // 用户资料（公开，允许未登录用户查看其他用户资料）
            "/api/user/totalLikes",   // 用户总赞数（公开，允许未登录查看）
            "/api/user/followStats"   // 用户关注统计（公开，允许未登录查看）
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String url = request.getRequestURI();
        
        // 调试输出当前请求和拦截结果
        log.debug("TokenInterceptor处理URL: {}", url);
        
        // 快速判断是否为静态资源请求
        if (RESOURCE_PATTERN.matcher(url).matches()) {
            log.debug("静态资源请求，跳过认证: {}", url);
            return true;
        }
        
        // 检查是否为白名单URL
        if (isWhiteListUrl(url)) {
            log.debug("白名单URL，跳过认证: {}", url);
            return true;
        }

        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        
        // 检查是否为公开访问但尝试获取用户信息的路径
        boolean isPublicPath = isPublicPath(url);
        
        if (authHeader == null || authHeader.isEmpty()) {
            // 如果是OPTIONS请求，表示这是预检请求，直接放行
            if ("OPTIONS".equals(request.getMethod())) {
                return true;
            }
            
            // 如果是公开访问路径，则允许访问但不设置用户上下文
            if (isPublicPath) {
                log.debug("公开访问路径，允许匿名访问: {}", url);
                return true;
            }
            
            // 对于其他请求，返回未授权错误
            log.warn("缺少Authorization头，拒绝访问: {}", url);
            responseError(response, R.error(ErrorCode.INVALID_TOKEN.getCode(), "未登录，请先登录"));
            return false;
        }

        try {
            // 提取token，移除Bearer前缀和空格
            String token = extractToken(authHeader);
            log.debug("提取的token: {}", token);
            
            // 验证token并获取用户信息
            LoginUser loginUser = authService.getUserByToken(token);
            
            if (loginUser == null) {
                // token无效或过期
                log.warn("无效的token，拒绝访问: {}", url);
                
                // 如果是公开访问路径，仍允许访问
                if (isPublicPath) {
                    log.debug("公开访问路径，允许匿名访问: {}", url);
                    return true;
                }
                
                responseError(response, R.error(ErrorCode.INVALID_TOKEN.getCode(), "登录已过期，请重新登录"));
                return false;
            }
            
            // 将用户信息存入请求属性
            request.setAttribute("loginUser", loginUser);
            
            // 同时存入UserContextHolder，兼容现有代码
            UserDto userDto = new UserDto();
            BeanUtils.copyProperties(loginUser, userDto);
            UserContextHolder.setUser(userDto);
            
            log.debug("用户认证成功: {}, URL: {}", loginUser.getUsername(), url);
            return true;
        } catch (Exception e) {
            // 只记录简洁的错误信息，不打印堆栈
            log.warn("Token验证失败: URL={}, 错误={}", url, e.getMessage());
            
            // 如果是公开访问路径，仍允许访问
            if (isPublicPath) {
                log.debug("公开访问路径，允许匿名访问: {}", url);
                return true;
            }
            
            responseError(response, R.error(ErrorCode.INVALID_TOKEN.getCode(), "登录已过期，请重新登录"));
            return false;
        }
    }

    /**
     * 从Authorization头中提取token
     * 移除Bearer前缀和多余的空格
     */
    private String extractToken(String authHeader) {
        // 去除头尾空格
        String trimmed = authHeader.trim();
        
        // 检查并移除Bearer前缀
        if (trimmed.startsWith("Bearer ")) {
            return trimmed.substring(7).trim();
        } else if (trimmed.startsWith("bearer ")) {
            return trimmed.substring(7).trim();
        }
        
        // 没有前缀，直接返回
        return trimmed;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 无需操作
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理上下文
        request.removeAttribute("loginUser");
        UserContextHolder.removeUser();
    }

    /**
     * 检查URL是否在白名单中
     */
    private boolean isWhiteListUrl(String url) {
        return Arrays.stream(WHITE_LIST).anyMatch(url::startsWith);
    }
    
    /**
     * 检查URL是否在公开访问路径中
     */
    private boolean isPublicPath(String url) {
        return Arrays.stream(PUBLIC_PATHS).anyMatch(url::startsWith);
    }

    /**
     * 返回错误响应
     */
    private void responseError(HttpServletResponse response, R<?> result) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        
        try (PrintWriter out = response.getWriter()) {
            out.append(objectMapper.writeValueAsString(result));
        }
    }
}
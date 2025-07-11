package com.ideafly.config;

import com.ideafly.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private TokenInterceptor tokenInterceptor;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 拦截所有 /api/** 路径的请求
                .allowedOrigins("*") // 允许来自 http://localhost:6365 的跨域请求 (明确指定)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(
                        "/v3/api-docs/**", // 排除API文档
                        "/api/user/totalLikes", // 排除获取用户总点赞接口
                        "/api/user/followStats", // 排除获取用户关注统计接口
                        "/api/user/profile/**", // 排除用户资料接口
                        "/api/comments/loadMoreChildren", // 排除加载更多子评论接口
                        "/api/comments/childrenCount" // 排除子评论数量接口
                        // 可以添加其他不需要token验证的路径
                );
    }
}
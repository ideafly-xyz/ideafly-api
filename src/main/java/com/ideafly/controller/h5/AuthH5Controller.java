package com.ideafly.controller.h5;

import cn.hutool.core.bean.BeanUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.dto.LoginSuccessOutputDto;
import com.ideafly.dto.user.UserDto;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("api/auth")
@Slf4j
public class AuthH5Controller {
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private UsersService usersService;

    @NoAuth
    @PostMapping("/refreshToken")
    public R<LoginSuccessOutputDto> refreshToken(@RequestHeader("Authorization") String authorizationHeader) {
        log.info("收到refreshToken请求: {}", authorizationHeader.startsWith("Bearer ") ? "Bearer开头" : authorizationHeader);
        
        String refreshToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            refreshToken = authorizationHeader.substring(7); // 去除 "Bearer " 前缀
            log.info("提取的refreshToken长度: {}", refreshToken.length());
        }
        
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("refreshToken为空");
            return R.error(ErrorCode.TOKEN_NULL);
        }
        
        try {
            String phoneNumber = jwtUtil.extractPhoneNumberIgnoreExpired(refreshToken);
            log.info("从refreshToken提取的phoneNumber: {}", phoneNumber);
            
            // 验证refreshToken是否有效 (包含过期验证)
            if (!jwtUtil.isRefreshTokenValid(refreshToken, phoneNumber)) { 
                log.warn("refreshToken无效或已过期: {}", refreshToken.substring(0, Math.min(20, refreshToken.length())));
                return R.error(ErrorCode.TOKEN_EXPIRED);
            }
            
            log.info("refreshToken有效，正在生成新的token对");
            
            // 验证通过，颁发新的accessToken和refreshToken
            LoginSuccessOutputDto outputDto = new LoginSuccessOutputDto();
            
            // 生成新的accessToken
            String newAccessToken = jwtUtil.generateToken(phoneNumber);
            outputDto.setAccessToken(newAccessToken);
            
            // 生成新的refreshToken
            String newRefreshToken = jwtUtil.generateRefreshToken(phoneNumber);
            outputDto.setRefreshToken(newRefreshToken);
            
            // 将旧的refreshToken加入黑名单
            jwtUtil.invalidateRefreshToken(refreshToken);
            log.info("旧refreshToken已加入黑名单，生成新的accessToken和refreshToken");
            
            // 获取用户信息
            outputDto.setUserInfo(BeanUtil.copyProperties(
                usersService.getOrAddByMobile(phoneNumber), 
                UserDto.class
            ));
            
            return R.success(outputDto);
        } catch (Exception e) {
            log.error("刷新令牌过程中发生异常", e);
            return R.error(ErrorCode.SYSTEM_ERROR.getCode(), "刷新令牌失败: " + e.getMessage());
        }
    }
}

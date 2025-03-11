package com.ideafly.controller.h5;

import cn.hutool.core.bean.BeanUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.ErrorCode;
import com.ideafly.common.R;
import com.ideafly.dto.LoginSuccessOutputDto;
import com.ideafly.dto.user.UserDto;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("api/auth")
public class AuthController {
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private UsersService usersService;

    @NoAuth
    @PostMapping("/refreshToken")
    public R<LoginSuccessOutputDto> refreshToken(@RequestHeader("Authorization") String authorizationHeader) { // 从 Authorization 请求头中获取 refreshToken
        String refreshToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            refreshToken = authorizationHeader.substring(7); // 去除 "Bearer " 前缀
        }
        if (refreshToken == null) {
            return R.error(ErrorCode.TOKEN_NULL);
        }
        String phoneNumber = jwtUtil.extractPhoneNumber(refreshToken); //  从 refreshToken 中提取手机号
        //  !!!  重要安全校验  !!!  在实际生产环境中，应该对 refreshToken 进行更严格的验证：
        //  1.  验证 refreshToken 是否过期 (JwtUtil.isTokenValid 已经包含了过期验证)
        //  2.  验证 refreshToken 的类型是否为 "refresh" (可以在 payload 中添加 "type": "refresh" 声明)
        //  3.  可以考虑将 refreshToken 存储在数据库或 Redis 中，验证 refreshToken 是否存在于有效列表中，防止 refreshToken 被伪造或泄露后被滥用
        if (!jwtUtil.isTokenValid(refreshToken, phoneNumber)) { //  验证 refreshToken 是否有效 (包含过期验证)
            return R.error(ErrorCode.TOKEN_EXPIRED);
        }
        //  验证通过，颁发新的 accessToken 和 refreshToken
        LoginSuccessOutputDto outputDto = new LoginSuccessOutputDto();
        outputDto.setAccessToken(jwtUtil.generateToken(phoneNumber));
        outputDto.setRefreshToken(jwtUtil.generateRefreshToken(phoneNumber));
        outputDto.setUserInfo(BeanUtil.copyProperties(usersService.saveUserByMobile(phoneNumber), UserDto.class));
        return R.success(outputDto);
    }

}

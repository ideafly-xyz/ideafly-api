package com.ideafly.controller;

import cn.hutool.core.bean.BeanUtil;
import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.LoginSuccessOutputDto;
import com.ideafly.dto.SmsLoginInputDto;
import com.ideafly.dto.user.UserDto;
import com.ideafly.model.Users;
import com.ideafly.service.SmsService;
import com.ideafly.service.UsersService;
import com.ideafly.service.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "短信相关接口")
@RestController
@RequestMapping("api/sms")
public class SmsH5Controller {
    @Resource
    private SmsService smsService;
    @Resource
    private JwtService jwtService;
    @Resource
    private UsersService usersService;

    @NoAuth
    @PostMapping("/sendSms")
    public R<String> sendSms(@RequestParam("phone_number") String phoneNumber) {
        return R.success("ok");
   /*     boolean isSent = smsService.sendSmsVerificationCode(phoneNumber);
        if (isSent) {
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");*/
    }

    @NoAuth
    @PostMapping("/login")
    public R<LoginSuccessOutputDto> login(@RequestBody SmsLoginInputDto dto) {
        if (smsService.verifyCode(dto.getVerificationCode())) {
            Users user = usersService.getOrAddByMobile(dto.getPhoneNumber());
            String userId = user.getId();
            LoginSuccessOutputDto outputDto = new LoginSuccessOutputDto();
            outputDto.setAccessToken(jwtService.generateToken(userId, false));
            outputDto.setRefreshToken(jwtService.generateToken(userId, true));
            outputDto.setUserInfo(BeanUtil.copyProperties(user, UserDto.class));
            return R.success(outputDto);
        }
        return R.error("验证码错误");
    }
}
package com.ideafly.controller.h5;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.dto.SmsLoginInputDto;
import com.ideafly.service.SmsService;
import com.ideafly.service.UsersService;
import com.ideafly.utils.JwtUtil;
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
    private JwtUtil jwtUtil;
    @Resource
    private UsersService usersService;

    @NoAuth
    @PostMapping("/sendSms")
    public R<String> sendSms(@RequestParam("phone_number") String phoneNumber) {
        return R.success("验证码发送成功");
       /* boolean isSent = smsService.sendSmsVerificationCode(phoneNumber);
        if (isSent) {
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");*/
    }

    @NoAuth
    @PostMapping("/login")
    public R<String> login(@RequestBody SmsLoginInputDto dto) {
        if (smsService.verifyCode(dto.getVerificationCode())) { //  !!!  安全漏洞  !!!  演示目的，存在安全风险
            //  !!!  这里应该进行真正的登录逻辑，例如：
            //  -  查询或创建用户
            //  -  生成 JWT Token 或 Session 等
            String token = jwtUtil.generateToken(dto.getPhoneNumber());
            usersService.saveUserByMobile(dto.getPhoneNumber());
            return R.success(token);
        }
        return R.error("验证码错误");
    }
}
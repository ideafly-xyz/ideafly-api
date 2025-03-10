package com.ideafly.controller.h5;

import com.ideafly.common.R;
import com.ideafly.service.SmsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "短信相关接口")
@RestController
@RequestMapping("api/sms")
public class SmsH5Controller {

    @Resource
    private SmsService smsService;

    @PostMapping("/sendSms")
    public R<String> sendSms(@RequestParam("phone_number") String phoneNumber) {
        boolean isSent = smsService.sendSmsVerificationCode(phoneNumber);
        if (isSent) {
            return R.success("验证码发送成功");
        }
        return R.error("验证码发送失败");
    }
}
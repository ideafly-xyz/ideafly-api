package com.ideafly.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.ideafly.config.AliyunSmsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Random;

@Slf4j
@Service
public class SmsService {

    @Resource
    private AliyunSmsConfig aliyunSmsConfig;

    // 存储验证码 (实际应用中应使用更安全的方式，如 Redis 或数据库，并设置过期时间)
    private String verificationCode;

    public String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) { // 生成6位验证码
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    public boolean sendSmsVerificationCode(String phoneNumber){ // 注意这里声明了异常抛出
        String code = generateVerificationCode();
        this.verificationCode = code; // 存储验证码
        Config config = new Config()
                // 您的 AccessKey ID
                .setAccessKeyId(aliyunSmsConfig.getAccessKeyId())
                // 您的 AccessKey Secret
                .setAccessKeySecret(aliyunSmsConfig.getAccessKeySecret());
        // 访问的域名
        config.endpoint = "dysmsapi.aliyuncs.com";
        try {
            Client client = new Client(config);

            SendSmsRequest sendSmsRequest = new SendSmsRequest()
                    .setPhoneNumbers(phoneNumber)
                    .setSignName(aliyunSmsConfig.getSignName())
                    .setTemplateCode(aliyunSmsConfig.getTemplateCode())
                    .setTemplateParam("{\"code\":\"" + code + "\"}"); // 模板参数，这里设置验证码
            SendSmsResponse response = client.sendSms(sendSmsRequest);
            SendSmsResponseBody body = response.getBody();
            System.out.println(body.getCode()); // 打印状态码
            System.out.println(body.getMessage()); // 打印消息
            return "OK".equalsIgnoreCase(body.getCode()); // 检查是否发送成功,  状态码 "OK" 表示成功
        } catch (Exception e) {
            log.error("Failed to send SMS verification code: ", e);
            return false;
        }
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public boolean verifyCode(String inputCode) {
        return this.verificationCode != null && this.verificationCode.equals(inputCode);
    }
}
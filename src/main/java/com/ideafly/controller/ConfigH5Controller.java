package com.ideafly.controller;

import com.ideafly.common.R;
import com.ideafly.dto.job.PublishJobInputDto;
import com.ideafly.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@Tag(name = "配置相关接口", description = "系统配置")
@RestController
@RequestMapping("api/config")
public class ConfigH5Controller {
    @Resource
    private ConfigService configService;

    @GetMapping("publishJob")
    @Operation(summary = "获取发布职位配置")
    public R<PublishJobInputDto> getPublishJobConfig() {
        return configService.getPublishJobConfig();
    }
}

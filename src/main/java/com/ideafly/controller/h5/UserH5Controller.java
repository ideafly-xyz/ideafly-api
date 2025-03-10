package com.ideafly.controller.h5;

import com.ideafly.common.R;
import com.ideafly.model.Users;
import com.ideafly.service.UsersService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "用户相关接口", description = "用户基本信息")
@RestController
@RequestMapping("api/user")
public class UserH5Controller {
    @Resource
    private UsersService usersService;

    @GetMapping("/get")
    public R<Users> get(@Parameter(description = "用户ID")@RequestParam("id") Integer id){
        return R.success(usersService.getById(id));
    }
}

package com.ideafly.controller.h5;

import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.model.Users;
import com.ideafly.service.UsersService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "用户相关接口", description = "用户基本信息")
@RestController
@RequestMapping("api/user")
public class UserH5Controller {
    @Resource
    private UsersService usersService;

    @GetMapping("/get")
    public R<Users> get(){
        return R.success(usersService.getUserByMobile(UserContextHolder.getUser().getMobile()));
    }
    @PostMapping("/update")
    public R<Boolean> update(@RequestBody UpdateUserInputDto dto){
        usersService.updateUser(dto);
        return R.success(Boolean.TRUE);
    }
}

package com.ideafly.controller.users;

import com.ideafly.common.R;
import com.ideafly.common.RequestUtils;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.model.users.Users;
import com.ideafly.service.impl.users.UsersService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Tag(name = "用户相关接口", description = "用户相关功能接口")
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UsersController {

    @Resource
    private UsersService usersService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public R<Users> getCurrentUserInfo(HttpServletRequest httpRequest) {
        String uid = RequestUtils.getCurrentUserId(httpRequest);
        if (uid == null) {
            return R.error("用户未登录");
        }
        log.info("获取当前用户信息: userId={}", uid);
        Users user = usersService.getById(uid);
        if (user == null) {
            return R.error("用户不存在");
        }
        log.info("获取当前用户信息成功: username={}", user.getUsername());
        return R.success(user);
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的信息")
    public R<Boolean> updateUser(@RequestBody UpdateUserInputDto dto, HttpServletRequest httpRequest) {
        String uid = RequestUtils.getCurrentUserId(httpRequest);
        if (uid == null) {
            return R.error("用户未登录");
        }
        log.info("更新用户信息: userId={} dto={} personalBio={} ", uid, dto, dto.getPersonalBio());
        usersService.updateUser(dto, uid);
        return R.success(Boolean.TRUE);
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户信息", description = "根据用户ID获取用户详细信息")
    public R<Users> getUserById(@PathVariable String userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        log.info("获取指定用户信息成功: userId={} username={}", userId, user.getUsername());
        return R.success(user);
    }
}
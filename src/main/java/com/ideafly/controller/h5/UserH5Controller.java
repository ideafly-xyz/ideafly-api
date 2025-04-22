package com.ideafly.controller.h5;

import com.ideafly.aop.anno.NoAuth;
import com.ideafly.common.R;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.user.UpdateUserInputDto;
import com.ideafly.dto.user.UserGetOutputDto;
import com.ideafly.model.Users;
import com.ideafly.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "用户相关接口", description = "用户相关接口")
@RestController
@RequestMapping("/api/user")
public class UserH5Controller {
    @Resource
    private UsersService usersService;

    @GetMapping("get")
    @Operation(summary = "获取用户信息", description = "获取用户信息")
    public R<UserGetOutputDto> getUserInfo() {
        Integer uid = UserContextHolder.getUid();
        if (Objects.isNull(uid)) {
            return R.error("未登录");
        }
        Users user = usersService.getById(uid);
        UserGetOutputDto userGetOutputDto = new UserGetOutputDto();
        userGetOutputDto.setId(user.getId());
        userGetOutputDto.setUsername(user.getUsername());
        userGetOutputDto.setEmail(user.getEmail());
        userGetOutputDto.setMobile(user.getMobile());
        userGetOutputDto.setAvatar(user.getAvatar());
        userGetOutputDto.setBio(user.getBio());
        userGetOutputDto.setTotalLikes(user.getTotalLikes() != null ? user.getTotalLikes() : 0);
        return R.success(userGetOutputDto);
    }

    @GetMapping("totalLikes")
    @Operation(summary = "获取用户总点赞数", description = "获取当前登录用户或指定用户的总点赞数")
    public R<Map<String, Object>> getUserTotalLikes(@RequestParam(value = "userId", required = false) Integer userId) {
        Integer targetUserId = userId;
        // 如果未指定用户ID，则使用当前登录用户ID
        if (targetUserId == null) {
            targetUserId = UserContextHolder.getUid();
            if (targetUserId == null) {
                return R.error("未登录且未指定用户ID");
            }
        }
        
        Users user = usersService.getById(targetUserId);
        if (user == null) {
            return R.error("用户不存在");
        }
        
        // 确保总点赞数不为null
        Integer totalLikes = user.getTotalLikes() != null ? user.getTotalLikes() : 0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("totalLikes", totalLikes);
        
        return R.success(result);
    }
    
    @NoAuth
    @GetMapping("profile/{userId}")
    @Operation(summary = "获取用户公开资料", description = "获取指定用户的公开资料，无需登录")
    public R<Map<String, Object>> getUserProfile(@PathVariable("userId") Integer userId) {
        Users user = usersService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("avatar", user.getAvatar());
        profile.put("bio", user.getBio());
        profile.put("totalLikes", user.getTotalLikes() != null ? user.getTotalLikes() : 0);
        
        return R.success(profile);
    }

    @PostMapping("/update")
    public R<Boolean> update(@RequestBody UpdateUserInputDto dto){
        usersService.updateUser(dto);
        return R.success(Boolean.TRUE);
    }
}

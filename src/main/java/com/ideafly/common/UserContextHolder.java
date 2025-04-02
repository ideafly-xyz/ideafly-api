package com.ideafly.common;


import com.ideafly.dto.user.UserDto;

import java.util.Objects;

public class UserContextHolder {

    private static final ThreadLocal<UserDto> currentUser = new ThreadLocal<>();

    public static void setUser(UserDto user) {
        currentUser.set(user);
    }

    public static UserDto getUser() {
        return currentUser.get();
    }
    public static Integer getUid() {
        if (Objects.isNull(currentUser.get())) {
            return null;
        }
        return currentUser.get().getId();
    }
    public static void removeUser() {
        currentUser.remove();
    }
}
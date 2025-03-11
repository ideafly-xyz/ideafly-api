package com.ideafly.common;


import com.ideafly.dto.user.UserDto;

public class UserContextHolder {

    private static final ThreadLocal<UserDto> currentUser = new ThreadLocal<>();

    public static void setUser(UserDto user) {
        currentUser.set(user);
    }

    public static UserDto getUser() {
        return currentUser.get();
    }

    public static void removeUser() {
        currentUser.remove();
    }
}
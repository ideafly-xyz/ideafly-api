package com.ideafly.common;

import lombok.Getter;

/**
 * @author rfs
 * @date 2025/03/31
 * 职业
 */
@Getter
public enum Profession {
    DEVELOPMENT(1, "开发"),
    PRODUCT(2, "产品"),
    DESIGN(3, "设计"),
    OPERATION(4, "运营"),
    WRITING(5, "写作"),
    MAINTENANCE(6, "运维"),
    OTHERS(9999, "其它职业");

    private final int code;
    private final String description;

    Profession(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Profession fromCode(int code) {
        for (Profession profession : Profession.values()) {
            if (profession.getCode() == code) {
                return profession;
            }
        }
        return OTHERS;
    }

     public static Profession fromDescription(String description) {
        for (Profession profession : Profession.values()) {
            if (profession.getDescription().equals(description)) {
                return profession;
            }
        }
        return null;
    }
}
package com.ideafly.common;

import lombok.Getter;

@Getter
public enum Profession {
    DEVELOPMENT(1, "开发"),
    PRODUCT(2, "产品"),
    DESIGN(3, "设计"),
    OPERATION(4, "运营"),
    WRITING(5, "写作"),
    MAINTENANCE(6, "运维"),
    OTHERS(7, "其它");

    private final short code;
    private final String description;

    Profession(int code, String description) {
        this.code = (short) code;
        this.description = description;
    }

    public static Profession fromCode(short code) {
        for (Profession profession : Profession.values()) {
            if (profession.getCode() == code) {
                return profession;
            }
        }
        return null;
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
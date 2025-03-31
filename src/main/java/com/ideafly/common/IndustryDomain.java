package com.ideafly.common;

import lombok.Getter;

@Getter
public enum IndustryDomain {
    MOBILE_INTERNET(1, "移动互联网"),
    E_COMMERCE(2, "电子商务"),
    EDUCATION_TRAINING(3, "教育培训"),
    FINANCE(4, "金融"),
    IT_SERVICES(5, "IT服务"),
    ARTIFICIAL_INTELLIGENCE(6, "人工智能"),
    GAME(7, "游戏"),
    CULTURE_MEDIA(8, "文化传媒"),
    MEDICAL_HEALTH(9, "医疗健康"),
    OTHER_DOMAIN(9999, "其他领域");

    private final int code;
    private final String description;

    IndustryDomain(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static IndustryDomain fromCode(int code) {
        for (IndustryDomain domain : IndustryDomain.values()) {
            if (domain.getCode() == code) {
                return domain;
            }
        }
        return OTHER_DOMAIN;
    }

    public static IndustryDomain fromDescription(String description) {
        for (IndustryDomain domain : IndustryDomain.values()) {
            if (domain.getDescription().equals(description)) {
                return domain;
            }
        }
        return null;
    }
}
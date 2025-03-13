package com.ideafly.common;

import lombok.Getter;

@Getter
public enum City {
    OVERSEAS(1, "海外"),
    BEIJING(2, "北京"),
    SHANGHAI(3, "上海"),
    GUANGZHOU(4, "广州"),
    SHENZHEN(5, "深圳"),
    HANGZHOU(6, "杭州"),
    CHENGDU(7, "成都"),
    XIAN(8, "西安"),
    XIAMEN(9, "厦门"),
    WUHAN(10, "武汉"),
    CHANGSHA(11, "长沙"),
    SUZHOU(12, "苏州"),
    ZHENGZHOU(13, "郑州"),
    NANJING(14, "南京"),
    YUNNAN(15, "云南"),
    HAINAN(16, "海南"),
    DALI(17, "大理"),
    OTHER_CITY(18, "其他");

    private final short code;
    private final String description;

    City(int code, String description) {
        this.code = (short) code;
        this.description = description;
    }

    public static City fromCode(short code) {
        for (City city : City.values()) {
            if (city.getCode() == code) {
                return city;
            }
        }
        return null;
    }

    public static City fromDescription(String description) {
        for (City city : City.values()) {
            if (city.getDescription().equals(description)) {
                return city;
            }
        }
        return null;
    }
}
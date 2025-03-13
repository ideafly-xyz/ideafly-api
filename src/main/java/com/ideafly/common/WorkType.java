package com.ideafly.common;

import lombok.Getter;

@Getter
public enum WorkType {
    FULL_TIME_OFFICE(1, "全职坐班"),
    REMOTE_WORK(2, "远程工作"),
    ONLINE_PART_TIME(3, "线上兼职"),
    ONSITE_TEMPORARY(4, "同城驻场");

    private final short code;
    private final String description;

    WorkType(int code, String description) {
        this.code = (short) code;
        this.description = description;
    }

    public static WorkType fromCode(short code) {
        for (WorkType workType : WorkType.values()) {
            if (workType.getCode() == code) {
                return workType;
            }
        }
        return null;
    }

     public static WorkType fromDescription(String description) {
        for (WorkType workType : WorkType.values()) {
            if (workType.getDescription().equals(description)) {
                return workType;
            }
        }
        return null;
    }
}
package com.ideafly.common;

import lombok.Getter;

/**
 * @author rfs
 * @date 2025/03/26
 * 工作方式
 */
@Getter
public enum WorkType {
    FULL_TIME_OFFICE(1, "全职坐班"),
    REMOTE_WORK(2, "远程工作"),
    ONLINE_PART_TIME(3, "线上兼职"),
    ONSITE_TEMPORARY(4, "同城驻场"),
    OTHER(9999, "其他工作方式");

    private final int code;
    private final String description;

    WorkType(int code, String description) {
        this.code = (short) code;
        this.description = description;
    }

    public static WorkType fromCode(int code) {
        for (WorkType workType : WorkType.values()) {
            if (workType.getCode() == code) {
                return workType;
            }
        }
        return OTHER;
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
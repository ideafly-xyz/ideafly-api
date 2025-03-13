package com.ideafly.common;

import lombok.Getter;

@Getter
public enum RecruitmentType {
    OUTSOURCING_FREELANCE(1, "外包零活"),
    COMPANY_DIRECT_RECRUITMENT(2, "企业直招"),
    HEADHUNTER_INTERMEDIARY(3, "猎头中介"),
    EMPLOYEE_REFERRAL(4, "员工内推"),
    TEAM_PARTNERSHIP(5, "组队合伙");

    private final short code;
    private final String description;

    RecruitmentType(int code, String description) {
        this.code = (short) code;
        this.description = description;
    }

    //  保留 fromCode 和 fromDescription 方法，虽然当前场景可能不直接使用，但作为通用方法，可以保留
    public static RecruitmentType fromCode(short code) {
        for (RecruitmentType type : RecruitmentType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }

    public static RecruitmentType fromDescription(String description) {
        for (RecruitmentType type : RecruitmentType.values()) {
            if (type.getDescription().equals(description)) {
                return type;
            }
        }
        return null;
    }
}
package com.ideafly.dto.job;

import com.ideafly.dto.PageBaseInputDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class JobListInputDto  extends PageBaseInputDto {
    private String city;
    private String profession;
    private String recruitmentType;
}
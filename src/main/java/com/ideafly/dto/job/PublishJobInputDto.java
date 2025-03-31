package com.ideafly.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
public class PublishJobInputDto {
    @Schema(description = "招聘类型", name = "recruitment_type_map")
    private Map<Integer,String> recruitmentTypeMap;
    @Schema(description = "职业", name = "profession_map")
    private Map<Integer,String> professionMap;
    @Schema(description = "城市", name = "city_map")
    private Map<Integer,String> citMap;
    @Schema(description = "工作方式", name = "work_type_map")
    private  Map<Integer,String> workTypeMap;
    @Schema(description = "领域", name = "industry_domain_map")
    private Map<Integer,String> industryDomainMap;
}

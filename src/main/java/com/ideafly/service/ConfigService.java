package com.ideafly.service;

import com.ideafly.common.*;
import com.ideafly.common.job.*;
import com.ideafly.dto.job.PublishJobInputDto;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class ConfigService {
    public R<PublishJobInputDto> getPublishJobConfig(){
        PublishJobInputDto publishJobInputDto = new PublishJobInputDto();
        publishJobInputDto.setProfessionMap(Arrays.stream(Profession.values()).collect(Collectors.toMap(Profession::getCode, Profession::getDescription)));
        publishJobInputDto.setCitMap(Arrays.stream(City.values()).collect(Collectors.toMap(City::getCode, City::getDescription)));
        publishJobInputDto.setWorkTypeMap(Arrays.stream(WorkType.values()).collect(Collectors.toMap(WorkType::getCode, WorkType::getDescription)));
        publishJobInputDto.setRecruitmentTypeMap(Arrays.stream(RecruitmentType.values()).collect(Collectors.toMap(RecruitmentType::getCode, RecruitmentType::getDescription)));
        publishJobInputDto.setIndustryDomainMap(Arrays.stream(IndustryDomain.values()).collect(Collectors.toMap(IndustryDomain::getCode, IndustryDomain::getDescription)));
        return R.success(publishJobInputDto);
    }
}

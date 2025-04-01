package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.mapper.JobCommentsMapper;
import com.ideafly.model.JobComments;
import org.springframework.stereotype.Service;

@Service
public class JobCommentsService extends ServiceImpl<JobCommentsMapper, JobComments> {
}

package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.mapper.JobCommentsMapper;
import com.ideafly.model.JobComments;
import com.ideafly.model.Users;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobCommentsService extends ServiceImpl<JobCommentsMapper, JobComments> {
    @Resource
    private UsersService usersService;
    @Resource
    private JobsService jobsService;

    public void addComment(JobCommentInputDto dto) {
        JobComments jobComments = BeanUtil.copyProperties(dto, JobComments.class);
        jobComments.setUserId(UserContextHolder.getUid());
        this.save(jobComments);
    }

    public void deleteComment(Long id) {
        JobComments jobComment = getById(id);
        if (Objects.nonNull(jobComment)) {
            this.removeById(id);
        }
    }

    public int getJobCommentCount(Integer jobId) {
        return this.lambdaQuery().eq(JobComments::getJobId, jobId).count().intValue();
    }

    public int getJobCommentsCount(Integer jobId) {
        return getJobCommentCount(jobId);
    }

    public List<JobComments> getCommentTreeByJobId(Integer jobId) {
        List<JobComments> allComments = this.lambdaQuery().eq(JobComments::getJobId, jobId).list();
        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }
        List<JobComments> rootComments = new ArrayList<>();
        Map<Integer, JobComments> commentMap = allComments.stream().collect(Collectors.toMap(JobComments::getId, comment -> comment));
        Set<Integer> uIds = allComments.stream().map(JobComments::getUserId).collect(Collectors.toSet());
        Map<Integer, Users> usersMap = usersService.lambdaQuery().in(Users::getId, uIds).list().stream().collect(Collectors.toMap(Users::getId, user -> user));
        for (JobComments comment : allComments) {
            comment.setUserName(usersMap.get(comment.getUserId()).getUsername());
            comment.setUserAvatar(usersMap.get(comment.getUserId()).getAvatar());
            Integer parentId = comment.getParentCommentId();
            if (parentId == null || parentId == 0) {
                rootComments.add(comment);
                continue;
            }
            JobComments parentComment = commentMap.get(parentId);
            if (parentComment != null) {
                parentComment.getChildren().add(comment);
            }
        }
        return rootComments;
    }
}

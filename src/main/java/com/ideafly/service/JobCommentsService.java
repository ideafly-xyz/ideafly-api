package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.mapper.JobCommentsMapper;
import com.ideafly.model.JobComments;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JobCommentsService extends ServiceImpl<JobCommentsMapper, JobComments> {

    public void addComment(JobCommentInputDto dto){
        JobComments jobComments = BeanUtil.copyProperties(dto, JobComments.class);
        this.save(jobComments);
    }
    public void deleteComment(Long id){
        this.removeById(id);
    }
    public List<JobComments> getCommentTreeByJobId(Integer jobId) {
        List<JobComments> allComments =this.lambdaQuery().eq(JobComments::getJobId,jobId).list();
        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }
        List<JobComments> rootComments = new ArrayList<>();
        Map<Integer, JobComments> commentMap = allComments.stream().collect(Collectors.toMap(JobComments::getId, comment -> comment));
        for (JobComments comment : allComments) {
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

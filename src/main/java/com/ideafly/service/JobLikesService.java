package com.ideafly.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobFavoriteInputDto;
import com.ideafly.dto.job.JobLikeInputDto;
import com.ideafly.mapper.JobFavoriteMapper;
import com.ideafly.mapper.JobLikesMapper;
import com.ideafly.model.JobFavorite;
import com.ideafly.model.JobLikes;
import com.ideafly.model.Jobs;
import com.ideafly.model.Users;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class JobLikesService extends ServiceImpl<JobLikesMapper, JobLikes> {
    @Resource
    private JobsService jobsService;
    
    @Resource
    private UsersService usersService;
    
    /**
     * 添加或取消点赞
     */
    public void addOrRemoveLike(JobLikeInputDto dto) {
        Integer uid = UserContextHolder.getUid();
        
        // 查找是否存在点赞记录
        JobLikes like = this.lambdaQuery()
            .eq(JobLikes::getJobId, dto.getJobId())
            .eq(JobLikes::getUserId, uid)
            .one();
            
        // 获取帖子信息，用于确定帖子作者
        Jobs job = jobsService.getById(dto.getJobId());
        if (job == null) {
            System.out.println("职位不存在，职位ID: " + dto.getJobId());
            return;
        }
        
        Integer authorId = job.getUserId();
        Users author = usersService.getById(authorId);
        if (author == null) {
            System.out.println("作者不存在，用户ID: " + authorId);
            return;
        }
        
        // 初始化总点赞数（如果为null）
        if (author.getTotalLikes() == null) {
            author.setTotalLikes(0);
        }
            
        // 判断操作类型
        if (Objects.equals(dto.getIsLike(), 1)) {
            // 添加点赞
            if (Objects.isNull(like)) {
                // 无记录，创建新记录
                JobLikes jobLike = new JobLikes();
                jobLike.setUserId(uid);
                jobLike.setJobId(dto.getJobId());
                jobLike.setStatus(1); // 有效状态
                this.save(jobLike);
                
                // 更新作者总点赞数
                author.setTotalLikes(author.getTotalLikes() + 1);
                usersService.updateById(author);
                System.out.println("增加作者点赞数，作者ID: " + authorId + ", 新点赞总数: " + author.getTotalLikes());
            } else if (like.getStatus() == 0) {
                // 有记录但已取消，更新状态
                like.setStatus(1);
                this.updateById(like);
                
                // 更新作者总点赞数
                author.setTotalLikes(author.getTotalLikes() + 1);
                usersService.updateById(author);
                System.out.println("恢复点赞，增加作者点赞数，作者ID: " + authorId + ", 新点赞总数: " + author.getTotalLikes());
            }
            // 已经点赞的不做处理
        } else {
            // 取消点赞
            if (Objects.nonNull(like) && like.getStatus() == 1) {
                // 更新状态为取消
                like.setStatus(0);
                this.updateById(like);
                
                // 更新作者总点赞数
                if (author.getTotalLikes() > 0) {
                    author.setTotalLikes(author.getTotalLikes() - 1);
                    usersService.updateById(author);
                    System.out.println("减少作者点赞数，作者ID: " + authorId + ", 新点赞总数: " + author.getTotalLikes());
                }
            }
            // 不存在或已取消的不做处理
        }
    }
    
    /**
     * 获取职位点赞数量
     */
    public int getJobLikesCount(Integer jobId) {
        return this.lambdaQuery()
            .eq(JobLikes::getJobId, jobId)
            .eq(JobLikes::getStatus, 1) // 只计算有效点赞
            .count().intValue();
    }
    
    /**
     * 判断用户是否点赞了职位
     */
    public boolean isJobLikedByUser(Integer jobId, Integer userId) {
        // 添加调试日志
        System.out.println("检查点赞状态 - jobId: " + jobId + ", userId: " + userId);
        
        boolean result = this.lambdaQuery()
            .eq(JobLikes::getJobId, jobId)
            .eq(JobLikes::getUserId, userId)
            .eq(JobLikes::getStatus, 1) // 只检查有效点赞
            .exists();
            
        System.out.println("点赞状态查询结果: " + result);
        return result;
    }
    
    /**
     * 判断当前登录用户是否点赞了职位
     */
    public boolean isJobLike(Integer jobId) {
        Integer uid = UserContextHolder.getUid();
        if (Objects.nonNull(uid)) {
            return isJobLikedByUser(jobId, uid);
        }
        return false;
    }
    
    // 添加批量查询点赞状态的方法
    public Map<Integer, Boolean> batchGetLikeStatus(List<Integer> jobIds, Integer userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("【性能日志】开始批量查询点赞状态 - 职位数量: " + jobIds.size());
        
        Map<Integer, Boolean> result = new HashMap<>();
        
        // 初始化默认状态为false
        for (Integer jobId : jobIds) {
            result.put(jobId, false);
        }
        
        if (jobIds.isEmpty() || userId == null) {
            return result;
        }
        
        try {
            // 批量查询所有已点赞的记录
            List<JobLikes> likesList = this.lambdaQuery()
                .in(JobLikes::getJobId, jobIds)
                .eq(JobLikes::getUserId, userId)
                .eq(JobLikes::getStatus, 1) // 只查询有效的点赞
                .list();
            
            // 更新点赞状态
            for (JobLikes like : likesList) {
                result.put(like.getJobId(), true);
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("【性能日志】批量查询点赞状态完成 - 耗时: " + (endTime - startTime) + 
                    "ms, 已点赞数量: " + likesList.size() + "/" + jobIds.size());
            
            return result;
        } catch (Exception e) {
            System.out.println("【性能日志】批量查询点赞状态异常: " + e.getMessage());
            return result;
        }
    }
}

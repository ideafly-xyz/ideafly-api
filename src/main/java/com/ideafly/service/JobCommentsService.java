package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobCommentCursorDto;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.dto.job.JobCommentPageDto;
import com.ideafly.mapper.JobCommentsMapper;
import com.ideafly.model.JobComments;
import com.ideafly.model.Users;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobCommentsService extends ServiceImpl<JobCommentsMapper, JobComments> {
    @Resource
    private UsersService usersService;
    @Resource
    private JobsService jobsService;

    // 默认的评论页大小
    private static final int DEFAULT_PAGE_SIZE = 10;
    // 游标格式
    private static final DateTimeFormatter CURSOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void addComment(JobCommentInputDto dto) {
        JobComments jobComments = BeanUtil.copyProperties(dto, JobComments.class);
        jobComments.setUserId(UserContextHolder.getUid());
        
        // 如果 parentCommentId 为 null，设置默认值为 0
        if (jobComments.getParentCommentId() == null) {
            jobComments.setParentCommentId(0);
        }
        
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
        
        // 获取所有评论作者的用户ID
        Set<Integer> uIds = allComments.stream().map(JobComments::getUserId).collect(Collectors.toSet());
        
        // 获取用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!uIds.isEmpty()) {
            usersMap = usersService.lambdaQuery().in(Users::getId, uIds).list().stream()
                .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        for (JobComments comment : allComments) {
            // 安全获取用户名和头像
            Users user = usersMap.get(comment.getUserId());
            if (user != null) {
                comment.setUserName(user.getUsername());
                comment.setUserAvatar(user.getAvatar());
            } else {
                // 如果找不到用户（可能被删除），设置默认值
                comment.setUserName("未知用户");
                comment.setUserAvatar("");
            }
            
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
    
    /**
     * 使用游标分页查询评论列表
     * @param request 评论分页请求
     * @return 游标分页结果
     */
    public JobCommentCursorDto getCommentsByCursor(JobCommentPageDto request) {
        if (request == null || request.getJobId() == null) {
            return new JobCommentCursorDto(new ArrayList<>(), null, false);
        }
        
        Integer jobId = request.getJobId();
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : DEFAULT_PAGE_SIZE;
        String maxCursor = request.getMaxCursor();
        
        // 创建查询条件
        LambdaQueryWrapper<JobComments> query = new LambdaQueryWrapper<>();
        // 确保只查询父评论(parentCommentId为0的评论)
        query.eq(JobComments::getJobId, jobId)
             .eq(JobComments::getParentCommentId, 0);
        
        // 如果有最大游标，则查询比该游标更早的评论
        if (maxCursor != null && !maxCursor.isEmpty()) {
            try {
                // 解析游标 - 格式为 commentId:timestamp
                String[] parts = maxCursor.split(":");
                if (parts.length == 2) {
                    int commentId = Integer.parseInt(parts[0]);
                    String timestamp = parts[1];
                    LocalDateTime cursorTime = LocalDateTime.parse(timestamp, CURSOR_FORMATTER);
                    
                    // 复合条件：创建时间早于等于游标时间的记录，如果时间相同则ID小于游标ID
                    query.and(wrapper -> wrapper
                            .lt(JobComments::getCreatedAt, cursorTime)
                            .or(w -> w
                                    .eq(JobComments::getCreatedAt, cursorTime)
                                    .lt(JobComments::getId, commentId)
                            )
                    );
                }
            } catch (Exception e) {
                System.out.println("解析评论游标失败: " + e.getMessage());
                // 游标解析失败，忽略游标条件
            }
        }
        
        // 按照创建时间降序，保证最新评论在前
        query.orderByDesc(JobComments::getCreatedAt, JobComments::getId);
        
        // 限制查询数量
        query.last("LIMIT " + (pageSize + 1)); // 多查询一条用于判断是否还有更多
        
        // 执行查询
        List<JobComments> comments = this.list(query);
        
        // 检查是否有更多评论
        boolean hasMore = comments.size() > pageSize;
        
        // 如果结果超出页大小，移除多余的记录
        if (hasMore) {
            comments = comments.subList(0, pageSize);
        }
        
        // 如果没有评论，直接返回空结果
        if (comments.isEmpty()) {
            return new JobCommentCursorDto(new ArrayList<>(), null, false);
        }
        
        // 填充子评论和用户信息
        List<JobComments> result = fillCommentDetails(comments, jobId);
        
        // 构建下一页游标
        String nextMaxCursor = null;
        if (!result.isEmpty() && hasMore) {
            JobComments lastComment = result.get(result.size() - 1);
            nextMaxCursor = lastComment.getId() + ":" + lastComment.getCreatedAt().format(CURSOR_FORMATTER);
        }
        
        return new JobCommentCursorDto(result, nextMaxCursor, hasMore);
    }
    
    /**
     * 填充评论的详细信息，包括子评论和用户信息
     */
    private List<JobComments> fillCommentDetails(List<JobComments> parentComments, Integer jobId) {
        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有父评论的ID
        List<Integer> parentIds = parentComments.stream()
                .map(JobComments::getId)
                .collect(Collectors.toList());
                
        // 查询这些父评论的所有子评论
        List<JobComments> childComments = this.lambdaQuery()
                .eq(JobComments::getJobId, jobId)
                .in(JobComments::getParentCommentId, parentIds)
                .list();
                
        // 为每个父评论设置子评论
        Map<Integer, List<JobComments>> childrenMap = childComments.stream()
                .collect(Collectors.groupingBy(JobComments::getParentCommentId));
                
        // 收集所有评论的用户ID (包括父评论和子评论)
        Set<Integer> allUserIds = new HashSet<>();
        parentComments.forEach(c -> allUserIds.add(c.getUserId()));
        childComments.forEach(c -> allUserIds.add(c.getUserId()));
        
        // 查询所有用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            usersMap = usersService.lambdaQuery()
                    .in(Users::getId, allUserIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        // 为父评论设置用户信息和子评论
        for (JobComments parent : parentComments) {
            // 设置用户信息
            Users user = usersMap.get(parent.getUserId());
            if (user != null) {
                parent.setUserName(user.getUsername());
                parent.setUserAvatar(user.getAvatar());
            } else {
                parent.setUserName("未知用户");
                parent.setUserAvatar("");
            }
            
            // 设置子评论
            List<JobComments> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
            
            // 为子评论设置用户信息
            for (JobComments child : children) {
                Users childUser = usersMap.get(child.getUserId());
                if (childUser != null) {
                    child.setUserName(childUser.getUsername());
                    child.setUserAvatar(childUser.getAvatar());
                } else {
                    child.setUserName("未知用户");
                    child.setUserAvatar("");
                }
            }
            
            parent.setChildren(children);
        }
        
        return parentComments;
    }
}

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
import com.ideafly.utils.CursorUtils;
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
    private static final int DEFAULT_PAGE_SIZE = 7;
    // 游标格式 - 仅用于日志记录
    private static final DateTimeFormatter CURSOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void addComment(JobCommentInputDto dto) {
        JobComments jobComments = BeanUtil.copyProperties(dto, JobComments.class);
        jobComments.setUserId(UserContextHolder.getUid());
        
        // 如果 parentCommentId 为 null，设置默认值为 0
        if (jobComments.getParentCommentId() == null) {
            jobComments.setParentCommentId(0);
        }
        
        // 如果 replyToCommentId 为 null 且是子评论，则将其设置为父评论ID
        if (jobComments.getReplyToCommentId() == null && jobComments.getParentCommentId() > 0) {
            jobComments.setReplyToCommentId(jobComments.getParentCommentId());
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
     * 使用游标分页查询评论列表 - 使用CursorUtils
     * @param request 评论分页请求
     * @return 游标分页结果
     */
    public JobCommentCursorDto getCommentsByCursor(JobCommentPageDto request) {
        if (request == null || request.getJobId() == null) {
            return new JobCommentCursorDto(new ArrayList<>(), null, false);
        }
        
        Integer jobId = request.getJobId();
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : DEFAULT_PAGE_SIZE;
        String cursor = request.getCursor();
        
        System.out.println("===== 评论游标分页请求 =====");
        System.out.println("请求参数: jobId=" + jobId + ", cursor=" + cursor + ", pageSize=" + pageSize);
        
        // 创建查询条件
        LambdaQueryWrapper<JobComments> query = new LambdaQueryWrapper<>();
        // 确保只查询父评论(parentCommentId为0的评论)
        query.eq(JobComments::getJobId, jobId)
             .eq(JobComments::getParentCommentId, 0);
        
        // 使用CursorUtils解析游标
        if (cursor != null && !cursor.isEmpty()) {
            // 使用CursorUtils解析Base64编码的游标
            Map<String, Object> cursorMap = CursorUtils.decodeCursor(cursor);
            if (cursorMap != null) {
                // 获取游标中的时间戳和ID
                Date timestampDate = (Date) cursorMap.get("timestamp");
                Integer cursorId = (Integer) cursorMap.get("id");
                
                if (timestampDate != null && cursorId != null) {
                    // 转换Date为LocalDateTime
                    LocalDateTime cursorTime = timestampDate.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                    
                    System.out.println("游标解析结果: ID=" + cursorId + ", 时间=" + cursorTime.format(CURSOR_FORMATTER));
                    
                    // 构建复合条件: 获取比游标时间更早的记录，或者相同时间但ID更小的记录
                    query.and(wrapper -> wrapper
                            .lt(JobComments::getCreatedAt, cursorTime)
                            .or(w -> w
                                    .eq(JobComments::getCreatedAt, cursorTime)
                                    .lt(JobComments::getId, cursorId)
                            )
                    );
                    
                    System.out.println("构建查询条件: 时间 < " + cursorTime + " 或 (时间 = " + cursorTime + " 且 ID < " + cursorId + ")");
                } else {
                    System.out.println("游标中缺少必要字段: timestamp=" + timestampDate + ", id=" + cursorId);
                }
            } else {
                System.out.println("游标解析失败，忽略游标条件: " + cursor);
            }
        }
        
        // 按照创建时间降序，保证最新评论在前
        query.orderByDesc(JobComments::getCreatedAt, JobComments::getId);
        
        // 限制查询数量
        query.last("LIMIT " + (pageSize + 1)); // 多查询一条用于判断是否还有更多
        
        System.out.println("最终SQL条件: " + query.getCustomSqlSegment());
        
        // 执行查询
        List<JobComments> comments = this.list(query);
        
        System.out.println("===== 评论游标分页查询结果 =====");
        System.out.println("获取评论数量: " + comments.size());
        if (!comments.isEmpty()) {
            System.out.println("第一条评论ID: " + comments.get(0).getId() + ", 时间: " + comments.get(0).getCreatedAt());
            if (comments.size() > 1) {
                System.out.println("最后一条评论ID: " + comments.get(comments.size()-1).getId() + ", 时间: " + comments.get(comments.size()-1).getCreatedAt());
            }
        }
        
        // 检查是否有更多评论
        boolean hasMore = comments.size() > pageSize;
        
        // 如果结果超出页大小，移除多余的记录
        if (hasMore) {
            comments = comments.subList(0, pageSize);
            System.out.println("有更多评论，移除多余评论后数量: " + comments.size());
        } else {
            System.out.println("没有更多评论");
        }
        
        // 如果没有评论，直接返回空结果
        if (comments.isEmpty()) {
            System.out.println("没有找到评论，返回空结果");
            return new JobCommentCursorDto(new ArrayList<>(), null, false);
        }
        
        // 填充子评论和用户信息
        List<JobComments> result = fillCommentDetails(comments, jobId);
        
        // 使用CursorUtils构建下一页游标
        String nextCursor = null;
        if (!result.isEmpty() && hasMore) {
            JobComments lastComment = result.get(result.size() - 1);
            nextCursor = CursorUtils.encodeCursor(lastComment.getCreatedAt(), lastComment.getId());
            System.out.println("构建新游标: " + nextCursor);
        } else {
            System.out.println("不构建新游标: 结果为空=" + result.isEmpty() + ", 没有更多评论=" + !hasMore);
        }
        
        System.out.println("===== 评论游标分页返回 =====");
        System.out.println("返回评论数: " + result.size() + ", nextCursor: " + nextCursor + ", hasMore: " + hasMore);
        
        return new JobCommentCursorDto(result, nextCursor, hasMore);
    }
    
    /**
     * 填充评论的详细信息，包括子评论和用户信息
     * 增强版：为子评论添加回复用户信息
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
        // 收集所有回复评论的ID
        Set<Integer> allReplyToCommentIds = new HashSet<>();
        parentComments.forEach(c -> allUserIds.add(c.getUserId()));
        childComments.forEach(c -> {
            allUserIds.add(c.getUserId());
            if (c.getReplyToCommentId() != null && c.getReplyToCommentId() > 0) {
                allReplyToCommentIds.add(c.getReplyToCommentId());
            }
        });
        
        // 查询所有用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            usersMap = usersService.lambdaQuery()
                    .in(Users::getId, allUserIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        // 查询回复评论的信息
        Map<Integer, JobComments> replyCommentMap = new HashMap<>();
        if (!allReplyToCommentIds.isEmpty()) {
            replyCommentMap = this.lambdaQuery()
                    .in(JobComments::getId, allReplyToCommentIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(JobComments::getId, comment -> comment));
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
            
            // 为子评论设置用户信息和回复信息
            for (JobComments child : children) {
                // 设置评论者信息
                Users childUser = usersMap.get(child.getUserId());
                if (childUser != null) {
                    child.setUserName(childUser.getUsername());
                    child.setUserAvatar(childUser.getAvatar());
                } else {
                    child.setUserName("未知用户");
                    child.setUserAvatar("");
                }
                
                // 设置被回复者信息
                if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0) {
                    JobComments replyToComment = replyCommentMap.get(child.getReplyToCommentId());
                    if (replyToComment != null && replyToComment.getUserId() != null) {
                        Users replyToUser = usersMap.get(replyToComment.getUserId());
                        if (replyToUser != null) {
                            child.setReplyToUserName(replyToUser.getUsername());
                        }
                    }
                }
            }
            
            // 清空现有列表并逐个添加子评论
            parent.getChildren().clear();
            for (JobComments child : children) {
                parent.getChildren().add(child);
            }
        }
        
        return parentComments;
    }
}

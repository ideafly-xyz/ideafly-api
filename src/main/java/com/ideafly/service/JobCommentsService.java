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
        
        // 处理回复关系：确保子评论的 parentCommentId 和 replyToCommentId 正确设置
        Integer replyToCommentId = jobComments.getReplyToCommentId();
        if (replyToCommentId != null && replyToCommentId > 0) {
            // 查找被回复的评论
            JobComments replyToComment = this.getById(replyToCommentId);
            if (replyToComment != null) {
                // 如果回复的是子评论，使用与被回复评论相同的父评论ID
                if (replyToComment.getParentCommentId() != null && replyToComment.getParentCommentId() > 0) {
                    // 设置父评论ID为被回复评论的父评论ID
                    jobComments.setParentCommentId(replyToComment.getParentCommentId());
                    // 保留replyToCommentId以标识具体回复的是哪条评论
                    System.out.println("回复子评论: 被回复评论ID=" + replyToCommentId + 
                                      ", 其父评论ID=" + replyToComment.getParentCommentId() + 
                                      ", 设置新评论parentCommentId=" + jobComments.getParentCommentId());
                } else {
                    // 如果回复的是一级评论，则父评论ID应为该评论的ID
                    jobComments.setParentCommentId(replyToComment.getId());
                    System.out.println("回复一级评论: 被回复评论ID=" + replyToCommentId + 
                                      ", 设置新评论parentCommentId=" + jobComments.getParentCommentId());
                }
            }
        } else if (jobComments.getParentCommentId() > 0) {
            // 如果只有parentCommentId没有replyToCommentId，将replyToCommentId设置为parentCommentId
            // 这表示直接回复父评论，而不是父评论中的某个子评论
            jobComments.setReplyToCommentId(jobComments.getParentCommentId());
            System.out.println("直接回复父评论: parentCommentId=" + jobComments.getParentCommentId() + 
                              ", 设置replyToCommentId=" + jobComments.getReplyToCommentId());
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
     * 增强版：为子评论添加回复用户信息，确保是平铺结构
     * 限制每个父评论下最多显示2条子评论，支持分页加载更多
     */
    private List<JobComments> fillCommentDetails(List<JobComments> parentComments, Integer jobId) {
        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有父评论的ID
        List<Integer> parentIds = parentComments.stream()
                .map(JobComments::getId)
                .collect(Collectors.toList());
                
        // 限制每个父评论下子评论的数量，每次最多查询2条子评论
        final int CHILD_COMMENTS_PAGE_SIZE = 2;
                
        // 为每个父评论收集子评论，并支持分页
        Map<Integer, List<JobComments>> childrenMap = new HashMap<>();
        Map<Integer, Boolean> hasMoreChildrenMap = new HashMap<>(); // 记录每个父评论是否有更多子评论
        Map<Integer, String> childrenNextCursorMap = new HashMap<>(); // 记录每个父评论子评论的下一页游标
        
        // 为每个父评论单独查询子评论
        for (JobComments parent : parentComments) {
            // 创建查询条件
            LambdaQueryWrapper<JobComments> query = new LambdaQueryWrapper<>();
            query.eq(JobComments::getJobId, jobId)
                 .eq(JobComments::getParentCommentId, parent.getId())
                 .orderByDesc(JobComments::getCreatedAt, JobComments::getId)
                 .last("LIMIT " + (CHILD_COMMENTS_PAGE_SIZE + 1)); // 多查询一条用于判断是否还有更多
                 
            List<JobComments> childList = this.list(query);
            
            // 检查是否有更多子评论
            boolean hasMoreChildren = childList.size() > CHILD_COMMENTS_PAGE_SIZE;
            hasMoreChildrenMap.put(parent.getId(), hasMoreChildren);
            
            // 如果有更多子评论，移除多余的一条，并生成下一页游标
            if (hasMoreChildren) {
                JobComments lastChild = childList.get(CHILD_COMMENTS_PAGE_SIZE);
                String nextCursor = CursorUtils.encodeCursor(lastChild.getCreatedAt(), lastChild.getId());
                childrenNextCursorMap.put(parent.getId(), nextCursor);
                
                // 移除多余的记录
                childList = childList.subList(0, CHILD_COMMENTS_PAGE_SIZE);
            }
            
            childrenMap.put(parent.getId(), childList);
        }
        
        // 收集所有评论中涉及的所有用户ID（包括父评论和子评论）
        Set<Integer> allUserIds = new HashSet<>();
        Set<Integer> allReplyToCommentIds = new HashSet<>();
        
        // 收集父评论用户ID
        parentComments.forEach(c -> allUserIds.add(c.getUserId()));
        
        // 收集所有子评论的用户ID和回复评论ID
        for (List<JobComments> children : childrenMap.values()) {
            for (JobComments child : children) {
                allUserIds.add(child.getUserId());
                if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0) {
                    allReplyToCommentIds.add(child.getReplyToCommentId());
                }
            }
        }
        
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
        Map<Integer, JobComments> allCommentsMap = new HashMap<>();
        
        // 先将父评论加入映射
        for (JobComments parent : parentComments) {
            allCommentsMap.put(parent.getId(), parent);
        }
        
        // 再将所有子评论加入映射
        for (List<JobComments> children : childrenMap.values()) {
            for (JobComments child : children) {
                allCommentsMap.put(child.getId(), child);
            }
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
            List<JobComments> children = childrenMap.get(parent.getId());
            if (children == null) {
                children = new ArrayList<>();
            }
            
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
                
                // 设置被回复者信息 - 只有当回复另一个子评论时才设置回复用户名
                if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0 && 
                    child.getParentCommentId() != child.getReplyToCommentId()) {
                    
                    JobComments replyToComment = allCommentsMap.get(child.getReplyToCommentId());
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
            parent.getChildren().addAll(children);
            
            // 设置是否有更多子评论标志和下一页游标
            parent.setHasMoreChildren(hasMoreChildrenMap.getOrDefault(parent.getId(), false));
            if (parent.getHasMoreChildren()) {
                parent.setChildrenNextCursor(childrenNextCursorMap.get(parent.getId()));
            }
        }
        
        return parentComments;
    }
    
    /**
     * 加载更多子评论
     * @param jobId 职位ID
     * @param parentId 父评论ID
     * @param cursor 游标
     * @return 子评论列表及分页信息
     */
    public Map<String, Object> loadMoreChildComments(Integer jobId, Integer parentId, String cursor) {
        System.out.println("===== 加载更多子评论 =====");
        System.out.println("参数: jobId=" + jobId + ", parentId=" + parentId + ", cursor=" + cursor);
        
        Map<String, Object> result = new HashMap<>();
        
        // 设置每次加载子评论的数量
        final int CHILD_COMMENTS_PAGE_SIZE = 2;
        
        // 创建查询条件
        LambdaQueryWrapper<JobComments> query = new LambdaQueryWrapper<>();
        query.eq(JobComments::getJobId, jobId)
             .eq(JobComments::getParentCommentId, parentId);
        
        // 使用CursorUtils解析游标
        if (cursor != null && !cursor.isEmpty()) {
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
                }
            }
        }
        
        // 按照创建时间降序，保证最新评论在前
        query.orderByDesc(JobComments::getCreatedAt, JobComments::getId);
        
        // 限制查询数量
        query.last("LIMIT " + (CHILD_COMMENTS_PAGE_SIZE + 1)); // 多查询一条用于判断是否还有更多
        
        // 执行查询
        List<JobComments> childComments = this.list(query);
        
        System.out.println("获取子评论数量: " + childComments.size());
        
        // 检查是否有更多子评论
        boolean hasMoreChildren = childComments.size() > CHILD_COMMENTS_PAGE_SIZE;
        
        // 如果有更多子评论，移除多余的一条
        String nextCursor = null;
        if (hasMoreChildren) {
            JobComments lastChild = childComments.get(CHILD_COMMENTS_PAGE_SIZE);
            nextCursor = CursorUtils.encodeCursor(lastChild.getCreatedAt(), lastChild.getId());
            
            // 移除多余的记录
            childComments = childComments.subList(0, CHILD_COMMENTS_PAGE_SIZE);
        }
        
        // 收集用户ID
        Set<Integer> userIds = childComments.stream()
                .map(JobComments::getUserId)
                .collect(Collectors.toSet());
        
        // 收集回复评论ID
        Set<Integer> replyToCommentIds = childComments.stream()
                .filter(c -> c.getReplyToCommentId() != null && c.getReplyToCommentId() > 0)
                .map(JobComments::getReplyToCommentId)
                .collect(Collectors.toSet());
        
        // 查询用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            usersMap = usersService.lambdaQuery()
                    .in(Users::getId, userIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        // 查询回复评论信息
        Map<Integer, JobComments> commentsMap = new HashMap<>();
        if (!replyToCommentIds.isEmpty()) {
            this.lambdaQuery()
                    .in(JobComments::getId, replyToCommentIds)
                    .list()
                    .forEach(c -> commentsMap.put(c.getId(), c));
        }
        
        // 填充子评论的用户信息和回复信息
        for (JobComments child : childComments) {
            // 设置用户信息
            Users user = usersMap.get(child.getUserId());
            if (user != null) {
                child.setUserName(user.getUsername());
                child.setUserAvatar(user.getAvatar());
            } else {
                child.setUserName("未知用户");
                child.setUserAvatar("");
            }
            
            // 设置回复用户信息
            if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0 && 
                !child.getReplyToCommentId().equals(child.getParentCommentId())) {
                
                JobComments replyToComment = commentsMap.get(child.getReplyToCommentId());
                if (replyToComment != null && replyToComment.getUserId() != null) {
                    Users replyToUser = usersMap.get(replyToComment.getUserId());
                    if (replyToUser != null) {
                        child.setReplyToUserName(replyToUser.getUsername());
                    }
                }
            }
        }
        
        // 构建返回结果
        result.put("children", childComments);
        result.put("next_cursor", nextCursor);
        result.put("has_more_children", hasMoreChildren);
        
        System.out.println("返回子评论数量: " + childComments.size() + 
                          ", nextCursor: " + nextCursor + 
                          ", hasMore: " + hasMoreChildren);
        
        return result;
    }
}

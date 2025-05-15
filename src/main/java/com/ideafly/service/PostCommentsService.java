package com.ideafly.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.job.JobCommentCursorDto;
import com.ideafly.dto.job.JobCommentInputDto;
import com.ideafly.dto.job.JobCommentPageDto;
import com.ideafly.mapper.PostCommentsMapper;
import com.ideafly.model.PostComments;
import com.ideafly.model.Users;
import com.ideafly.utils.CursorUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostCommentsService extends ServiceImpl<PostCommentsMapper, PostComments> {
    @Resource
    private UsersService usersService;
    @Resource
    private JobsService jobsService;

    // 默认的评论页大小
    private static final int DEFAULT_PAGE_SIZE = 7;
    // 子评论页大小
    private static final int CHILD_COMMENTS_PAGE_SIZE = 2;
    // 游标格式 - 仅用于日志记录
    private static final DateTimeFormatter CURSOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public void addComment(JobCommentInputDto dto) {
        PostComments postComments = BeanUtil.copyProperties(dto, PostComments.class);
        postComments.setUserId(UserContextHolder.getUid());
        
        // 如果 parentCommentId 为 null，设置默认值为 0
        if (postComments.getParentCommentId() == null) {
            postComments.setParentCommentId(0);
        }
        
        // 处理回复关系：确保子评论的 parentCommentId 和 replyToCommentId 正确设置
        Integer replyToCommentId = postComments.getReplyToCommentId();
        if (replyToCommentId != null && replyToCommentId > 0) {
            // 查找被回复的评论
            PostComments replyToComment = this.getById(replyToCommentId);
            if (replyToComment != null) {
                // 如果回复的是子评论，使用与被回复评论相同的父评论ID
                if (replyToComment.getParentCommentId() != null && replyToComment.getParentCommentId() > 0) {
                    // 设置父评论ID为被回复评论的父评论ID
                    postComments.setParentCommentId(replyToComment.getParentCommentId());
                    // 保留replyToCommentId以标识具体回复的是哪条评论
                    System.out.println("回复子评论: 被回复评论ID=" + replyToCommentId + 
                                      ", 其父评论ID=" + replyToComment.getParentCommentId() + 
                                      ", 设置新评论parentCommentId=" + postComments.getParentCommentId());
                } else {
                    // 如果回复的是一级评论，则父评论ID应为该评论的ID
                    postComments.setParentCommentId(replyToComment.getId());
                    System.out.println("回复一级评论: 被回复评论ID=" + replyToCommentId + 
                                      ", 设置新评论parentCommentId=" + postComments.getParentCommentId());
                }
            }
        } else if (postComments.getParentCommentId() > 0) {
            // 如果只有parentCommentId没有replyToCommentId，将replyToCommentId设置为parentCommentId
            // 这表示直接回复父评论，而不是父评论中的某个子评论
            postComments.setReplyToCommentId(postComments.getParentCommentId());
            System.out.println("直接回复父评论: parentCommentId=" + postComments.getParentCommentId() + 
                              ", 设置replyToCommentId=" + postComments.getReplyToCommentId());
        }
        
        this.save(postComments);
    }

    public void deleteComment(Long id) {
        PostComments postComment = getById(id);
        if (Objects.nonNull(postComment)) {
            this.removeById(id);
        }
    }

    public int getJobCommentCount(Integer jobId) {
        return this.lambdaQuery().eq(PostComments::getJobId, jobId).count().intValue();
    }

    public int getJobCommentsCount(Integer jobId) {
        return getJobCommentCount(jobId);
    }

    public List<PostComments> getCommentTreeByJobId(Integer jobId) {
        List<PostComments> allComments = this.lambdaQuery().eq(PostComments::getJobId, jobId).list();
        if (allComments.isEmpty()) {
            return new ArrayList<>();
        }
        List<PostComments> rootComments = new ArrayList<>();
        Map<Integer, PostComments> commentMap = allComments.stream().collect(Collectors.toMap(PostComments::getId, comment -> comment));
        
        // 获取所有评论作者的用户ID
        Set<Integer> uIds = allComments.stream().map(PostComments::getUserId).collect(Collectors.toSet());
        
        // 获取用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!uIds.isEmpty()) {
            usersMap = usersService.lambdaQuery().in(Users::getId, uIds).list().stream()
                .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        for (PostComments comment : allComments) {
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
            PostComments parentComment = commentMap.get(parentId);
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
        LambdaQueryWrapper<PostComments> query = new LambdaQueryWrapper<>();
        // 确保只查询父评论(parentCommentId为0的评论)
        query.eq(PostComments::getJobId, jobId)
             .eq(PostComments::getParentCommentId, 0);
        
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
                            .lt(PostComments::getCreatedAt, cursorTime)
                            .or(w -> w
                                    .eq(PostComments::getCreatedAt, cursorTime)
                                    .lt(PostComments::getId, cursorId)
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
        query.orderByDesc(PostComments::getCreatedAt, PostComments::getId);
        
        // 限制查询数量
        query.last("LIMIT " + (pageSize + 1)); // 多查询一条用于判断是否还有更多
        
        System.out.println("最终SQL条件: " + query.getCustomSqlSegment());
        
        // 执行查询
        List<PostComments> comments = this.list(query);
        
        // 检查是否还有更多评论
        boolean hasMore = comments.size() > pageSize;
        // 如果查询到的记录数大于pageSize，移除多余的一条
        if (hasMore) {
            comments = comments.subList(0, pageSize);
        }
        
        // 填充评论的详细信息（包括用户名，子评论等）
        List<PostComments> result = fillCommentDetails(comments, jobId);
        
        // 构建下一页游标
        String nextCursor = null;
        if (hasMore && !result.isEmpty()) {
            // 使用最后一条记录的createdAt和id作为下一页游标
            PostComments lastComment = result.get(result.size() - 1);
            
            // 使用正确的参数调用encodeCursor
            LocalDateTime createdAt = lastComment.getCreatedAt();
            Integer id = lastComment.getId();
            nextCursor = CursorUtils.encodeCursor(createdAt, id);
            
            System.out.println("生成下一页游标: ID=" + lastComment.getId() + 
                              ", 时间=" + lastComment.getCreatedAt().format(CURSOR_FORMATTER) + 
                              ", 编码后=" + nextCursor);
        }
        
        // 返回结果
        return new JobCommentCursorDto(result, nextCursor, hasMore);
    }

    /**
     * 填充评论的详细信息，包括用户信息和子评论
     * @param parentComments 父评论列表
     * @param jobId 职位ID
     * @return 填充了详细信息的评论列表
     */
    private List<PostComments> fillCommentDetails(List<PostComments> parentComments, Integer jobId) {
        if (parentComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取父评论IDs
        List<Integer> parentIds = parentComments.stream()
                .map(PostComments::getId)
                .collect(Collectors.toList());
        
        // 获取父评论的用户IDs
        Set<Integer> parentUserIds = parentComments.stream()
                .map(PostComments::getUserId)
                .collect(Collectors.toSet());
        
        // 获取每个父评论下的子评论（限制每个父评论只取前CHILD_COMMENTS_PAGE_SIZE + 1条）
        Map<Integer, List<PostComments>> childrenMap = new HashMap<>();
        
        // 为每个父评论加载子评论
        for (PostComments parent : parentComments) {
            Integer parentId = parent.getId();
            // 查询该父评论下的子评论
            LambdaQueryWrapper<PostComments> query = new LambdaQueryWrapper<>();
            query.eq(PostComments::getJobId, jobId)
                 .eq(PostComments::getParentCommentId, parent.getId())
                 .orderByDesc(PostComments::getCreatedAt, PostComments::getId)
                 .last("LIMIT " + (CHILD_COMMENTS_PAGE_SIZE + 1)); // 多查一条判断是否有更多
            
            List<PostComments> childList = this.list(query);
            
            // 设置是否有更多子评论的标志
            boolean hasMoreChildren = childList.size() > CHILD_COMMENTS_PAGE_SIZE;
            parent.setHasMoreChildren(hasMoreChildren);
            
            // 如果有更多子评论，生成子评论的下一页游标
            if (hasMoreChildren) {
                // 从列表中移除多余的一条
                PostComments lastChild = childList.get(CHILD_COMMENTS_PAGE_SIZE);
                childList = childList.subList(0, CHILD_COMMENTS_PAGE_SIZE);
                
                // 使用正确的参数调用encodeCursor
                String childNextCursor = CursorUtils.encodeCursor(lastChild.getCreatedAt(), lastChild.getId());
                parent.setChildrenNextCursor(childNextCursor);
                
                System.out.println("父评论ID=" + parentId + " 设置子评论游标=" + childNextCursor);
            }
            
            // 存储子评论列表
            childrenMap.put(parentId, childList);
            
            // 收集子评论的用户IDs
            childList.forEach(child -> parentUserIds.add(child.getUserId()));
        }
        
        // 收集所有子评论中被回复的评论IDs
        Set<Integer> replyToCommentIds = new HashSet<>();
        for (List<PostComments> children : childrenMap.values()) {
            for (PostComments child : children) {
                if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0) {
                    replyToCommentIds.add(child.getReplyToCommentId());
                }
            }
        }
        
        // 所有评论的映射表
        Map<Integer, PostComments> allCommentsMap = new HashMap<>();
        
        // 将父评论加入映射表
        for (PostComments parent : parentComments) {
            allCommentsMap.put(parent.getId(), parent);
        }
        
        // 将子评论加入映射表
        for (List<PostComments> children : childrenMap.values()) {
            for (PostComments child : children) {
                allCommentsMap.put(child.getId(), child);
            }
        }
        
        // 获取用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!parentUserIds.isEmpty()) {
            usersMap = usersService.lambdaQuery()
                    .in(Users::getId, parentUserIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        // 将子评论添加到对应的父评论中，同时设置用户信息
        for (PostComments parent : parentComments) {
            // 设置父评论的用户信息
            Users parentUser = usersMap.get(parent.getUserId());
            if (parentUser != null) {
                parent.setUserName(parentUser.getUsername());
                parent.setUserAvatar(parentUser.getAvatar());
            } else {
                parent.setUserName("未知用户");
                parent.setUserAvatar("");
            }
            
            // 添加子评论
            List<PostComments> children = childrenMap.get(parent.getId());
            if (children != null && !children.isEmpty()) {
                // 设置子评论的用户信息和回复关系
                for (PostComments child : children) {
                    // 设置子评论的用户信息
                    Users childUser = usersMap.get(child.getUserId());
                    if (childUser != null) {
                        child.setUserName(childUser.getUsername());
                        child.setUserAvatar(childUser.getAvatar());
                    } else {
                        child.setUserName("未知用户");
                        child.setUserAvatar("");
                    }
                    
                    // 设置被回复用户的名称
                    if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0) {
                        PostComments replyToComment = allCommentsMap.get(child.getReplyToCommentId());
                        if (replyToComment != null && replyToComment.getUserName() != null) {
                            child.setReplyToUserName(replyToComment.getUserName());
                        } else {
                            child.setReplyToUserName("未知用户");
                        }
                    }
                }
                
                // 添加到父评论的children列表
                parent.getChildren().addAll(children);
            }
        }
        
        return parentComments;
    }
    
    /**
     * 加载更多子评论
     * @param jobId 职位ID
     * @param parentId 父评论ID
     * @param cursor 游标
     * @return 子评论列表和分页信息
     */
    public Map<String, Object> loadMoreChildComments(Integer jobId, Integer parentId, String cursor) {
        System.out.println("===== 加载更多子评论 =====");
        System.out.println("请求参数: jobId=" + jobId + ", parentId=" + parentId + ", cursor=" + cursor);
        
        Map<String, Object> result = new HashMap<>();
        int pageSize = CHILD_COMMENTS_PAGE_SIZE;
        
        // 创建查询条件
        LambdaQueryWrapper<PostComments> query = new LambdaQueryWrapper<>();
        query.eq(PostComments::getJobId, jobId)
             .eq(PostComments::getParentCommentId, parentId);
        
        // 解析游标
        if (cursor != null && !cursor.isEmpty()) {
            Map<String, Object> cursorMap = CursorUtils.decodeCursor(cursor);
            if (cursorMap != null) {
                Date timestampDate = (Date) cursorMap.get("timestamp");
                Integer cursorId = (Integer) cursorMap.get("id");
                
                if (timestampDate != null && cursorId != null) {
                    LocalDateTime cursorTime = timestampDate.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                    
                    System.out.println("子评论游标解析结果: ID=" + cursorId + ", 时间=" + cursorTime.format(CURSOR_FORMATTER));
                    
                    // 构建游标条件
                    query.and(wrapper -> wrapper
                            .lt(PostComments::getCreatedAt, cursorTime)
                            .or(w -> w
                                    .eq(PostComments::getCreatedAt, cursorTime)
                                    .lt(PostComments::getId, cursorId)
                            )
                    );
                }
            }
        }
        
        // 排序并限制查询数量
        query.orderByDesc(PostComments::getCreatedAt, PostComments::getId);
        query.last("LIMIT " + (pageSize + 1)); // 多查一条用于判断是否有更多
        
        // 执行查询
        List<PostComments> childComments = this.list(query);
        
        // 判断是否有更多
        boolean hasMore = childComments.size() > pageSize;
        if (hasMore) {
            // 记录最后一条评论用于生成下一页游标
            PostComments lastChild = childComments.get(pageSize);
            childComments = childComments.subList(0, pageSize);
            
            // 使用正确的参数调用encodeCursor
            String nextCursor = CursorUtils.encodeCursor(lastChild.getCreatedAt(), lastChild.getId());
            
            result.put("next_cursor", nextCursor);
            System.out.println("生成子评论下一页游标: ID=" + lastChild.getId() + 
                              ", 时间=" + lastChild.getCreatedAt().format(CURSOR_FORMATTER) + 
                              ", 编码后=" + nextCursor);
        }
        
        // 填充子评论的用户信息
        // 1. 收集用户ID和被回复评论ID
        Set<Integer> userIds = childComments.stream()
                .map(PostComments::getUserId)
                .collect(Collectors.toSet());
        
        Set<Integer> replyToCommentIds = childComments.stream()
                .map(PostComments::getReplyToCommentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        // 2. 获取用户信息
        Map<Integer, Users> usersMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            usersMap = usersService.lambdaQuery()
                    .in(Users::getId, userIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(Users::getId, user -> user));
        }
        
        // 3. 获取被回复的评论信息
        Map<Integer, PostComments> commentsMap = new HashMap<>();
        if (!replyToCommentIds.isEmpty()) {
            commentsMap = this.lambdaQuery()
                    .in(PostComments::getId, replyToCommentIds)
                    .list()
                    .stream()
                    .collect(Collectors.toMap(PostComments::getId, comment -> comment));
        }
        
        // 4. 设置用户信息和被回复用户信息
        for (PostComments child : childComments) {
            // 设置用户信息
            Users user = usersMap.get(child.getUserId());
            if (user != null) {
                child.setUserName(user.getUsername());
                child.setUserAvatar(user.getAvatar());
            } else {
                child.setUserName("未知用户");
                child.setUserAvatar("");
            }
            
            // 设置被回复用户信息
            if (child.getReplyToCommentId() != null && child.getReplyToCommentId() > 0) {
                PostComments replyToComment = commentsMap.get(child.getReplyToCommentId());
                if (replyToComment != null) {
                    // 获取被回复评论的用户
                    Users replyToUser = usersMap.get(replyToComment.getUserId());
                    if (replyToUser != null) {
                        child.setReplyToUserName(replyToUser.getUsername());
                    } else if (replyToComment.getUserName() != null) {
                        child.setReplyToUserName(replyToComment.getUserName());
                    } else {
                        child.setReplyToUserName("未知用户");
                    }
                } else {
                    child.setReplyToUserName("未知用户");
                }
            }
        }
        
        // 设置结果
        result.put("children", childComments);
        result.put("has_more_children", hasMore);
        
        return result;
    }
    
    /**
     * 获取某个父评论下的子评论数量
     * @param jobId 职位ID
     * @param parentId 父评论ID
     * @return 子评论数量
     */
    public int getChildCommentsCount(Integer jobId, Integer parentId) {
        return this.lambdaQuery()
                .eq(PostComments::getJobId, jobId)
                .eq(PostComments::getParentCommentId, parentId)
                .count().intValue();
    }
} 
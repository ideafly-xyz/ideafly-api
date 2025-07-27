package com.ideafly.service.impl.interact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ideafly.common.UserContextHolder;
import com.ideafly.dto.interact.ChildCommentCursorDto;
import com.ideafly.dto.interact.JobCommentInputDto;
import com.ideafly.dto.interact.JobCommentPageDto;
import com.ideafly.dto.interact.ParentCommentCursorDto;
import com.ideafly.dto.job.*;
import com.ideafly.mapper.interact.ChildCommentMapper;
import com.ideafly.mapper.interact.ParentCommentMapper;
import com.ideafly.model.interact.ChildComment;
import com.ideafly.model.interact.ParentComment;
import com.ideafly.model.users.Users;
import com.ideafly.service.impl.users.UsersService;
import com.ideafly.utils.CursorUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService extends ServiceImpl<ParentCommentMapper, ParentComment> {
    @Resource
    private UsersService usersService;
    
    @Resource
    private ChildCommentMapper childCommentMapper;
    
    // 默认的父评论页大小
    private static final int DEFAULT_PARENT_COMMENTS_PAGE_SIZE = 7;
    // 默认的子评论页大小
    private static final int DEFAULT_CHILD_COMMENTS_PAGE_SIZE = 2;
    // 游标格式 - 仅用于日志记录
    private static final DateTimeFormatter CURSOR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 添加评论
     * @return 返回新创建的评论ID
     */
    public Integer addComment(JobCommentInputDto dto) {
        // 获取当前登录用户ID
        String userId = UserContextHolder.getUid();
        if (userId == null) {
            throw new IllegalArgumentException("用户未登录");
        }
        
        Integer commentId = null;
        
        // 创建新的父评论或子评论
        if (dto.getParentCommentId() == null || dto.getParentCommentId() == 0) {
            // 创建父评论
            ParentComment parentComment = new ParentComment();
            parentComment.setJobId(dto.getJobId());
            parentComment.setUserId(userId);
            parentComment.setParentCommentId(0); // 父评论的parentCommentId为0
            parentComment.setReplyToCommentId(0); // 父评论通常不回复其他评论
            parentComment.setContent(dto.getContent());
            parentComment.setCreatedAt(LocalDateTime.now());
            
            // 保存父评论
            this.save(parentComment);
            commentId = parentComment.getId();
        } else {
            // 创建子评论
            ChildComment childComment = new ChildComment();
            childComment.setJobId(dto.getJobId());
            childComment.setUserId(userId);
            childComment.setParentCommentId(dto.getParentCommentId());
            childComment.setReplyToCommentId(dto.getReplyToCommentId() != null ? dto.getReplyToCommentId() : 0);
            childComment.setContent(dto.getContent());
            childComment.setCreatedAt(LocalDateTime.now());
            
            // 保存子评论 - 使用通用Mapper保存到同一张表
            childCommentMapper.insert(childComment);
            commentId = childComment.getId();
        }
        
        return commentId;
    }
    
    /**
     * 获取父评论列表（游标分页）
     */
    public ParentCommentCursorDto getParentCommentsByCursor(JobCommentPageDto request) {
        if (request == null || request.getJobId() == null) {
            return new ParentCommentCursorDto(new ArrayList<>(), null, false);
        }
        
        Integer jobId = request.getJobId();
        Integer pageSize = request.getPageSize() != null ? request.getPageSize() : DEFAULT_PARENT_COMMENTS_PAGE_SIZE;
        String cursor = request.getCursor();
        
        System.out.println("===== 父评论游标分页请求 =====");
        System.out.println("请求参数: jobId=" + jobId + ", cursor=" + cursor + ", pageSize=" + pageSize);
        
        // 构建查询条件
        LambdaQueryWrapper<ParentComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ParentComment::getJobId, jobId);
        queryWrapper.eq(ParentComment::getParentCommentId, 0); // 父评论的parentCommentId为0
        
        System.out.println("===== 查询条件日志 =====");
        System.out.println("查询职位ID: " + jobId);
        
        // 处理游标
        if (cursor != null && !cursor.isEmpty()) {
            try {
                // 从游标中获取时间戳和ID
                Map<String, Object> cursorMap = CursorUtils.decodeCursor(cursor);
                Date timestamp = (Date) cursorMap.get("timestamp");
                Integer id = (Integer) cursorMap.get("id");
                
                // 转换时间戳
                LocalDateTime initialCursorTime = LocalDateTime.ofInstant(timestamp.toInstant(), 
                                                              java.time.ZoneId.systemDefault());
                
                System.out.println("游标解析: 时间=" + CURSOR_FORMATTER.format(initialCursorTime) + ", ID=" + id);
                
                // 检查时间戳是否是未来日期，如果是则使用当前时间
                LocalDateTime now = LocalDateTime.now();
                final LocalDateTime cursorTime;
                if (initialCursorTime.isAfter(now)) {
                    System.out.println("警告: 检测到未来日期游标 " + CURSOR_FORMATTER.format(initialCursorTime) + 
                                    "，将使用当前时间: " + CURSOR_FORMATTER.format(now));
                    cursorTime = now;
                } else {
                    cursorTime = initialCursorTime;
                }
                
                // 使用复合条件：创建时间小于游标时间，或者时间相同但ID小于等于游标ID
                queryWrapper.and(w -> w
                    .lt(ParentComment::getCreatedAt, cursorTime)
                    .or(o -> o
                        .eq(ParentComment::getCreatedAt, cursorTime)
                        .lt(ParentComment::getId, id)
                    )
                );
                System.out.println("查询条件: 创建时间 < " + CURSOR_FORMATTER.format(cursorTime) + " 或 (创建时间 = " + CURSOR_FORMATTER.format(cursorTime) + " 且 ID < " + id + ")");
            } catch (Exception e) {
                System.out.println("游标解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("无游标，查询最新评论");
        }
        
        // 排序并限制结果数量
        queryWrapper.orderByDesc(ParentComment::getCreatedAt, ParentComment::getId);
        queryWrapper.last("LIMIT " + (pageSize + 1)); // 多查询一条用于判断是否还有更多
        
        // 执行查询
        List<ParentComment> parentComments = this.list(queryWrapper);
        
        System.out.println("查询结果: 获取到 " + parentComments.size() + " 条评论");
        if (!parentComments.isEmpty()) {
            ParentComment first = parentComments.get(0);
            ParentComment last = parentComments.get(parentComments.size() - 1);
            System.out.println("结果范围: 第一条ID=" + first.getId() + ", 时间=" + CURSOR_FORMATTER.format(first.getCreatedAt()) + 
                              "; 最后一条ID=" + last.getId() + ", 时间=" + CURSOR_FORMATTER.format(last.getCreatedAt()));
        }
        
        // 处理查询结果
        boolean hasMore = false;
        String nextCursor = null;
        
        if (parentComments.size() > pageSize) {
            // 如果结果数量大于pageSize，说明还有更多数据
            hasMore = true;
            // 使用页面最后一条评论作为游标，而不是下一条评论
            ParentComment lastComment = parentComments.get(pageSize-1);
            nextCursor = CursorUtils.encodeCursor(lastComment.getCreatedAt(), lastComment.getId());
            System.out.println("生成下一页游标: ID=" + lastComment.getId() + ", 时间=" + 
                              CURSOR_FORMATTER.format(lastComment.getCreatedAt()) + 
                              ", 游标=" + nextCursor);
                              
            // 移除多余的数据
            parentComments = parentComments.subList(0, pageSize);
        }
        
        // 加载用户信息
        loadUserInfo(parentComments);
        
        // 加载子评论信息
        for (ParentComment parent : parentComments) {
            // 加载子评论数量
            int childCount = getChildCommentsCount(parent.getJobId(), parent.getId());
            parent.setChildrenCount(childCount);
            
            // 加载默认显示的子评论列表
            List<ChildComment> childComments = getTopChildComments(parent.getJobId(), parent.getId(), DEFAULT_CHILD_COMMENTS_PAGE_SIZE);
            parent.setChildren(childComments);
            
            // 设置是否有更多子评论
            if (childCount > childComments.size()) {
                parent.setHasMoreChildren(true);
                // 设置子评论游标
                if (!childComments.isEmpty()) {
                    ChildComment lastChild = childComments.get(childComments.size() - 1);
                    parent.setChildrenNextCursor(CursorUtils.encodeCursor(
                        lastChild.getCreatedAt(), lastChild.getId()));
                }
            }
        }
        
        System.out.println("===== 父评论游标分页响应 =====");
        System.out.println("响应结果: 父评论数=" + parentComments.size() + 
                           ", nextCursor=" + nextCursor + 
                           ", hasMore=" + hasMore);
        
        return new ParentCommentCursorDto(parentComments, nextCursor, hasMore);
    }
    
    /**
     * 获取顶部几条子评论
     */
    private List<ChildComment> getTopChildComments(Integer jobId, Integer parentId, int limit) {
        LambdaQueryWrapper<ChildComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChildComment::getJobId, jobId);
        queryWrapper.eq(ChildComment::getParentCommentId, parentId);
        queryWrapper.orderByDesc(ChildComment::getCreatedAt, ChildComment::getId); // 按时间降序排列，最新的在最前面
        queryWrapper.last("LIMIT " + limit);
        
        List<ChildComment> childComments = childCommentMapper.selectList(queryWrapper);
        
        // 加载用户信息
        loadChildUserInfo(childComments);
        
        return childComments;
    }
    
    /**
     * 加载更多子评论（游标分页）
     */
    public ChildCommentCursorDto loadMoreChildComments(JobLoadMoreChildrenDto request) {
        Integer jobId = request.getJobId();
        Integer parentId = request.getParentId();
        String cursor = request.getCursor();
        
        System.out.println("===== 加载更多子评论请求 =====");
        System.out.println("请求参数: jobId=" + jobId + ", parentId=" + parentId + ", cursor=" + cursor);
        
        // 构建查询
        LambdaQueryWrapper<ChildComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChildComment::getJobId, jobId);
        queryWrapper.eq(ChildComment::getParentCommentId, parentId);
        
        System.out.println("===== 子评论查询条件日志 =====");
        System.out.println("查询职位ID: " + jobId + ", 父评论ID: " + parentId);
        
        // 处理游标
        if (cursor != null && !cursor.isEmpty()) {
            try {
                // 从游标中获取时间戳和ID
                Map<String, Object> cursorMap = CursorUtils.decodeCursor(cursor);
                Date timestamp = (Date) cursorMap.get("timestamp");
                Integer id = (Integer) cursorMap.get("id");
                
                // 转换时间戳
                LocalDateTime initialCursorTime = LocalDateTime.ofInstant(timestamp.toInstant(), 
                                                             java.time.ZoneId.systemDefault());
                
                System.out.println("子评论游标解析: 时间=" + CURSOR_FORMATTER.format(initialCursorTime) + ", ID=" + id);
                
                // 检查时间戳是否是未来日期，如果是则使用当前时间
                LocalDateTime now = LocalDateTime.now();
                final LocalDateTime cursorTime;
                if (initialCursorTime.isAfter(now)) {
                    System.out.println("警告: 检测到未来日期游标 " + CURSOR_FORMATTER.format(initialCursorTime) + 
                                    "，将使用当前时间: " + CURSOR_FORMATTER.format(now));
                    cursorTime = now;
                } else {
                    cursorTime = initialCursorTime;
                }
                
                // 使用复合条件：创建时间小于游标时间，或者时间相同但ID更小（降序查询）
                queryWrapper.and(w -> w
                    .lt(ChildComment::getCreatedAt, cursorTime)
                    .or(o -> o
                        .eq(ChildComment::getCreatedAt, cursorTime)
                        .lt(ChildComment::getId, id)
                    )
                );
                
                System.out.println("子评论查询条件: 创建时间 < " + CURSOR_FORMATTER.format(cursorTime) + 
                               " 或 (创建时间 = " + CURSOR_FORMATTER.format(cursorTime) + 
                               " 且 ID < " + id + ")");
            } catch (Exception e) {
                System.out.println("子评论游标解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("无游标，查询最新子评论");
        }
        
        // 排序并限制结果数量
        queryWrapper.orderByDesc(ChildComment::getCreatedAt, ChildComment::getId); // 按时间降序排列，最新的在最前面
        queryWrapper.last("LIMIT " + (DEFAULT_CHILD_COMMENTS_PAGE_SIZE + 1)); // 多查询一条用于判断是否还有更多
        
        // 执行查询
        List<ChildComment> childComments = childCommentMapper.selectList(queryWrapper);
        
        System.out.println("子评论查询结果: 获取到 " + childComments.size() + " 条子评论");
        if (!childComments.isEmpty()) {
            ChildComment first = childComments.get(0);
            ChildComment last = childComments.get(childComments.size() - 1);
            System.out.println("子评论结果范围: 第一条ID=" + first.getId() + ", 时间=" + 
                           CURSOR_FORMATTER.format(first.getCreatedAt()) + 
                           "; 最后一条ID=" + last.getId() + ", 时间=" + 
                           CURSOR_FORMATTER.format(last.getCreatedAt()));
        }
        
        // 处理查询结果
        boolean hasMore = false;
        String nextCursor = null;
        
        if (childComments.size() > DEFAULT_CHILD_COMMENTS_PAGE_SIZE) {
            // 如果结果数量大于pageSize，说明还有更多数据
            hasMore = true;
            // 使用当前页最后一条评论作为游标，而不是下一条评论
            ChildComment lastComment = childComments.get(DEFAULT_CHILD_COMMENTS_PAGE_SIZE-1);
            nextCursor = CursorUtils.encodeCursor(lastComment.getCreatedAt(), lastComment.getId());
            System.out.println("生成子评论下一页游标: ID=" + lastComment.getId() + ", 时间=" + 
                            CURSOR_FORMATTER.format(lastComment.getCreatedAt()) + 
                            ", 游标=" + nextCursor);
            
            // 移除多余的数据
            childComments = childComments.subList(0, DEFAULT_CHILD_COMMENTS_PAGE_SIZE);
        }
        
        // 加载用户信息
        loadChildUserInfo(childComments);
        
        // 获取子评论总数
        int total = getChildCommentsCount(jobId, parentId);
        
        System.out.println("===== 加载更多子评论响应 =====");
        System.out.println("响应结果: 子评论数=" + childComments.size() + 
                         ", nextCursor=" + nextCursor + 
                         ", hasMore=" + hasMore +
                         ", 总数=" + total);
        
        // 返回新的DTO
        return new ChildCommentCursorDto(childComments, nextCursor, hasMore, total);
    }
    
    /**
     * 获取某个父评论下的子评论数量
     */
    public int getChildCommentsCount(Integer jobId, Integer parentId) {
        LambdaQueryWrapper<ChildComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChildComment::getJobId, jobId);
        queryWrapper.eq(ChildComment::getParentCommentId, parentId);
        
        return childCommentMapper.selectCount(queryWrapper).intValue();
    }
    
    /**
     * 获取职位下的评论总数
     */
    public int getCommentsCount(Integer jobId) {
        LambdaQueryWrapper<ParentComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ParentComment::getJobId, jobId);
        
        return this.baseMapper.selectCount(queryWrapper).intValue();
    }
    
    /**
     * 获取职位评论总数 (兼容旧的 PostCommentsService 方法)
     */
    public int getJobCommentsCount(Integer jobId) {
        return getCommentsCount(jobId);
    }
    
    /**
     * 加载用户信息
     */
    private void loadUserInfo(List<ParentComment> parentComments) {
        if (parentComments == null || parentComments.isEmpty()) {
            return;
        }
        
        // 收集所有用户ID
        Set<String> userIds = parentComments.stream()
            .map(ParentComment::getUserId)
            .collect(Collectors.toSet());
        
        // 批量查询用户信息
        List<Users> users = usersService.listByIds(new ArrayList<>(userIds));
        Map<String, Users> userMap = users.stream()
            .collect(Collectors.toMap(Users::getId, user -> user, (u1, u2) -> u1));
        
        // 设置用户信息
        for (ParentComment comment : parentComments) {
            Users user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUserName(user.getUsername());
                comment.setUserAvatar(user.getAvatar());
            } else {
                comment.setUserName("未知用户");
                comment.setUserAvatar("");
            }
        }
    }
    
    /**
     * 加载子评论用户信息
     */
    private void loadChildUserInfo(List<ChildComment> childComments) {
        if (childComments == null || childComments.isEmpty()) {
            return;
        }
        
        // 收集所有用户ID和被回复的用户ID
        Set<String> userIds = new HashSet<>();
        Set<String> replyToUserIds = new HashSet<>();
        
        for (ChildComment comment : childComments) {
            userIds.add(comment.getUserId());
            if (comment.getReplyToCommentId() != null && comment.getReplyToCommentId() > 0) {
                // 获取被回复评论的用户ID
                ChildComment replyToComment = childCommentMapper.selectById(comment.getReplyToCommentId());
                if (replyToComment != null) {
                    replyToUserIds.add(replyToComment.getUserId());
                }
            }
        }
        
        // 合并所有需要查询的用户ID
        userIds.addAll(replyToUserIds);
        
        // 批量查询用户信息
        List<Users> users = usersService.listByIds(new ArrayList<>(userIds));
        Map<String, Users> userMap = users.stream()
            .collect(Collectors.toMap(Users::getId, user -> user, (u1, u2) -> u1));
        
        // 设置用户信息
        for (ChildComment comment : childComments) {
            // 设置评论者信息
            Users user = userMap.get(comment.getUserId());
            if (user != null) {
                comment.setUserName(user.getUsername());
                comment.setUserAvatar(user.getAvatar());
            } else {
                comment.setUserName("未知用户");
                comment.setUserAvatar("");
            }
            
            // 设置被回复用户信息
            if (comment.getReplyToCommentId() != null && comment.getReplyToCommentId() > 0) {
                ChildComment replyToComment = childCommentMapper.selectById(comment.getReplyToCommentId());
                if (replyToComment != null) {
                    Users replyToUser = userMap.get(replyToComment.getUserId());
                    if (replyToUser != null) {
                        comment.setReplyToUserName(replyToUser.getUsername());
                    } else {
                        comment.setReplyToUserName("未知用户");
                    }
                }
            }
        }
    }
} 
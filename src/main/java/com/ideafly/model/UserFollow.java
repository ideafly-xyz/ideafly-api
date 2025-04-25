package com.ideafly.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户关注关系实体
 */
@Data
@TableName("user_follows")
public class UserFollow {
    /**
     * 关注关系ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    /**
     * 关注者用户ID (谁关注别人)
     */
    private Integer followerId;
    
    /**
     * 被关注者用户ID (被关注的人)
     */
    private Integer followedId;
    
    /**
     * 关注时间
     */
    private Date createdAt;
    
    /**
     * 关注状态: 1(有效), 0(已取消)
     */
    private Integer status;
} 
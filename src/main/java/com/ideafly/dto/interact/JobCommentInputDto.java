package com.ideafly.dto.interact;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class JobCommentInputDto {
    @NotNull(message = "职位ID不能为空")
    private Integer jobId; // 职位ID (关联 jobs 表)
    
    @Schema(name = "parent_comment_id", description = "父评论ID，如果是顶级评论则默认值为0")
    private Integer parentCommentId; // 父级评论ID (用于实现评论树结构)
    
    @Schema(name = "reply_to_comment_id", description = "被回复的评论ID，用于标识具体回复关系")
    private Integer replyToCommentId; // 回复的评论ID (标识回复关系)
    
    @NotBlank(message = "评论内容不能为空")
    private String content; // 评论内容
}

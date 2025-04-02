package com.ideafly.dto.job;

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
    @NotNull(message = "回复评论id不能为空 如果回复帖子 默认值为0")
    @Schema(name = "parent_comment_id", description = "回复评论id不能为空 如果回复帖子 默认值为0")
    private Integer parentCommentId; // 父级评论ID (用于实现评论回复)
    @NotBlank(message = "评论内容不能为空")
    private String content; // 评论内容
}

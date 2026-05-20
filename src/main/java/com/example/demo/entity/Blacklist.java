package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("blacklist")
public class Blacklist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;       // 被拉黑用户
    private Long sellerId;     // 拉黑的商家（null表示平台全局拉黑）
    private String reason;
    private Long operatorId;   // 操作人ID
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
}
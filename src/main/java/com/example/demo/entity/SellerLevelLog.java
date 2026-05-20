package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seller_level_log")
public class SellerLevelLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
    private Integer oldLevel;
    private Integer newLevel;
    private String reason;          // 调整原因（如“交易额达标”“好评率过低”）
    private String operator;        // 触发者：system 或 管理员用户名
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
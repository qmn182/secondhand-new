package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seller_punishment")
public class SellerPunishment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
    private Integer punishType;   // 1禁止发布商品 2店铺全部下架 3禁止登录
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Long operatorId;
    private Integer status;       // 1生效中 2已解除
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
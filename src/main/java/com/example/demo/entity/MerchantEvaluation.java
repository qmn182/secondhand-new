package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("merchant_evaluation")
public class MerchantEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long sellerId;
    private Long buyerId;
    private Integer serviceRating;   // 1-5星
    private String serviceComment;
    private String reply;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
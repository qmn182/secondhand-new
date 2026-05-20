package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("buyer_evaluation")
public class BuyerEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long sellerId;
    private Integer rating;
    private String comment;
    private String reply;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
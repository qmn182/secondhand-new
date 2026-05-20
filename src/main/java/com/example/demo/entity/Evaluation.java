package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("evaluation")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long userId;       // 买家ID
    private Integer rating;    // 1-5星
    private String comment;
    private String images;     // JSON数组
    private String reply;      // 商家回复
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
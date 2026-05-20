package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String shopName;
    private String name;
    private String category;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discount;
    private Integer stock;
    private Integer sold;
    private String description;
    private String imageUrl;      // 封面图
    private String images;        // JSON数组存储多张图片路径
    private Boolean negotiable;   // 是否允许议价
    @TableField("`condition`") 
    private String condition;     // 新旧程度
    private Integer status;       // 0待审核 1已上架 2下架 3售罄
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private String auditRemark;     // 审核备注（拒绝理由）
    private Double avgRating;     // 平均评分，冗余字段，方便查询展示
}
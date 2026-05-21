package com.example.demo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductAuditVO {
    private Long id;
    private String name;
    private String sellerUsername;   // 商家用户名
    private String shopName;
    private BigDecimal price;
    private Integer stock;
    private LocalDateTime createTime;
    // 可以添加其他需要的字段
}
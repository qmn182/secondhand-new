package com.example.demo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;


@Data
public class OrderItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal total;
    private String productImage;   // 商品封面图
    private Boolean evaluated; // 是否已评价
}
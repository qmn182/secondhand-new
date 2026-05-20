package com.example.demo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ShopProductVO {
    private Long id;
    private String name;
    private String imageUrl;        // 商品封面图
    private BigDecimal price;
    private Integer sold;           // 销量
    private Boolean negotiable;     // 是否允许议价
    private String condition;       // 新旧程度
}
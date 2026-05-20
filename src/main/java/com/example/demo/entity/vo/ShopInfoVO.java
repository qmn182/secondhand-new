package com.example.demo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShopInfoVO {
    private Long sellerId;
    private String shopName;
    private Integer level;          // 商家等级
    private LocalDateTime createTime; // 开店时间（注册时间）
    private Long totalSold;         // 店铺总销量（所有商品销量之和）
    private Double avgRating;       // 店铺平均评分（商品评价+服务评价综合）
    private Long totalReviews;      // 总评价数（商品评价+服务评价总数）
}
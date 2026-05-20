// entity/vo/EvaluationVO.java
package com.example.demo.entity.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EvaluationVO {
    private Long id;
    private Long productId;
    private String productName;    // 商品名称（冗余）
    private String productImage;   // 商品封面图
    private Integer rating;
    private String comment;
    private List<String> images;   // 图片列表
    private String reply;
    private String username;       // 评价人用户名
    private LocalDateTime createTime;
}
package com.example.demo.entity.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
    private String remark;
    private BigDecimal platformFee;
    private BigDecimal sellerIncome;
    private String deliveryCompany;
    private String deliveryNo;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime confirmTime;
    private List<OrderItemVO> items;
    private Long refundId;  // 关联的退货申请ID
}
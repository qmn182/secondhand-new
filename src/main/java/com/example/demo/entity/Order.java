package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private Integer pointsDeduct;
    private Integer status;          // 1待付款 2待发货 3待收货 4已完成 5已取消 6退货中 7已退款
    private String remark;
    private BigDecimal platformFee;
    private BigDecimal sellerIncome;
    private Integer escrowStatus;    // 1待支付 2已托管 3已结算
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime confirmTime;
    private LocalDateTime cancelTime;
    private LocalDateTime autoConfirmDeadline;
    private String deliveryCompany;
    private String deliveryNo;
}
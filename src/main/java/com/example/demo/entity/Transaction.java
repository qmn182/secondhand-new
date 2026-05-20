package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("transaction")
public class Transaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private String type;       // 充值,消费,退款,手续费,积分兑换
    private BigDecimal balance; // 变动后余额
    private Long linkId;       // 关联订单ID等
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
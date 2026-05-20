package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String city;
    private Integer gender;          // 1男 2女
    private String bankAccount;
    private Integer role;            // 1普通用户 2商家 3管理员
    private Integer status;          // 审核状态 0待审核 1已通过
    private String businessLicense;  // 营业执照路径
    private String idCardImage;      // 身份证图片路径
    private String shopName;
    private Integer level;           // 商家等级 1-5
    private BigDecimal wallet;       // 钱包余额
    private Integer points;          // 当前积分
    private Integer merchantStatus;  // 商家申请状态 0未申请 1待审核 2已通过 3拒绝
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private Double buyerRating;   // 买家评价评分
}
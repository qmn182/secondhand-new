package com.example.demo.entity; // 定义包名为 com.example.demo.entity

import com.baomidou.mybatisplus.annotation.*; // 导入 MyBatis-Plus 注解相关类（通配符）
import lombok.Data; // 导入 Lombok 的 Data 注解，用于自动生成 getter/setter 等方法
import java.time.LocalDateTime; // 导入 LocalDateTime 类，用于处理日期时间
import java.math.BigDecimal; // 导入 BigDecimal 类，用于高精度小数运算

@Data // Lombok 注解：自动生成 toString, equals, hashCode, getter, setter 等方法
@TableName("cart") // MyBatis-Plus 注解：指定该实体类对应的数据库表名为 "cart"
public class Cart { // 定义购物车实体类
    @TableId(type = IdType.AUTO) // MyBatis-Plus 注解：标记该字段为主键，且主键为数据库自增
    private Long id; // 购物车记录的唯一ID
    private Long userId; // 用户ID，关联到购物车所属用户
    private Long productId; // 商品ID，关联到添加的商品
    private Integer quantity; // 商品数量
    @TableField(fill = FieldFill.INSERT) // MyBatis-Plus 注解：插入时自动填充该字段
    private LocalDateTime createTime; // 购物车记录的创建时间

    // 非数据库字段，用于关联查询展示
    @TableField(exist = false) // MyBatis-Plus 注解：标记该字段在数据库中不存在，仅用于业务逻辑
    private String productName; // 商品名称（关联查询用）
    @TableField(exist = false) // MyBatis-Plus 注解：标记该字段在数据库中不存在，仅用于业务逻辑
    private BigDecimal productPrice; // 商品价格（关联查询用）
    @TableField(exist = false) // MyBatis-Plus 注解：标记该字段在数据库中不存在，仅用于业务逻辑
    private String productImage; // 商品图片URL（关联查询用）
    
}
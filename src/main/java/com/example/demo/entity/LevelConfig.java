// entity/LevelConfig.java
package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("level_config")
public class LevelConfig {
    @TableId
    private Integer level;
    private BigDecimal feeRate;
    private String description;
}
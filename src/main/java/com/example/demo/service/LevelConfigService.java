// service/LevelConfigService.java
package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.LevelConfig;
import com.example.demo.mapper.LevelConfigMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class LevelConfigService extends ServiceImpl<LevelConfigMapper, LevelConfig> {

    public BigDecimal getFeeRateByLevel(Integer level) {
        LevelConfig config = this.getById(level);
        return config == null ? BigDecimal.valueOf(0.01) : config.getFeeRate(); // 默认1%
    }
}
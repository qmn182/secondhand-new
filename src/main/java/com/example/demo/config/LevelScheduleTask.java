package com.example.demo.config;

import com.example.demo.service.MerchantLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class LevelScheduleTask {

    @Autowired
    private MerchantLevelService merchantLevelService;

    // 每月1日凌晨2点执行
    @Scheduled(cron = "0 0 2 1 * ?")
    public void recalculateLevels() {
        merchantLevelService.recalculateAllMerchantLevels("system");
    }
}
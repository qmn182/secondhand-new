package com.example.demo.config; // 定义包名为 com.example.demo.config

import com.example.demo.service.MerchantLevelService; // 导入商家等级服务类
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.scheduling.annotation.EnableScheduling; // 导入 Spring 定时任务启用注解
import org.springframework.scheduling.annotation.Scheduled; // 导入 Spring 定时任务调度注解
import org.springframework.stereotype.Component; // 导入 Spring 组件注解

@Component // 标记该类为 Spring 组件，以便自动扫描和管理
@EnableScheduling // 启用定时任务调度功能
public class LevelScheduleTask { // 定义商家等级定时任务类

    @Autowired // 自动装配 MerchantLevelService 实例
    private MerchantLevelService merchantLevelService; // 商家等级服务对象

    // 每月1日凌晨2点执行
    @Scheduled(cron = "0 0 2 1 * ?") // 配置定时任务执行表达式：每月1日凌晨2点0分0秒执行
    public void recalculateLevels() { // 定义重新计算等级的方法
        merchantLevelService.recalculateAllMerchantLevels("system"); // 调用商家等级服务，重新计算所有商家等级，操作人标记为 "system"
    }
}
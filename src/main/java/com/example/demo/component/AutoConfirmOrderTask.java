// component/AutoConfirmOrderTask.java
package com.example.demo.component; // 定义包名为 com.example.demo.component

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.example.demo.entity.Order; // 导入订单实体类 Order
import com.example.demo.service.OrderService; // 导入订单服务类
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.scheduling.annotation.EnableScheduling; // 导入 Spring 定时任务启用注解
import org.springframework.scheduling.annotation.Scheduled; // 导入 Spring 定时任务调度注解
import org.springframework.stereotype.Component; // 导入 Spring 组件注解
import org.springframework.transaction.annotation.Transactional; // 导入 Spring 事务注解

import java.time.LocalDateTime; // 导入 LocalDateTime 类，用于处理日期时间
import java.util.List; // 导入 List 集合接口

@Component // 标记该类为 Spring 组件，以便自动扫描和管理
@EnableScheduling // 启用定时任务调度功能
public class AutoConfirmOrderTask { // 定义自动确认收货定时任务类

    @Autowired // 自动装配 OrderService 实例
    private OrderService orderService; // 订单服务对象

    // 每天凌晨1点执行一次
    @Scheduled(cron = "0 0 1 * * ?") // 配置定时任务执行表达式：每天凌晨1点0分0秒执行
    @Transactional // 标记该方法需要在事务中执行
    public void autoConfirm() { // 定义自动确认收货的方法
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件构造器
        wrapper.eq(Order::getStatus, 3)  // 添加条件：订单状态为3（待收货）
               .le(Order::getAutoConfirmDeadline, LocalDateTime.now()); // 添加条件：自动确认截止时间小于等于当前时间
        List<Order> orders = orderService.list(wrapper); // 调用订单服务查询符合条件的订单列表
        for (Order order : orders) { // 遍历每个待自动确认的订单
            try { // 开始 try 块
                orderService.confirmReceive(order.getOrderNo(), order.getUserId()); // 调用订单服务确认收货，传入订单号和用户ID
            } catch (Exception e) { // 捕获异常
                // 记录日志，继续处理下一个
                e.printStackTrace(); // 打印异常堆栈信息到控制台
            }
        }
    }
}
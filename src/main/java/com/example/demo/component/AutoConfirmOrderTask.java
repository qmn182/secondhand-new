// component/AutoConfirmOrderTask.java
package com.example.demo.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class AutoConfirmOrderTask {

    @Autowired
    private OrderService orderService;

    // 每天凌晨1点执行一次
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoConfirm() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 3)  // 待收货
               .le(Order::getAutoConfirmDeadline, LocalDateTime.now());
        List<Order> orders = orderService.list(wrapper);
        for (Order order : orders) {
            try {
                orderService.confirmReceive(order.getOrderNo(), order.getUserId());
            } catch (Exception e) {
                // 记录日志，继续处理下一个
                e.printStackTrace();
            }
        }
    }
}
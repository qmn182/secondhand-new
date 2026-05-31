package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.OrderItem; // 导入订单项实体类 OrderItem
import com.example.demo.mapper.OrderItemMapper; // 导入订单项 Mapper 接口
import org.springframework.stereotype.Service; // 导入 Spring 的 Service 注解

@Service // 标记该类为 Spring 的服务层组件
public class OrderItemService extends ServiceImpl<OrderItemMapper, OrderItem> { // 定义订单项服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体
}
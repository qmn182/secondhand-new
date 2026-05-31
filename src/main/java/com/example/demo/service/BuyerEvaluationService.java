// BuyerEvaluationService.java
package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.BuyerEvaluation; // 导入买家评价实体类 BuyerEvaluation
import com.example.demo.mapper.BuyerEvaluationMapper; // 导入买家评价 Mapper 接口
import org.springframework.stereotype.Service; // 导入 Spring 的 Service 注解

@Service // 标记该类为 Spring 的服务层组件
public class BuyerEvaluationService extends ServiceImpl<BuyerEvaluationMapper, BuyerEvaluation> { // 定义买家评价服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体
}
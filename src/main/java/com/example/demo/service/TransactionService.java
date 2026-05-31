// service/TransactionService.java // 文件路径注释
package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.Transaction; // 导入交易流水实体类 Transaction
import com.example.demo.mapper.TransactionMapper; // 导入交易流水 Mapper 接口
import org.springframework.stereotype.Service; // 导入 Spring 的 Service 注解

import java.math.BigDecimal; // 导入 BigDecimal 高精度小数类
import java.time.LocalDateTime; // 导入 LocalDateTime 日期时间类

@Service // 标记该类为 Spring 的服务层组件
public class TransactionService extends ServiceImpl<TransactionMapper, Transaction> { // 定义交易流水服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    /**
     * 记录交易流水
     * @param userId    用户ID
     * @param amount    变动金额（正收入，负支出）
     * @param type      类型：充值、消费、退款、手续费、结算
     * @param balance   变动后余额
     * @param linkId    关联订单ID
     * @param remark    备注
     */
    public void record(Long userId, BigDecimal amount, String type, BigDecimal balance, Long linkId, String remark) { // 记录交易流水的方法，参数为用户ID、金额、类型、余额、关联ID、备注
        Transaction tx = new Transaction(); // 创建 Transaction 实体对象
        tx.setUserId(userId); // 设置用户ID
        tx.setAmount(amount); // 设置变动金额
        tx.setType(type); // 设置交易类型
        tx.setBalance(balance); // 设置变动后余额
        tx.setLinkId(linkId); // 设置关联订单ID
        tx.setRemark(remark); // 设置备注
        tx.setCreateTime(LocalDateTime.now()); // 设置创建时间为当前时间
        this.save(tx); // 调用 MyBatis-Plus 的 save 方法保存记录
    }
}
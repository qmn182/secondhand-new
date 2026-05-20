// service/TransactionService.java
package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Transaction;
import com.example.demo.mapper.TransactionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService extends ServiceImpl<TransactionMapper, Transaction> {

    /**
     * 记录交易流水
     * @param userId    用户ID
     * @param amount    变动金额（正收入，负支出）
     * @param type      类型：充值、消费、退款、手续费、结算
     * @param balance   变动后余额
     * @param linkId    关联订单ID
     * @param remark    备注
     */
    public void record(Long userId, BigDecimal amount, String type, BigDecimal balance, Long linkId, String remark) {
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setBalance(balance);
        tx.setLinkId(linkId);
        tx.setRemark(remark);
        tx.setCreateTime(LocalDateTime.now());
        this.save(tx);
    }
}
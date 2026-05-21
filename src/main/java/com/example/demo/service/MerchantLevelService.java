package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.service.EvaluationService;
import com.example.demo.service.MerchantEvaluationService;
import com.example.demo.service.SellerLevelLogService;

import com.example.demo.entity.SellerLevelLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MerchantLevelService {

    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ProductService productService;
    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private MerchantEvaluationService merchantEvaluationService;
    @Autowired
    private SellerLevelLogService levelLogService;
    
    /**
     * 重新计算所有商家的等级（定时任务调用）
     * @param operator 操作者标识（如 "system"）
     */
    @Transactional
    public void recalculateAllMerchantLevels(String operator) {
        List<User> sellers = userService.lambdaQuery().eq(User::getRole, 2).list();
        for (User seller : sellers) {
            int newLevel = calculateLevel(seller.getId());   // 改用 calculateLevel
            if (newLevel != seller.getLevel()) {
                seller.setLevel(newLevel);
                userService.updateById(seller);
                SellerLevelLog log = new SellerLevelLog();
                log.setSellerId(seller.getId());
                log.setOldLevel(seller.getLevel());
                log.setNewLevel(newLevel);
                log.setReason("自动重算");
                log.setOperator(operator);
                levelLogService.save(log);
            }
        }
    }

    /**
     * 重新计算单个商家的等级
     */
    @Transactional
    public void recalculateSingleMerchantLevel(Long sellerId, String operator) {
        User seller = userService.getById(sellerId);
        if (seller == null || seller.getRole() != 2) {
            throw new RuntimeException("商家不存在");
        }
        recalculateSingleMerchantLevel(seller, operator);
    }

    private void recalculateSingleMerchantLevel(User seller, String operator) {
        Integer oldLevel = seller.getLevel() == null ? 1 : seller.getLevel();
        Integer newLevel = calculateLevel(seller.getId());
        if (!oldLevel.equals(newLevel)) {
            seller.setLevel(newLevel);
            userService.updateById(seller);
            // 记录日志
            SellerLevelLog log = new SellerLevelLog();
            log.setSellerId(seller.getId());
            log.setOldLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setReason("根据交易额、好评率等自动调整");
            log.setOperator(operator);
            levelLogService.save(log);
        }
    }

    /**
     * 计算商家等级的核心算法
     * 规则：
     * 1. 近3个月交易总额（已完成订单的 sellerIncome 之和）确定基础等级
     * 2. 商品评价平均分和商家服务评价平均分影响等级（±1级）
     * 3. 最终等级限制在1-5之间
     */
    private Integer calculateLevel(Long sellerId) {
        // 1. 获取该商家的所有商品ID
        List<Long> productIds = productService.list(
                new LambdaQueryWrapper<Product>().eq(Product::getUserId, sellerId)
        ).stream().map(Product::getId).toList();

        if (productIds.isEmpty()) {
            return 1; // 无商品，最低等级
        }

        // 2. 计算近3个月的交易总额（通过订单明细关联订单，订单状态为已完成）
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        BigDecimal totalIncome = BigDecimal.ZERO;
        // 先查出该商家所有已完成订单的 sellerIncome 总和
        // 通过订单明细找到订单ID，再查询订单
        List<Long> orderIds = orderItemService.list(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds)
        ).stream().map(OrderItem::getOrderId).distinct().toList();

        if (!orderIds.isEmpty()) {
            List<Order> orders = orderService.list(new LambdaQueryWrapper<Order>()
                    .in(Order::getId, orderIds)
                    .eq(Order::getStatus, 4) // 已完成
                    .ge(Order::getConfirmTime, threeMonthsAgo));
            totalIncome = orders.stream()
                    .map(Order::getSellerIncome)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 3. 交易额映射基础等级
        int baseLevel = 1;
        if (totalIncome.compareTo(BigDecimal.valueOf(1000)) < 0) baseLevel = 1;
        else if (totalIncome.compareTo(BigDecimal.valueOf(5000)) < 0) baseLevel = 2;
        else if (totalIncome.compareTo(BigDecimal.valueOf(20000)) < 0) baseLevel = 3;
        else if (totalIncome.compareTo(BigDecimal.valueOf(50000)) < 0) baseLevel = 4;
        else baseLevel = 5;

        // 4. 好评率调整（±1级）
        // 4.1 商品评价平均分
        double avgProductRating = 0.0;
        List<Evaluation> evals = evaluationService.list(
                new LambdaQueryWrapper<Evaluation>().in(Evaluation::getProductId, productIds)
        );
        if (!evals.isEmpty()) {
            avgProductRating = evals.stream().mapToInt(Evaluation::getRating).average().orElse(0);
        }
        // 4.2 商家服务评价平均分
        double avgServiceRating = 0.0;
        List<MerchantEvaluation> merchantEvals = merchantEvaluationService.list(
                new LambdaQueryWrapper<MerchantEvaluation>().eq(MerchantEvaluation::getSellerId, sellerId)
        );
        if (!merchantEvals.isEmpty()) {
            avgServiceRating = merchantEvals.stream().mapToInt(MerchantEvaluation::getServiceRating).average().orElse(0);
        }
        double overallRating = (avgProductRating + avgServiceRating) / 2.0;

        int adjust = 0;
        if (overallRating >= 4.5) adjust = 1;
        else if (overallRating < 3.0) adjust = -1;

        int finalLevel = baseLevel + adjust;
        if (finalLevel < 1) finalLevel = 1;
        if (finalLevel > 5) finalLevel = 5;

        return finalLevel;
    }
}
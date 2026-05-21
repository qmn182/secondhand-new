// service/OrderService.java (完整替换，仅将 convertToVO 改为 public)
package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.*;
import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.service.PointsRecordService;

import com.example.demo.service.RefundService;

import com.example.demo.mapper.RefundMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.service.EvaluationService;

import com.example.demo.entity.vo.OrderVO;
import com.example.demo.entity.vo.OrderItemVO;
import org.springframework.beans.BeanUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;


@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {

    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private LevelConfigService levelConfigService;
    @Autowired
    private PointsRecordService pointsRecordService;
    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private RefundService refundService;

    /**
     * 一键下单（从购物车结算）- 资金托管版
     * @param userId 买家ID
     * @return 订单号
     */
    @Transactional
    public String createOrderFromCart(Long userId, Integer usePoints) {
        // 1. 获取购物车商品（同原有逻辑）
        List<Cart> cartList = cartService.list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (cartList.isEmpty()) throw new RuntimeException("购物车为空");

        // 2. 计算订单总金额，校验库存、获取卖家ID
        BigDecimal total = BigDecimal.ZERO;
        Long sellerId = null;
        for (Cart cart : cartList) {
            Product product = productService.getById(cart.getProductId());
            if (product == null || product.getStatus() != 1) throw new RuntimeException("商品已下架");
            if (product.getStock() < cart.getQuantity()) throw new RuntimeException("商品库存不足");
            // ========== 新增开始 ==========
            if (product.getUserId().equals(userId)) {
                throw new RuntimeException("不能购买自己发布的商品");
            }
            // ========== 新增结束 ==========
            if (sellerId == null) sellerId = product.getUserId();
            else if (!sellerId.equals(product.getUserId())) throw new RuntimeException("暂不支持跨商家结算");
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
        }

        // 3. 积分抵扣计算
        User buyer = userService.getById(userId);
        if (buyer == null) throw new RuntimeException("用户不存在");
        int availablePoints = buyer.getPoints() != null ? buyer.getPoints() : 0;
        if (usePoints == null) usePoints = 0;
        if (usePoints < 0) throw new RuntimeException("积分不能为负数");
        if (usePoints > availablePoints) throw new RuntimeException("积分不足，可用积分：" + availablePoints);
        
        BigDecimal deductAmount = BigDecimal.valueOf(usePoints / 100.0).setScale(2, RoundingMode.HALF_DOWN);
        if (deductAmount.compareTo(total) > 0) {
            throw new RuntimeException("积分抵扣金额不能超过订单总金额");
        }
        BigDecimal payAmount = total.subtract(deductAmount);

        // 4. 扣减用户钱包（按实付金额）
        if (buyer.getWallet().compareTo(payAmount) < 0) {
            throw new RuntimeException("余额不足，请充值");
        }
        buyer.setWallet(buyer.getWallet().subtract(payAmount));
        userService.updateById(buyer);

        // 记录消费流水（实付金额）
        transactionService.record(buyer.getId(), payAmount.negate(), "消费", buyer.getWallet(), null, "购物下单（积分抵扣"+usePoints+"）");

        // 5. 扣减积分
        if (usePoints > 0) {
            buyer.setPoints(availablePoints - usePoints);
            userService.updateById(buyer);
            // 记录积分明细
            pointsRecordService.record(buyer.getId(), -usePoints, buyer.getPoints(), "订单抵扣", null, "使用积分抵扣");
        }

        // 6. 计算平台手续费和商家实收（基于实付金额）
        User seller = userService.getById(sellerId);
        BigDecimal feeRate = levelConfigService.getFeeRateByLevel(seller.getLevel());
        BigDecimal platformFee = payAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sellerIncome = payAmount.subtract(platformFee);

        // 7. 扣减库存、增加销量（同原有）
        for (Cart cart : cartList) {
            Product product = productService.getById(cart.getProductId());
            product.setStock(product.getStock() - cart.getQuantity());
            product.setSold(product.getSold() + cart.getQuantity());
            // ========== 新增以下代码 ==========
            if (product.getStock() == 0) {
                product.setStatus(3); // 3=已售罄
            }
            // ========== 新增结束 ==========
            productService.updateById(product);
        }

        // 8. 生成订单（记录积分抵扣信息）
        String orderNo = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setOriginalAmount(total);
        order.setPointsDeduct(usePoints);
        order.setPlatformFee(platformFee);
        order.setSellerIncome(sellerIncome);
        order.setStatus(2);        // 待发货
        order.setEscrowStatus(2);  // 已托管
        order.setPayTime(LocalDateTime.now());
        order.setCreateTime(LocalDateTime.now());
        this.save(order);

        // 9. 生成订单明细（同原有）
        for (Cart cart : cartList) {
            Product product = productService.getById(cart.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(cart.getQuantity());
            item.setTotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            orderItemService.save(item);
        }

        // 10. 清空购物车
        cartService.clearCart(userId);

        // 11. 增加积分（按实付金额，1元1积分）
        int newPoints = payAmount.intValue();
        if (newPoints > 0) {
            buyer.setPoints(buyer.getPoints() + newPoints);
            userService.updateById(buyer);
            pointsRecordService.record(buyer.getId(), newPoints, buyer.getPoints(), "消费得积分", order.getId(), "订单消费得积分");
        }

        return orderNo;
    }
    /**
     * 商家发货（保存物流信息）
     */
    @Transactional
    public void deliverOrder(String orderNo, Long sellerId, String deliveryCompany, String deliveryNo) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new RuntimeException("订单不存在");
        Long actualSellerId = getSellerIdByOrder(order.getId());
        if (!actualSellerId.equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (order.getStatus() != 2) throw new RuntimeException("订单状态不是待发货");
        
        order.setStatus(3);  // 待收货
        order.setDeliveryTime(LocalDateTime.now());
        order.setAutoConfirmDeadline(LocalDateTime.now().plusDays(7));
        order.setDeliveryCompany(deliveryCompany);
        order.setDeliveryNo(deliveryNo);
        this.updateById(order);
    }

    /**
     * 买家确认收货（资金结算）
     */
    @Transactional
    public void confirmReceive(String orderNo, Long buyerId) {
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) throw new RuntimeException("订单不存在");
        if (!order.getUserId().equals(buyerId)) throw new RuntimeException("非本人订单");
        if (order.getStatus() != 3) throw new RuntimeException("订单状态不是待收货");
        if (order.getEscrowStatus() != 2) throw new RuntimeException("资金未托管，无法结算");

        // 1. 将商家实收金额（已扣平台费）转入卖家钱包
        User seller = userService.getById(getSellerIdByOrder(order.getId())); // 需要实现 getSellerIdByOrder
        BigDecimal sellerIncome = order.getSellerIncome();
        seller.setWallet(seller.getWallet().add(sellerIncome));
        userService.updateById(seller);

        // 2. 记录商家收入流水
        transactionService.record(seller.getId(), sellerIncome, "结算", seller.getWallet(), order.getId(), "订单确认收货");

        // 3. 记录平台手续费收入（可选，如果平台有独立账户）
        BigDecimal platformFee = order.getPlatformFee();
        // 假设平台账户ID为0，记录收入流水
        // transactionService.record(0L, platformFee, "手续费", null, order.getId(), "平台手续费");

        // 4. 更新订单状态
        order.setStatus(4);        // 已完成
        order.setEscrowStatus(3);  // 已结算
        order.setConfirmTime(LocalDateTime.now());
        this.updateById(order);
    }
    /**
     * 买家查询自己的订单列表（分页 + 状态筛选）
     * @param userId 买家ID
     * @param status 订单状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页订单VO
     */
    public Page<OrderVO> getUserOrders(Long userId, Integer status, Integer page, Integer size) {
        Page<Order> pageOrder = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> orderPage = this.page(pageOrder, wrapper);
        
        // 转换为 VO 并填充商品明细
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderVO> voList = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            voList.add(convertToVO(order));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 商家查询自己的订单列表（通过商品关联）
     * @param sellerId 商家ID
     * @param status 订单状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页订单VO
     */
    public Page<OrderVO> getMerchantOrders(Long sellerId, Integer status, Integer page, Integer size) {
        // 1. 先查出该商家的所有商品ID
        List<Long> productIds = productService.list(
            new LambdaQueryWrapper<Product>().eq(Product::getUserId, sellerId)
        ).stream().map(Product::getId).collect(Collectors.toList());
        
        if (productIds.isEmpty()) {
            Page<OrderVO> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        // 2. 查出订单明细中关联这些商品的所有订单ID
        List<Long> orderIds = orderItemService.list(
            new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds)
        ).stream().map(OrderItem::getOrderId).distinct().collect(Collectors.toList());
        
        if (orderIds.isEmpty()) {
            Page<OrderVO> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        // 3. 查询订单并分页
        Page<Order> pageOrder = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getId, orderIds);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> orderPage = this.page(pageOrder, wrapper);
        
        // 转换为 VO
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        List<OrderVO> voList = new ArrayList<>();
        for (Order order : orderPage.getRecords()) {
            voList.add(convertToVO(order));
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 转换 Order 实体为 OrderVO（包含订单商品明细）
     * 访问修饰符已从 private 改为 public，以便 OrderController 调用
     */
    public OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 1. 查询订单商品明细
        List<OrderItem> items = orderItemService.list(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())
        );
        if (items.isEmpty()) {
            vo.setItems(new ArrayList<>());
            return vo;
        }

        // 2. 批量查询该订单中所有商品是否已被评价（一次性查询）
        List<Long> productIds = items.stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
        List<Evaluation> evals = evaluationService.list(
            new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, order.getId())
                .in(Evaluation::getProductId, productIds)
        );
        // 构建已评价的 (orderId_productId) 集合
        Set<String> evaluatedKeys = evals.stream()
                .map(e -> e.getOrderId() + "_" + e.getProductId())
                .collect(Collectors.toSet());

        // 3. 循环构建 OrderItemVO
        List<OrderItemVO> itemVOs = new ArrayList<>();
        for (OrderItem item : items) {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);

            // 补充商品封面图
            Product product = productService.getById(item.getProductId());
            if (product != null) {
                itemVO.setProductImage(product.getImageUrl());
            }

            // 根据集合判断是否已评价
            boolean evaluated = evaluatedKeys.contains(order.getId() + "_" + item.getProductId());
            itemVO.setEvaluated(evaluated);

            itemVOs.add(itemVO);
        }
        vo.setItems(itemVOs);
        // 查询该订单是否有待审核的退货申请（status=1）
        List<Refund> refunds = refundService.list(
            new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, order.getId())
                .eq(Refund::getStatus, 1)   // 待审核
        );
        if (!refunds.isEmpty()) {
            vo.setRefundId(refunds.get(0).getId());  // 需要 OrderVO 有 refundId 字段
        }
        return vo;
    }
    /**
     * 根据订单ID获取商家ID（通过订单明细中的商品）
     */
    public Long getSellerIdByOrder(Long orderId) {
        List<OrderItem> items = orderItemService.list(
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)
        );
        if (items.isEmpty()) {
            throw new RuntimeException("订单无商品");
        }
        Product product = productService.getById(items.get(0).getProductId());
        return product.getUserId();
    }
    /**
     * 买家申请退货（仅限已完成订单，且确认收货后24小时内）
     * @param orderId    订单ID
     * @param orderItemId 订单商品明细ID（支持单个商品退货，也可整单退货）
     * @param userId     买家ID
     * @param reason     退货原因
     * @param images     凭证图片（可选，JSON数组）
     */
    @Transactional
    public void applyRefund(Long orderId, Long orderItemId, Long userId, String reason, List<String> images) {
        // 1. 校验订单
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        if (order.getStatus() != 4) {
            throw new RuntimeException("只有已完成订单才能申请退货");
        }
        // 2. 校验24小时限制（确认收货时间 + 24小时）
        if (order.getConfirmTime() == null) {
            throw new RuntimeException("订单尚未确认收货");
        }
        LocalDateTime deadline = order.getConfirmTime().plusHours(24);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new RuntimeException("已超过24小时退货期限");
        }
        // 3. 校验订单明细是否存在且未退货
        OrderItem item = orderItemService.getById(orderItemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new RuntimeException("订单商品不存在");
        }
        if (item.getStatus() == 4) {
            throw new RuntimeException("该商品已退货过");
        }
        // 4. 检查是否已有待审核的退货申请
        long existing = refundService.count(new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, orderId)
                .eq(Refund::getOrderItemId, orderItemId)
                .in(Refund::getStatus, 1, 2)); // 待审核或已同意未完成
        if (existing > 0) {
            throw new RuntimeException("已有退货申请正在处理中");
        }
        // 5. 计算退款金额（该商品的实付金额，需考虑积分抵扣比例分摊）
        BigDecimal refundAmount = calculateItemRefundAmount(order, item);
        // 6. 创建退款申请记录
        Refund refund = new Refund();
        refund.setOrderId(orderId);
        refund.setOrderItemId(orderItemId);
        refund.setUserId(userId);
        refund.setReason(reason);
        refund.setAmount(refundAmount);
        refund.setStatus(1); // 待审核
        refund.setApplyTime(LocalDateTime.now());
        refundService.save(refund);
        // 7. 将订单状态改为“退货中”（仅当该订单所有商品都进入退货流程时才改，为简化，直接改订单状态）
        order.setStatus(6); // 退货中
        this.updateById(order);
        // 可选：更新订单明细状态为“退货中”
        item.setStatus(3);
        orderItemService.updateById(item);
    }

    /**
     * 计算单个商品在订单中的实际退款金额（考虑积分抵扣比例）
     * 例如：订单总金额100元，抵扣积分1000（10元），实付90元。某商品单价50元，退款金额 = 50 * (90/100) = 45元
     */
    private BigDecimal calculateItemRefundAmount(Order order, OrderItem item) {
        if (order.getPointsDeduct() == null || order.getPointsDeduct() == 0) {
            return item.getTotal(); // 无积分抵扣，直接退商品小计
        }
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal payAmount = totalAmount.subtract(BigDecimal.valueOf(order.getPointsDeduct() / 100.0).setScale(2, RoundingMode.HALF_DOWN));
        BigDecimal ratio = payAmount.divide(totalAmount, 4, RoundingMode.HALF_DOWN);
        return item.getTotal().multiply(ratio).setScale(2, RoundingMode.HALF_DOWN);
    }

    /**
     * 商家审核退货申请
     * @param refundId   退货申请ID
     * @param sellerId   商家ID
     * @param approved   true=同意，false=拒绝
     * @param rejectReason 拒绝理由（拒绝时必填）
     */
    @Transactional
    public void auditRefund(Long refundId, Long sellerId, boolean approved, String rejectReason) {
        Refund refund = refundService.getById(refundId);
        if (refund == null) throw new RuntimeException("退货申请不存在");
        Order order = this.getById(refund.getOrderId());
        if (order == null) throw new RuntimeException("订单不存在");
        // 校验商家是否有权操作（通过商品关联）
        Long actualSellerId = getSellerIdByOrder(order.getId());
        if (!actualSellerId.equals(sellerId)) {
            throw new RuntimeException("无权操作此订单");
        }
        if (refund.getStatus() != 1) {
            throw new RuntimeException("该申请已处理过");
        }
        if (approved) {
            // 同意退货：执行退款逻辑
            // 1. 退款到买家钱包
            User buyer = userService.getById(refund.getUserId());
            BigDecimal refundAmount = refund.getAmount();
            buyer.setWallet(buyer.getWallet().add(refundAmount));
            userService.updateById(buyer);
            // 记录交易流水
            transactionService.record(buyer.getId(), refundAmount, "退款", buyer.getWallet(), order.getId(), "订单退货退款");
            // 2. 回滚使用的积分（退回用户积分）
            // 注意：原订单使用了 pointsDeduct 积分，需要按比例退回（简化：全额退回该商品对应的积分）
            // 如果只退部分商品，应计算该商品占用的积分。这里简化：只退回订单整体积分的比例。
            if (order.getPointsDeduct() != null && order.getPointsDeduct() > 0) {
                // 按商品金额占订单总金额比例退回积分
                OrderItem item = orderItemService.getById(refund.getOrderItemId());
                BigDecimal ratio = item.getTotal().divide(order.getTotalAmount(), 4, RoundingMode.HALF_DOWN);
                int pointsToReturn = (int) Math.floor(order.getPointsDeduct() * ratio.doubleValue());
                if (pointsToReturn > 0) {
                    buyer.setPoints(buyer.getPoints() + pointsToReturn);
                    userService.updateById(buyer);
                    pointsRecordService.record(buyer.getId(), pointsToReturn, buyer.getPoints(), "退货退回积分", refund.getId(), "退货退回已使用积分");
                }
            }
            // 3. 恢复商品库存
            OrderItem item = orderItemService.getById(refund.getOrderItemId());
            Product product = productService.getById(item.getProductId());
            product.setStock(product.getStock() + item.getQuantity());
            productService.updateById(product);
            // 4. 更新退货申请状态
            refund.setStatus(2); // 同意
            refund.setAuditTime(LocalDateTime.now());
            refund.setCompleteTime(LocalDateTime.now());
            refundService.updateById(refund);
            // 5. 更新订单明细状态为“已退货”
            item.setStatus(4);
            orderItemService.updateById(item);
            // 6. 如果订单所有商品都已退货，则将订单状态改为“已退款”；否则仍为“退货中”或改回“已完成”
            List<OrderItem> items = orderItemService.list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
            boolean allRefunded = items.stream().allMatch(i -> i.getStatus() == 4);
            if (allRefunded) {
                order.setStatus(7); // 已退款
                order.setEscrowStatus(3); // 资金已结算（原资金已给商家，这里退款后应调整，但实际已从平台支出，平台可能亏损，此处简化）
            } else {
                // 还有未退货商品，订单状态改回“已完成”
                order.setStatus(4);
            }
            this.updateById(order);
        } else {
            // 拒绝退货
            refund.setStatus(3); // 拒绝
            refund.setAuditTime(LocalDateTime.now());
            refund.setRemark(rejectReason); // 需要 Refund 实体有 remark 字段，可先添加
            refundService.updateById(refund);
            // 订单状态改回“已完成”
            order.setStatus(4);
            this.updateById(order);
            // 订单明细状态改回正常（如果之前改为退货中）
            OrderItem item = orderItemService.getById(refund.getOrderItemId());
            if (item.getStatus() == 3) {
                item.setStatus(1);
                orderItemService.updateById(item);
            }
        }
    }
}
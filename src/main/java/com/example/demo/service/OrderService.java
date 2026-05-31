// service/OrderService.java (完整替换，仅将 convertToVO 改为 public)
package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.*; // 导入所有实体类（通配符）
import com.example.demo.mapper.OrderMapper; // 导入订单 Mapper 接口
import com.example.demo.mapper.OrderMapper; // 重复导入订单 Mapper 接口（按原样保留）
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.stereotype.Service; // 导入 Spring 服务层注解
import org.springframework.transaction.annotation.Transactional; // 导入 Spring 事务注解
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 重复导入 Lambda 条件构造器
import com.example.demo.service.PointsRecordService; // 导入积分记录服务类

import com.example.demo.service.RefundService; // 导入退款服务类

import com.example.demo.mapper.RefundMapper; // 导入退款 Mapper 接口

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 第三次导入 Lambda 条件构造器
import com.example.demo.service.EvaluationService; // 导入商品评价服务类

import com.example.demo.entity.vo.OrderVO; // 导入订单视图对象类
import com.example.demo.entity.vo.OrderItemVO; // 导入订单项视图对象类
import org.springframework.beans.BeanUtils; // 导入 BeanUtils 用于属性拷贝
import java.util.ArrayList; // 导入 ArrayList 集合类
import java.util.List; // 导入 List 集合接口
import java.util.Set; // 导入 Set 集合接口
import java.util.stream.Collectors; // 导入 Collectors 用于流式收集
import java.math.BigDecimal; // 导入 BigDecimal 高精度小数类
import java.math.RoundingMode; // 导入小数舍入模式枚举
import java.time.LocalDateTime; // 导入 LocalDateTime 日期时间类
import java.util.List; // 重复导入 List 集合接口
import java.util.UUID; // 导入 UUID 工具类
import java.util.stream.Collectors; // 重复导入 Collectors
import java.util.Set; // 重复导入 Set
import java.util.HashSet; // 导入 HashSet 集合类


@Service // 标记该类为 Spring 的服务层组件
public class OrderService extends ServiceImpl<OrderMapper, Order> { // 定义订单服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    @Autowired // 自动装配 OrderItemService 实例
    private OrderItemService orderItemService; // 订单项服务对象
    @Autowired // 自动装配 CartService 实例
    private CartService cartService; // 购物车服务对象
    @Autowired // 自动装配 ProductService 实例
    private ProductService productService; // 商品服务对象
    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象
    @Autowired // 自动装配 TransactionService 实例
    private TransactionService transactionService; // 交易流水服务对象
    @Autowired // 自动装配 LevelConfigService 实例
    private LevelConfigService levelConfigService; // 商家等级配置服务对象
    @Autowired // 自动装配 PointsRecordService 实例
    private PointsRecordService pointsRecordService; // 积分记录服务对象
    @Autowired // 自动装配 EvaluationService 实例
    private EvaluationService evaluationService; // 商品评价服务对象
    @Autowired // 自动装配 RefundService 实例
    private RefundService refundService; // 退款服务对象

    /**
     * 一键下单（从购物车结算）- 资金托管版
     * @param userId 买家ID
     * @return 订单号
     */
    @Transactional // 标记该方法需要在事务中执行
    public String createOrderFromCart(Long userId, Integer usePoints) { // 从购物车创建订单的方法，参数为用户ID和使用的积分数量
        // 1. 获取购物车商品（同原有逻辑）
        List<Cart> cartList = cartService.list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId)); // 根据用户ID查询购物车列表
        if (cartList.isEmpty()) throw new RuntimeException("购物车为空"); // 如果购物车为空，抛出运行时异常

        // 2. 计算订单总金额，校验库存、获取卖家ID
        BigDecimal total = BigDecimal.ZERO; // 初始化订单总金额为0
        Long sellerId = null; // 初始化卖家ID为null
        for (Cart cart : cartList) { // 遍历购物车中的每个商品
            Product product = productService.getById(cart.getProductId()); // 根据商品ID查询商品信息
            if (product == null || product.getStatus() != 1) throw new RuntimeException("商品已下架"); // 如果商品不存在或状态不是已上架，抛出异常
            if (product.getStock() < cart.getQuantity()) throw new RuntimeException("商品库存不足"); // 如果库存不足，抛出异常
            // ========== 新增开始 ==========
            if (product.getUserId().equals(userId)) { // 如果商品所属用户ID等于当前用户ID（即购买自己的商品）
                throw new RuntimeException("不能购买自己发布的商品"); // 抛出异常
            }
            // ========== 新增结束 ==========
            if (sellerId == null) sellerId = product.getUserId(); // 如果卖家ID为空，设置为当前商品的卖家ID
            else if (!sellerId.equals(product.getUserId())) throw new RuntimeException("暂不支持跨商家结算"); // 如果商品属于不同卖家，抛出异常
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()))); // 累加商品总价（单价×数量）
        }

        // 3. 积分抵扣计算
        User buyer = userService.getById(userId); // 根据买家ID查询买家信息
        if (buyer == null) throw new RuntimeException("用户不存在"); // 如果买家不存在，抛出异常
        int availablePoints = buyer.getPoints() != null ? buyer.getPoints() : 0; // 获取买家可用积分，若为null则设为0
        if (usePoints == null) usePoints = 0; // 如果使用积分为null，设为0
        if (usePoints < 0) throw new RuntimeException("积分不能为负数"); // 如果使用积分为负数，抛出异常
        if (usePoints > availablePoints) throw new RuntimeException("积分不足，可用积分：" + availablePoints); // 如果使用积分超过可用积分，抛出异常
        
        BigDecimal deductAmount = BigDecimal.valueOf(usePoints / 100.0).setScale(2, RoundingMode.HALF_DOWN); // 计算抵扣金额（1积分=0.01元），保留2位小数，向下舍入
        if (deductAmount.compareTo(total) > 0) { // 如果抵扣金额大于订单总金额
            throw new RuntimeException("积分抵扣金额不能超过订单总金额"); // 抛出异常
        }
        BigDecimal payAmount = total.subtract(deductAmount); // 计算实付金额 = 总金额 - 抵扣金额

        // 4. 扣减用户钱包（按实付金额）
        if (buyer.getWallet().compareTo(payAmount) < 0) { // 如果钱包余额小于实付金额
            throw new RuntimeException("余额不足，请充值"); // 抛出异常
        }
        buyer.setWallet(buyer.getWallet().subtract(payAmount)); // 买家钱包减去实付金额
        userService.updateById(buyer); // 更新买家信息

        // 记录消费流水（实付金额）
        transactionService.record(buyer.getId(), payAmount.negate(), "消费", buyer.getWallet(), null, "购物下单（积分抵扣"+usePoints+"）"); // 记录消费流水，金额为负数

        // 5. 扣减积分
        if (usePoints > 0) { // 如果使用了积分
            buyer.setPoints(availablePoints - usePoints); // 买家积分减去使用的积分
            userService.updateById(buyer); // 更新买家信息
            // 记录积分明细
            pointsRecordService.record(buyer.getId(), -usePoints, buyer.getPoints(), "订单抵扣", null, "使用积分抵扣"); // 记录积分扣减明细
        }

        // 6. 计算平台手续费和商家实收（基于实付金额）
        User seller = userService.getById(sellerId); // 根据卖家ID查询卖家信息
        BigDecimal feeRate = levelConfigService.getFeeRateByLevel(seller.getLevel()); // 根据卖家等级获取平台手续费率
        BigDecimal platformFee = payAmount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP); // 计算平台手续费 = 实付金额 × 费率，四舍五入
        BigDecimal sellerIncome = payAmount.subtract(platformFee); // 计算商家实收 = 实付金额 - 手续费

        // 7. 扣减库存、增加销量（同原有）
        for (Cart cart : cartList) { // 遍历购物车商品
            Product product = productService.getById(cart.getProductId()); // 根据商品ID查询商品
            product.setStock(product.getStock() - cart.getQuantity()); // 扣减库存
            product.setSold(product.getSold() + cart.getQuantity()); // 增加销量
            // ========== 新增以下代码 ==========
            if (product.getStock() == 0) { // 如果库存变为0
                product.setStatus(3); // 3=已售罄，设置商品状态为已售罄
            }
            // ========== 新增结束 ==========
            productService.updateById(product); // 更新商品信息
        }

        // 8. 生成订单（记录积分抵扣信息）
        String orderNo = UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase(); // 生成20位大写订单号（去掉UUID中的横线并取前20位）
        Order order = new Order(); // 创建订单实体对象
        order.setOrderNo(orderNo); // 设置订单号
        order.setUserId(userId); // 设置买家ID
        order.setTotalAmount(total); // 设置订单总金额
        order.setOriginalAmount(total); // 设置原始金额（与总金额相同）
        order.setPointsDeduct(usePoints); // 设置使用的积分数量
        order.setPlatformFee(platformFee); // 设置平台手续费
        order.setSellerIncome(sellerIncome); // 设置商家实收金额
        order.setStatus(2);        // 订单状态：待发货
        order.setEscrowStatus(2);  // 资金托管状态：已托管
        order.setPayTime(LocalDateTime.now()); // 设置支付时间为当前时间
        order.setCreateTime(LocalDateTime.now()); // 设置创建时间为当前时间
        this.save(order); // 保存订单到数据库

        // 9. 生成订单明细（同原有）
        for (Cart cart : cartList) { // 遍历购物车商品
            Product product = productService.getById(cart.getProductId()); // 查询商品信息
            OrderItem item = new OrderItem(); // 创建订单项实体
            item.setOrderId(order.getId()); // 设置订单ID
            item.setProductId(product.getId()); // 设置商品ID
            item.setProductName(product.getName()); // 设置商品名称
            item.setPrice(product.getPrice()); // 设置商品单价
            item.setQuantity(cart.getQuantity()); // 设置购买数量
            item.setTotal(product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()))); // 计算商品小计
            orderItemService.save(item); // 保存订单项
        }

        // 10. 清空购物车
        cartService.clearCart(userId); // 清空该用户的购物车

        // 11. 增加积分（按实付金额，1元1积分）
        int newPoints = payAmount.intValue(); // 计算新增积分 = 实付金额的整数部分（1元=1积分）
        if (newPoints > 0) { // 如果新增积分大于0
            buyer.setPoints(buyer.getPoints() + newPoints); // 买家积分增加
            userService.updateById(buyer); // 更新买家信息
            pointsRecordService.record(buyer.getId(), newPoints, buyer.getPoints(), "消费得积分", order.getId(), "订单消费得积分"); // 记录积分增加明细
        }

        return orderNo; // 返回订单号
    }
    /**
     * 商家发货（保存物流信息）
     */
    @Transactional // 标记该方法需要在事务中执行
    public void deliverOrder(String orderNo, Long sellerId, String deliveryCompany, String deliveryNo) { // 发货方法，参数为订单号、商家ID、物流公司、物流单号
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); // 根据订单号查询订单
        if (order == null) throw new RuntimeException("订单不存在"); // 如果订单不存在，抛出异常
        Long actualSellerId = getSellerIdByOrder(order.getId()); // 获取订单对应的实际商家ID
        if (!actualSellerId.equals(sellerId)) { // 如果实际商家ID与当前商家ID不一致
            throw new RuntimeException("无权操作此订单"); // 抛出异常
        }
        if (order.getStatus() != 2) throw new RuntimeException("订单状态不是待发货"); // 如果订单状态不是待发货，抛出异常
        
        order.setStatus(3);  // 更新订单状态为待收货
        order.setDeliveryTime(LocalDateTime.now()); // 设置发货时间为当前时间
        order.setAutoConfirmDeadline(LocalDateTime.now().plusDays(7)); // 设置自动确认收货截止时间为当前时间 +7天
        order.setDeliveryCompany(deliveryCompany); // 设置物流公司
        order.setDeliveryNo(deliveryNo); // 设置物流单号
        this.updateById(order); // 更新订单信息
    }

    /**
     * 买家确认收货（资金结算）
     */
    @Transactional // 标记该方法需要在事务中执行
    public void confirmReceive(String orderNo, Long buyerId) { // 确认收货方法，参数为订单号和买家ID
        Order order = this.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); // 根据订单号查询订单
        if (order == null) throw new RuntimeException("订单不存在"); // 如果订单不存在，抛出异常
        if (!order.getUserId().equals(buyerId)) throw new RuntimeException("非本人订单"); // 如果订单买家ID与当前买家ID不一致，抛出异常
        if (order.getStatus() != 3) throw new RuntimeException("订单状态不是待收货"); // 如果订单状态不是待收货，抛出异常
        if (order.getEscrowStatus() != 2) throw new RuntimeException("资金未托管，无法结算"); // 如果资金托管状态不是已托管，抛出异常

        // 1. 将商家实收金额（已扣平台费）转入卖家钱包
        User seller = userService.getById(getSellerIdByOrder(order.getId())); // 根据订单获取卖家信息
        BigDecimal sellerIncome = order.getSellerIncome(); // 获取商家实收金额
        seller.setWallet(seller.getWallet().add(sellerIncome)); // 卖家钱包增加实收金额
        userService.updateById(seller); // 更新卖家信息

        // 2. 记录商家收入流水
        transactionService.record(seller.getId(), sellerIncome, "结算", seller.getWallet(), order.getId(), "订单确认收货"); // 记录商家收入流水

        // 3. 记录平台手续费收入（可选，如果平台有独立账户）
        BigDecimal platformFee = order.getPlatformFee(); // 获取平台手续费
        // 假设平台账户ID为0，记录收入流水
        // transactionService.record(0L, platformFee, "手续费", null, order.getId(), "平台手续费");

        // 4. 更新订单状态
        order.setStatus(4);        // 订单状态：已完成
        order.setEscrowStatus(3);  // 资金托管状态：已结算
        order.setConfirmTime(LocalDateTime.now()); // 设置确认收货时间为当前时间
        this.updateById(order); // 更新订单信息

        // --- 修改开始：更新订单中所有商品的明细状态为“待评价”(2) ---
        List<OrderItem> items = orderItemService.list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())); // 查询该订单的所有订单项
        if (!items.isEmpty()) { // 如果订单项不为空
            for (OrderItem item : items) { // 遍历每个订单项
                item.setStatus(2); // 2 表示待评价，设置订单项状态为待评价
                orderItemService.updateById(item); // 更新订单项
            }
        }
        // --- 修改结束 ---
    }
    /**
     * 买家查询自己的订单列表（分页 + 状态筛选）
     * @param userId 买家ID
     * @param status 订单状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页订单VO
     */
    public Page<OrderVO> getUserOrders(Long userId, Integer status, Integer page, Integer size) { // 获取用户订单列表的方法，参数为买家ID、状态、页码、每页大小
        Page<Order> pageOrder = new Page<>(page, size); // 创建订单分页对象
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Order::getUserId, userId); // 添加条件：买家ID匹配
        if (status != null) { // 如果状态参数不为空
            wrapper.eq(Order::getStatus, status); // 添加条件：订单状态匹配
        }
        wrapper.orderByDesc(Order::getCreateTime); // 按创建时间倒序排序
        Page<Order> orderPage = this.page(pageOrder, wrapper); // 执行分页查询
        
        // 转换为 VO 并填充商品明细
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal()); // 创建订单VO分页对象，复制原分页信息
        List<OrderVO> voList = new ArrayList<>(); // 创建VO列表
        for (Order order : orderPage.getRecords()) { // 遍历订单分页记录
            voList.add(convertToVO(order)); // 将每个订单转换为VO并添加到列表
        }
        voPage.setRecords(voList); // 设置VO列表到分页对象
        return voPage; // 返回VO分页对象
    }

    /**
     * 商家查询自己的订单列表（通过商品关联）
     * @param sellerId 商家ID
     * @param status 订单状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页订单VO
     */
    public Page<OrderVO> getMerchantOrders(Long sellerId, Integer status, Integer page, Integer size) { // 获取商家订单列表的方法，参数为商家ID、状态、页码、每页大小
        // 1. 先查出该商家的所有商品ID
        List<Long> productIds = productService.list( // 查询该商家的所有商品
            new LambdaQueryWrapper<Product>().eq(Product::getUserId, sellerId) // 条件：商品用户ID等于商家ID
        ).stream().map(Product::getId).collect(Collectors.toList()); // 提取商品ID并收集为列表
        
        if (productIds.isEmpty()) { // 如果商品ID列表为空
            Page<OrderVO> emptyPage = new Page<>(page, size, 0); // 创建空分页对象，总记录数为0
            emptyPage.setRecords(new ArrayList<>()); // 设置空列表
            return emptyPage; // 返回空分页
        }
        
        // 2. 查出订单明细中关联这些商品的所有订单ID
        List<Long> orderIds = orderItemService.list( // 查询订单项列表
            new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, productIds) // 条件：商品ID在商品ID列表中
        ).stream().map(OrderItem::getOrderId).distinct().collect(Collectors.toList()); // 提取订单ID并去重
        
        if (orderIds.isEmpty()) { // 如果订单ID列表为空
            Page<OrderVO> emptyPage = new Page<>(page, size, 0); // 创建空分页对象
            emptyPage.setRecords(new ArrayList<>()); // 设置空列表
            return emptyPage; // 返回空分页
        }
        
        // 3. 查询订单并分页
        Page<Order> pageOrder = new Page<>(page, size); // 创建订单分页对象
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.in(Order::getId, orderIds); // 添加条件：订单ID在订单ID列表中
        if (status != null) { // 如果状态参数不为空
            wrapper.eq(Order::getStatus, status); // 添加条件：订单状态匹配
        }
        wrapper.orderByDesc(Order::getCreateTime); // 按创建时间倒序排序
        Page<Order> orderPage = this.page(pageOrder, wrapper); // 执行分页查询
        
        // 转换为 VO
        Page<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal()); // 创建订单VO分页对象
        List<OrderVO> voList = new ArrayList<>(); // 创建VO列表
        for (Order order : orderPage.getRecords()) { // 遍历订单分页记录
            voList.add(convertToVO(order)); // 将每个订单转换为VO并添加到列表
        }
        voPage.setRecords(voList); // 设置VO列表到分页对象
        return voPage; // 返回VO分页对象
    }

    /**
     * 转换 Order 实体为 OrderVO（包含订单商品明细）
     * 访问修饰符已从 private 改为 public，以便 OrderController 调用
     */
    public OrderVO convertToVO(Order order) { // 将订单实体转换为订单VO的方法，参数为订单实体
        OrderVO vo = new OrderVO(); // 创建订单VO对象
        BeanUtils.copyProperties(order, vo); // 将订单实体的属性拷贝到VO对象中

        // 1. 查询订单商品明细
        List<OrderItem> items = orderItemService.list( // 查询该订单的所有订单项
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()) // 条件：订单ID匹配
        );
        if (items.isEmpty()) { // 如果订单项列表为空
            vo.setItems(new ArrayList<>()); // 设置空列表
            return vo; // 返回VO
        }

        // 2. 批量查询该订单中所有商品是否已被评价（一次性查询）
        List<Long> productIds = items.stream() // 从订单项中提取商品ID列表
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
        List<Evaluation> evals = evaluationService.list( // 查询评价列表
            new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, order.getId()) // 条件：订单ID匹配
                .in(Evaluation::getProductId, productIds) // 条件：商品ID在商品ID列表中
        );
        // 构建已评价的 (orderId_productId) 集合
        Set<String> evaluatedKeys = evals.stream() // 从评价列表中构建已评价键集合
                .map(e -> e.getOrderId() + "_" + e.getProductId()) // 格式："订单ID_商品ID"
                .collect(Collectors.toSet());

        // 3. 循环构建 OrderItemVO
        List<OrderItemVO> itemVOs = new ArrayList<>(); // 创建订单项VO列表
        for (OrderItem item : items) { // 遍历每个订单项
            OrderItemVO itemVO = new OrderItemVO(); // 创建订单项VO对象
            BeanUtils.copyProperties(item, itemVO); // 复制属性

            // 补充商品封面图
            Product product = productService.getById(item.getProductId()); // 根据商品ID查询商品信息
            if (product != null) { // 如果商品存在
                itemVO.setProductImage(product.getImageUrl()); // 设置商品封面图
            }

            // 根据集合判断是否已评价
            boolean evaluated = evaluatedKeys.contains(order.getId() + "_" + item.getProductId()); // 判断该订单项是否已评价
            itemVO.setEvaluated(evaluated); // 设置是否已评价

            itemVOs.add(itemVO); // 添加订单项VO到列表
        }
        vo.setItems(itemVOs); // 设置订单项VO列表到订单VO
        // 查询该订单是否有待审核的退货申请（status=1）
        List<Refund> refunds = refundService.list( // 查询退款申请列表
            new LambdaQueryWrapper<Refund>()
                .eq(Refund::getOrderId, order.getId()) // 条件：订单ID匹配
                .eq(Refund::getStatus, 1)   // 状态：待审核
        );
        if (!refunds.isEmpty()) { // 如果存在待审核的退货申请
            vo.setRefundId(refunds.get(0).getId());  // 需要 OrderVO 有 refundId 字段，设置第一个退货申请的ID
        }
        return vo; // 返回订单VO
    }
    /**
     * 根据订单ID获取商家ID（通过订单明细中的商品）
     */
    public Long getSellerIdByOrder(Long orderId) { // 根据订单ID获取商家ID的方法
        List<OrderItem> items = orderItemService.list( // 查询该订单的所有订单项
            new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId) // 条件：订单ID匹配
        );
        if (items.isEmpty()) { // 如果订单项列表为空
            throw new RuntimeException("订单无商品"); // 抛出异常
        }
        Product product = productService.getById(items.get(0).getProductId()); // 获取第一个订单项的商品信息
        return product.getUserId(); // 返回商品所属的用户ID（商家ID）
    }
    /**
     * 买家申请退货（仅限已完成订单，且确认收货后24小时内）
     * @param orderId    订单ID
     * @param orderItemId 订单商品明细ID（支持单个商品退货，也可整单退货）
     * @param userId     买家ID
     * @param reason     退货原因
     * @param images     凭证图片（可选，JSON数组）
     */
    @Transactional // 标记该方法需要在事务中执行
    public void applyRefund(Long orderId, Long orderItemId, Long userId, String reason, List<String> images) { // 申请退货的方法，参数为订单ID、订单项ID、买家ID、退货原因、凭证图片列表
        // 1. 校验订单
        Order order = this.getById(orderId); // 根据订单ID查询订单
        if (order == null || !order.getUserId().equals(userId)) { // 如果订单不存在或订单买家ID与当前用户ID不一致
            throw new RuntimeException("订单不存在或无权操作"); // 抛出异常
        }
        if (order.getStatus() != 4) { // 如果订单状态不是已完成
            throw new RuntimeException("只有已完成订单才能申请退货"); // 抛出异常
        }
        // 2. 校验24小时限制（确认收货时间 + 24小时）
        if (order.getConfirmTime() == null) { // 如果确认收货时间为空
            throw new RuntimeException("订单尚未确认收货"); // 抛出异常
        }
        LocalDateTime deadline = order.getConfirmTime().plusHours(24); // 计算退货截止时间 = 确认收货时间 + 24小时
        if (LocalDateTime.now().isAfter(deadline)) { // 如果当前时间晚于截止时间
            throw new RuntimeException("已超过24小时退货期限"); // 抛出异常
        }
        // 3. 校验订单明细是否存在且未退货
        OrderItem item = orderItemService.getById(orderItemId); // 根据订单项ID查询订单项
        if (item == null || !item.getOrderId().equals(orderId)) { // 如果订单项不存在或不属于该订单
            throw new RuntimeException("订单商品不存在"); // 抛出异常
        }
        if (item.getStatus() == 4) { // 如果订单项状态为已退货（4）
            throw new RuntimeException("该商品已退货过"); // 抛出异常
        }
        // 4. 检查是否已有待审核的退货申请
        long existing = refundService.count(new LambdaQueryWrapper<Refund>() // 统计符合条件的退货申请数量
                .eq(Refund::getOrderId, orderId) // 条件：订单ID匹配
                .eq(Refund::getOrderItemId, orderItemId) // 条件：订单项ID匹配
                .in(Refund::getStatus, 1, 2)); // 状态为待审核(1)或已同意未完成(2)
        if (existing > 0) { // 如果已存在处理中的申请
            throw new RuntimeException("已有退货申请正在处理中"); // 抛出异常
        }
        // 5. 计算退款金额（该商品的实付金额，需考虑积分抵扣比例分摊）
        BigDecimal refundAmount = calculateItemRefundAmount(order, item); // 调用私有方法计算退款金额
        // 6. 创建退款申请记录
        Refund refund = new Refund(); // 创建退款实体对象
        refund.setOrderId(orderId); // 设置订单ID
        refund.setOrderItemId(orderItemId); // 设置订单项ID
        refund.setUserId(userId); // 设置买家ID
        refund.setReason(reason); // 设置退货原因
        refund.setAmount(refundAmount); // 设置退款金额
        refund.setStatus(1); // 设置状态为待审核(1)
        refund.setApplyTime(LocalDateTime.now()); // 设置申请时间为当前时间
        refundService.save(refund); // 保存退款申请
        // 7. 将订单状态改为“退货中”（仅当该订单所有商品都进入退货流程时才改，为简化，直接改订单状态）
        order.setStatus(6); // 设置订单状态为退货中(6)
        this.updateById(order); // 更新订单
        // 可选：更新订单明细状态为“退货中”
        item.setStatus(3); // 设置订单项状态为退货中(3)
        orderItemService.updateById(item); // 更新订单项
    }

    /**
     * 计算单个商品在订单中的实际退款金额（考虑积分抵扣比例）
     * 例如：订单总金额100元，抵扣积分1000（10元），实付90元。某商品单价50元，退款金额 = 50 * (90/100) = 45元
     */
    private BigDecimal calculateItemRefundAmount(Order order, OrderItem item) { // 私有方法：计算单个商品的退款金额，参数为订单实体和订单项实体
        if (order.getPointsDeduct() == null || order.getPointsDeduct() == 0) { // 如果没有使用积分抵扣
            return item.getTotal(); // 直接返回订单项小计（商品原价×数量）
        }
        BigDecimal totalAmount = order.getTotalAmount(); // 获取订单总金额
        BigDecimal payAmount = totalAmount.subtract(BigDecimal.valueOf(order.getPointsDeduct() / 100.0).setScale(2, RoundingMode.HALF_DOWN)); // 计算实付金额 = 总金额 - 抵扣金额
        BigDecimal ratio = payAmount.divide(totalAmount, 4, RoundingMode.HALF_DOWN); // 计算实付金额占总金额的比例，保留4位小数
        return item.getTotal().multiply(ratio).setScale(2, RoundingMode.HALF_DOWN); // 退款金额 = 订单项小计 × 比例，保留2位小数
    }

    /**
     * 商家审核退货申请
     * @param refundId   退货申请ID
     * @param sellerId   商家ID
     * @param approved   true=同意，false=拒绝
     * @param rejectReason 拒绝理由（拒绝时必填）
     */
    @Transactional // 标记该方法需要在事务中执行
    public void auditRefund(Long refundId, Long sellerId, boolean approved, String rejectReason) { // 审核退货申请的方法，参数为退款ID、商家ID、是否通过、拒绝理由
        Refund refund = refundService.getById(refundId); // 根据退款ID查询退款申请
        if (refund == null) throw new RuntimeException("退货申请不存在"); // 如果退款申请不存在，抛出异常
        Order order = this.getById(refund.getOrderId()); // 根据订单ID查询订单
        if (order == null) throw new RuntimeException("订单不存在"); // 如果订单不存在，抛出异常
        // 校验商家是否有权操作（通过商品关联）
        Long actualSellerId = getSellerIdByOrder(order.getId()); // 获取订单对应的实际商家ID
        if (!actualSellerId.equals(sellerId)) { // 如果实际商家ID与当前商家ID不一致
            throw new RuntimeException("无权操作此订单"); // 抛出异常
        }
        if (refund.getStatus() != 1) { // 如果退款申请状态不是待审核(1)
            throw new RuntimeException("该申请已处理过"); // 抛出异常
        }
        if (approved) { // 如果同意退货
            // 同意退货：执行退款逻辑
            // 1. 退款到买家钱包
            User buyer = userService.getById(refund.getUserId()); // 根据退款申请中的用户ID查询买家
            BigDecimal refundAmount = refund.getAmount(); // 获取退款金额
            buyer.setWallet(buyer.getWallet().add(refundAmount)); // 买家钱包增加退款金额
            userService.updateById(buyer); // 更新买家信息
            // 记录交易流水
            transactionService.record(buyer.getId(), refundAmount, "退款", buyer.getWallet(), order.getId(), "订单退货退款"); // 记录退款流水
            // 2. 回滚使用的积分（退回用户积分）
            // 注意：原订单使用了 pointsDeduct 积分，需要按比例退回（简化：全额退回该商品对应的积分）
            // 如果只退部分商品，应计算该商品占用的积分。这里简化：只退回订单整体积分的比例。
            if (order.getPointsDeduct() != null && order.getPointsDeduct() > 0) { // 如果订单使用了积分
                // 按商品金额占订单总金额比例退回积分
                OrderItem item = orderItemService.getById(refund.getOrderItemId()); // 获取订单项
                BigDecimal ratio = item.getTotal().divide(order.getTotalAmount(), 4, RoundingMode.HALF_DOWN); // 计算该订单项总价占订单总金额的比例
                int pointsToReturn = (int) Math.floor(order.getPointsDeduct() * ratio.doubleValue()); // 计算应退回的积分
                if (pointsToReturn > 0) { // 如果应退回积分大于0
                    buyer.setPoints(buyer.getPoints() + pointsToReturn); // 买家积分增加
                    userService.updateById(buyer); // 更新买家信息
                    pointsRecordService.record(buyer.getId(), pointsToReturn, buyer.getPoints(), "退货退回积分", refund.getId(), "退货退回已使用积分"); // 记录积分退回明细
                }
            }
            // 3. 恢复商品库存
            OrderItem item = orderItemService.getById(refund.getOrderItemId()); // 获取订单项
            Product product = productService.getById(item.getProductId()); // 获取商品信息
            product.setStock(product.getStock() + item.getQuantity()); // 恢复库存：原库存 + 退货数量
            productService.updateById(product); // 更新商品信息
            // 4. 更新退货申请状态
            refund.setStatus(2); // 设置退款状态为同意(2)
            refund.setAuditTime(LocalDateTime.now()); // 设置审核时间为当前时间
            refund.setCompleteTime(LocalDateTime.now()); // 设置完成时间为当前时间
            refundService.updateById(refund); // 更新退款申请
            // 5. 更新订单明细状态为“已退货”
            item.setStatus(4); // 设置订单项状态为已退货(4)
            orderItemService.updateById(item); // 更新订单项
            // 6. 如果订单所有商品都已退货，则将订单状态改为“已退款”；否则仍为“退货中”或改回“已完成”
            List<OrderItem> items = orderItemService.list(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())); // 查询该订单的所有订单项
            boolean allRefunded = items.stream().allMatch(i -> i.getStatus() == 4); // 判断是否所有订单项都已退货
            if (allRefunded) { // 如果都已退货
                order.setStatus(7); // 设置订单状态为已退款(7)
                order.setEscrowStatus(3); // 资金托管状态设为已结算（原资金已给商家，这里退款后应调整，但实际已从平台支出，平台可能亏损，此处简化）
            } else { // 还有未退货商品
                // 订单状态改回“已完成”
                order.setStatus(4); // 设置订单状态为已完成(4)
            }
            this.updateById(order); // 更新订单
        } else { // 如果拒绝退货
            // 拒绝退货
            refund.setStatus(3); // 设置退款状态为拒绝(3)
            refund.setAuditTime(LocalDateTime.now()); // 设置审核时间为当前时间
            refund.setRemark(rejectReason); // 需要 Refund 实体有 remark 字段，设置拒绝理由
            refundService.updateById(refund); // 更新退款申请
            // 订单状态改回“已完成”
            order.setStatus(4); // 设置订单状态为已完成(4)
            this.updateById(order); // 更新订单
            // 订单明细状态改回正常（如果之前改为退货中）
            OrderItem item = orderItemService.getById(refund.getOrderItemId()); // 获取订单项
            if (item.getStatus() == 3) { // 如果订单项状态为退货中(3)
                item.setStatus(1); // 设置回正常状态(1)
                orderItemService.updateById(item); // 更新订单项
            }
        }
    }
}
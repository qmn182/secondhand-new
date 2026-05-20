// service/EvaluationService.java
package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Evaluation;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.entity.vo.EvaluationVO;
import com.example.demo.mapper.EvaluationMapper;
import com.example.demo.service.OrderItemService;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EvaluationService extends ServiceImpl<EvaluationMapper, Evaluation> {

    @Autowired
    @Lazy 
    private OrderService orderService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserService userService;



    /**
     * 评价商品（买家）
     * @param orderId    订单ID
     * @param productId  商品ID
     * @param userId     买家ID
     * @param rating     星级 1-5
     * @param comment    评价内容
     * @param images     图片列表（可选）
     */
    @Transactional
    public void evaluateProduct(Long orderId, Long productId, Long userId, Integer rating, String comment, List<String> images) {
        // 1. 校验订单属于该用户且已完成
        Order order = orderService.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        if (order.getStatus() != 4) {
            throw new RuntimeException("只有已完成订单才能评价");
        }
        // 2. 校验该商品是否在订单中
        OrderItem item = orderItemService.getOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getProductId, productId));
        if (item == null) {
            throw new RuntimeException("订单中不含该商品");
        }
        // 3. 检查是否已评价（通过 evaluation 表查询）
        long count = this.count(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderId, orderId)
                .eq(Evaluation::getProductId, productId));
        if (count > 0) {
            throw new RuntimeException("该商品已评价过");
        }
        // 4. 保存评价
        Evaluation eval = new Evaluation();
        eval.setOrderId(orderId);
        eval.setProductId(productId);
        eval.setUserId(userId);
        eval.setRating(rating);
        eval.setComment(comment);
        if (images != null && !images.isEmpty()) {
            eval.setImages(com.alibaba.fastjson2.JSON.toJSONString(images));
        }
        this.save(eval);
        // 5. 更新订单明细状态为“已评价”
        item.setStatus(2);
        orderItemService.updateById(item);
        // 6. 更新商品的好评率（可选，后续用于排序）
        updateProductRating(productId);

    }

    /**
     * 商家回复评价
     */
    @Transactional
    public void replyEvaluation(Long evalId, Long sellerId, String reply) {
        Evaluation eval = this.getById(evalId);
        if (eval == null) throw new RuntimeException("评价不存在");
        Product product = productService.getById(eval.getProductId());
        if (product == null || !product.getUserId().equals(sellerId)) {
            throw new RuntimeException("无权回复此评价");
        }
        eval.setReply(reply);
        this.updateById(eval);
    }

    /**
     * 获取商品评价列表（分页）
     */
    public Page<EvaluationVO> getProductEvaluations(Long productId, Integer page, Integer size, String sort) {
        Page<Evaluation> pageEval = new Page<>(page, size);
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Evaluation::getProductId, productId);
        if ("rating_desc".equals(sort)) {
            wrapper.orderByDesc(Evaluation::getRating);
        } else {
            wrapper.orderByDesc(Evaluation::getCreateTime);
        }
        Page<Evaluation> evalPage = this.page(pageEval, wrapper);
        
        Page<EvaluationVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), evalPage.getTotal());
        List<EvaluationVO> voList = new ArrayList<>();
        for (Evaluation eval : evalPage.getRecords()) {
            EvaluationVO vo = new EvaluationVO();
            BeanUtils.copyProperties(eval, vo);
            // 转换图片 JSON 为 List
            if (eval.getImages() != null && !eval.getImages().isEmpty()) {
                vo.setImages(com.alibaba.fastjson2.JSON.parseArray(eval.getImages(), String.class));
            }
            // 补充商品名称、图片（冗余存储，也可从商品表查）
            Product product = productService.getById(productId);
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getImageUrl());
            }
            // 评价人用户名（脱敏可选）
            User user = userService.getById(eval.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 更新商品的好评率（rating 平均值）
     */
    private void updateProductRating(Long productId) {
        List<Evaluation> evals = this.list(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getProductId, productId));
        if (evals.isEmpty()) {
            return;
        }
        double avg = evals.stream().mapToInt(Evaluation::getRating).average().orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        Product product = productService.getById(productId);
        if (product != null) {
            product.setAvgRating(avgRating.doubleValue());
            productService.updateById(product);
        }
    }
}
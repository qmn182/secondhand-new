// controller/EvaluationController.java
package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.service.EvaluationService;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;
import com.example.demo.entity.BuyerEvaluation;
import com.example.demo.service.BuyerEvaluationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.vo.EvaluationVO;

@RestController
@RequestMapping("/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private BuyerEvaluationService buyerEvaluationService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;

    /**
     * 评价商品
     */
    @PostMapping("/product")
    public Result evaluateProduct(@RequestParam Long orderId,
                                  @RequestParam Long productId,
                                  @RequestParam Integer rating,
                                  @RequestParam(required = false) String comment,
                                  @RequestParam(required = false) List<String> images,
                                  HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.fail("请先登录");
        if (rating < 1 || rating > 5) return Result.fail("评分必须是1-5");
        try {
            evaluationService.evaluateProduct(orderId, productId, user.getId(), rating, comment, images);
            return Result.success("评价成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 商家回复评价
     */
    @PostMapping("/reply")
    public Result replyEvaluation(@RequestParam Long evalId,
                                  @RequestParam String reply,
                                  HttpSession session) {
        User seller = (User) session.getAttribute("user");
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        try {
            evaluationService.replyEvaluation(evalId, seller.getId(), reply);
            return Result.success("回复成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取商品评价列表（公开）
     */
    @GetMapping("/product/list")
    public Result getProductEvaluations(@RequestParam Long productId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false, defaultValue = "time") String sort) {
        // sort: time(最新) / rating_desc(好评优先)
        Page<EvaluationVO> voPage = evaluationService.getProductEvaluations(productId, page, size, sort);
        return Result.success(voPage);
    }


    /**
     * 商家评价买家（订单完成后）
     */
    @PostMapping("/buyer")
    public Result evaluateBuyer(@RequestParam Long orderId,
                                @RequestParam Long buyerId,
                                @RequestParam Integer rating,
                                @RequestParam(required = false) String comment,
                                HttpSession session) {
        User seller = (User) session.getAttribute("user");
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        // 1. 校验订单属于该商家，且已完成
        Order order = orderService.getById(orderId);
        if (order == null || order.getStatus() != 4) {
            return Result.fail("订单不存在或未完成");
        }
        // 校验订单中的商品是否属于该商家
        Long actualSellerId = orderService.getSellerIdByOrder(order.getId());
        if (!actualSellerId.equals(seller.getId())) {
            return Result.fail("无权操作此订单");
        }
        // 2. 检查是否已评价过该买家
        long count = buyerEvaluationService.count(new LambdaQueryWrapper<BuyerEvaluation>()
                .eq(BuyerEvaluation::getOrderId, orderId)
                .eq(BuyerEvaluation::getBuyerId, buyerId));
        if (count > 0) {
            return Result.fail("该订单的买家已评价过");
        }
        // 3. 保存评价
        BuyerEvaluation eval = new BuyerEvaluation();
        eval.setOrderId(orderId);
        eval.setBuyerId(buyerId);
        eval.setSellerId(seller.getId());
        eval.setRating(rating);
        eval.setComment(comment);
        buyerEvaluationService.save(eval);
        // 4. 更新买家的平均评分
        updateBuyerRating(buyerId);
        return Result.success("评价成功");
    }

    private void updateBuyerRating(Long buyerId) {
        List<BuyerEvaluation> evals = buyerEvaluationService.list(
                new LambdaQueryWrapper<BuyerEvaluation>().eq(BuyerEvaluation::getBuyerId, buyerId));
        if (evals.isEmpty()) return;
        double avg = evals.stream().mapToInt(BuyerEvaluation::getRating).average().orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        User buyer = userService.getById(buyerId);
        if (buyer != null) {
            buyer.setBuyerRating(avgRating.doubleValue());
            userService.updateById(buyer);
        }
    }

    @GetMapping("/user/buyer-evaluations")
    public Result getMyEvaluations(HttpSession session) {
        User buyer = (User) session.getAttribute("user");
        if (buyer == null) return Result.fail("请先登录");
        List<BuyerEvaluation> evals = buyerEvaluationService.list(
                new LambdaQueryWrapper<BuyerEvaluation>().eq(BuyerEvaluation::getBuyerId, buyer.getId())
                        .orderByDesc(BuyerEvaluation::getCreateTime));
        return Result.success(evals);
    }
}
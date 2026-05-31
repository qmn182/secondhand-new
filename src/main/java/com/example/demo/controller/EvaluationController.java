// controller/EvaluationController.java
package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.Order; // 导入订单实体类
import com.example.demo.entity.User; // 导入用户实体类
import com.example.demo.service.EvaluationService; // 导入商品评价服务类
import com.example.demo.service.OrderService; // 导入订单服务类
import com.example.demo.service.UserService; // 导入用户服务类

import jakarta.servlet.http.HttpSession; // 导入 HttpSession 用于获取会话信息
import com.example.demo.entity.BuyerEvaluation; // 导入买家评价实体类
import com.example.demo.service.BuyerEvaluationService; // 导入买家评价服务类
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 条件构造器
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解

import java.math.BigDecimal; // 导入高精度小数类
import java.math.RoundingMode; // 导入小数舍入模式枚举
import java.util.List; // 导入 List 集合接口
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.example.demo.entity.vo.EvaluationVO; // 导入商品评价 VO 类

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/evaluation") // 定义该类下所有接口的公共前缀为 /evaluation
public class EvaluationController { // 定义评价控制器类

    @Autowired // 自动装配 EvaluationService 实例
    private EvaluationService evaluationService; // 商品评价服务对象
    @Autowired // 自动装配 BuyerEvaluationService 实例
    private BuyerEvaluationService buyerEvaluationService; // 买家评价服务对象
    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象
    @Autowired // 自动装配 OrderService 实例
    private OrderService orderService; // 订单服务对象

    /**
     * 评价商品
     */
    // --- 修改开始：orderId 改为非必传 ---
    @PostMapping("/product") // 处理 POST 请求，路径为 /evaluation/product
    public Result evaluateProduct(@RequestParam(required = false) Long orderId, // 接收请求参数 orderId，非必传
    // --- 修改结束 ---
                                  @RequestParam Long productId, // 接收请求参数 productId，商品ID
                                  @RequestParam Integer rating, // 接收请求参数 rating，评分（1-5）
                                  @RequestParam(required = false) String comment, // 接收请求参数 comment，评价内容，非必传
                                  @RequestParam(required = false) List<String> images, // 接收请求参数 images，评价图片列表，非必传
                                  HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) return Result.fail("请先登录"); // 如果用户未登录，返回失败结果
        if (rating < 1 || rating > 5) return Result.fail("评分必须是1-5"); // 如果评分不在1-5范围内，返回失败结果

        // --- 修改开始：如果未传 orderId，则自动查找一个可评价订单 ---
        if (orderId == null) { // 如果订单ID参数为 null
            Order order = orderService.getOne(new LambdaQueryWrapper<Order>() // 调用订单服务查询一条符合条件的订单
                    .eq(Order::getUserId, user.getId()) // 添加条件：订单用户ID等于当前用户ID
                    .eq(Order::getStatus, 4) // 状态为“已完成”
                    .orderByDesc(Order::getCreateTime) // 按创建时间倒序排序
                    .last("LIMIT 1")); // 限制只取一条
            if (order == null) { // 如果未找到可评价的订单
                return Result.fail("未找到可评价的订单"); // 返回失败结果
            }
            orderId = order.getId(); // 将查到的订单ID赋值给 orderId
        }
        // --- 修改结束 ---

        try { // 开始 try 块
            evaluationService.evaluateProduct(orderId, productId, user.getId(), rating, comment, images); // 调用评价服务执行商品评价
            return Result.success("评价成功"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果，异常信息作为提示
        }
    }

    /**
     * 商家回复评价
     */
    @PostMapping("/reply") // 处理 POST 请求，路径为 /evaluation/reply
    public Result replyEvaluation(@RequestParam Long evalId, // 接收请求参数 evalId，评价ID
                                  @RequestParam String reply, // 接收请求参数 reply，回复内容
                                  HttpSession session) { // HttpSession 参数
        User seller = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (seller == null || seller.getRole() != 2) { // 如果用户未登录或角色不是商家(role=2)
            return Result.fail("请登录商家账号"); // 返回失败结果，提示需要登录商家账号
        }
        try { // 开始 try 块
            evaluationService.replyEvaluation(evalId, seller.getId(), reply); // 调用评价服务执行回复
            return Result.success("回复成功"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果
        }
    }

    /**
     * 获取商品评价列表（公开）
     */
    @GetMapping("/product/list") // 处理 GET 请求，路径为 /evaluation/product/list
    public Result getProductEvaluations(@RequestParam Long productId, // 接收请求参数 productId，商品ID
                                        @RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                                        @RequestParam(defaultValue = "10") Integer size, // 接收每页大小，默认10
                                        @RequestParam(required = false, defaultValue = "time") String sort) { // 接收排序方式，默认 time（最新）
        // sort: time(最新) / rating_desc(好评优先)
        Page<EvaluationVO> voPage = evaluationService.getProductEvaluations(productId, page, size, sort); // 调用评价服务获取商品评价分页数据
        return Result.success(voPage); // 返回成功结果，包含 VO 分页对象
    }


    /**
     * 商家评价买家（订单完成后）
     */
    @PostMapping("/buyer") // 处理 POST 请求，路径为 /evaluation/buyer
    public Result evaluateBuyer(@RequestParam Long orderId, // 接收请求参数 orderId，订单ID
                                @RequestParam Long buyerId, // 接收请求参数 buyerId，买家ID
                                @RequestParam Integer rating, // 接收请求参数 rating，评分（1-5）
                                @RequestParam(required = false) String comment, // 接收请求参数 comment，评价内容，非必传
                                HttpSession session) { // HttpSession 参数
        User seller = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (seller == null || seller.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("请登录商家账号"); // 返回失败结果
        }
        // 1. 校验订单属于该商家，且已完成
        Order order = orderService.getById(orderId); // 根据订单ID查询订单信息
        if (order == null || order.getStatus() != 4) { // 如果订单不存在或状态不是已完成
            return Result.fail("订单不存在或未完成"); // 返回失败结果
        }
        // 校验订单中的商品是否属于该商家
        Long actualSellerId = orderService.getSellerIdByOrder(order.getId()); // 调用订单服务获取订单对应的商家ID
        if (!actualSellerId.equals(seller.getId())) { // 如果订单的商家ID与当前登录商家ID不一致
            return Result.fail("无权操作此订单"); // 返回失败结果
        }
        // 2. 检查是否已评价过该买家
        long count = buyerEvaluationService.count(new LambdaQueryWrapper<BuyerEvaluation>() // 统计符合条件的买家评价记录数
                .eq(BuyerEvaluation::getOrderId, orderId) // 添加条件：订单ID等于当前订单ID
                .eq(BuyerEvaluation::getBuyerId, buyerId)); // 添加条件：买家ID等于传入的买家ID
        if (count > 0) { // 如果已经存在评价记录
            return Result.fail("该订单的买家已评价过"); // 返回失败结果
        }
        // 3. 保存评价
        BuyerEvaluation eval = new BuyerEvaluation(); // 创建 BuyerEvaluation 实体对象
        eval.setOrderId(orderId); // 设置订单ID
        eval.setBuyerId(buyerId); // 设置买家ID
        eval.setSellerId(seller.getId()); // 设置商家ID
        eval.setRating(rating); // 设置评分
        eval.setComment(comment); // 设置评价内容
        buyerEvaluationService.save(eval); // 保存买家评价
        // 4. 更新买家的平均评分
        updateBuyerRating(buyerId); // 调用私有方法更新买家的平均评分
        return Result.success("评价成功"); // 返回成功结果
    }

    private void updateBuyerRating(Long buyerId) { // 私有方法：更新买家的平均评分，参数为买家ID
        List<BuyerEvaluation> evals = buyerEvaluationService.list( // 查询该买家的所有评价记录
                new LambdaQueryWrapper<BuyerEvaluation>().eq(BuyerEvaluation::getBuyerId, buyerId)); // 条件：买家ID匹配
        if (evals.isEmpty()) return; // 如果没有评价记录，直接返回
        double avg = evals.stream().mapToInt(BuyerEvaluation::getRating).average().orElse(0.0); // 计算评分的平均值，若无则默认0.0
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP); // 将平均值转换为 BigDecimal，保留1位小数，四舍五入
        User buyer = userService.getById(buyerId); // 根据买家ID查询用户信息
        if (buyer != null) { // 如果买家存在
            buyer.setBuyerRating(avgRating.doubleValue()); // 设置买家的平均评分属性
            userService.updateById(buyer); // 更新用户信息
        }
    }

    @GetMapping("/user/buyer-evaluations") // 处理 GET 请求，路径为 /evaluation/user/buyer-evaluations
    public Result getMyEvaluations(HttpSession session) { // 获取当前用户的买家评价列表，参数为 HttpSession
        User buyer = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (buyer == null) return Result.fail("请先登录"); // 如果用户未登录，返回失败结果
        List<BuyerEvaluation> evals = buyerEvaluationService.list( // 查询当前买家收到的评价列表
                new LambdaQueryWrapper<BuyerEvaluation>().eq(BuyerEvaluation::getBuyerId, buyer.getId()) // 条件：买家ID匹配
                        .orderByDesc(BuyerEvaluation::getCreateTime)); // 按创建时间倒序排序
        return Result.success(evals); // 返回成功结果，包含评价列表
    }
}
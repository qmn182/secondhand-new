// service/EvaluationService.java
package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.Evaluation; // 导入商品评价实体类 Evaluation
import com.example.demo.entity.Order; // 导入订单实体类 Order
import com.example.demo.entity.OrderItem; // 导入订单项实体类 OrderItem
import com.example.demo.entity.Product; // 导入商品实体类 Product
import com.example.demo.entity.User; // 导入用户实体类 User
import com.example.demo.entity.vo.EvaluationVO; // 导入商品评价视图对象类 EvaluationVO
import com.example.demo.mapper.EvaluationMapper; // 导入评价 Mapper 接口
import com.example.demo.service.OrderItemService; // 导入订单项服务类
import com.example.demo.service.OrderService; // 导入订单服务类
import com.example.demo.service.ProductService; // 导入商品服务类
import com.example.demo.service.UserService; // 导入用户服务类
import org.springframework.context.annotation.Lazy; // 导入 Spring 懒加载注解
import org.springframework.beans.BeanUtils; // 导入 BeanUtils 用于属性拷贝
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.stereotype.Service; // 导入 Spring 服务层注解
import org.springframework.transaction.annotation.Transactional; // 导入 Spring 事务注解

import java.math.BigDecimal; // 导入 BigDecimal 高精度小数类
import java.math.RoundingMode; // 导入小数舍入模式枚举
import java.util.ArrayList; // 导入 ArrayList 集合类
import java.util.Arrays; // 导入 Arrays 工具类
import java.util.List; // 导入 List 集合接口
import java.util.stream.Collectors; // 导入 Collectors 用于流式收集

@Service // 标记该类为 Spring 的服务层组件
public class EvaluationService extends ServiceImpl<EvaluationMapper, Evaluation> { // 定义商品评价服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    @Autowired // 自动装配 OrderService 实例
    @Lazy // 懒加载注解，避免循环依赖
    private OrderService orderService; // 订单服务对象
    @Autowired // 自动装配 OrderItemService 实例
    private OrderItemService orderItemService; // 订单项服务对象
    @Autowired // 自动装配 ProductService 实例
    private ProductService productService; // 商品服务对象
    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象



    /**
     * 评价商品（买家）
     * @param orderId    订单ID
     * @param productId  商品ID
     * @param userId     买家ID
     * @param rating     星级 1-5
     * @param comment    评价内容
     * @param images     图片列表（可选）
     */
    @Transactional // 标记该方法需要在事务中执行
    public void evaluateProduct(Long orderId, Long productId, Long userId, Integer rating, String comment, List<String> images) { // 评价商品的方法，参数为订单ID、商品ID、用户ID、评分、评论内容、图片列表
        // 1. 校验订单属于该用户且已完成
        Order order = orderService.getById(orderId); // 根据订单ID查询订单
        if (order == null || !order.getUserId().equals(userId)) { // 如果订单不存在或订单买家ID与当前用户ID不一致
            throw new RuntimeException("订单不存在或无权操作"); // 抛出运行时异常
        }
        if (order.getStatus() != 4) { // 如果订单状态不是已完成(4)
            throw new RuntimeException("只有已完成订单才能评价"); // 抛出运行时异常
        }
        // 2. 校验该商品是否在订单中
        OrderItem item = orderItemService.getOne(new LambdaQueryWrapper<OrderItem>() // 查询订单项
                .eq(OrderItem::getOrderId, orderId) // 条件：订单ID匹配
                .eq(OrderItem::getProductId, productId)); // 条件：商品ID匹配
        if (item == null) { // 如果订单项不存在
            throw new RuntimeException("订单中不含该商品"); // 抛出运行时异常
        }
        // 3. 检查是否已评价（通过 evaluation 表查询）
        long count = this.count(new LambdaQueryWrapper<Evaluation>() // 统计评价记录数
                .eq(Evaluation::getOrderId, orderId) // 条件：订单ID匹配
                .eq(Evaluation::getProductId, productId)); // 条件：商品ID匹配
        if (count > 0) { // 如果已存在评价
            throw new RuntimeException("该商品已评价过"); // 抛出运行时异常
        }
        // 4. 保存评价
        Evaluation eval = new Evaluation(); // 创建评价实体对象
        eval.setOrderId(orderId); // 设置订单ID
        eval.setProductId(productId); // 设置商品ID
        eval.setUserId(userId); // 设置用户ID
        eval.setRating(rating); // 设置评分
        eval.setComment(comment); // 设置评论内容
        if (images != null && !images.isEmpty()) { // 如果图片列表不为空
            eval.setImages(com.alibaba.fastjson2.JSON.toJSONString(images)); // 将图片列表转为 JSON 字符串存储
        }
        this.save(eval); // 保存评价
        // 5. 更新订单明细状态为“已评价”
        item.setStatus(2); // 设置订单项状态为已评价(2)
        orderItemService.updateById(item); // 更新订单项
        // 6. 更新商品的好评率（可选，后续用于排序）
        updateProductRating(productId); // 调用私有方法更新商品平均评分

    }

    /**
     * 商家回复评价
     */
    @Transactional // 标记该方法需要在事务中执行
    public void replyEvaluation(Long evalId, Long sellerId, String reply) { // 回复评价的方法，参数为评价ID、商家ID、回复内容
        Evaluation eval = this.getById(evalId); // 根据评价ID查询评价
        if (eval == null) throw new RuntimeException("评价不存在"); // 如果评价不存在，抛出异常
        Product product = productService.getById(eval.getProductId()); // 根据评价中的商品ID查询商品
        if (product == null || !product.getUserId().equals(sellerId)) { // 如果商品不存在或商品所属商家不是当前商家
            throw new RuntimeException("无权回复此评价"); // 抛出异常
        }
        eval.setReply(reply); // 设置回复内容
        this.updateById(eval); // 更新评价
    }

    /**
     * 获取商品评价列表（分页）
     */
    public Page<EvaluationVO> getProductEvaluations(Long productId, Integer page, Integer size, String sort) { // 获取商品评价列表的方法，参数为商品ID、页码、每页大小、排序方式
        Page<Evaluation> pageEval = new Page<>(page, size); // 创建评价分页对象
        LambdaQueryWrapper<Evaluation> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Evaluation::getProductId, productId); // 添加条件：商品ID匹配
        if ("rating_desc".equals(sort)) { // 如果排序方式为评分降序
            wrapper.orderByDesc(Evaluation::getRating); // 按评分降序排列
        } else { // 否则（包括默认情况）
            wrapper.orderByDesc(Evaluation::getCreateTime); // 按创建时间倒序排列
        }
        Page<Evaluation> evalPage = this.page(pageEval, wrapper); // 执行分页查询
        
        Page<EvaluationVO> voPage = new Page<>(evalPage.getCurrent(), evalPage.getSize(), evalPage.getTotal()); // 创建评价VO分页对象，复制原分页信息
        List<EvaluationVO> voList = new ArrayList<>(); // 创建VO列表
        for (Evaluation eval : evalPage.getRecords()) { // 遍历评价记录
            EvaluationVO vo = new EvaluationVO(); // 创建评价VO对象
            BeanUtils.copyProperties(eval, vo); // 将评价实体的属性拷贝到VO对象
            // 转换图片 JSON 为 List
            if (eval.getImages() != null && !eval.getImages().isEmpty()) { // 如果图片JSON字段不为空
                vo.setImages(com.alibaba.fastjson2.JSON.parseArray(eval.getImages(), String.class)); // 将JSON字符串解析为List<String>
            }
            // 补充商品名称、图片（冗余存储，也可从商品表查）
            Product product = productService.getById(productId); // 根据商品ID查询商品
            if (product != null) { // 如果商品存在
                vo.setProductName(product.getName()); // 设置商品名称
                vo.setProductImage(product.getImageUrl()); // 设置商品图片URL
            }
            // 评价人用户名（脱敏可选）
            User user = userService.getById(eval.getUserId()); // 根据用户ID查询用户
            if (user != null) { // 如果用户存在
                vo.setUsername(user.getUsername()); // 设置用户名
            }
            voList.add(vo); // 添加VO到列表
        }
        voPage.setRecords(voList); // 设置VO列表到分页对象
        return voPage; // 返回VO分页对象
    }

    /**
     * 更新商品的好评率（rating 平均值）
     */
    private void updateProductRating(Long productId) { // 私有方法：更新商品平均评分，参数为商品ID
        List<Evaluation> evals = this.list(new LambdaQueryWrapper<Evaluation>() // 查询该商品的所有评价
                .eq(Evaluation::getProductId, productId)); // 条件：商品ID匹配
        if (evals.isEmpty()) { // 如果没有评价记录
            return; // 直接返回
        }
        double avg = evals.stream().mapToInt(Evaluation::getRating).average().orElse(0.0); // 计算评分的平均值，若无则默认0.0
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP); // 将平均值转换为BigDecimal，保留1位小数，四舍五入
        Product product = productService.getById(productId); // 根据商品ID查询商品
        if (product != null) { // 如果商品存在
            product.setAvgRating(avgRating.doubleValue()); // 设置商品平均评分
            productService.updateById(product); // 更新商品信息
        }
    }
}
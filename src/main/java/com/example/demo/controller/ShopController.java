package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 条件构造器
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.*; // 导入所有实体类（通配符）
import com.example.demo.entity.vo.ShopInfoVO; // 导入店铺信息视图对象类
import com.example.demo.entity.vo.ShopProductVO; // 导入店铺商品视图对象类
import com.example.demo.service.*; // 导入所有服务类（通配符）
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解

import java.math.BigDecimal; // 导入高精度小数类
import java.math.RoundingMode; // 导入小数舍入模式枚举
import java.util.List; // 导入 List 集合接口
import java.util.stream.Collectors; // 导入 Collectors 用于流式收集

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/shop") // 定义该类下所有接口的公共前缀为 /shop
public class ShopController { // 定义店铺控制器类

    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象
    @Autowired // 自动装配 ProductService 实例
    private ProductService productService; // 商品服务对象
    @Autowired // 自动装配 OrderItemService 实例
    private OrderItemService orderItemService; // 订单项服务对象
    @Autowired // 自动装配 EvaluationService 实例
    private EvaluationService evaluationService; // 商品评价服务对象
    @Autowired // 自动装配 MerchantEvaluationService 实例
    private MerchantEvaluationService merchantEvaluationService; // 商家评价服务对象

    /**
     * 获取店铺基本信息
     * GET /shop/{sellerId}/info
     */
    @GetMapping("/{sellerId}/info") // 处理 GET 请求，路径为 /shop/{sellerId}/info
    public Result getShopInfo(@PathVariable Long sellerId) { // 获取店铺信息方法，接收商家ID路径变量
        User seller = userService.getById(sellerId); // 根据商家ID查询用户信息
        if (seller == null || seller.getRole() != 2) { // 如果商家不存在或角色不是商家(role=2)
            return Result.fail("商家不存在"); // 返回失败结果
        }
        ShopInfoVO info = new ShopInfoVO(); // 创建店铺信息 VO 对象
        info.setSellerId(sellerId); // 设置商家ID
        info.setShopName(seller.getShopName()); // 设置店铺名称
        info.setLevel(seller.getLevel() == null ? 1 : seller.getLevel()); // 设置店铺等级，若为null则默认为1
        info.setCreateTime(seller.getCreateTime()); // 设置创建时间

        // 1. 总销量：该商家所有商品的销量之和（sold字段）
        Long totalSold = productService.list(new LambdaQueryWrapper<Product>() // 查询该商家的所有商品
                .eq(Product::getUserId, sellerId)) // 添加条件：商品用户ID等于商家ID
                .stream().mapToLong(Product::getSold).sum(); // 流式处理，提取销量字段并求和
        info.setTotalSold(totalSold); // 设置总销量

        // 2. 店铺平均评分（商品评价 + 商家服务评价）
        // 2.1 获取该商家所有商品ID
        List<Long> productIds = productService.list(new LambdaQueryWrapper<Product>() // 查询该商家的所有商品
                .eq(Product::getUserId, sellerId)) // 添加条件：商品用户ID等于商家ID
                .stream().map(Product::getId).collect(Collectors.toList()); // 提取商品ID并收集为列表

        double avgProductRating = 0.0; // 初始化商品平均评分
        long productReviewCount = 0; // 初始化商品评价数量
        if (!productIds.isEmpty()) { // 如果商品ID列表不为空
            List<Evaluation> productReviews = evaluationService.list(new LambdaQueryWrapper<Evaluation>() // 查询所有商品评价
                    .in(Evaluation::getProductId, productIds)); // 添加条件：商品ID在商品ID列表中
            productReviewCount = productReviews.size(); // 获取商品评价数量
            avgProductRating = productReviews.stream() // 流式处理评价列表
                    .mapToInt(Evaluation::getRating) // 提取评分值
                    .average().orElse(0.0); // 计算平均值，若无则默认0.0
        }

        // 2.2 商家服务评价
        List<MerchantEvaluation> merchantReviews = merchantEvaluationService.list(new LambdaQueryWrapper<MerchantEvaluation>() // 查询商家服务评价
                .eq(MerchantEvaluation::getSellerId, sellerId)); // 添加条件：商家ID匹配
        long merchantReviewCount = merchantReviews.size(); // 获取商家评价数量
        double avgServiceRating = merchantReviews.stream() // 流式处理商家评价列表
                .mapToInt(MerchantEvaluation::getServiceRating) // 提取服务评分值
                .average().orElse(0.0); // 计算平均值，若无则默认0.0

        long totalReviews = productReviewCount + merchantReviewCount; // 总评价数 = 商品评价数 + 商家服务评价数
        double totalScore = avgProductRating * productReviewCount + avgServiceRating * merchantReviewCount; // 总分数 = 商品平均分×商品评价数 + 服务平均分×服务评价数
        double avgRating = totalReviews == 0 ? 0.0 : totalScore / totalReviews; // 计算综合平均分，若无评价则为0.0
        // 保留一位小数
        avgRating = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP).doubleValue(); // 转换为BigDecimal，保留1位小数，四舍五入

        info.setAvgRating(avgRating); // 设置平均评分
        info.setTotalReviews(totalReviews); // 设置总评价数

        return Result.success(info); // 返回成功结果，包含店铺信息 VO
    }

    /**
     * 获取店铺商品列表（仅上架商品）
     * GET /shop/{sellerId}/products?page=1&size=12&sort=sold_desc
     * sort可选：time_desc(默认), price_asc, price_desc, sold_desc
     */
    @GetMapping("/{sellerId}/products") // 处理 GET 请求，路径为 /shop/{sellerId}/products
    public Result getShopProducts(@PathVariable Long sellerId, // 接收商家ID路径变量
                                  @RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                                  @RequestParam(defaultValue = "12") Integer size, // 接收每页大小，默认12
                                  @RequestParam(required = false, defaultValue = "time_desc") String sort) { // 接收排序方式，默认 time_desc
        // 校验商家是否存在
        User seller = userService.getById(sellerId); // 根据商家ID查询用户信息
        if (seller == null || seller.getRole() != 2) { // 如果商家不存在或角色不是商家
            return Result.fail("商家不存在"); // 返回失败结果
        }
        Page<Product> pageObj = new Page<>(page, size); // 创建分页对象
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Product::getUserId, sellerId) // 添加条件：商品用户ID等于商家ID
               .eq(Product::getStatus, 1); // 添加条件：商品状态为已上架(1)

        // 排序处理
        switch (sort) { // 根据排序方式参数进行分支处理
            case "price_asc": // 如果排序方式为价格升序
                wrapper.orderByAsc(Product::getPrice); // 按价格升序排列
                break; // 跳出 switch
            case "price_desc": // 如果排序方式为价格降序
                wrapper.orderByDesc(Product::getPrice); // 按价格降序排列
                break; // 跳出 switch
            case "sold_desc": // 如果排序方式为销量降序
                wrapper.orderByDesc(Product::getSold); // 按销量降序排列
                break; // 跳出 switch
            default: // 默认情况（包括 time_desc）
                wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序排列
                break; // 跳出 switch
        }

        Page<Product> productPage = productService.page(pageObj, wrapper); // 执行分页查询
        // 转换为 VO
        Page<ShopProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal()); // 创建分页 VO 对象，复制原分页信息
        List<ShopProductVO> voList = productPage.getRecords().stream().map(p -> { // 遍历商品记录，转换为 VO 对象
            ShopProductVO vo = new ShopProductVO(); // 创建 ShopProductVO 实例
            vo.setId(p.getId()); // 设置商品ID
            vo.setName(p.getName()); // 设置商品名称
            vo.setImageUrl(p.getImageUrl()); // 设置商品图片URL
            vo.setPrice(p.getPrice()); // 设置商品价格
            vo.setSold(p.getSold() == null ? 0 : p.getSold()); // 设置销量，若为null则设为0
            vo.setNegotiable(p.getNegotiable()); // 设置是否可议价
            vo.setCondition(p.getCondition()); // 设置商品成色
            return vo; // 返回 VO 对象
        }).collect(Collectors.toList()); // 收集为 List
        voPage.setRecords(voList); // 将 VO 列表设置到分页对象中
        return Result.success(voPage); // 返回成功结果，包含 VO 分页对象
    }
}
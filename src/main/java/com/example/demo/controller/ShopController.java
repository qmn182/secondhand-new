package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.*;
import com.example.demo.entity.vo.ShopInfoVO;
import com.example.demo.entity.vo.ShopProductVO;
import com.example.demo.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderItemService orderItemService;
    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private MerchantEvaluationService merchantEvaluationService;

    /**
     * 获取店铺基本信息
     * GET /shop/{sellerId}/info
     */
    @GetMapping("/{sellerId}/info")
    public Result getShopInfo(@PathVariable Long sellerId) {
        User seller = userService.getById(sellerId);
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("商家不存在");
        }
        ShopInfoVO info = new ShopInfoVO();
        info.setSellerId(sellerId);
        info.setShopName(seller.getShopName());
        info.setLevel(seller.getLevel() == null ? 1 : seller.getLevel());
        info.setCreateTime(seller.getCreateTime());

        // 1. 总销量：该商家所有商品的销量之和（sold字段）
        Long totalSold = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, sellerId))
                .stream().mapToLong(Product::getSold).sum();
        info.setTotalSold(totalSold);

        // 2. 店铺平均评分（商品评价 + 商家服务评价）
        // 2.1 获取该商家所有商品ID
        List<Long> productIds = productService.list(new LambdaQueryWrapper<Product>()
                .eq(Product::getUserId, sellerId))
                .stream().map(Product::getId).collect(Collectors.toList());

        double avgProductRating = 0.0;
        long productReviewCount = 0;
        if (!productIds.isEmpty()) {
            List<Evaluation> productReviews = evaluationService.list(new LambdaQueryWrapper<Evaluation>()
                    .in(Evaluation::getProductId, productIds));
            productReviewCount = productReviews.size();
            avgProductRating = productReviews.stream()
                    .mapToInt(Evaluation::getRating)
                    .average().orElse(0.0);
        }

        // 2.2 商家服务评价
        List<MerchantEvaluation> merchantReviews = merchantEvaluationService.list(new LambdaQueryWrapper<MerchantEvaluation>()
                .eq(MerchantEvaluation::getSellerId, sellerId));
        long merchantReviewCount = merchantReviews.size();
        double avgServiceRating = merchantReviews.stream()
                .mapToInt(MerchantEvaluation::getServiceRating)
                .average().orElse(0.0);

        long totalReviews = productReviewCount + merchantReviewCount;
        double totalScore = avgProductRating * productReviewCount + avgServiceRating * merchantReviewCount;
        double avgRating = totalReviews == 0 ? 0.0 : totalScore / totalReviews;
        // 保留一位小数
        avgRating = BigDecimal.valueOf(avgRating).setScale(1, RoundingMode.HALF_UP).doubleValue();

        info.setAvgRating(avgRating);
        info.setTotalReviews(totalReviews);

        return Result.success(info);
    }

    /**
     * 获取店铺商品列表（仅上架商品）
     * GET /shop/{sellerId}/products?page=1&size=12&sort=sold_desc
     * sort可选：time_desc(默认), price_asc, price_desc, sold_desc
     */
    @GetMapping("/{sellerId}/products")
    public Result getShopProducts(@PathVariable Long sellerId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "12") Integer size,
                                  @RequestParam(required = false, defaultValue = "time_desc") String sort) {
        // 校验商家是否存在
        User seller = userService.getById(sellerId);
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("商家不存在");
        }
        Page<Product> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getUserId, sellerId)
               .eq(Product::getStatus, 1); // 仅上架商品

        // 排序处理
        switch (sort) {
            case "price_asc":
                wrapper.orderByAsc(Product::getPrice);
                break;
            case "price_desc":
                wrapper.orderByDesc(Product::getPrice);
                break;
            case "sold_desc":
                wrapper.orderByDesc(Product::getSold);
                break;
            default:
                wrapper.orderByDesc(Product::getCreateTime);
                break;
        }

        Page<Product> productPage = productService.page(pageObj, wrapper);
        // 转换为 VO
        Page<ShopProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ShopProductVO> voList = productPage.getRecords().stream().map(p -> {
            ShopProductVO vo = new ShopProductVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setImageUrl(p.getImageUrl());
            vo.setPrice(p.getPrice());
            vo.setSold(p.getSold() == null ? 0 : p.getSold());
            vo.setNegotiable(p.getNegotiable());
            vo.setCondition(p.getCondition());
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }
}
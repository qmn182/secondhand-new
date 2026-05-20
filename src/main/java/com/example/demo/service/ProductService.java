package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {

    @Autowired
    private UserService userService;

    /**
     * 分页查询上架商品（支持关键词、分类、排序）
     */
    public Page<Product> getOnSaleProducts(Page<Product> page, String keyword, String category, String sort) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Product::getCategory, category);
        }
        // 排序
        if ("price_asc".equals(sort)) {
            wrapper.orderByAsc(Product::getPrice);
        } else if ("price_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getPrice);
        } else if ("sold_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getSold);
        } else if ("rating_desc".equals(sort)) {
            wrapper.orderByDesc(Product::getAvgRating);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }
        return this.page(page, wrapper);
    }

    /**
     * 发布商品（商家） - 修改：状态改为待审核(0)
     */
    public boolean publishProduct(Product product, Long userId) {
        User seller = userService.getById(userId);
        if (seller == null || seller.getRole() != 2) {
            return false;
        }
        product.setUserId(userId);
        product.setShopName(seller.getShopName());
        product.setStatus(1);  
        product.setSold(0);
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            product.setDiscount(product.getPrice().divide(product.getOriginalPrice(), 2, RoundingMode.HALF_UP));
        } else {
            product.setDiscount(BigDecimal.ONE);
        }
        return this.save(product);
    }

    /**
     * 管理员审核商品
     * @param productId 商品ID
     * @param approved true=通过(上架), false=拒绝(下架)
     * @return 是否成功
     */
    @Transactional
    public boolean auditProduct(Long productId, boolean approved) {
        Product product = this.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStatus() != 0) {
            throw new RuntimeException("商品不是待审核状态");
        }
        if (approved) {
            product.setStatus(1);  // 审核通过，上架
        } else {
            product.setStatus(2);  // 审核拒绝，下架
        }
        return this.updateById(product);
    }

    /**
     * 商家重新提交被拒商品（将下架商品重新改为待审核）
     * @param productId 商品ID
     * @param userId 商家ID
     * @param updatedProduct 可选的更新内容（如价格、库存等）
     */
    @Transactional
    public boolean resubmitProduct(Long productId, Long userId, Product updatedProduct) {
        Product product = this.getById(productId);
        if (product == null || !product.getUserId().equals(userId)) {
            throw new RuntimeException("商品不存在或无权限");
        }
        if (product.getStatus() != 2) {
            throw new RuntimeException("只有审核拒绝的商品才能重新提交");
        }
        // 更新商品字段（若传入了新值）
        if (updatedProduct != null) {
            if (updatedProduct.getPrice() != null) product.setPrice(updatedProduct.getPrice());
            if (updatedProduct.getStock() != null) product.setStock(updatedProduct.getStock());
            if (updatedProduct.getName() != null) product.setName(updatedProduct.getName());
            if (updatedProduct.getDescription() != null) product.setDescription(updatedProduct.getDescription());
            if (updatedProduct.getImages() != null) product.setImages(updatedProduct.getImages());
            // ... 其他需要更新的字段
        }
        product.setStatus(0);  // 重新进入待审核
        return this.updateById(product);
    }

    /**
     * 下架商品
     */
    public boolean offlineProduct(Long productId, Long userId) {
        Product product = this.getById(productId);
        if (product == null || !product.getUserId().equals(userId)) {
            return false;
        }
        product.setStatus(2);
        return this.updateById(product);
    }

    /**
     * 上架商品（重新上架）
     */
    public boolean onlineProduct(Long productId, Long userId) {
        Product product = this.getById(productId);
        if (product == null || !product.getUserId().equals(userId)) {
            return false;
        }
        product.setStatus(1);
        return this.updateById(product);
    }

    /**
     * 获取商家的商品列表（含上下架状态）
     */
    public Page<Product> getSellerProducts(Page<Product> page, Long userId, Integer status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getUserId, userId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return this.page(page, wrapper);
    }
}
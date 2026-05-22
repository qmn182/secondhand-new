package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Product;
import com.example.demo.mapper.CartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
// ===== 修改开始：添加 ConcurrentHashMap 导入 =====
import java.util.concurrent.ConcurrentHashMap;
// ===== 修改结束 =====

@Service
public class CartService extends ServiceImpl<CartMapper, Cart> {

    @Autowired
    private ProductService productService;

    // ===== 修改开始：定义防抖缓存 =====
    private final ConcurrentHashMap<String, Long> addCartCache = new ConcurrentHashMap<>();
    // ===== 修改结束 =====

    /**
     * 添加商品到购物车（若已存在则增加数量，否则新增）
     */
    public boolean addToCart(Long userId, Long productId, Integer quantity) {
        // ===== 修改开始：防抖逻辑 =====
        String key = userId + "_" + productId;
        Long lastTime = addCartCache.get(key);
        long now = System.currentTimeMillis();
        if (lastTime != null && (now - lastTime) < 500) {
            // 500ms 内重复请求直接忽略
            System.out.println("防抖拦截：用户[" + userId + "]商品[" + productId + "]在短时间内重复请求");
            return true;
        }
        addCartCache.put(key, now);
        // ===== 修改结束 =====

        // --- 修改开始：检查是否购买自己的商品 ---
        Product product = productService.getById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getUserId().equals(userId)) {
            throw new RuntimeException("不能购买自己发布的商品");
        }
        // --- 修改结束 ---

        // 查询已有购物车项
        Cart existing = this.getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
        if (existing != null) {
            // 记录原有数量，便于调试
            int oldQty = existing.getQuantity();
            existing.setQuantity(oldQty + quantity);
            boolean updated = this.updateById(existing);
            // 打印日志，确认实际增加的数量
            System.out.println("购物车更新：用户[" + userId + "]商品[" + productId + "]原数量=" + oldQty + "，增加" + quantity + "，新数量=" + existing.getQuantity());
            return updated;
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            boolean saved = this.save(cart);
            System.out.println("购物车新增：用户[" + userId + "]商品[" + productId + "]，数量=" + quantity);
            return saved;
        }
    }

    // 获取用户购物车列表（附带商品信息）
    public List<Cart> getUserCartWithProduct(Long userId) {
        List<Cart> carts = this.list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        for (Cart cart : carts) {
            Product product = productService.getById(cart.getProductId());
            if (product != null) {
                cart.setProductName(product.getName());
                cart.setProductPrice(product.getPrice());
                cart.setProductImage(product.getImageUrl());
            }
        }
        return carts;
    }

    // 清空用户购物车
    public void clearCart(Long userId) {
        this.remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
    }
}
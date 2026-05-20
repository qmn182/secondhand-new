package com.example.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Cart;
import com.example.demo.entity.Product;
import com.example.demo.mapper.CartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CartService extends ServiceImpl<CartMapper, Cart> {

    @Autowired
    private ProductService productService;

    // 添加商品到购物车（如果已存在则增加数量）
    public boolean addToCart(Long userId, Long productId, Integer quantity) {
        Cart existing = this.getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
            return this.updateById(existing);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            return this.save(cart);
        }
    }

    // 获取用户购物车列表（附带商品信息）
    public List<Cart> getUserCartWithProduct(Long userId) {
        List<Cart> carts = this.list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        // 填充商品名称、价格、图片等（可选，可实时查询 product 表）
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
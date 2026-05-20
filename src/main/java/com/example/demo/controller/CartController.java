package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.Cart;
import com.example.demo.entity.User;
import com.example.demo.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public Result add(@RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (quantity <= 0) {
            return Result.fail("数量必须大于0");
        }
        boolean success = cartService.addToCart(user.getId(), productId, quantity);
        return success ? Result.success("已加入购物车") : Result.fail("加入失败");
    }

    @GetMapping("/list")
    public Result list(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        List<Cart> carts = cartService.getUserCartWithProduct(user.getId());
        return Result.success(carts);
    }

    @DeleteMapping("/remove/{cartId}")
    public Result remove(@PathVariable Long cartId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        boolean removed = cartService.removeById(cartId);
        return removed ? Result.success("已删除") : Result.fail("删除失败");
    }

    @PutMapping("/updateQuantity")
    public Result updateQuantity(@RequestParam Long cartId, @RequestParam Integer quantity, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.fail("请先登录");
        if (quantity <= 0) return Result.fail("数量必须大于0");
        Cart cart = cartService.getById(cartId);
        if (cart == null || !cart.getUserId().equals(user.getId())) {
            return Result.fail("非法操作");
        }
        cart.setQuantity(quantity);
        cartService.updateById(cart);
        return Result.success("修改成功");
    }
}
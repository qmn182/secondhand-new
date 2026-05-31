package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.Cart; // 导入购物车实体类
import com.example.demo.entity.User; // 导入用户实体类
import com.example.demo.service.CartService; // 导入购物车服务类
import jakarta.servlet.http.HttpSession; // 导入 HttpSession 用于获取会话信息
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解

import java.util.List; // 导入 List 集合接口

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/cart") // 定义该类下所有接口的公共前缀为 /cart
public class CartController { // 定义购物车控制器类

    @Autowired // 自动装配 CartService 实例
    private CartService cartService; // 购物车服务对象

    @PostMapping("/add") // 处理 POST 请求，路径为 /cart/add
    public Result add(@RequestParam Long productId, @RequestParam(defaultValue = "1") Integer quantity, HttpSession session) { // 添加商品到购物车方法，接收商品ID、数量（默认1）和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果，提示先登录
        }
        if (quantity <= 0) { // 如果数量小于等于0
            return Result.fail("数量必须大于0"); // 返回失败结果，提示数量无效
        }
        boolean success = cartService.addToCart(user.getId(), productId, quantity); // 调用购物车服务添加商品，返回是否成功
        return success ? Result.success("已加入购物车") : Result.fail("加入失败"); // 成功返回成功消息，失败返回失败消息
    }

    @GetMapping("/list") // 处理 GET 请求，路径为 /cart/list
    public Result list(HttpSession session) { // 查看购物车列表方法，接收 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果，提示先登录
        }
        List<Cart> carts = cartService.getUserCartWithProduct(user.getId()); // 调用购物车服务获取用户购物车列表（带商品信息）
        return Result.success(carts); // 返回成功结果，包含购物车列表数据
    }

    @DeleteMapping("/remove/{cartId}") // 处理 DELETE 请求，路径为 /cart/remove/{cartId}
    public Result remove(@PathVariable Long cartId, HttpSession session) { // 删除购物车项方法，接收购物车ID和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果，提示先登录
        }
        boolean removed = cartService.removeById(cartId); // 调用购物车服务根据ID删除购物车项，返回是否删除成功
        return removed ? Result.success("已删除") : Result.fail("删除失败"); // 成功返回成功消息，失败返回失败消息
    }

    @PutMapping("/updateQuantity") // 处理 PUT 请求，路径为 /cart/updateQuantity
    public Result updateQuantity(@RequestParam Long cartId, @RequestParam Integer quantity, HttpSession session) { // 修改购物车商品数量方法，接收购物车ID、新数量和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) return Result.fail("请先登录"); // 如果用户未登录，返回失败结果
        if (quantity <= 0) return Result.fail("数量必须大于0"); // 如果数量小于等于0，返回失败结果
        Cart cart = cartService.getById(cartId); // 调用购物车服务根据ID查询购物车项
        if (cart == null || !cart.getUserId().equals(user.getId())) { // 如果购物车项不存在或不属于当前用户
            return Result.fail("非法操作"); // 返回失败结果，提示非法操作
        }
        cart.setQuantity(quantity); // 设置新的数量
        cartService.updateById(cart); // 调用购物车服务更新购物车项
        return Result.success("修改成功"); // 返回成功结果，提示修改成功
    }
}
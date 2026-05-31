package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.Cart; // 导入购物车实体类 Cart
import com.example.demo.entity.Product; // 导入商品实体类 Product
import com.example.demo.mapper.CartMapper; // 导入购物车 Mapper 接口
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.stereotype.Service; // 导入 Spring 服务层注解
import java.util.List; // 导入 List 集合接口
// ===== 修改开始：添加 ConcurrentHashMap 导入 =====
import java.util.concurrent.ConcurrentHashMap; // 导入线程安全的 ConcurrentHashMap 类
// ===== 修改结束 =====

@Service // 标记该类为 Spring 的服务层组件
public class CartService extends ServiceImpl<CartMapper, Cart> { // 定义购物车服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    @Autowired // 自动装配 ProductService 实例
    private ProductService productService; // 商品服务对象

    // ===== 修改开始：定义防抖缓存 =====
    private final ConcurrentHashMap<String, Long> addCartCache = new ConcurrentHashMap<>(); // 定义并发哈希映射，用于存储用户ID+商品ID的防抖缓存，键为"userId_productId"，值为上次请求时间戳
    // ===== 修改结束 =====

    /**
     * 添加商品到购物车（若已存在则增加数量，否则新增）
     */
    public boolean addToCart(Long userId, Long productId, Integer quantity) { // 添加商品到购物车方法，参数：用户ID、商品ID、数量
        // ===== 修改开始：防抖逻辑 =====
        String key = userId + "_" + productId; // 构建防抖缓存的键，格式为"用户ID_商品ID"
        Long lastTime = addCartCache.get(key); // 从缓存中获取上次请求的时间戳
        long now = System.currentTimeMillis(); // 获取当前系统时间戳（毫秒）
        if (lastTime != null && (now - lastTime) < 500) { // 如果上次请求时间存在且当前时间与上次时间间隔小于500毫秒
            // 500ms 内重复请求直接忽略
            System.out.println("防抖拦截：用户[" + userId + "]商品[" + productId + "]在短时间内重复请求"); // 输出防抖拦截日志到控制台
            return true; // 拦截重复请求，返回 true（表示操作成功，但不实际处理）
        }
        addCartCache.put(key, now); // 将当前请求的时间戳存入缓存
        // ===== 修改结束 =====

        // --- 修改开始：检查是否购买自己的商品 ---
        Product product = productService.getById(productId); // 根据商品ID查询商品信息
        if (product == null) { // 如果商品不存在
            throw new RuntimeException("商品不存在"); // 抛出运行时异常，提示商品不存在
        }
        if (product.getUserId().equals(userId)) { // 如果商品所属用户ID等于当前用户ID（即自己卖的商品）
            throw new RuntimeException("不能购买自己发布的商品"); // 抛出运行时异常，提示不能购买自己的商品
        }
        // --- 修改结束 ---

        // 查询已有购物车项
        Cart existing = this.getOne(new LambdaQueryWrapper<Cart>() // 查询当前用户对该商品是否已有购物车记录
                .eq(Cart::getUserId, userId) // 添加条件：用户ID匹配
                .eq(Cart::getProductId, productId)); // 添加条件：商品ID匹配
        if (existing != null) { // 如果已有购物车记录
            // 记录原有数量，便于调试
            int oldQty = existing.getQuantity(); // 获取原有的数量
            existing.setQuantity(oldQty + quantity); // 将原有数量加上新数量后设置回购物车项
            boolean updated = this.updateById(existing); // 调用更新方法更新购物车项，返回是否更新成功
            // 打印日志，确认实际增加的数量
            System.out.println("购物车更新：用户[" + userId + "]商品[" + productId + "]原数量=" + oldQty + "，增加" + quantity + "，新数量=" + existing.getQuantity()); // 输出更新日志到控制台
            return updated; // 返回更新结果
        } else { // 如果没有购物车记录
            Cart cart = new Cart(); // 创建新的购物车对象
            cart.setUserId(userId); // 设置用户ID
            cart.setProductId(productId); // 设置商品ID
            cart.setQuantity(quantity); // 设置数量
            boolean saved = this.save(cart); // 调用保存方法将购物车项存入数据库，返回是否保存成功
            System.out.println("购物车新增：用户[" + userId + "]商品[" + productId + "]，数量=" + quantity); // 输出新增日志到控制台
            return saved; // 返回保存结果
        }
    }

    // 获取用户购物车列表（附带商品信息）
    public List<Cart> getUserCartWithProduct(Long userId) { // 获取用户购物车列表方法，参数：用户ID
        List<Cart> carts = this.list(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId)); // 根据用户ID查询该用户的所有购物车记录
        for (Cart cart : carts) { // 遍历购物车列表
            Product product = productService.getById(cart.getProductId()); // 根据购物车中的商品ID查询商品详情
            if (product != null) { // 如果商品存在
                cart.setProductName(product.getName()); // 设置购物车项中的商品名称
                cart.setProductPrice(product.getPrice()); // 设置购物车项中的商品价格
                cart.setProductImage(product.getImageUrl()); // 设置购物车项中的商品图片URL
            }
        }
        return carts; // 返回补充了商品信息的购物车列表
    }

    // 清空用户购物车
    public void clearCart(Long userId) { // 清空用户购物车方法，参数：用户ID
        this.remove(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId)); // 根据用户ID删除该用户的所有购物车记录
    }
}
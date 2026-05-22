// controller/AdminController.java
package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import com.example.demo.service.MerchantLevelService;
import com.example.demo.service.SellerLevelLogService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import com.example.demo.entity.SellerLevelLog;
// ===== 修改开始：新增导入 =====
import com.example.demo.entity.vo.ProductAuditVO;      // 商品审核VO
import org.springframework.beans.BeanUtils;     // 属性拷贝工具
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
// ===== 修改结束 =====

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private MerchantLevelService merchantLevelService;

    @Autowired
    private UserService userService;

    @Autowired
    private SellerLevelLogService levelLogService;

    /**
     * 检查是否为管理员
     */
    private User checkAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 3) {
            throw new RuntimeException("无管理员权限");
        }
        return user;
    }

    /**
     * 分页查询待审核商品列表（status=0）
     * ===== 修改开始：返回类型改为 Page<ProductAuditVO>，并手动转换 =====
     */
    @GetMapping("/products/pending")
    public Result pendingProducts(@RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "10") Integer size,
                                  HttpSession session) {
        try {
            checkAdmin(session);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        // 1. 查询 status=0 的商品分页数据
        Page<Product> pageObj = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 0).orderByDesc(Product::getCreateTime);
        Page<Product> productPage = productService.page(pageObj, wrapper);
        
        // 2. 提取所有商家ID，查询对应的用户名
        List<Long> userIds = productPage.getRecords().stream()
                .map(Product::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            // 没有待审核商品时直接返回空分页
            return Result.success(new Page<ProductAuditVO>());
        }
        List<User> sellers = userService.listByIds(userIds);
        Map<Long, String> usernameMap = sellers.stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        
        // 3. 转换为 VO 分页对象
        Page<ProductAuditVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductAuditVO> voList = productPage.getRecords().stream().map(p -> {
            ProductAuditVO vo = new ProductAuditVO();
            BeanUtils.copyProperties(p, vo);
            vo.setSellerUsername(usernameMap.getOrDefault(p.getUserId(), "未知"));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        
        return Result.success(voPage);
    }
    // ===== 修改结束 =====

    /**
     * 审核商品
     * 请求体示例：{"productId": 123, "approved": true}
     */
    @PutMapping("/products/audit")
    public Result auditProduct(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            checkAdmin(session);
            Long productId = Long.valueOf(params.get("productId").toString());
            Boolean approved = (Boolean) params.get("approved");
            if (approved == null) {
                return Result.fail("缺少审核结果参数");
            }
            productService.auditProduct(productId, approved);
            String msg = approved ? "审核通过，商品已上架" : "审核拒绝，商品已下架";
            return Result.success(msg);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // 可选：查询所有商品（带筛选），用于更完整的管理后台
    @GetMapping("/products/list")
    public Result allProducts(@RequestParam(defaultValue = "1") Integer page,
                              @RequestParam(defaultValue = "10") Integer size,
                              @RequestParam(required = false) Integer status,
                              HttpSession session) {
        try {
            checkAdmin(session);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
        Page<Product> pageObj = new Page<>(page, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Product> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productService.page(pageObj, wrapper);
        return Result.success(result);
    }

    /**
     * 手动触发重新计算所有商家等级
     */
    @PostMapping("/merchant/level/recalculate")
    public Result recalculateAllLevels(HttpSession session) {
        try {
            checkAdmin(session);
            merchantLevelService.recalculateAllMerchantLevels("admin");
            return Result.success("等级重算完成");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 管理员手动调整指定商家的等级
     */
    @PutMapping("/merchant/level")
    public Result setMerchantLevel(@RequestParam Long sellerId,
                                @RequestParam Integer newLevel,
                                @RequestParam(required = false) String reason,
                                HttpSession session) {
        try {
            User admin = checkAdmin(session);
            User seller = userService.getById(sellerId);
            if (seller == null || seller.getRole() != 2) {
                return Result.fail("商家不存在");
            }
            if (newLevel < 1 || newLevel > 5) {
                return Result.fail("等级范围1-5");
            }
            Integer oldLevel = seller.getLevel() == null ? 1 : seller.getLevel();
            seller.setLevel(newLevel);
            userService.updateById(seller);
            // 记录日志
            SellerLevelLog log = new SellerLevelLog();
            log.setSellerId(sellerId);
            log.setOldLevel(oldLevel);
            log.setNewLevel(newLevel);
            log.setReason(reason != null ? reason : "管理员手动调整");
            log.setOperator(admin.getUsername());
            levelLogService.save(log);
            return Result.success("等级修改成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 获取所有商家列表（用于等级管理）
     * GET /admin/sellers
     */
    @GetMapping("/sellers")
    public Result getSellers(HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        List<User> sellers = userService.lambdaQuery()
                .eq(User::getRole, 2)  // 角色为商家
                .select(User::getId, User::getUsername, User::getShopName, User::getLevel)
                .list();
        return Result.success(sellers);
    }
}
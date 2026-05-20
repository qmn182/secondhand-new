package com.example.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 图片上传（单张或多张）
     */
    @PostMapping("/upload")
    public Result uploadImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.fail("请选择图片");
        }
        List<String> urls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String originalName = file.getOriginalFilename();
                String ext = originalName.substring(originalName.lastIndexOf("."));
                String newName = UUID.randomUUID().toString() + ext;
                File dest = new File(uploadDir + "/products/");
                if (!dest.exists()) dest.mkdirs();
                file.transferTo(new File(dest, newName));
                String url = "/uploads/products/" + newName;
                urls.add(url);
            }
            return Result.success(urls);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.fail("上传失败");
        }
    }

    /**
     * 发布商品（需登录且角色为商家）
     */
    @PostMapping("/publish")
    public Result publish(@RequestBody Product product, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (user.getRole() != 2) {
            return Result.fail("只有商家可以发布商品");
        }
        if (product.getStock() == null || product.getStock() <= 0) {
            return Result.fail("库存必须大于0");
        }
        boolean success = productService.publishProduct(product, user.getId());
        return success ? Result.success("发布成功") : Result.fail("发布失败");
    }

    /**
     * 下架商品
     */
    @PutMapping("/offline/{id}")
    public Result offline(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("无权限");
        }
        boolean success = productService.offlineProduct(id, user.getId());
        return success ? Result.success("已下架") : Result.fail("操作失败");
    }

    /**
     * 上架商品
     */
    @PutMapping("/online/{id}")
    public Result online(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("无权限");
        }
        boolean success = productService.onlineProduct(id, user.getId());
        return success ? Result.success("已上架") : Result.fail("操作失败");
    }

    /**
     * 商家查看自己的商品列表
     */
    @GetMapping("/my-list")
    public Result myProducts(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Integer status,
                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        Page<Product> pageObj = new Page<>(page, size);
        Page<Product> result = productService.getSellerProducts(pageObj, user.getId(), status);
        return Result.success(result);
    }

    /**
     * 首页商品列表（公开接口）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "12") Integer size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String sort) {
        Page<Product> pageObj = new Page<>(page, size);
        Page<Product> result = productService.getOnSaleProducts(pageObj, keyword, category, sort);
        return Result.success(result);
    }

    /**
     * 商品详情
     */
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null || product.getStatus() != 1) {
            return Result.fail("商品不存在或已下架");
        }
        return Result.success(product);
    }

    /**
     * 商家重新提交被拒商品
     */
    @PutMapping("/resubmit/{id}")
    public Result resubmit(@PathVariable Long id, @RequestBody Product updatedProduct, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        try {
            productService.resubmitProduct(id, user.getId(), updatedProduct);
            return Result.success("已重新提交审核");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // 编辑商品
    @PutMapping("/edit/{id}")
    public Result editProduct(@PathVariable Long id, @RequestBody Product product, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("无权限");
        }
        Product existing = productService.getById(id);
        if (existing == null || !existing.getUserId().equals(user.getId())) {
            return Result.fail("商品不存在或无权限");
        }
        // 允许修改的字段
        if (product.getName() != null) existing.setName(product.getName());
        if (product.getCategory() != null) existing.setCategory(product.getCategory());
        if (product.getPrice() != null) existing.setPrice(product.getPrice());
        if (product.getOriginalPrice() != null) existing.setOriginalPrice(product.getOriginalPrice());
        if (product.getStock() != null) existing.setStock(product.getStock());
        if (product.getDescription() != null) existing.setDescription(product.getDescription());
        if (product.getNegotiable() != null) existing.setNegotiable(product.getNegotiable());
        if (product.getCondition() != null) existing.setCondition(product.getCondition());
        if (product.getImages() != null) existing.setImages(product.getImages());
        existing.setStatus(1); // 编辑后重新上架
        boolean success = productService.updateById(existing);
        return success ? Result.success("修改成功") : Result.fail("修改失败");
    }

    // 删除商品
    @DeleteMapping("/{id}")
    public Result deleteProduct(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        Product product = productService.getById(id);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        // 商家或管理员可删除
        if (user.getRole() != 3 && !product.getUserId().equals(user.getId())) {
            return Result.fail("无权限");
        }
        boolean success = productService.removeById(id);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
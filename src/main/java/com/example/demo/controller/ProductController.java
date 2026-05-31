package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 条件构造器
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.Product; // 导入商品实体类
import com.example.demo.entity.User; // 导入用户实体类
import com.example.demo.service.ProductService; // 导入商品服务类
import jakarta.servlet.http.HttpSession; // 导入 HttpSession 用于获取会话信息
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.beans.factory.annotation.Value; // 导入配置值注入注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解
import org.springframework.web.multipart.MultipartFile; // 导入文件上传 MultipartFile 类

import java.io.File; // 导入 File 类用于文件操作
import java.io.IOException; // 导入 IOException 异常类
import java.util.ArrayList; // 导入 ArrayList 集合类
import java.util.List; // 导入 List 集合接口
import java.util.UUID; // 导入 UUID 工具类

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/product") // 定义该类下所有接口的公共前缀为 /product
public class ProductController { // 定义商品控制器类

    @Autowired // 自动装配 ProductService 实例
    private ProductService productService; // 商品服务对象

    @Value("${file.upload-dir}") // 从配置文件注入文件上传根目录路径
    private String uploadDir; // 文件上传根目录

    /**
     * 图片上传（单张或多张）
     */
    @PostMapping("/upload") // 处理 POST 请求，路径为 /product/upload
    public Result uploadImages(@RequestParam("files") MultipartFile[] files) { // 上传图片方法，接收文件数组参数
        if (files == null || files.length == 0) { // 如果文件数组为空或长度为0
            return Result.fail("请选择图片"); // 返回失败结果，提示请选择图片
        }
        List<String> urls = new ArrayList<>(); // 创建 List 用于存储上传后的图片 URL
        try { // 开始 try 块
            for (MultipartFile file : files) { // 遍历每个上传的文件
                if (file.isEmpty()) continue; // 如果文件为空，跳过本次循环
                String originalName = file.getOriginalFilename(); // 获取原始文件名
                String ext = originalName.substring(originalName.lastIndexOf(".")); // 提取文件扩展名（包含点）
                String newName = UUID.randomUUID().toString() + ext; // 生成新的唯一文件名（UUID + 扩展名）
                File dest = new File(uploadDir + "/products/"); // 创建目标目录 File 对象（上传根目录/products/）
                if (!dest.exists()) dest.mkdirs(); // 如果目录不存在，则创建多级目录
                file.transferTo(new File(dest, newName)); // 将文件传输到目标文件
                String url = "/uploads/products/" + newName; // 构造访问 URL
                urls.add(url); // 将 URL 添加到列表
            }
            return Result.success(urls); // 返回成功结果，包含所有图片 URL 列表
        } catch (IOException e) { // 捕获 IO 异常
            e.printStackTrace(); // 打印异常堆栈
            return Result.fail("上传失败"); // 返回失败结果
        }
    }

    /**
     * 发布商品（需登录且角色为商家）
     */
    @PostMapping("/publish") // 处理 POST 请求，路径为 /product/publish
    public Result publish(@RequestBody Product product, HttpSession session) { // 发布商品方法，接收商品实体和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        if (user.getRole() != 2) { // 如果用户角色不是商家(role=2)
            return Result.fail("只有商家可以发布商品"); // 返回失败结果
        }
        if (product.getStock() == null || product.getStock() <= 0) { // 如果库存为空或小于等于0
            return Result.fail("库存必须大于0"); // 返回失败结果
        }
        boolean success = productService.publishProduct(product, user.getId()); // 调用商品服务发布商品
        return success ? Result.success("发布成功") : Result.fail("发布失败"); // 成功返回成功消息，失败返回失败消息
    }

    /**
     * 下架商品
     */
    @PutMapping("/offline/{id}") // 处理 PUT 请求，路径为 /product/offline/{id}
    public Result offline(@PathVariable Long id, HttpSession session) { // 下架商品方法，接收商品ID和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null || user.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("无权限"); // 返回失败结果
        }
        boolean success = productService.offlineProduct(id, user.getId()); // 调用商品服务下架商品
        return success ? Result.success("已下架") : Result.fail("操作失败"); // 成功返回成功消息，失败返回失败消息
    }

    /**
     * 上架商品
     */
    @PutMapping("/online/{id}") // 处理 PUT 请求，路径为 /product/online/{id}
    public Result online(@PathVariable Long id, HttpSession session) { // 上架商品方法，接收商品ID和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null || user.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("无权限"); // 返回失败结果
        }
        boolean success = productService.onlineProduct(id, user.getId()); // 调用商品服务上架商品
        return success ? Result.success("已上架") : Result.fail("操作失败"); // 成功返回成功消息，失败返回失败消息
    }

    /**
     * 商家查看自己的商品列表
     */
    @GetMapping("/my-list") // 处理 GET 请求，路径为 /product/my-list
    public Result myProducts(@RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                             @RequestParam(defaultValue = "10") Integer size, // 接收每页大小，默认10
                             @RequestParam(required = false) Integer status, // 接收商品状态筛选，非必传
                             HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null || user.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("请登录商家账号"); // 返回失败结果
        }
        Page<Product> pageObj = new Page<>(page, size); // 创建分页对象
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Product::getUserId, user.getId()); // 添加条件：商品用户ID等于当前商家ID
        if (status != null) { // 如果状态参数不为空
            wrapper.eq(Product::getStatus, status); // 添加状态等值条件
        }
        wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序排序
        Page<Product> productPage = productService.page(pageObj, wrapper); // 执行分页查询
        return Result.success(productPage); // 返回成功结果，包含商品分页数据
    }

    /**
     * 首页商品列表（公开接口）
     */
    @GetMapping("/list") // 处理 GET 请求，路径为 /product/list
    public Result list(@RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                       @RequestParam(defaultValue = "12") Integer size, // 接收每页大小，默认12
                       @RequestParam(required = false) String keyword, // 接收搜索关键词，非必传
                       @RequestParam(required = false) String category, // 接收商品分类，非必传
                       @RequestParam(required = false) String sort) { // 接收排序方式，非必传
        Page<Product> pageObj = new Page<>(page, size); // 创建分页对象
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Product::getStatus, 1); // 添加条件：状态为已上架(1)
        if (keyword != null && !keyword.isEmpty()) { // 如果关键词不为空
            wrapper.like(Product::getName, keyword); // 添加模糊匹配商品名称条件
        }
        if (category != null && !category.isEmpty()) { // 如果分类不为空
            wrapper.eq(Product::getCategory, category); // 添加分类等值条件
        }
        if ("price_asc".equals(sort)) { // 如果排序方式为价格升序
            wrapper.orderByAsc(Product::getPrice); // 按价格升序
        } else if ("price_desc".equals(sort)) { // 如果排序方式为价格降序
            wrapper.orderByDesc(Product::getPrice); // 按价格降序
        } else if ("sold_desc".equals(sort)) { // 如果排序方式为销量降序
            wrapper.orderByDesc(Product::getSold); // 按销量降序
        } else { // 其他情况（包括默认）
            wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序（最新）
        }
        Page<Product> productPage = productService.page(pageObj, wrapper); // 执行分页查询
        return Result.success(productPage); // 返回成功结果，包含商品分页数据
    }

    /**
     * 商品详情
     */
    @GetMapping("/detail/{id}") // 处理 GET 请求，路径为 /product/detail/{id}
    public Result detail(@PathVariable Long id) { // 获取商品详情方法，接收商品ID路径变量
        Product product = productService.getById(id); // 根据ID查询商品
        if (product == null) { // 如果商品不存在
            return Result.fail("商品不存在"); // 返回失败结果
        }
        if (product.getStatus() == 2) { // 如果商品状态为已下架(2)
            return Result.fail("商品已下架，无法查看"); // 返回失败结果
        }
        return Result.success(product); // 返回成功结果，包含商品信息
    }

    /**
     * 商家重新提交被拒商品
     */
    @PutMapping("/resubmit/{id}") // 处理 PUT 请求，路径为 /product/resubmit/{id}
    public Result resubmit(@PathVariable Long id, @RequestBody Product updatedProduct, HttpSession session) { // 重新提交被拒商品方法，接收商品ID、更新后的商品实体和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null || user.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("请登录商家账号"); // 返回失败结果
        }
        try { // 开始 try 块
            productService.resubmitProduct(id, user.getId(), updatedProduct); // 调用商品服务重新提交商品
            return Result.success("已重新提交审核"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果，异常信息作为提示
        }
    }

    // 编辑商品
    @PutMapping("/edit/{id}") // 处理 PUT 请求，路径为 /product/edit/{id}
    public Result editProduct(@PathVariable Long id, @RequestBody Product product, HttpSession session) { // 编辑商品方法，接收商品ID、更新内容、HttpSession
        // ===== 修改开始：优化权限校验逻辑 =====
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        Product existing = productService.getById(id); // 根据ID查询原有商品信息
        if (existing == null) { // 如果商品不存在
            return Result.fail("商品不存在"); // 返回失败结果
        }
        // 权限检查：管理员可编辑任何商品，商家只能编辑自己的商品
        if (user.getRole() != 3 && (user.getRole() != 2 || !existing.getUserId().equals(user.getId()))) { // 如果不是管理员，且（不是商家 或 不是自己店铺的商品）
            return Result.fail("无权限"); // 返回失败结果
        }
        // ===== 修改结束 =====

        // 允许修改的字段
        if (product.getName() != null) existing.setName(product.getName()); // 如果传入了商品名称，则更新
        if (product.getCategory() != null) existing.setCategory(product.getCategory()); // 如果传入了分类，则更新
        if (product.getPrice() != null) existing.setPrice(product.getPrice()); // 如果传入了价格，则更新
        if (product.getOriginalPrice() != null) existing.setOriginalPrice(product.getOriginalPrice()); // 如果传入了原价，则更新
        if (product.getStock() != null) existing.setStock(product.getStock()); // 如果传入了库存，则更新
        if (product.getDescription() != null) existing.setDescription(product.getDescription()); // 如果传入了描述，则更新
        if (product.getNegotiable() != null) existing.setNegotiable(product.getNegotiable()); // 如果传入了是否可议价，则更新
        if (product.getCondition() != null) existing.setCondition(product.getCondition()); // 如果传入了商品成色，则更新
        if (product.getImages() != null) existing.setImages(product.getImages()); // 如果传入了图片列表，则更新
        existing.setStatus(1); // 编辑后重新上架（状态设为1）
        boolean success = productService.updateById(existing); // 执行更新操作
        return success ? Result.success("修改成功") : Result.fail("修改失败"); // 成功返回成功消息，失败返回失败消息
    }

    // 删除商品
    @DeleteMapping("/{id}") // 处理 DELETE 请求，路径为 /product/{id}
    public Result deleteProduct(@PathVariable Long id, HttpSession session) { // 删除商品方法，接收商品ID和 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        Product product = productService.getById(id); // 根据ID查询商品
        if (product == null) { // 如果商品不存在
            return Result.fail("商品不存在"); // 返回失败结果
        }
        if (user.getRole() != 3 && !product.getUserId().equals(user.getId())) { // 如果不是管理员且不是商品所属商家
            return Result.fail("无权限"); // 返回失败结果
        }
        boolean success = productService.removeById(id); // 执行删除操作
        return success ? Result.success("删除成功") : Result.fail("删除失败"); // 成功返回成功消息，失败返回失败消息
    }
}
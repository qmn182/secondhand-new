package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.Product; // 导入商品实体类 Product
import com.example.demo.entity.User; // 导入用户实体类 User
import com.example.demo.mapper.ProductMapper; // 导入商品 Mapper 接口
import org.springframework.beans.factory.annotation.Autowired; // 导入 Spring 自动装配注解
import org.springframework.stereotype.Service; // 导入 Spring 服务层注解
import org.springframework.util.StringUtils; // 导入 Spring 字符串工具类
import java.math.BigDecimal; // 导入 BigDecimal 高精度小数类
import java.math.RoundingMode; // 导入小数舍入模式枚举
import org.springframework.transaction.annotation.Transactional; // 导入 Spring 事务注解

@Service // 标记该类为 Spring 的服务层组件
public class ProductService extends ServiceImpl<ProductMapper, Product> { // 定义商品服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象

    /**
     * 分页查询上架商品（支持关键词、分类、排序）
     */
    public Page<Product> getOnSaleProducts(Page<Product> page, String keyword, String category, String sort) { // 获取上架商品的分页方法，参数为分页对象、关键词、分类、排序方式
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Product::getStatus, 1); // 添加条件：商品状态为已上架(1)
        if (StringUtils.hasText(keyword)) { // 如果关键词不为空
            wrapper.like(Product::getName, keyword); // 添加模糊匹配商品名称条件
        }
        if (StringUtils.hasText(category)) { // 如果分类不为空
            wrapper.eq(Product::getCategory, category); // 添加分类等值条件
        }
        // 排序
        if ("price_asc".equals(sort)) { // 如果排序方式为价格升序
            wrapper.orderByAsc(Product::getPrice); // 按价格升序排列
        } else if ("price_desc".equals(sort)) { // 如果排序方式为价格降序
            wrapper.orderByDesc(Product::getPrice); // 按价格降序排列
        } else if ("sold_desc".equals(sort)) { // 如果排序方式为销量降序
            wrapper.orderByDesc(Product::getSold); // 按销量降序排列
        } else if ("rating_desc".equals(sort)) { // 如果排序方式为评分降序
            wrapper.orderByDesc(Product::getAvgRating); // 按平均评分降序排列
        } else { // 其他情况（包括默认）
            wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序排列
        }
        return this.page(page, wrapper); // 执行分页查询并返回结果
    }

    /**
     * 发布商品（商家） - 修改：状态改为待审核(0)
     */
    public boolean publishProduct(Product product, Long userId) { // 发布商品的方法，参数为商品实体和商家ID
        User seller = userService.getById(userId); // 根据商家ID查询用户信息
        if (seller == null || seller.getRole() != 2) { // 如果商家不存在或角色不是商家
            return false; // 返回 false
        }
        product.setUserId(userId); // 设置商品所属用户ID为商家ID
        product.setShopName(seller.getShopName()); // 设置店铺名称
        // ===== 修改开始：状态改为待审核(0) =====
        product.setStatus(0);   // 0=待审核，1=已上架，设置商品状态为待审核
        // ===== 修改结束 =====
        product.setSold(0); // 初始化销量为0
        if (product.getOriginalPrice() != null && product.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) { // 如果原价不为空且大于0
            product.setDiscount(product.getPrice().divide(product.getOriginalPrice(), 2, RoundingMode.HALF_UP)); // 计算折扣 = 现价 / 原价，保留2位小数，四舍五入
        } else { // 否则
            product.setDiscount(BigDecimal.ONE); // 折扣设为1（无折扣）
        }
        return this.save(product); // 保存商品到数据库并返回是否成功
    }

    /**
     * 管理员审核商品
     * @param productId 商品ID
     * @param approved true=通过(上架), false=拒绝(下架)
     * @return 是否成功
     */
    @Transactional // 标记该方法需要在事务中执行
    public boolean auditProduct(Long productId, boolean approved) { // 审核商品的方法，参数为商品ID和是否通过
        Product product = this.getById(productId); // 根据商品ID查询商品
        if (product == null) { // 如果商品不存在
            throw new RuntimeException("商品不存在"); // 抛出运行时异常
        }
        if (product.getStatus() != 0) { // 如果商品状态不是待审核(0)
            throw new RuntimeException("商品不是待审核状态"); // 抛出运行时异常
        }
        if (approved) { // 如果审核通过
            product.setStatus(1);  // 设置商品状态为已上架(1)
        } else { // 如果审核拒绝
            product.setStatus(2);  // 设置商品状态为已下架/拒绝(2)
        }
        return this.updateById(product); // 更新商品信息并返回是否成功
    }
    
    /**
     * 商家重新提交被拒商品（将下架商品重新改为待审核）
     * @param productId 商品ID
     * @param userId 商家ID
     * @param updatedProduct 可选的更新内容（如价格、库存等）
     */
    @Transactional // 标记该方法需要在事务中执行
    public boolean resubmitProduct(Long productId, Long userId, Product updatedProduct) { // 重新提交被拒商品的方法，参数为商品ID、商家ID、可选的更新商品信息
        Product product = this.getById(productId); // 根据商品ID查询商品
        if (product == null || !product.getUserId().equals(userId)) { // 如果商品不存在或商品所属用户不是当前商家
            throw new RuntimeException("商品不存在或无权限"); // 抛出运行时异常
        }
        if (product.getStatus() != 2) { // 如果商品状态不是已下架/被拒(2)
            throw new RuntimeException("只有审核拒绝的商品才能重新提交"); // 抛出运行时异常
        }
        // 更新商品字段（若传入了新值）
        if (updatedProduct != null) { // 如果传入了更新内容
            if (updatedProduct.getPrice() != null) product.setPrice(updatedProduct.getPrice()); // 如果新价格不为空，则更新价格
            if (updatedProduct.getStock() != null) product.setStock(updatedProduct.getStock()); // 如果新库存不为空，则更新库存
            if (updatedProduct.getName() != null) product.setName(updatedProduct.getName()); // 如果新名称不为空，则更新名称
            if (updatedProduct.getDescription() != null) product.setDescription(updatedProduct.getDescription()); // 如果新描述不为空，则更新描述
            if (updatedProduct.getImages() != null) product.setImages(updatedProduct.getImages()); // 如果新图片列表不为空，则更新图片
            // ... 其他需要更新的字段
        }
        product.setStatus(0);  // 重新进入待审核，设置状态为待审核(0)
        return this.updateById(product); // 更新商品信息并返回是否成功
    }

    /**    
     * 下架商品
     */
    public boolean offlineProduct(Long productId, Long userId) { // 下架商品的方法，参数为商品ID和商家ID
        Product product = this.getById(productId); // 根据商品ID查询商品
        if (product == null || !product.getUserId().equals(userId)) { // 如果商品不存在或商品所属用户不是当前商家
            return false; // 返回 false
        }
        product.setStatus(2); // 设置商品状态为已下架(2)
        return this.updateById(product); // 更新商品信息并返回是否成功
    }

    /**
     * 上架商品（重新上架）
     */
    public boolean onlineProduct(Long productId, Long userId) { // 上架商品的方法，参数为商品ID和商家ID
        Product product = this.getById(productId); // 根据商品ID查询商品
        if (product == null || !product.getUserId().equals(userId)) { // 如果商品不存在或商品所属用户不是当前商家
            return false; // 返回 false
        }
        product.setStatus(1); // 设置商品状态为已上架(1)
        return this.updateById(product); // 更新商品信息并返回是否成功
    }

    /**
     * 获取商家的商品列表（含上下架状态）
     */
    public Page<Product> getSellerProducts(Page<Product> page, Long userId, Integer status) { // 获取商家商品列表的方法，参数为分页对象、商家ID、状态筛选
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>(); // 创建 Lambda 条件包装器
        wrapper.eq(Product::getUserId, userId); // 添加条件：商品用户ID等于商家ID
        if (status != null) { // 如果状态参数不为空
            wrapper.eq(Product::getStatus, status); // 添加状态等值条件
        }
        wrapper.orderByDesc(Product::getCreateTime); // 按创建时间倒序排序
        return this.page(page, wrapper); // 执行分页查询并返回结果
    }
}
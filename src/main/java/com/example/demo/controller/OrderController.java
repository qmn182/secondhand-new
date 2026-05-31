package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 条件构造器
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 MyBatis-Plus 分页类
import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.Order; // 导入订单实体类
import com.example.demo.entity.User; // 导入用户实体类
import com.example.demo.entity.vo.OrderVO; // 导入订单视图对象类
import com.example.demo.service.OrderService; // 导入订单服务类
import jakarta.servlet.http.HttpSession; // 导入 HttpSession 用于获取会话信息
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解

import java.util.List; // 导入 List 集合接口

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/order") // 定义该类下所有接口的公共前缀为 /order
public class OrderController { // 定义订单控制器类

    @Autowired // 自动装配 OrderService 实例
    private OrderService orderService; // 订单服务对象

    @PostMapping("/createFromCart") // 处理 POST 请求，路径为 /order/createFromCart
    public Result createFromCart(@RequestParam(required = false, defaultValue = "0") Integer usePoints, // 接收请求参数 usePoints，是否使用积分，非必传默认0
                                HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果，提示先登录
        }
        try { // 开始 try 块
            String orderNo = orderService.createOrderFromCart(user.getId(), usePoints); // 调用订单服务从购物车创建订单，返回订单号
            return Result.success(orderNo); // 返回成功结果，包含订单号
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果，异常信息作为提示
        }
    }

    @PostMapping("/deliver") // 处理 POST 请求，路径为 /order/deliver
    public Result deliver(@RequestParam String orderNo, // 接收请求参数 orderNo，订单号
                        @RequestParam String deliveryCompany, // 接收请求参数 deliveryCompany，物流公司
                        @RequestParam String deliveryNo, // 接收请求参数 deliveryNo，物流单号
                        HttpSession session) { // HttpSession 参数
        User seller = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (seller == null || seller.getRole() != 2) { // 如果用户未登录或角色不是商家(role=2)
            return Result.fail("请登录商家账号"); // 返回失败结果，提示需要登录商家账号
        }
        try { // 开始 try 块
            orderService.deliverOrder(orderNo, seller.getId(), deliveryCompany, deliveryNo); // 调用订单服务执行发货
            return Result.success("发货成功"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果
        }
    }

    @PostMapping("/confirmReceive") // 处理 POST 请求，路径为 /order/confirmReceive
    public Result confirmReceive(@RequestParam("orderNo") String orderNo, HttpSession session) { // 确认收货方法，接收订单号和 HttpSession
        User buyer = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (buyer == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        try { // 开始 try 块
            orderService.confirmReceive(orderNo, buyer.getId()); // 调用订单服务确认收货
            return Result.success("确认收货成功"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果
        }
    }

    // ===== 新增订单详情接口开始 =====
    /**
     * 获取订单详情（支持买家、卖家、管理员查看）
     * GET /order/detail/{orderNo}
     */
    @GetMapping("/detail/{orderNo}") // 处理 GET 请求，路径为 /order/detail/{orderNo}，路径变量为订单号
    public Result getOrderDetail(@PathVariable String orderNo, HttpSession session) { // 获取订单详情方法，接收订单号路径变量和 HttpSession
        try { // 开始 try 块
            User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
            if (user == null) { // 如果用户未登录
                return Result.fail("请先登录"); // 返回失败结果
            }
            // 查询订单
            Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo)); // 根据订单号查询订单
            if (order == null) { // 如果订单不存在
                return Result.fail("订单不存在"); // 返回失败结果
            }
            // 权限校验：只有买家、卖家或管理员可查看
            Long sellerId = orderService.getSellerIdByOrder(order.getId()); // 调用订单服务获取订单对应的商家ID
            if (!order.getUserId().equals(user.getId()) && !sellerId.equals(user.getId()) && user.getRole() != 3) { // 如果当前用户不是买家、不是卖家、也不是管理员
                return Result.fail("无权限查看此订单"); // 返回失败结果
            }
            // 转换为 VO（包含商品明细）—— convertToVO 已改为 public
            OrderVO vo = orderService.convertToVO(order); // 将订单实体转换为视图对象
            // 补充物流信息（如果已发货）—— 如果 OrderVO 没有这两个字段，请注释掉
            if (order.getDeliveryCompany() != null) { // 如果物流公司不为空
                vo.setDeliveryCompany(order.getDeliveryCompany()); // 设置物流公司到 VO
                vo.setDeliveryNo(order.getDeliveryNo()); // 设置物流单号到 VO
            }
            return Result.success(vo); // 返回成功结果，包含订单 VO
        } catch (Exception e) { // 捕获通用异常
            e.printStackTrace(); // 打印详细错误到控制台
            return Result.fail("服务器内部错误：" + e.getMessage()); // 返回失败结果，包含错误信息
        }
    }
    // ===== 新增订单详情接口结束 =====

    // 以下方法保持不变（买家订单列表、商家订单列表、退货申请等）
    @GetMapping("/user/orders") // 处理 GET 请求，路径为 /order/user/orders
    public Result getUserOrders(@RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                                @RequestParam(defaultValue = "10") Integer size, // 接收每页大小，默认10
                                @RequestParam(required = false) Integer status, // 接收订单状态筛选，非必传
                                HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        Page<OrderVO> orders = orderService.getUserOrders(user.getId(), status, page, size); // 调用订单服务获取用户的订单分页数据
        return Result.success(orders); // 返回成功结果，包含订单 VO 分页对象
    }

    @GetMapping("/merchant/orders") // 处理 GET 请求，路径为 /order/merchant/orders
    public Result getMerchantOrders(@RequestParam(defaultValue = "1") Integer page, // 接收分页页码，默认1
                                    @RequestParam(defaultValue = "10") Integer size, // 接收每页大小，默认10
                                    @RequestParam(required = false) Integer status, // 接收订单状态筛选，非必传
                                    HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null || user.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("请登录商家账号"); // 返回失败结果
        }
        Page<OrderVO> orders = orderService.getMerchantOrders(user.getId(), status, page, size); // 调用订单服务获取商家的订单分页数据
        return Result.success(orders); // 返回成功结果
    }

    @PostMapping("/refund/apply") // 处理 POST 请求，路径为 /order/refund/apply
    public Result applyRefund(@RequestParam Long orderId, // 接收请求参数 orderId，订单ID
                            @RequestParam Long orderItemId, // 接收请求参数 orderItemId，订单项ID
                            @RequestParam String reason, // 接收请求参数 reason，退货原因
                            @RequestParam(required = false) List<String> images, // 接收请求参数 images，凭证图片列表，非必传
                            HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (user == null) { // 如果用户未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        try { // 开始 try 块
            orderService.applyRefund(orderId, orderItemId, user.getId(), reason, images); // 调用订单服务申请退货
            return Result.success("退货申请已提交，等待商家审核"); // 返回成功结果
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果
        }
    }

    @PostMapping("/refund/audit") // 处理 POST 请求，路径为 /order/refund/audit
    public Result auditRefund(@RequestParam Long refundId, // 接收请求参数 refundId，退款申请ID
                            @RequestParam Boolean approved, // 接收请求参数 approved，是否通过审核
                            @RequestParam(required = false) String rejectReason, // 接收请求参数 rejectReason，拒绝理由，非必传
                            HttpSession session) { // HttpSession 参数
        User seller = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户对象
        if (seller == null || seller.getRole() != 2) { // 如果用户未登录或角色不是商家
            return Result.fail("请登录商家账号"); // 返回失败结果
        }
        if (!approved && (rejectReason == null || rejectReason.trim().isEmpty())) { // 如果拒绝且拒绝理由为空或仅空白
            return Result.fail("拒绝时必须填写拒绝理由"); // 返回失败结果
        }
        try { // 开始 try 块
            orderService.auditRefund(refundId, seller.getId(), approved, rejectReason); // 调用订单服务审核退款申请
            String msg = approved ? "已同意退货，退款已到账" : "已拒绝退货，理由：" + rejectReason; // 根据审核结果生成消息
            return Result.success(msg); // 返回成功结果，包含消息
        } catch (RuntimeException e) { // 捕获运行时异常
            return Result.fail(e.getMessage()); // 返回失败结果
        }
    }
}
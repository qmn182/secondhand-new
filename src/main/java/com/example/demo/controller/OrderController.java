package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.Result;
import com.example.demo.entity.Order;
import com.example.demo.entity.User;
import com.example.demo.entity.vo.OrderVO;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/createFromCart")
    public Result createFromCart(@RequestParam(required = false, defaultValue = "0") Integer usePoints,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        try {
            String orderNo = orderService.createOrderFromCart(user.getId(), usePoints);
            return Result.success(orderNo);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/deliver")
    public Result deliver(@RequestParam String orderNo,
                        @RequestParam String deliveryCompany,
                        @RequestParam String deliveryNo,
                        HttpSession session) {
        User seller = (User) session.getAttribute("user");
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        try {
            orderService.deliverOrder(orderNo, seller.getId(), deliveryCompany, deliveryNo);
            return Result.success("发货成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/confirmReceive")
    public Result confirmReceive(@RequestParam("orderNo") String orderNo, HttpSession session) {
        User buyer = (User) session.getAttribute("user");
        if (buyer == null) {
            return Result.fail("请先登录");
        }
        try {
            orderService.confirmReceive(orderNo, buyer.getId());
            return Result.success("确认收货成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    // ===== 新增订单详情接口开始 =====
    /**
     * 获取订单详情（支持买家、卖家、管理员查看）
     * GET /order/detail/{orderNo}
     */
    @GetMapping("/detail/{orderNo}")
    public Result getOrderDetail(@PathVariable String orderNo, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                return Result.fail("请先登录");
            }
            // 查询订单
            Order order = orderService.getOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                return Result.fail("订单不存在");
            }
            // 权限校验：只有买家、卖家或管理员可查看
            Long sellerId = orderService.getSellerIdByOrder(order.getId());
            if (!order.getUserId().equals(user.getId()) && !sellerId.equals(user.getId()) && user.getRole() != 3) {
                return Result.fail("无权限查看此订单");
            }
            // 转换为 VO（包含商品明细）—— convertToVO 已改为 public
            OrderVO vo = orderService.convertToVO(order);
            // 补充物流信息（如果已发货）—— 如果 OrderVO 没有这两个字段，请注释掉
            if (order.getDeliveryCompany() != null) {
                vo.setDeliveryCompany(order.getDeliveryCompany());
                vo.setDeliveryNo(order.getDeliveryNo());
            }
            return Result.success(vo);
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细错误到控制台
            return Result.fail("服务器内部错误：" + e.getMessage());
        }
    }
    // ===== 新增订单详情接口结束 =====

    // 以下方法保持不变（买家订单列表、商家订单列表、退货申请等）
    @GetMapping("/user/orders")
    public Result getUserOrders(@RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(required = false) Integer status,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        Page<OrderVO> orders = orderService.getUserOrders(user.getId(), status, page, size);
        return Result.success(orders);
    }

    @GetMapping("/merchant/orders")
    public Result getMerchantOrders(@RequestParam(defaultValue = "1") Integer page,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    @RequestParam(required = false) Integer status,
                                    HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        Page<OrderVO> orders = orderService.getMerchantOrders(user.getId(), status, page, size);
        return Result.success(orders);
    }

    @PostMapping("/refund/apply")
    public Result applyRefund(@RequestParam Long orderId,
                            @RequestParam Long orderItemId,
                            @RequestParam String reason,
                            @RequestParam(required = false) List<String> images,
                            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        try {
            orderService.applyRefund(orderId, orderItemId, user.getId(), reason, images);
            return Result.success("退货申请已提交，等待商家审核");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/refund/audit")
    public Result auditRefund(@RequestParam Long refundId,
                            @RequestParam Boolean approved,
                            @RequestParam(required = false) String rejectReason,
                            HttpSession session) {
        User seller = (User) session.getAttribute("user");
        if (seller == null || seller.getRole() != 2) {
            return Result.fail("请登录商家账号");
        }
        if (!approved && (rejectReason == null || rejectReason.trim().isEmpty())) {
            return Result.fail("拒绝时必须填写拒绝理由");
        }
        try {
            orderService.auditRefund(refundId, seller.getId(), approved, rejectReason);
            String msg = approved ? "已同意退货，退款已到账" : "已拒绝退货，理由：" + rejectReason;
            return Result.success(msg);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
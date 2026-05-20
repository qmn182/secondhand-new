package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import com.example.demo.entity.vo.OrderVO;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;


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

    /**
     * 买家订单列表
     * GET /order/user/orders?page=1&size=10&status=2
     */
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

    /**
     * 买家申请退货
     */
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

    /**
     * 商家审核退货
     */
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
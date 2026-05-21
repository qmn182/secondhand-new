package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.common.Result;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private DefaultKaptcha captchaProducer;

    @GetMapping("/captcha")
    public Result captcha(HttpSession session) throws Exception {
        String capText = captchaProducer.createText();
        session.setAttribute("code", capText);
        BufferedImage image = captchaProducer.createImage(capText);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        return Result.success(base64);
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user,
                           @RequestParam String code,
                           HttpSession session) {
        String realCode = (String) session.getAttribute("code");
        if (realCode == null || !realCode.equalsIgnoreCase(code)) {
            return Result.fail("验证码错误");
        }
        // 检查用户名是否存在
        User exist = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (exist != null) {
            return Result.fail("用户名已存在");
        }
        user.setStatus(0);     // 待审核
        user.setRole(1);       // 普通用户
        userService.registerUser(user);   // 密码自动加密
        return Result.success("注册成功，待审核");
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user, HttpSession session) {
        User loginUser = userService.loginUser(user.getUsername(), user.getPassword());
        if (loginUser == null) {
            return Result.fail("账号或密码错误");
        }
        if (loginUser.getStatus() != 1) {
            return Result.fail("账号未审核");
        }
        // 保存用户信息到 session
        session.setAttribute("user", loginUser);
        loginUser.setPassword(null);
        return Result.success(loginUser);
    }


    @GetMapping("/current")
    public Result current(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.fail("未登录");
        user.setPassword(null);
        return Result.success(user);
    }
    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        session.removeAttribute("user");
        session.invalidate();
        return Result.success("已退出");
    }
    /**
     * 获取当前用户的余额
     */
    @GetMapping("/wallet/balance")
    public Result getBalance(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        return Result.success(user.getWallet());
    }

    /**
     * 充值（需验证密码）
     * @param amount 充值金额
     * @param password 银行卡密码（默认0000）
     */
    @PostMapping("/wallet/recharge")
    public Result recharge(@RequestParam BigDecimal amount,
                        @RequestParam String password,
                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        // 验证密码（简单实现，实际应加密）
        if (!"0000".equals(password)) {
            return Result.fail("银行卡密码错误");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("充值金额必须大于0");
        }
        user.setWallet(user.getWallet().add(amount));
        userService.updateById(user);
        // 可选：记录交易流水（略）
        return Result.success("充值成功，当前余额：" + user.getWallet());
    }

    /**
     * 提现（需验证密码且余额足够）
     * @param amount 提现金额
     * @param password 银行卡密码
     */
    @PostMapping("/wallet/withdraw")
    public Result withdraw(@RequestParam BigDecimal amount,
                        @RequestParam String password,
                        HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (!"0000".equals(password)) {
            return Result.fail("银行卡密码错误");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("提现金额必须大于0");
        }
        if (user.getWallet().compareTo(amount) < 0) {
            return Result.fail("余额不足");
        }
        user.setWallet(user.getWallet().subtract(amount));
        userService.updateById(user);
        return Result.success("提现成功，当前余额：" + user.getWallet());
    }
    /**
     * 普通用户申请成为商家
     * @param shopName 店铺名称
     * @param businessLicense 营业执照图片文件
     * @param idCardImage 身份证图片文件
     * @param session 当前会话
     * @return 申请结果
     */
    @PostMapping("/applyMerchant")
    public Result applyMerchant(@RequestParam String shopName,
                                @RequestParam("license") MultipartFile businessLicense,
                                @RequestParam("idCard") MultipartFile idCardImage,
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.fail("请先登录");
        }
        if (user.getRole() != 1) {
            return Result.fail("只有普通用户可以申请成为商家");
        }
        if (user.getMerchantStatus() != null && user.getMerchantStatus() == 1) {
            return Result.fail("您已提交申请，请等待审核");
        }
        // 保存图片
        String licensePath = saveFile(businessLicense, "license");
        String idCardPath = saveFile(idCardImage, "idcard");
        if (licensePath == null || idCardPath == null) {
            return Result.fail("图片上传失败");
        }
        user.setShopName(shopName);
        user.setBusinessLicense(licensePath);
        user.setIdCardImage(idCardPath);
        user.setMerchantStatus(1); // 待审核
        userService.updateById(user);
        return Result.success("申请已提交，请等待管理员审核");
    }

    // 辅助方法：保存文件并返回访问路径
    private String saveFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) return null;
        try {
            String originalName = file.getOriginalFilename();
            String ext = originalName.substring(originalName.lastIndexOf("."));
            String newName = UUID.randomUUID().toString() + ext;
            String uploadDir = "D:/secondhand/demo/uploads/" + subDir;
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, newName);
            file.transferTo(dest);
            return "/uploads/" + subDir + "/" + newName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 管理员审核商家申请
     * @param userId 用户ID
     * @param approved 是否通过 (true/false)
     */
    @PostMapping("/admin/auditMerchant")
    public Result auditMerchant(@RequestParam Long userId, @RequestParam Boolean approved) {
        User user = userService.getById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (user.getMerchantStatus() != 1) {
            return Result.fail("该用户未申请商家或已处理");
        }
        if (approved) {
            user.setRole(2);               // 升级为商家
            user.setMerchantStatus(2);     // 已通过
        } else {
            user.setMerchantStatus(3);     // 拒绝
        }
        userService.updateById(user);
        return Result.success(approved ? "商家申请已通过" : "已拒绝");
    }
    @GetMapping("/admin/merchantApplications")
    public Result getMerchantApplications() {
        List<User> applicants = userService.lambdaQuery()
                .eq(User::getMerchantStatus, 1)
                .list();
        return Result.success(applicants);
    }
    // ========== 管理员接口（需要角色=3） ==========
    @GetMapping("/admin/list")
    public Result list(HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        return Result.success(userService.list());
    }

    @GetMapping("/admin/audit")
    public Result audit(@RequestParam Long id, @RequestParam Integer status, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        User user = userService.getById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
        return Result.success("审核成功");
    }
    /**
     * 更新个人信息（登录用户自己修改）
     * 可修改字段：phone, email, city, gender, bankAccount
     */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody User updatedUser, HttpSession session) {
        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) return Result.fail("请先登录");

        User user = userService.getById(loginUser.getId());
        if (user == null) return Result.fail("用户不存在");

        // 只更新允许修改的字段（没有 avatar）
        if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone());
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if (updatedUser.getCity() != null) user.setCity(updatedUser.getCity());
        if (updatedUser.getGender() != null) user.setGender(updatedUser.getGender());
        if (updatedUser.getBankAccount() != null) user.setBankAccount(updatedUser.getBankAccount());

        userService.updateById(user);
        session.setAttribute("user", user);   // 更新 session
        return Result.success("更新成功");
    }
    /**
     * 管理员修改用户信息（角色、状态、基本信息等）
     * PUT /user/admin/update
     */
    @PutMapping("/admin/update")
    public Result adminUpdateUser(@RequestBody User updatedUser, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        if (updatedUser.getId() == null) {
            return Result.fail("用户ID不能为空");
        }
        User user = userService.getById(updatedUser.getId());
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 只允许管理员修改以下字段
        if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone());
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail());
        if (updatedUser.getCity() != null) user.setCity(updatedUser.getCity());
        if (updatedUser.getGender() != null) user.setGender(updatedUser.getGender());
        if (updatedUser.getBankAccount() != null) user.setBankAccount(updatedUser.getBankAccount());
        if (updatedUser.getRole() != null) user.setRole(updatedUser.getRole());
        if (updatedUser.getStatus() != null) user.setStatus(updatedUser.getStatus());
        if (updatedUser.getShopName() != null) user.setShopName(updatedUser.getShopName());

        userService.updateById(user);
        return Result.success("修改成功");
    }

    /**
     * 管理员为用户充值（直接增加钱包余额）
     * POST /user/admin/recharge
     */
    @PostMapping("/admin/recharge")
    public Result adminRecharge(@RequestParam Long userId, @RequestParam BigDecimal amount, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("参数错误，金额必须大于0");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        user.setWallet(user.getWallet().add(amount));
        userService.updateById(user);
        // 可选：记录管理员充值流水（建议添加 transaction）
        return Result.success("充值成功，当前余额：" + user.getWallet());
    }

    /**
     * 管理员删除用户
     * DELETE /user/admin/delete/{userId}
     */
    @DeleteMapping("/admin/delete/{userId}")
    public Result adminDeleteUser(@PathVariable Long userId, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || admin.getRole() != 3) {
            return Result.fail("无权限");
        }
        if (userId == null) {
            return Result.fail("用户ID不能为空");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 不能删除自己
        if (userId.equals(admin.getId())) {
            return Result.fail("不能删除当前登录的管理员账号");
        }
        boolean removed = userService.removeById(userId);
        return removed ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
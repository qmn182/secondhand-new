package com.example.demo.controller; // 定义包名为 com.example.demo.controller

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 条件构造器
import com.example.demo.common.Result; // 导入统一响应结果类
import com.example.demo.entity.User; // 导入用户实体类
import com.example.demo.entity.vo.OrderVO; // 导入订单视图对象类（此处虽导入但未使用，按原样保留）
import com.example.demo.entity.vo.ShopProductVO; // 导入店铺商品视图对象类（此处虽导入但未使用，按原样保留）
import com.example.demo.entity.PointsRecord; // 导入积分记录实体类
import com.example.demo.entity.Transaction; // 导入交易流水实体类

import com.example.demo.service.UserService; // 导入用户服务类
import com.google.code.kaptcha.impl.DefaultKaptcha; // 导入 Kaptcha 验证码生成器类
import jakarta.servlet.http.HttpSession; // 导入 HttpSession 用于获取会话信息
import org.springframework.beans.factory.annotation.Autowired; // 导入自动装配注解
import org.springframework.web.bind.annotation.*; // 导入 Spring MVC Web 相关注解
import org.springframework.web.multipart.MultipartFile; // 导入文件上传 MultipartFile 类
import com.example.demo.service.PointsRecordService; // 导入积分记录服务类
import com.example.demo.service.TransactionService; // 导入交易流水服务类

import javax.imageio.ImageIO; // 导入图片 IO 工具类
import java.awt.image.BufferedImage; // 导入缓冲图像类
import java.io.ByteArrayOutputStream; // 导入字节数组输出流
import java.io.File; // 导入 File 类用于文件操作
import java.io.IOException; // 导入 IOException 异常类
import java.math.BigDecimal; // 导入高精度小数类
import java.util.Base64; // 导入 Base64 编解码工具
import java.util.List; // 导入 List 集合接口
import java.util.UUID; // 导入 UUID 工具类

@RestController // 标记该类为 REST 控制器，所有方法返回 JSON
@RequestMapping("/user") // 定义该类下所有接口的公共前缀为 /user
public class UserController { // 定义用户控制器类

    @Autowired // 自动装配 UserService 实例
    private UserService userService; // 用户服务对象

    @Autowired // 自动装配 DefaultKaptcha 实例
    private DefaultKaptcha captchaProducer; // 验证码生成器对象
    @Autowired // 自动装配 PointsRecordService 实例
    private PointsRecordService pointsRecordService; // 积分记录服务对象
    @Autowired // 自动装配 TransactionService 实例
    private TransactionService transactionService; // 交易流水服务对象
    @GetMapping("/captcha") // 处理 GET 请求，路径为 /user/captcha
    public Result captcha(HttpSession session) throws Exception { // 生成验证码方法，接收 HttpSession
        String capText = captchaProducer.createText(); // 生成验证码文本
        session.setAttribute("code", capText); // 将验证码文本存入 session
        BufferedImage image = captchaProducer.createImage(capText); // 根据文本生成验证码图片
        ByteArrayOutputStream out = new ByteArrayOutputStream(); // 创建字节数组输出流
        ImageIO.write(image, "png", out); // 将图片以 PNG 格式写入输出流
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray()); // 将图片转为 Base64 编码字符串
        return Result.success(base64); // 返回成功结果，包含 Base64 图片数据
    }

    @PostMapping("/register") // 处理 POST 请求，路径为 /user/register
    public Result register(@RequestBody User user, // 接收请求体中的用户对象
                           @RequestParam String code, // 接收请求参数 code（验证码）
                           HttpSession session) { // HttpSession 参数
        String realCode = (String) session.getAttribute("code"); // 从 session 中获取正确的验证码
        if (realCode == null || !realCode.equalsIgnoreCase(code)) { // 如果验证码不存在或不匹配（忽略大小写）
            return Result.fail("验证码错误"); // 返回失败结果
        }
        // 检查用户名是否存在
        User exist = userService.getOne(new LambdaQueryWrapper<User>() // 根据用户名查询用户
                .eq(User::getUsername, user.getUsername())); // 添加条件：用户名匹配
        if (exist != null) { // 如果用户已存在
            return Result.fail("用户名已存在"); // 返回失败结果
        }
        user.setStatus(0);     // 设置状态为0（待审核）
        user.setRole(1);       // 设置角色为1（普通用户）
        userService.registerUser(user);   // 调用注册服务（密码自动加密）
        return Result.success("注册成功，待审核"); // 返回成功结果
    }

    @PostMapping("/login") // 处理 POST 请求，路径为 /user/login
    public Result login(@RequestBody User user, HttpSession session) { // 登录方法，接收用户对象和 HttpSession
        User loginUser = userService.loginUser(user.getUsername(), user.getPassword()); // 调用登录服务验证账号密码
        if (loginUser == null) { // 如果登录失败
            return Result.fail("账号或密码错误"); // 返回失败结果
        }
        if (loginUser.getStatus() != 1) { // 如果用户状态不是已审核（status != 1）
            return Result.fail("账号未审核"); // 返回失败结果
        }
        // 保存用户信息到 session
        session.setAttribute("user", loginUser); // 将用户对象存入 session
        loginUser.setPassword(null); // 清空密码字段，避免返回前端
        return Result.success(loginUser); // 返回成功结果，包含用户信息
    }


    @GetMapping("/current") // 处理 GET 请求，路径为 /user/current
    public Result current(HttpSession session) { // 获取当前登录用户信息方法，接收 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) return Result.fail("未登录"); // 如果未登录，返回失败结果
        user.setPassword(null); // 清空密码
        return Result.success(user); // 返回成功结果，包含用户信息
    }
    @PostMapping("/logout") // 处理 POST 请求，路径为 /user/logout
    public Result logout(HttpSession session) { // 退出登录方法，接收 HttpSession
        session.removeAttribute("user"); // 移除 session 中的用户属性
        session.invalidate(); // 使 session 失效
        return Result.success("已退出"); // 返回成功结果
    }
    /**
     * 获取当前用户的余额
     */
    @GetMapping("/wallet/balance") // 处理 GET 请求，路径为 /user/wallet/balance
    public Result getBalance(HttpSession session) { // 获取钱包余额方法，接收 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) { // 如果未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        return Result.success(user.getWallet()); // 返回成功结果，包含钱包余额
    }

    /**
     * 充值（需验证密码）
     * @param amount 充值金额
     * @param password 银行卡密码（默认0000）
     */
    @PostMapping("/wallet/recharge") // 处理 POST 请求，路径为 /user/wallet/recharge
    public Result recharge(@RequestParam BigDecimal amount, // 接收充值金额参数
                        @RequestParam String password, // 接收银行卡密码参数
                        HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) { // 如果未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        // 验证密码（简单实现，实际应加密）
        if (!"0000".equals(password)) { // 如果密码不是默认0000
            return Result.fail("银行卡密码错误"); // 返回失败结果
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) { // 如果金额为空或小于等于0
            return Result.fail("充值金额必须大于0"); // 返回失败结果
        }
        user.setWallet(user.getWallet().add(amount)); // 钱包余额增加充值金额
        userService.updateById(user); // 更新用户信息
        // 可选：记录交易流水（略）
        return Result.success("充值成功，当前余额：" + user.getWallet()); // 返回成功结果，包含当前余额
    }

    /**
     * 提现（需验证密码且余额足够）
     * @param amount 提现金额
     * @param password 银行卡密码
     */
    @PostMapping("/wallet/withdraw") // 处理 POST 请求，路径为 /user/wallet/withdraw
    public Result withdraw(@RequestParam BigDecimal amount, // 接收提现金额参数
                        @RequestParam String password, // 接收银行卡密码参数
                        HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) { // 如果未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        if (!"0000".equals(password)) { // 如果密码不是默认0000
            return Result.fail("银行卡密码错误"); // 返回失败结果
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) { // 如果金额为空或小于等于0
            return Result.fail("提现金额必须大于0"); // 返回失败结果
        }
        if (user.getWallet().compareTo(amount) < 0) { // 如果钱包余额小于提现金额
            return Result.fail("余额不足"); // 返回失败结果
        }
        user.setWallet(user.getWallet().subtract(amount)); // 钱包余额减去提现金额
        userService.updateById(user); // 更新用户信息
        return Result.success("提现成功，当前余额：" + user.getWallet()); // 返回成功结果，包含当前余额
    }
    /**
     * 普通用户申请成为商家
     * @param shopName 店铺名称
     * @param businessLicense 营业执照图片文件
     * @param idCardImage 身份证图片文件
     * @param session 当前会话
     * @return 申请结果
     */
    @PostMapping("/applyMerchant") // 处理 POST 请求，路径为 /user/applyMerchant
    public Result applyMerchant(@RequestParam String shopName, // 接收店铺名称参数
                                @RequestParam("license") MultipartFile businessLicense, // 接收营业执照文件，参数名 license
                                @RequestParam("idCard") MultipartFile idCardImage, // 接收身份证图片文件，参数名 idCard
                                HttpSession session) { // HttpSession 参数
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) { // 如果未登录
            return Result.fail("请先登录"); // 返回失败结果
        }
        if (user.getRole() != 1) { // 如果用户角色不是普通用户(role=1)
            return Result.fail("只有普通用户可以申请成为商家"); // 返回失败结果
        }
        if (user.getMerchantStatus() != null && user.getMerchantStatus() == 1) { // 如果已提交过申请且状态为待审核(1)
            return Result.fail("您已提交申请，请等待审核"); // 返回失败结果
        }
        // 保存图片
        String licensePath = saveFile(businessLicense, "license"); // 保存营业执照图片，返回访问路径
        String idCardPath = saveFile(idCardImage, "idcard"); // 保存身份证图片，返回访问路径
        if (licensePath == null || idCardPath == null) { // 如果任一图片保存失败
            return Result.fail("图片上传失败"); // 返回失败结果
        }
        user.setShopName(shopName); // 设置店铺名称
        user.setBusinessLicense(licensePath); // 设置营业执照路径
        user.setIdCardImage(idCardPath); // 设置身份证图片路径
        user.setMerchantStatus(1); // 设置商家申请状态为1（待审核）
        userService.updateById(user); // 更新用户信息
        return Result.success("申请已提交，请等待管理员审核"); // 返回成功结果
    }

    // 辅助方法：保存文件并返回访问路径
    private String saveFile(MultipartFile file, String subDir) { // 私有方法：保存上传文件，参数为文件和子目录名
        if (file == null || file.isEmpty()) return null; // 如果文件为空则返回 null
        try { // 开始 try 块
            String originalName = file.getOriginalFilename(); // 获取原始文件名
            String ext = originalName.substring(originalName.lastIndexOf(".")); // 提取文件扩展名（包含点）
            String newName = UUID.randomUUID().toString() + ext; // 生成新的唯一文件名
            String uploadDir = "D:/secondhand/demo/uploads/" + subDir; // 构造上传目录路径
            File dir = new File(uploadDir); // 创建目录 File 对象
            if (!dir.exists()) dir.mkdirs(); // 如果目录不存在则创建多级目录
            File dest = new File(dir, newName); // 创建目标文件对象
            file.transferTo(dest); // 将上传文件传输到目标文件
            return "/uploads/" + subDir + "/" + newName; // 返回可访问的 URL 路径
        } catch (IOException e) { // 捕获 IO 异常
            e.printStackTrace(); // 打印异常堆栈
            return null; // 返回 null
        }
    }
    /**
     * 管理员审核商家申请
     * @param userId 用户ID
     * @param approved 是否通过 (true/false)
     */
    @PostMapping("/admin/auditMerchant") // 处理 POST 请求，路径为 /user/admin/auditMerchant
    public Result auditMerchant(@RequestParam Long userId, @RequestParam Boolean approved) { // 审核商家申请方法，接收用户ID和是否通过
        User user = userService.getById(userId); // 根据用户ID查询用户信息
        if (user == null) return Result.fail("用户不存在"); // 如果用户不存在，返回失败结果
        if (user.getMerchantStatus() != 1) { // 如果商家申请状态不是待审核(1)
            return Result.fail("该用户未申请商家或已处理"); // 返回失败结果
        }
        if (approved) { // 如果审核通过
            user.setRole(2);               // 升级为商家（角色=2）
            user.setMerchantStatus(2);     // 设置申请状态为2（已通过）
        } else { // 如果审核拒绝
            user.setMerchantStatus(3);     // 设置申请状态为3（拒绝）
        }
        userService.updateById(user); // 更新用户信息
        return Result.success(approved ? "商家申请已通过" : "已拒绝"); // 返回成功结果，根据结果返回不同消息
    }
    @GetMapping("/admin/merchantApplications") // 处理 GET 请求，路径为 /user/admin/merchantApplications
    public Result getMerchantApplications() { // 获取待审核商家申请列表方法
        List<User> applicants = userService.lambdaQuery() // 使用 Lambda 查询
                .eq(User::getMerchantStatus, 1) // 添加条件：商家申请状态为待审核(1)
                .list(); // 执行查询，返回列表
        return Result.success(applicants); // 返回成功结果，包含申请人列表
    }
    // ========== 管理员接口（需要角色=3） ==========
    @GetMapping("/admin/list") // 处理 GET 请求，路径为 /user/admin/list
    public Result list(HttpSession session) { // 获取所有用户列表方法（管理员），接收 HttpSession
        User admin = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (admin == null || admin.getRole() != 3) { // 如果未登录或角色不是管理员(role=3)
            return Result.fail("无权限"); // 返回失败结果
        }
        return Result.success(userService.list()); // 返回成功结果，包含所有用户列表
    }

    @GetMapping("/admin/audit") // 处理 GET 请求，路径为 /user/admin/audit
    public Result audit(@RequestParam Long id, @RequestParam Integer status, HttpSession session) { // 审核用户注册方法，接收用户ID、状态和 HttpSession
        User admin = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (admin == null || admin.getRole() != 3) { // 如果未登录或角色不是管理员
            return Result.fail("无权限"); // 返回失败结果
        }
        User user = userService.getById(id); // 根据ID查询用户
        if (user == null) { // 如果用户不存在
            return Result.fail("用户不存在"); // 返回失败结果
        }
        user.setStatus(status); // 设置用户状态（0待审核/1已审核/2禁用等）
        userService.updateById(user); // 更新用户
        return Result.success("审核成功"); // 返回成功结果
    }
    /**
     * 更新个人信息（登录用户自己修改）
     * 可修改字段：phone, email, city, gender, bankAccount
     */
    @PutMapping("/profile") // 处理 PUT 请求，路径为 /user/profile
    public Result updateProfile(@RequestBody User updatedUser, HttpSession session) { // 更新个人信息方法，接收更新的用户对象和 HttpSession
        User loginUser = (User) session.getAttribute("user"); // 从 session 中获取当前登录用户
        if (loginUser == null) return Result.fail("请先登录"); // 如果未登录，返回失败结果

        User user = userService.getById(loginUser.getId()); // 根据登录用户ID查询完整用户信息
        if (user == null) return Result.fail("用户不存在"); // 如果用户不存在，返回失败结果

        // 只更新允许修改的字段（没有 avatar）
        if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone()); // 如果传入了手机号则更新
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail()); // 如果传入了邮箱则更新
        if (updatedUser.getCity() != null) user.setCity(updatedUser.getCity()); // 如果传入了城市则更新
        if (updatedUser.getGender() != null) user.setGender(updatedUser.getGender()); // 如果传入了性别则更新
        if (updatedUser.getBankAccount() != null) user.setBankAccount(updatedUser.getBankAccount()); // 如果传入了银行卡号则更新

        userService.updateById(user); // 更新用户信息
        session.setAttribute("user", user);   // 更新 session 中的用户信息
        return Result.success("更新成功"); // 返回成功结果
    }
    /**
     * 管理员修改用户信息（角色、状态、基本信息等）
     * PUT /user/admin/update
     */
    @PutMapping("/admin/update") // 处理 PUT 请求，路径为 /user/admin/update
    public Result adminUpdateUser(@RequestBody User updatedUser, HttpSession session) { // 管理员更新用户信息方法，接收更新的用户对象和 HttpSession
        User admin = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (admin == null || admin.getRole() != 3) { // 如果未登录或角色不是管理员
            return Result.fail("无权限"); // 返回失败结果
        }
        if (updatedUser.getId() == null) { // 如果更新的用户ID为空
            return Result.fail("用户ID不能为空"); // 返回失败结果
        }
        User user = userService.getById(updatedUser.getId()); // 根据ID查询用户
        if (user == null) { // 如果用户不存在
            return Result.fail("用户不存在"); // 返回失败结果
        }
        // 只允许管理员修改以下字段
        if (updatedUser.getPhone() != null) user.setPhone(updatedUser.getPhone()); // 如果传入了手机号则更新
        if (updatedUser.getEmail() != null) user.setEmail(updatedUser.getEmail()); // 如果传入了邮箱则更新
        if (updatedUser.getCity() != null) user.setCity(updatedUser.getCity()); // 如果传入了城市则更新
        if (updatedUser.getGender() != null) user.setGender(updatedUser.getGender()); // 如果传入了性别则更新
        if (updatedUser.getBankAccount() != null) user.setBankAccount(updatedUser.getBankAccount()); // 如果传入了银行卡号则更新
        if (updatedUser.getRole() != null) user.setRole(updatedUser.getRole()); // 如果传入了角色则更新
        if (updatedUser.getStatus() != null) user.setStatus(updatedUser.getStatus()); // 如果传入了状态则更新
        if (updatedUser.getShopName() != null) user.setShopName(updatedUser.getShopName()); // 如果传入了店铺名称则更新

        userService.updateById(user); // 更新用户信息
        return Result.success("修改成功"); // 返回成功结果
    }

    /**
     * 管理员为用户充值（直接增加钱包余额）
     * POST /user/admin/recharge
     */
    @PostMapping("/admin/recharge") // 处理 POST 请求，路径为 /user/admin/recharge
    public Result adminRecharge(@RequestParam Long userId, @RequestParam BigDecimal amount, HttpSession session) { // 管理员充值方法，接收用户ID、金额和 HttpSession
        User admin = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (admin == null || admin.getRole() != 3) { // 如果未登录或角色不是管理员
            return Result.fail("无权限"); // 返回失败结果
        }
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) { // 如果参数无效或金额不大于0
            return Result.fail("参数错误，金额必须大于0"); // 返回失败结果
        }
        User user = userService.getById(userId); // 根据用户ID查询用户
        if (user == null) { // 如果用户不存在
            return Result.fail("用户不存在"); // 返回失败结果
        }
        user.setWallet(user.getWallet().add(amount)); // 钱包余额增加指定金额
        userService.updateById(user); // 更新用户信息
        // 可选：记录管理员充值流水（建议添加 transaction）
        return Result.success("充值成功，当前余额：" + user.getWallet()); // 返回成功结果，包含当前余额
    }

    /**
     * 管理员删除用户
     * DELETE /user/admin/delete/{userId}
     */
    @DeleteMapping("/admin/delete/{userId}") // 处理 DELETE 请求，路径为 /user/admin/delete/{userId}
    public Result adminDeleteUser(@PathVariable Long userId, HttpSession session) { // 管理员删除用户方法，接收用户ID路径变量和 HttpSession
        User admin = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (admin == null || admin.getRole() != 3) { // 如果未登录或角色不是管理员
            return Result.fail("无权限"); // 返回失败结果
        }
        if (userId == null) { // 如果用户ID为空
            return Result.fail("用户ID不能为空"); // 返回失败结果
        }
        User user = userService.getById(userId); // 根据用户ID查询用户
        if (user == null) { // 如果用户不存在
            return Result.fail("用户不存在"); // 返回失败结果
        }
        // 不能删除自己
        if (userId.equals(admin.getId())) { // 如果要删除的用户ID等于当前管理员ID
            return Result.fail("不能删除当前登录的管理员账号"); // 返回失败结果
        }
        boolean removed = userService.removeById(userId); // 执行删除操作
        return removed ? Result.success("删除成功") : Result.fail("删除失败"); // 成功返回成功消息，失败返回失败消息
    }
    // 获取当前用户的积分记录
    @GetMapping("/points/records") // 处理 GET 请求，路径为 /user/points/records
    public Result getPointsRecords(HttpSession session) { // 获取积分记录方法，接收 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) return Result.fail("未登录"); // 如果未登录，返回失败结果
        List<PointsRecord> records = pointsRecordService.list( // 查询积分记录列表
            new LambdaQueryWrapper<PointsRecord>().eq(PointsRecord::getUserId, user.getId()) // 添加条件：用户ID匹配
                .orderByDesc(PointsRecord::getCreateTime) // 按创建时间倒序排序
        );
        return Result.success(records); // 返回成功结果，包含积分记录列表
    }

    // 获取当前用户的交易流水
    @GetMapping("/transaction/records") // 处理 GET 请求，路径为 /user/transaction/records
    public Result getTransactionRecords(HttpSession session) { // 获取交易流水方法，接收 HttpSession
        User user = (User) session.getAttribute("user"); // 从 session 中获取用户对象
        if (user == null) return Result.fail("未登录"); // 如果未登录，返回失败结果
        List<Transaction> records = transactionService.list( // 查询交易流水列表
            new LambdaQueryWrapper<Transaction>().eq(Transaction::getUserId, user.getId()) // 添加条件：用户ID匹配
                .orderByDesc(Transaction::getCreateTime) // 按创建时间倒序排序
        );
        return Result.success(records); // 返回成功结果，包含交易流水列表
    }

}
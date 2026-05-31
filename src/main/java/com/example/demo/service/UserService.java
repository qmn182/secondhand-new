package com.example.demo.service; // 定义包名为 com.example.demo.service

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 导入 MyBatis-Plus 的 Lambda 条件构造器
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl; // 导入 MyBatis-Plus 的 ServiceImpl 基类
import com.example.demo.entity.User; // 导入用户实体类 User
import com.example.demo.mapper.UserMapper; // 导入用户 Mapper 接口
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 导入 Spring Security 的 BCrypt 密码编码器
import org.springframework.stereotype.Service; // 导入 Spring 服务层注解

@Service // 标记该类为 Spring 的服务层组件
public class UserService extends ServiceImpl<UserMapper, User> { // 定义用户服务类，继承 MyBatis-Plus 的 ServiceImpl，泛型为 Mapper 和实体

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(); // 创建 BCrypt 密码编码器实例（final 不可变）
    public static void main(String[] args) { // 定义一个 main 方法（用于测试，在服务类中不推荐但按原样保留）
        System.out.println(new BCryptPasswordEncoder().encode("12345")); // 输出对 "12345" 进行 BCrypt 加密后的密文
    }
    public class TestBCrypt { // 定义一个内部类 TestBCrypt
    public static void main(String[] args) { // 内部类中的 main 方法（用于测试 BCrypt 加密）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(); // 创建 BCrypt 密码编码器实例
        String rawPassword = "12345";   // 你要设置的明文密码，字符串 "12345"
        String encodedPassword = encoder.encode(rawPassword); // 对明文密码进行加密
        System.out.println(encodedPassword); // 输出加密后的密文
    }
}
    /**
     * 注册：对明文密码加密后保存
     */
    public boolean registerUser(User user) { // 注册用户的方法，参数为用户实体（包含明文密码）
        // 加密密码
        String encodedPassword = passwordEncoder.encode(user.getPassword()); // 使用 BCrypt 编码器对明文密码进行加密
        user.setPassword(encodedPassword); // 将加密后的密码设置回用户对象
        return this.save(user); // 调用 MyBatis-Plus 的 save 方法保存用户到数据库，返回是否成功
    }

    /**
     * 登录校验：根据用户名查询加密密码，与输入的明文进行比对
     */
    public User loginUser(String username, String rawPassword) { // 登录校验方法，参数为用户名和明文密码
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)); // 根据用户名查询用户
        if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) { // 如果用户存在且明文密码与数据库中加密密码匹配
            return user; // 返回用户对象（密码字段仍为加密状态，通常调用方会置空或忽略）
        }
        return null; // 用户名不存在或密码错误，返回 null
    }
}
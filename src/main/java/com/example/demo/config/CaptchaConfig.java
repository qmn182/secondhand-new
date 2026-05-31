package com.example.demo.config; // 定义包名为 com.example.demo.config

import com.google.code.kaptcha.impl.DefaultKaptcha; // 导入 Kaptcha 验证码实现类
import com.google.code.kaptcha.util.Config; // 导入 Kaptcha 配置类
import org.springframework.context.annotation.Bean; // 导入 Spring 的 Bean 注解
import org.springframework.context.annotation.Configuration; // 导入 Spring 的 Configuration 注解

import java.util.Properties; // 导入 Properties 类，用于管理配置属性

@Configuration // 标记该类为 Spring 配置类
public class CaptchaConfig { // 定义验证码配置类
    @Bean // 标记该方法返回一个 Spring Bean 对象
    public DefaultKaptcha captcha() { // 定义创建 DefaultKaptcha 实例的方法
        DefaultKaptcha kaptcha = new DefaultKaptcha(); // 创建 DefaultKaptcha 实例
        Properties properties = new Properties(); // 创建 Properties 对象用于设置验证码参数
        properties.setProperty("kaptcha.image.width", "120"); // 设置验证码图片宽度为120像素
        properties.setProperty("kaptcha.image.height", "40"); // 设置验证码图片高度为40像素
        properties.setProperty("kaptcha.textproducer.char.length", "4"); // 设置验证码字符长度为4
        kaptcha.setConfig(new Config(properties)); // 将配置应用到 DefaultKaptcha 实例
        return kaptcha; // 返回配置好的 DefaultKaptcha 实例
    }
}
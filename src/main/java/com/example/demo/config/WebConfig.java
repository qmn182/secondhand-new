package com.example.demo.config; // 定义包名为 com.example.demo.config

import org.springframework.beans.factory.annotation.Value; // 导入 Spring 的配置值注入注解
import org.springframework.context.annotation.Configuration; // 导入 Spring 的配置注解
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry; // 导入 Spring MVC 的资源处理器注册类
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 导入 Spring MVC 的配置接口

@Configuration // 标记该类为 Spring 配置类
public class WebConfig implements WebMvcConfigurer { // 定义 Web 配置类，实现 WebMvcConfigurer 接口
    @Value("${file.upload-dir}") // 从配置文件中注入 file.upload-dir 属性值
    private String uploadDir; // 文件上传根目录路径

    @Override // 表示该方法重写父接口的方法
    public void addResourceHandlers(ResourceHandlerRegistry registry) { // 重写添加资源处理器的方法
        // 映射 /uploads/** 到本地文件夹
        registry.addResourceHandler("D:/secondhand/demo/uploads/**") // 添加资源处理器，匹配该磁盘路径（注意：此处写法可能应使用 URL 路径，按原样保留）
                .addResourceLocations("file:" + uploadDir + "/"); // 设置资源实际位置为 file: + 上传目录路径
    }
}
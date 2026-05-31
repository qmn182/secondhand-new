package com.example.demo.config; // 定义包名为 com.example.demo.config

import org.springframework.context.annotation.Configuration; // 导入 Spring 的 Configuration 注解
import org.springframework.web.servlet.config.annotation.CorsRegistry; // 导入 Spring MVC 的跨域注册类
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 导入 Spring MVC 的配置接口

@Configuration // 标记该类为 Spring 配置类
public class CorsConfig implements WebMvcConfigurer { // 定义跨域配置类，实现 WebMvcConfigurer 接口
    @Override // 表示该方法重写父接口的方法
    public void addCorsMappings(CorsRegistry registry) { // 重写添加跨域映射的方法，参数为 CorsRegistry 注册器
        registry.addMapping("/**") // 添加跨域映射，匹配所有路径
                .allowedOrigins("http://localhost:5173")   // 设置允许跨域请求的前端地址（Vite 开发服务器默认端口）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 设置允许的 HTTP 方法
                .allowedHeaders("*") // 设置允许的请求头，* 表示全部允许
                .allowCredentials(true)                    // 允许携带 cookie（凭证信息）
                .maxAge(3600); // 设置预检请求的有效期为 3600 秒
    }
}
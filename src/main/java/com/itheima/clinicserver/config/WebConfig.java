package com.itheima.clinicserver.config;

import com.itheima.clinicserver.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    // ========== 配置拦截器==========
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")           // 拦截所有
                .excludePathPatterns("/login", "/uploads/**");   // 放行登录 + 静态资源
    }

    // ========== 配置静态资源映射==========
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")            // 浏览器访问 /uploads/xxx.jpg
                .addResourceLocations("file:D:/uploads/");    //  映射到本地磁盘目录
    }
}

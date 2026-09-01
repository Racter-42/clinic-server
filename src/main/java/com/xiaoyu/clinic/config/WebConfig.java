package com.xiaoyu.clinic.config;

import com.xiaoyu.clinic.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    // ========== 上传根目录：和 UploadController 读同一个配置 ==========
    // 保证"文件存到哪"和"从哪读出来"指向同一个目录，不会因为两边不一致导致 404
    @Value("${clinic.upload.path}")
    private String uploadPath;

    // ========== 配置拦截器==========
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")           // 拦截所有
                .excludePathPatterns("/login", "/uploads/**", "/doc.html", "/webjars/**", "/v3/api-docs/**",      //  放行 Knife4j 页面 + 资源 + 数据（/** 覆盖 swagger-config、?group=default 等子路径）
                        "/swagger-ui/**", "/knife4j/**"
                );   // 放行登录 + 静态资源
    }

    // ========== 配置静态资源映射==========
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ① 相对路径 ./uploads/ 转成绝对路径
        //    为什么要转？相对路径在 IDEA 里能跑，但打成 jar 用 java -jar 启动时，
        //    会按 JVM 启动目录解析，可能指到别的地方 → 图片访问 404
        String absPath = new File(uploadPath).getAbsolutePath();

        // ② 路径结尾必须补分隔符 /
        //    这是个经典坑：Spring 会把 URL 里 /uploads/ 后面的文件名拼到 location 后面，
        //    如果 location 是 D:/uploads（结尾无斜杠），拼出来是 D:/uploadsa.jpg → 找不到文件 → 404
        if (!absPath.endsWith("/") && !absPath.endsWith("\\")) {
            absPath = absPath + "/";
        }

        // ③ 搭桥：浏览器访问 /uploads/xxx.jpg → 去 absPath 目录下找 xxx.jpg
        //    file: 前缀不能省！省了 Spring 会按 classpath 找（jar 包内的 resources），必然 404
        registry.addResourceHandler("/uploads/**")     // 浏览器访问 /uploads/xxx.jpg
                .addResourceLocations("file:" + absPath);   //  映射到本地磁盘目录
    }

}

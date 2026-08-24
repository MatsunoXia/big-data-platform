package com.example.bigdata.config;

import com.example.bigdata.interceptor.QueryTimeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 * - 跨域放行
 * - 注册查询耗时统计拦截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private QueryTimeInterceptor queryTimeInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册查询耗时统计拦截器
        // 只拦截 /api/search/** 下的 GET 请求
        registry.addInterceptor(queryTimeInterceptor)
                .addPathPatterns("/api/search/**");
    }
}

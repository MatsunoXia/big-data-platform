package com.example.bigdata.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 查询耗时统计拦截器
 *
 * 原理：
 *   1. preHandle 在请求处理之前记录起始时间，存到 request attribute
 *   2. afterCompletion 在请求完成后计算耗时，写入响应头 X-Query-Time
 *   3. 前端从响应头读取耗时，展示在页面上
 *
 * 面试点：
 *   - 为什么用拦截器而不是 Filter？拦截器是 Spring 层面的，可以精确控制哪些请求需要统计
 *   - 为什么放响应头而不是响应体？解耦，不影响业务返回结构
 */
@Slf4j
@Component
public class QueryTimeInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        // 只对 GET 查询请求统计（POST 的数据生成等不需要）
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            request.setAttribute(START_TIME_ATTR, System.nanoTime());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime != null) {
            long costNs = System.nanoTime() - startTime;
            long costMs = costNs / 1_000_000;
            // 写入响应头，前端 axios 拦截器可以读取
            response.setHeader("X-Query-Time", String.valueOf(costMs));

            // 慢查询日志：超过 500ms 的查询记录警告
            if (costMs > 500) {
                log.warn("[慢查询] {} {} 耗时 {}ms",
                        request.getMethod(), request.getRequestURI(), costMs);
            }
        }
    }
}

package com.example.bigdata.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置
 * 统一管理项目中用到的线程池，避免各处随意创建
 *
 * 面试点：
 *   - 核心线程数：CPU密集型=CPU核数+1，IO密集型=CPU核数*2
 *   - 最大线程数：同上，或者根据业务峰值设定
 *   - 队列大小：不宜太大（占用内存），不宜太小（容易触发拒绝策略）
 *   - 拒绝策略：CallerRunsPolicy 适合不想丢弃任务的场景
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 数据导入/导出专用线程池
     * IO密集型任务，线程数可以多一些
     */
    @Bean("dataProcessPool")
    public ThreadPoolExecutor dataProcessPool() {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                cpuCores * 2,              // 核心线程数
                cpuCores * 4,              // 最大线程数
                60, TimeUnit.SECONDS,      // 空闲线程存活时间
                new LinkedBlockingQueue<>(200),  // 任务队列
                new ThreadFactory() {
                    private int count = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "data-process-" + (++count));
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行
        );
    }

    /**
     * 通用异步任务线程池
     */
    @Bean("asyncTaskPool")
    public ThreadPoolExecutor asyncTaskPool() {
        return new ThreadPoolExecutor(
                4,
                8,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadFactory() {
                    private int count = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "async-task-" + (++count));
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}

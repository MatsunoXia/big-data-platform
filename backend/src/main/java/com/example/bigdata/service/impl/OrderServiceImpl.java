package com.example.bigdata.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bigdata.entity.Order;
import com.example.bigdata.mapper.OrderMapper;
import com.example.bigdata.service.OrderService;
import com.example.bigdata.util.DataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Override
    public long getTotalCount() {
        return orderMapper.selectTotalCount();
    }

    @Override
    public Long getMaxId() {
        return orderMapper.selectMaxId();
    }

    @Override
    public Long getMinId() {
        return orderMapper.selectMinId();
    }

    @Override
    public long generateTestData(int totalCount, int batchSize, int threadCount) {
        log.info("开始生成测试数据: 总量={}, 每批={}, 线程数={}", totalCount, batchSize, threadCount);

        long startTime = System.currentTimeMillis();
        AtomicLong insertedCount = new AtomicLong(0);

        // 计算每个线程需要处理的数据量
        int perThread = totalCount / threadCount;
        int remainder = totalCount % threadCount;

        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadCount,           // 核心线程数
                threadCount,           // 最大线程数
                60, TimeUnit.SECONDS,  // 空闲线程存活时间
                new LinkedBlockingQueue<>(100),  // 任务队列
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略：调用者执行
        );

        // 提交任务
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int count = perThread + (i < remainder ? 1 : 0);
            int startIndex = i * perThread + Math.min(i, remainder);
            futures.add(executor.submit(() -> insertBatch(count, batchSize, startIndex, insertedCount)));
        }

        // 等待所有任务完成
        long totalInserted = 0;
        for (Future<Long> future : futures) {
            try {
                totalInserted += future.get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("数据生成任务执行失败", e);
            }
        }

        executor.shutdown();
        long costTime = System.currentTimeMillis() - startTime;
        log.info("测试数据生成完成: 实际插入={}, 耗时={}ms", totalInserted, costTime);

        return totalInserted;
    }

    /**
     * 分批插入数据
     */
    private long insertBatch(int count, int batchSize, int startIndex, AtomicLong globalCounter) {
        long localCount = 0;
        int remaining = count;

        while (remaining > 0) {
            int currentBatch = Math.min(remaining, batchSize);
            List<Order> batch = new ArrayList<>(currentBatch);

            for (int j = 0; j < currentBatch; j++) {
                batch.add(DataGenerator.randomOrder(startIndex + localCount + j));
            }

            // 批量插入
            saveBatch(batch, currentBatch);
            localCount += currentBatch;
            remaining -= currentBatch;

            // 更新全局计数器（用于进度展示）
            long total = globalCounter.addAndGet(currentBatch);
            if (total % 50000 == 0) {
                log.info("已生成 {} 条数据", total);
            }
        }

        return localCount;
    }
}

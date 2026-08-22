package com.example.bigdata.controller;

import com.example.bigdata.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据生成器 Controller
 * 提供测试数据生成接口
 */
@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取当前数据统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", orderService.getTotalCount());
        stats.put("maxId", orderService.getMaxId());
        stats.put("minId", orderService.getMinId());
        return stats;
    }

    /**
     * 生成测试数据
     * @param count 生成条数，默认10万
     * @param batchSize 每批插入条数，默认5000
     * @param threadCount 线程数，默认4
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(
            @RequestParam(defaultValue = "100000") int count,
            @RequestParam(defaultValue = "5000") int batchSize,
            @RequestParam(defaultValue = "4") int threadCount) {

        log.info("收到数据生成请求: count={}, batchSize={}, threadCount={}", count, batchSize, threadCount);

        long startTime = System.currentTimeMillis();
        long inserted = orderService.generateTestData(count, batchSize, threadCount);
        long costTime = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("inserted", inserted);
        result.put("costTimeMs", costTime);
        result.put("totalCount", orderService.getTotalCount());
        result.put("batchSize", batchSize);
        result.put("threadCount", threadCount);

        log.info("数据生成完成: inserted={}, costTime={}ms", inserted, costTime);
        return result;
    }
}

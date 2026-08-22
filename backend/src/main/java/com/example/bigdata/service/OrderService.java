package com.example.bigdata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.bigdata.entity.Order;

public interface OrderService extends IService<Order> {

    /**
     * 获取订单总数
     */
    long getTotalCount();

    /**
     * 获取最大ID
     */
    Long getMaxId();

    /**
     * 获取最小ID
     */
    Long getMinId();

    /**
     * 生成测试数据
     * @param totalCount 总条数
     * @param batchSize 每批插入条数
     * @param threadCount 并发线程数
     * @return 实际插入条数
     */
    long generateTestData(int totalCount, int batchSize, int threadCount);
}

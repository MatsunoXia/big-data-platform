package com.example.bigdata.service;

import com.example.bigdata.dto.OrderQueryDTO;
import com.example.bigdata.dto.PageResult;
import com.example.bigdata.entity.Order;

/**
 * 订单检索服务
 *
 * 核心模块，包含传统分页、游标分页、Redis 缓存的完整实现
 */
public interface OrderSearchService {

    /**
     * 检索订单（自动选择分页方式）
     */
    PageResult<Order> search(OrderQueryDTO query);

    /**
     * 传统 OFFSET 分页
     */
    PageResult<Order> searchByOffset(OrderQueryDTO query);

    /**
     * 游标分页
     */
    PageResult<Order> searchByCursor(OrderQueryDTO query);
}

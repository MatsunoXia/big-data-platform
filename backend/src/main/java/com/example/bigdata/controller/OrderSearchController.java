package com.example.bigdata.controller;

import com.example.bigdata.dto.OrderQueryDTO;
import com.example.bigdata.dto.PageResult;
import com.example.bigdata.entity.Order;
import com.example.bigdata.service.OrderSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 订单检索 Controller
 *
 * 提供两种分页方式的查询接口：
 * 1. GET /api/search/orders — 统一入口，通过 pageType 参数切换分页方式
 * 2. GET /api/search/compare — 性能对比接口，同时返回两种分页方式的结果
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
public class OrderSearchController {

    @Autowired
    private OrderSearchService orderSearchService;

    /**
     * 统一检索接口
     *
     * 前端通过 pageType 参数选择分页方式：
     * - offset：传统 OFFSET 分页（深分页会很慢）
     * - cursor：游标分页（深分页依然很快）
     */
    @GetMapping("/orders")
    public PageResult<Order> searchOrders(OrderQueryDTO query) {
        log.info("检索请求: pageType={}, pageNum={}, cursorId={}, status={}, category={}",
                query.getPageType(), query.getPageNum(), query.getCursorId(),
                query.getStatus(), query.getCategory());
        return orderSearchService.search(query);
    }

    /**
     * 性能对比接口
     *
     * 同时用两种分页方式查询同一页数据，返回两者耗时对比
     * 用于前端展示"传统 vs 游标"的性能差异面板
     */
    @GetMapping("/compare")
    public Object comparePerformance(OrderQueryDTO query) {
        // 先查 OFFSET 分页
        query.setPageType("offset");
        PageResult<Order> offsetResult = orderSearchService.searchByOffset(query);

        // 再查游标分页（用 offsetResult 的最后一条 ID 作为游标）
        if (offsetResult.getList() != null && !offsetResult.getList().isEmpty()) {
            Long lastId = offsetResult.getList().get(offsetResult.getList().size() - 1).getId();
            query.setCursorId(lastId);
        }
        query.setPageType("cursor");
        PageResult<Order> cursorResult = orderSearchService.searchByCursor(query);

        // 组装对比结果
        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("offset", offsetResult);
        compare.put("cursor", cursorResult);
        compare.put("speedup", offsetResult.getQueryTimeMs() > 0 && cursorResult.getQueryTimeMs() > 0
                ? String.format("%.1fx", (double) offsetResult.getQueryTimeMs() / cursorResult.getQueryTimeMs())
                : "N/A");

        return compare;
    }
}

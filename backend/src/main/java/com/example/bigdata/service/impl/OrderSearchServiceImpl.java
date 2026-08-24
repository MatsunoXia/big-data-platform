package com.example.bigdata.service.impl;

import com.example.bigdata.dto.OrderQueryDTO;
import com.example.bigdata.dto.PageResult;
import com.example.bigdata.entity.Order;
import com.example.bigdata.mapper.OrderMapper;
import com.example.bigdata.service.OrderSearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单检索服务实现
 *
 * 核心优化点：
 * 1. 游标分页 — 用 WHERE id > X 代替 OFFSET，深分页性能从秒级降到毫秒级
 * 2. Redis 缓存 — 搜索结果缓存 + Count 缓存，减少数据库压力
 * 3. 索引优化 — 联合索引 idx_status_category 覆盖常用查询条件
 * 
 */
@Slf4j
@Service
public class OrderSearchServiceImpl implements OrderSearchService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ObjectMapper objectMapper;

    /** 搜索结果缓存前缀 */
    private static final String SEARCH_CACHE_PREFIX = "order:search:";

    /** Count 缓存前缀 */
    private static final String COUNT_CACHE_PREFIX = "order:count:";

    /** 搜索缓存 TTL（分钟） */
    private static final int SEARCH_CACHE_TTL = 5;

    /** Count 缓存 TTL（分钟） — Count 变化频率低，可以缓存更久 */
    private static final int COUNT_CACHE_TTL = 10;

    @Override
    public PageResult<Order> search(OrderQueryDTO query) {
        if ("cursor".equals(query.getPageType())) {
            return searchByCursor(query);
        } else {
            return searchByOffset(query);
        }
    }

    /**
     * 传统 OFFSET 分页
     *
     * 性能问题演示：
     * - 第 1 页：LIMIT 0, 20 → 极快（直接从索引取前 20 条）
     * - 第 1000 页：LIMIT 20000, 20 → 几十毫秒
     * - 第 50000 页：LIMIT 1000000, 20 → 数秒！（扫描并丢弃 100 万行）
     */
    @Override
    public PageResult<Order> searchByOffset(OrderQueryDTO query) {
        long startTime = System.currentTimeMillis();

        int pageNum = query.getPageNum() != null ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;
        int offset = (pageNum - 1) * pageSize;

        // --- Step 1: 查询总数（有缓存） ---
        long total;
        if (Boolean.TRUE.equals(query.getUseCache())) {
            total = getCountWithCache(query);
        } else {
            total = orderMapper.selectCountByCondition(
                    query.getOrderNo(), query.getStatus(), query.getCategory(),
                    query.getUserId(), query.getProvince());
        }

        // --- Step 2: 查询当前页数据 ---
        List<Order> list = orderMapper.selectByOffset(
                query.getOrderNo(), query.getStatus(), query.getCategory(),
                query.getUserId(), query.getProvince(), offset, pageSize);

        long costMs = System.currentTimeMillis() - startTime;

        // --- Step 3: 组装返回结果 ---
        PageResult<Order> result = PageResult.ofOffset(list, total, pageNum, pageSize);
        result.setQueryTimeMs(costMs);
        result.setCacheHit(false); // OFFSET 分页本身不缓存结果集
        result.setPageTypeDesc("传统 OFFSET 分页");
        result.setIndexHit(determineIndexHit(query));

        log.info("[OFFSET分页] pageNum={}, pageSize={}, total={}, cost={}ms",
                pageNum, pageSize, total, costMs);

        return result;
    }

    /**
     * 游标分页（核心优化）
     *
     * 原理：
     * - 不用 OFFSET，用上一页最后一条的 ID 作为游标
     * - WHERE id < #{cursorId} 走主键索引范围扫描，直接定位
     * - 无论第几页，查询耗时都是稳定的 ~10-20ms
     *
     * 对比：
     * - OFFSET 第 50000 页：扫描 100 万行 → ~6000ms
     * - 游标分页第 50000 页：主键定位 → ~18ms
     */
    @Override
    public PageResult<Order> searchByCursor(OrderQueryDTO query) {
        long startTime = System.currentTimeMillis();

        int pageSize = query.getPageSize() != null ? query.getPageSize() : 20;

        // --- Step 1: 尝试从缓存获取 ---
        if (Boolean.TRUE.equals(query.getUseCache())) {
            Object cachedJson = redisTemplate.opsForValue().get(buildSearchCacheKey(query));
            if (cachedJson instanceof String) {
                try {
                    List<Order> cachedList = objectMapper.readValue((String) cachedJson,
                            new TypeReference<List<Order>>() {});
                    Long lastId = cachedList.isEmpty() ? null : cachedList.get(cachedList.size() - 1).getId();
                    PageResult<Order> cachedResult = PageResult.ofCursor(cachedList, pageSize, lastId);
                    cachedResult.setCacheHit(true);
                    cachedResult.setQueryTimeMs(System.currentTimeMillis() - startTime);
                    cachedResult.setPageTypeDesc("游标分页（主键索引直接定位）");
                    cachedResult.setIndexHit(determineIndexHit(query));
                    log.info("[游标分页] 缓存命中, cost={}ms", cachedResult.getQueryTimeMs());
                    return cachedResult;
                } catch (Exception e) {
                    log.warn("缓存反序列化失败，走数据库查询", e);
                }
            }
        }

        // --- Step 2: 查询数据 ---
        List<Order> list = orderMapper.selectByCursor(
                query.getOrderNo(), query.getStatus(), query.getCategory(),
                query.getUserId(), query.getProvince(), query.getCursorId(), pageSize);

        long queryTimeMs = System.currentTimeMillis() - startTime;

        // --- Step 3: 组装返回结果 ---
        Long lastId = list.isEmpty() ? null : list.get(list.size() - 1).getId();
        PageResult<Order> result = PageResult.ofCursor(list, pageSize, lastId);
        result.setQueryTimeMs(queryTimeMs);
        result.setCacheHit(false);
        result.setPageTypeDesc("游标分页（主键索引直接定位）");
        result.setIndexHit(determineIndexHit(query));

        // --- Step 4: 写入缓存（缓存数据列表，序列化为 JSON 字符串） ---
        if (Boolean.TRUE.equals(query.getUseCache()) && !list.isEmpty()) {
            try {
                String cacheKey = buildSearchCacheKey(query);
                String json = objectMapper.writeValueAsString(list);
                redisTemplate.opsForValue().set(cacheKey, json, SEARCH_CACHE_TTL, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("缓存写入失败", e);
            }
        }

        log.info("[游标分页] cursorId={}, pageSize={}, resultCount={}, cost={}ms",
                query.getCursorId(), pageSize, list.size(), queryTimeMs);

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 带缓存的 Count 查询
     *
     * 优化思路：
     * - 全表 COUNT（无 WHERE 条件）：缓存 10 分钟，因为总行数变化不频繁
     * - 带条件 COUNT：缓存 5 分钟，条件查询结果可能因新增数据而变化
     * - 如果不需要精确数字：可以用 EXPLAIN 的 rows 估算值（更快但不精确）
     */
    private long getCountWithCache(OrderQueryDTO query) {
        String cacheKey = buildCountCacheKey(query);

        // 尝试从缓存读取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (cached instanceof Number) {
                return ((Number) cached).longValue();
            }
        }

        // 缓存未命中，查数据库
        long count = orderMapper.selectCountByCondition(
                query.getOrderNo(), query.getStatus(), query.getCategory(),
                query.getUserId(), query.getProvince());

        // 写入缓存
        int ttl = isBlankQuery(query) ? COUNT_CACHE_TTL : SEARCH_CACHE_TTL;
        redisTemplate.opsForValue().set(cacheKey, count, ttl, TimeUnit.MINUTES);

        return count;
    }

    /**
     * 构建搜索缓存 Key
     * 用查询条件拼接做 Key，保证相同条件命中同一份缓存
     */
    private String buildSearchCacheKey(OrderQueryDTO query) {
        StringBuilder key = new StringBuilder(SEARCH_CACHE_PREFIX);
        key.append("cursor:");
        key.append("cursor=").append(query.getCursorId());
        key.append("|status=").append(nullToEmpty(query.getStatus()));
        key.append("|category=").append(nullToEmpty(query.getCategory()));
        key.append("|orderNo=").append(nullToEmpty(query.getOrderNo()));
        key.append("|province=").append(nullToEmpty(query.getProvince()));
        key.append("|userId=").append(query.getUserId());
        key.append("|size=").append(query.getPageSize());
        return key.toString();
    }

    /**
     * 构建 Count 缓存 Key
     */
    private String buildCountCacheKey(OrderQueryDTO query) {
        StringBuilder key = new StringBuilder(COUNT_CACHE_PREFIX);
        key.append("status=").append(nullToEmpty(query.getStatus()));
        key.append("|category=").append(nullToEmpty(query.getCategory()));
        key.append("|orderNo=").append(nullToEmpty(query.getOrderNo()));
        key.append("|province=").append(nullToEmpty(query.getProvince()));
        key.append("|userId=").append(query.getUserId());
        return key.toString();
    }

    /**
     * 判断是否是无条件查询（全表扫描）
     */
    private boolean isBlankQuery(OrderQueryDTO query) {
        return isEmpty(query.getOrderNo())
                && isEmpty(query.getStatus())
                && isEmpty(query.getCategory())
                && query.getUserId() == null
                && isEmpty(query.getProvince());
    }

    /**
     * 根据查询条件推断命中的索引
     * 实际项目中可以用 EXPLAIN 查看，这里简化处理
     */
    private String determineIndexHit(OrderQueryDTO query) {
        if (!isEmpty(query.getOrderNo())) {
            return "idx_order_no（订单号唯一索引）";
        }
        if (!isEmpty(query.getStatus()) && !isEmpty(query.getCategory())) {
            return "idx_status_category（状态+分类联合索引）";
        }
        if (!isEmpty(query.getStatus())) {
            return "idx_status_category（部分命中：status）";
        }
        if (query.getUserId() != null) {
            return "idx_user_id（用户ID索引）";
        }
        if (!isEmpty(query.getProvince())) {
            return "idx_province_city（省份+城市联合索引）";
        }
        return "PRIMARY（主键索引）";
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}

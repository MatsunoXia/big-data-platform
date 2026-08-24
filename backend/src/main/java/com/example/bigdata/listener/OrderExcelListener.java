package com.example.bigdata.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.example.bigdata.dto.ImportProgress;
import com.example.bigdata.entity.Order;
import com.example.bigdata.entity.OrderExcelDTO;
import com.example.bigdata.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EasyExcel 订单导入监听器
 *
 * 核心原理：
 *   EasyExcel 采用 SAX 模式逐行读取 Excel，不在内存中加载整个文件
 *   每读到一行数据，回调 invoke() 方法
 *   我们在 invoke() 中积累批次，达到阈值后提交到线程池批量插入
 *
 *   1. 为什么用 EasyExcel 而不是 POI？
 *      POI 的 XSSFWorkbook 会把整个 Excel 加载到内存，100MB 文件可能 OOM
 *      EasyExcel 基于 SAX 流式读取，内存占用恒定（~50MB）
 *
 *   2. 为什么用多线程批量插入？
 *      单线程逐条 INSERT：100万条 ≈ 30分钟
 *      多线程批量 INSERT（batch=5000, 4线程）：100万条 ≈ 2分钟
 *
 *   3. 批量大小怎么选？
 *      太小（如 100）：网络往返多，慢
 *      太大（如 50000）：单条 SQL 太长，MySQL 报 packet too large
 *      推荐：2000~5000，实测 5000 最优
 */
@Slf4j
public class OrderExcelListener extends AnalysisEventListener<OrderExcelDTO> {

    /** 每批积累多少条后提交插入 */
    private final int batchSize;

    /** 线程池（外部传入，统一管理） */
    private final ThreadPoolExecutor threadPool;

    /** MyBatis Mapper（外部传入） */
    private final OrderMapper orderMapper;

    /** 进度追踪对象（外部传入，前端轮询读取） */
    private final ImportProgress progress;

    /** 当前批次缓冲区 */
    private List<Order> currentBatch = new ArrayList<>();

    /** 已提交的 Future 列表（用于等待全部完成） */
    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();

    /** 行号计数器 */
    private final AtomicInteger rowNumber = new AtomicInteger(0);

    /** 时间格式化 */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OrderExcelListener(int batchSize,
                               ThreadPoolExecutor threadPool,
                               OrderMapper orderMapper,
                               ImportProgress progress) {
        this.batchSize = batchSize;
        this.threadPool = threadPool;
        this.orderMapper = orderMapper;
        this.progress = progress;
    }

    /**
     * 每读到一行数据就调用
     * 这里做：DTO → Entity 转换 + 积累批次
     */
    @Override
    public void invoke(OrderExcelDTO dto, AnalysisContext context) {
        // DTO → Entity
        Order order = convertToOrder(dto);
        currentBatch.add(order);

        int current = rowNumber.incrementAndGet();
        // Excel 读取阶段的进度（粗略，因为总行数可能还不知道）
        progress.getProcessedRows().set(current);

        // 达到批次阈值，提交插入任务
        if (currentBatch.size() >= batchSize) {
            submitBatch();
        }
    }

    /**
     * Excel 全部读完后调用
     * 这里做：提交剩余数据 + 等待所有线程完成
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("Excel 读取完毕，共 {} 行", rowNumber.get());
        progress.getTotalRows().set(rowNumber.get());

        // 提交最后一批（不满 batchSize 的剩余数据）
        if (!currentBatch.isEmpty()) {
            submitBatch();
        }

        // 等待所有插入任务完成
        waitForCompletion();

        progress.markCompleted();
        log.info("导入完成: 成功={}, 重复={}, 失败={}, 耗时={}ms",
                progress.getSuccessCount().get(),
                progress.getDuplicateCount().get(),
                progress.getFailCount().get(),
                progress.getCostMs());
    }

    /**
     * 异常处理
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) {
        log.error("Excel 读取异常，行号: {}", rowNumber.get(), exception);
        progress.markFailed("Excel 解析错误，行号 " + rowNumber.get() + ": " + exception.getMessage());
    }

    // ==================== 私有方法 ====================

    /**
     * 提交当前批次到线程池
     * 提交后清空缓冲区，继续读下一批
     */
    private void submitBatch() {
        List<Order> batch = new ArrayList<>(currentBatch);
        currentBatch = new ArrayList<>(batchSize);

        progress.setStatus(ImportProgress.Status.INSERTING);

        Future<?> future = threadPool.submit(() -> insertBatchWithDuplicateCheck(batch));
        futures.add(future);
    }

    /**
     * 批量插入（带去重检查）
     *
     * 流程：
     * 1. 先查这批数据的 order_no 是否已存在
     * 2. 过滤掉已存在的（记录为重复）
     * 3. 批量插入剩余数据
     *
     * 为什么不在 INSERT 时用 INSERT IGNORE？
     * 因为需要精确统计"重复了多少条"，INSERT IGNORE 会静默跳过，拿不到数字
     */
    private void insertBatchWithDuplicateCheck(List<Order> batch) {
        try {
            // 1. 收集这批所有 order_no
            List<String> orderNos = new ArrayList<>(batch.size());
            for (Order o : batch) {
                orderNos.add(o.getOrderNo());
            }

            // 2. 批量查询已存在的 order_no
            Set<String> existingNos = new HashSet<>();
            // 分批查询，每批 1000 个（避免 IN 子句太长）
            for (int i = 0; i < orderNos.size(); i += 1000) {
                List<String> sub = orderNos.subList(i, Math.min(i + 1000, orderNos.size()));
                List<String> found = orderMapper.selectOrderNosByOrderNos(sub);
                existingNos.addAll(found);
            }

            // 3. 过滤掉重复的
            List<Order> toInsert = new ArrayList<>();
            for (Order o : batch) {
                if (existingNos.contains(o.getOrderNo())) {
                    progress.getDuplicateCount().incrementAndGet();
                    progress.getProcessedRows().incrementAndGet();
                } else {
                    toInsert.add(o);
                }
            }

            // 4. 批量插入
            if (!toInsert.isEmpty()) {
                // 分小批插入（避免单条 SQL 过大）
                for (int i = 0; i < toInsert.size(); i += batchSize) {
                    List<Order> sub = toInsert.subList(i, Math.min(i + batchSize, toInsert.size()));
                    orderMapper.batchInsert(sub);
                    progress.getSuccessCount().addAndGet(sub.size());
                    progress.getProcessedRows().addAndGet(sub.size());
                }
            }

        } catch (Exception e) {
            log.error("批量插入失败，批次大小={}", batch.size(), e);
            progress.getFailCount().addAndGet(batch.size());
            progress.getProcessedRows().addAndGet(batch.size());
        }
    }

    /**
     * 等待所有异步插入任务完成
     */
    private void waitForCompletion() {
        for (Future<?> future : futures) {
            try {
                future.get(5, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("等待插入任务完成失败", e);
            }
        }
    }

    /**
     * ExcelDTO → Order 实体转换
     */
    private Order convertToOrder(OrderExcelDTO dto) {
        Order order = new Order();
        order.setOrderNo(dto.getOrderNo());
        order.setUserId(dto.getUserId() != null ? dto.getUserId() : 1L);
        order.setUserName(dto.getUserName() != null ? dto.getUserName() : "导入用户");
        order.setProductName(dto.getProductName() != null ? dto.getProductName() : "未知商品");
        order.setCategory(dto.getCategory() != null ? dto.getCategory() : "其他");
        order.setAmount(dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO);
        order.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : "已导入");
        order.setProvince(dto.getProvince() != null ? dto.getProvince() : "未知");
        order.setCity(dto.getCity() != null ? dto.getCity() : "未知");
        order.setDeleted(0);

        // 解析时间
        if (dto.getCreateTime() != null && !dto.getCreateTime().isEmpty()) {
            try {
                order.setCreateTime(LocalDateTime.parse(dto.getCreateTime(), FORMATTER));
            } catch (Exception e) {
                order.setCreateTime(LocalDateTime.now());
            }
        } else {
            order.setCreateTime(LocalDateTime.now());
        }
        order.setUpdateTime(LocalDateTime.now());

        return order;
    }
}

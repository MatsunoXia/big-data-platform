package com.example.bigdata.dto;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 导入进度追踪对象
 *
 * 使用 AtomicInteger 保证多线程更新安全
 * 前端通过轮询 /api/import/progress/{progressId} 获取实时进度
 */
@Data
public class ImportProgress {

    /** 任务状态 */
    public enum Status { READING, INSERTING, COMPLETED, FAILED }

    /** 进度ID（UUID） */
    private String progressId;

    /** 文件名 */
    private String fileName;

    /** 当前状态 */
    private volatile Status status = Status.READING;

    /** 总行数（Excel 读取完成后才准确） */
    private final AtomicInteger totalRows = new AtomicInteger(0);

    /** 已成功插入 */
    private final AtomicInteger successCount = new AtomicInteger(0);

    /** 重复数据（order_no 已存在） */
    private final AtomicInteger duplicateCount = new AtomicInteger(0);

    /** 插入失败 */
    private final AtomicInteger failCount = new AtomicInteger(0);

    /** 当前已处理行数（用于计算进度百分比） */
    private final AtomicInteger processedRows = new AtomicInteger(0);

    /** 错误信息 */
    private volatile String errorMsg;

    /** 开始时间 */
    private long startTime;

    /** 结束时间 */
    private long endTime;

    public ImportProgress() {
        this.startTime = System.currentTimeMillis();
    }

    public ImportProgress(String progressId, String fileName) {
        this.progressId = progressId;
        this.fileName = fileName;
        this.startTime = System.currentTimeMillis();
    }

    /** 计算进度百分比 */
    public int getProgressPercent() {
        int total = totalRows.get();
        if (total <= 0) return 0;
        return Math.min(100, processedRows.get() * 100 / total);
    }

    /** 计算耗时（毫秒） */
    public long getCostMs() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return end - startTime;
    }

    /** 是否已完成 */
    public boolean isFinished() {
        return status == Status.COMPLETED || status == Status.FAILED;
    }

    /** 标记完成 */
    public void markCompleted() {
        this.status = Status.COMPLETED;
        this.endTime = System.currentTimeMillis();
    }

    /** 标记失败 */
    public void markFailed(String errorMsg) {
        this.status = Status.FAILED;
        this.errorMsg = errorMsg;
        this.endTime = System.currentTimeMillis();
    }
}

package com.example.bigdata.service;

import com.example.bigdata.entity.ExportTask;

import java.util.List;

/**
 * 数据导出服务
 *
 * 异步导出流程：
 * 1. POST /api/export/start → 创建任务（PENDING），提交到线程池，返回 taskNo
 * 2. 后台线程：分段并行查询 → 流式写入 Excel → 更新状态为 COMPLETED
 * 3. 前端轮询 GET /api/export/progress/{taskNo} 获取进度
 * 4. 完成后 GET /api/export/download/{taskNo} 下载文件
 *
 * - 为什么异步？100 万条导出可能需要 1-2 分钟，HTTP 不能等这么久
 * - 为什么分段并行？单次查 100 万条会锁表太久，分 10 段并行查询更快
 * - 为什么用 EasyExcel 而不是 POI？SXSSFWorkbook 虽然流式但 API 复杂，EasyExcel 更简洁
 */
public interface ExportService {

    /**
     * 启动异步导出任务
     *
     * @param status  按状态筛选（可选）
     * @param category 按分类筛选（可选）
     * @return 导出任务对象（含 taskNo 供轮询）
     */
    ExportTask startExport(String status, String category);

    /**
     * 查询导出任务进度
     */
    ExportTask getTask(String taskNo);

    /**
     * 获取最近的导出任务列表
     */
    List<ExportTask> getRecentTasks(int limit);
}

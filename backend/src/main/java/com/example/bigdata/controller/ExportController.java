package com.example.bigdata.controller;

import com.example.bigdata.entity.ExportTask;
import com.example.bigdata.service.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据导出 Controller
 *
 * 接口设计：
 * 1. POST /api/export/start            — 启动异步导出，返回 taskNo
 * 2. GET  /api/export/progress/{taskNo} — 查询导出进度（前端轮询）
 * 3. GET  /api/export/download/{taskNo} — 下载导出文件
 * 4. GET  /api/export/tasks             — 获取最近的导出任务列表
 *
 * 面试点：
 * - 为什么用异步？大数据量导出耗时长，HTTP 不能等
 * - 为什么分段并行？单次查 100 万条锁表太久，分段查询降低数据库压力
 * - 为什么存文件而不是流式返回？方便重试下载，且前端可以展示进度
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Value("${export.dir:#{systemProperties['user.dir'] + '/export'}}")
    private String exportDir;

    /**
     * 启动异步导出
     *
     * @param status   按状态筛选（可选）
     * @param category 按分类筛选（可选）
     * @return 任务信息（含 taskNo 供前端轮询）
     */
    @PostMapping("/start")
    public Map<String, Object> startExport(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        log.info("收到导出请求: status={}, category={}", status, category);

        ExportTask task = exportService.startExport(status, category);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskNo", task.getTaskNo());
        result.put("message", "导出任务已创建，请轮询进度");
        return result;
    }

    /**
     * 查询导出进度
     *
     * 前端每 1s 轮询一次
     * 返回：taskNo, status, totalCount, processedCount, progressPercent, costMs
     */
    @GetMapping("/progress/{taskNo}")
    public Map<String, Object> getProgress(@PathVariable String taskNo) {
        ExportTask task = exportService.getTask(taskNo);

        Map<String, Object> result = new HashMap<>();
        if (task == null) {
            result.put("success", false);
            result.put("message", "任务不存在");
            return result;
        }

        result.put("success", true);
        result.put("taskNo", task.getTaskNo());
        result.put("status", task.getStatus());
        result.put("totalCount", task.getTotalCount());
        result.put("processedCount", task.getProcessedCount());
        result.put("fileName", task.getFileName());
        result.put("errorMsg", task.getErrorMsg());
        result.put("createTime", task.getCreateTime());
        result.put("finishTime", task.getFinishTime());

        // 计算进度百分比
        if (task.getTotalCount() != null && task.getTotalCount() > 0) {
            result.put("progressPercent",
                    Math.min(100, task.getProcessedCount() * 100 / task.getTotalCount()));
        } else {
            result.put("progressPercent", 0);
        }

        // 计算耗时
        if (task.getCreateTime() != null) {
            long start = task.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = task.getFinishTime() != null
                    ? task.getFinishTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis();
            result.put("costMs", end - start);
        }

        // 是否完成
        result.put("finished", "COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus()));

        return result;
    }

    /**
     * 下载导出文件
     *
     * 通过 taskNo 查找文件路径，以流式方式返回给前端
     */
    @GetMapping("/download/{taskNo}")
    public void download(@PathVariable String taskNo, HttpServletResponse response) throws Exception {
        ExportTask task = exportService.getTask(taskNo);

        if (task == null || !"COMPLETED".equals(task.getStatus()) || task.getFilePath() == null) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"success\":false,\"message\":\"文件不存在或任务未完成\"}");
            return;
        }

        File file = new File(task.getFilePath());
        if (!file.exists()) {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"success\":false,\"message\":\"文件已被删除\"}");
            return;
        }

        // 设置响应头
        String downloadName = URLEncoder.encode(
                task.getFileName() != null ? task.getFileName() : "导出数据.xlsx",
                StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + downloadName);
        response.setContentLengthLong(file.length());

        // 流式写入响应
        try (InputStream is = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }

        log.info("文件下载: taskNo={}, file={}", taskNo, task.getFileName());
    }

    /**
     * 获取最近的导出任务列表
     */
    @GetMapping("/tasks")
    public List<ExportTask> getRecentTasks(@RequestParam(defaultValue = "10") int limit) {
        return exportService.getRecentTasks(limit);
    }
}

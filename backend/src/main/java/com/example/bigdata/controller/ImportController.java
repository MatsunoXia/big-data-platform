package com.example.bigdata.controller;

import com.alibaba.excel.EasyExcel;
import com.example.bigdata.dto.ImportProgress;
import com.example.bigdata.entity.OrderExcelDTO;
import com.example.bigdata.service.ImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据导入 Controller
 *
 * 接口设计:
 * 1. POST /api/import/upload - 上传文件,返回 progressId(异步启动导入)
 * 2. GET  /api/import/progress/{id} - 查询导入进度(前端轮询)
 *
 * 为什么用异步 + 轮询?
 * - 100万条数据导入可能需要几分钟,HTTP 请求不能等这么久
 * - 前端拿到 progressId 后每 500ms 轮询一次进度
 * - 进度条实时更新,用户体验好
 */
@Slf4j
@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private ImportService importService;

    /**
     * 上传 Excel 文件并启动导入
     *
     * @param file 上传的 Excel 文件(.xlsx / .xls)
     * @return 包含 progressId 的响应,前端用它轮询进度
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        log.info("收到文件上传: name={}, size={}KB", file.getOriginalFilename(), file.getSize() / 1024);

        Map<String, Object> result = new HashMap<>();

        // 校验文件
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "文件为空");
            return result;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            result.put("success", false);
            result.put("message", "仅支持 .xlsx / .xls 文件");
            return result;
        }

        // 启动异步导入
        ImportProgress progress = importService.importFromExcel(file);

        result.put("success", true);
        result.put("progressId", progress.getProgressId());
        result.put("message", "文件上传成功,导入已启动");
        result.put("fileName", file.getOriginalFilename());

        return result;
    }
    /**
     * 下载导入模板
     *
     * 生成一个包含示例数据的 Excel 文件供用户参考
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("订单导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // 生成 5 条示例数据
        List<OrderExcelDTO> sampleData = new ArrayList<>();
        String[][] samples = {
            {"ORD000001", "10001", "张三", "旗舰手机", "电子产品", "2999.00", "1", "已完成", "北京", "北京", "2024-06-15 10:30:00"},
            {"ORD000002", "10002", "李四", "经典T恤", "服饰鞋包", "199.00", "2", "已付款", "上海", "上海", "2024-06-16 14:20:00"},
            {"ORD000003", "10003", "王五", "坚果礼盒", "食品饮料", "89.90", "3", "已发货", "广东", "深圳", "2024-06-17 09:15:00"},
            {"ORD000004", "10004", "赵六", "简约台灯", "家居用品", "159.00", "1", "待付款", "浙江", "杭州", "2024-06-18 16:45:00"},
            {"ORD000005", "10005", "孙七", "潮流小说", "图书文具", "39.90", "5", "已取消", "四川", "成都", "2024-06-19 11:00:00"},
        };
        for (String[] s : samples) {
            OrderExcelDTO dto = new OrderExcelDTO();
            dto.setOrderNo(s[0]);
            dto.setUserId(Long.parseLong(s[1]));
            dto.setUserName(s[2]);
            dto.setProductName(s[3]);
            dto.setCategory(s[4]);
            dto.setAmount(new java.math.BigDecimal(s[5]));
            dto.setQuantity(Integer.parseInt(s[6]));
            dto.setStatus(s[7]);
            dto.setProvince(s[8]);
            dto.setCity(s[9]);
            dto.setCreateTime(s[10]);
            sampleData.add(dto);
        }

        EasyExcel.write(response.getOutputStream(), OrderExcelDTO.class)
                .sheet("订单数据")
                .doWrite(sampleData);
    }

    /**
     * 查询导入进度
     *
     * 前端每 500ms 轮询一次,实时更新进度条
     * 返回的数据结构:
     * {
     *   progressId, fileName, status, totalRows,
     *   successCount, duplicateCount, failCount,
     *   progressPercent, costMs, errorMsg
     * }
     */
    @GetMapping("/progress/{progressId}")
    public Map<String, Object> getProgress(@PathVariable String progressId) {
        ImportProgress progress = importService.getProgress(progressId);

        Map<String, Object> result = new HashMap<>();
        if (progress == null) {
            result.put("success", false);
            result.put("message", "进度ID不存在");
            return result;
        }

        result.put("success", true);
        result.put("progressId", progress.getProgressId());
        result.put("fileName", progress.getFileName());
        result.put("status", progress.getStatus().name());
        result.put("totalRows", progress.getTotalRows().get());
        result.put("successCount", progress.getSuccessCount().get());
        result.put("duplicateCount", progress.getDuplicateCount().get());
        result.put("failCount", progress.getFailCount().get());
        result.put("progressPercent", progress.getProgressPercent());
        result.put("costMs", progress.getCostMs());
        result.put("errorMsg", progress.getErrorMsg());
        result.put("finished", progress.isFinished());

        return result;
    }
}

package com.example.bigdata.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.bigdata.dto.ImportProgress;
import com.example.bigdata.entity.OrderExcelDTO;
import com.example.bigdata.listener.OrderExcelListener;
import com.example.bigdata.mapper.OrderMapper;
import com.example.bigdata.service.ImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据导入服务实现
 *
 * 核心流程：
 * 1. 前端上传 Excel 文件
 * 2. 后端保存临时文件，返回 progressId
 * 3. 后台线程用 EasyExcel 流式读取，每积累 5000 条提交一次批量插入
 * 4. 前端轮询 /api/import/progress/{progressId} 获取实时进度
 * 5. 完成后前端展示结果
 *
 */
@Slf4j
@Service
public class ImportServiceImpl implements ImportService {

    @Resource
    private OrderMapper orderMapper;

    @Autowired
    @Qualifier("dataProcessPool")
    private ThreadPoolExecutor threadPool;

    /** 进度存储（内存 Map，生产环境可用 Redis） */
    private final Map<String, ImportProgress> progressMap = new ConcurrentHashMap<>();

    /** 每批大小 */
    private static final int BATCH_SIZE = 5000;

    @Override
    public ImportProgress importFromExcel(MultipartFile file) {
        // 1. 生成进度 ID
        String progressId = UUID.randomUUID().toString().replace("-", "");
        String fileName = file.getOriginalFilename();
        ImportProgress progress = new ImportProgress(progressId, fileName);
        progressMap.put(progressId, progress);

        // 2. 保存临时文件（EasyExcel 需要 File 对象，MultipartFile 是流式的）
        File tempFile = null;
        try {
            tempFile = File.createTempFile("import_", ".xlsx");
            file.transferTo(tempFile);
            log.info("临时文件已保存: {}", tempFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存临时文件失败", e);
            progress.markFailed("文件保存失败: " + e.getMessage());
            return progress;
        }

        // 3. 在后台线程执行导入（不阻塞 HTTP 请求）
        final File finalTempFile = tempFile;
        threadPool.submit(() -> {
            try {
                doImport(finalTempFile, progress);
            } catch (Exception e) {
                log.error("导入异常", e);
                progress.markFailed("导入异常: " + e.getMessage());
            } finally {
                // 清理临时文件
                if (finalTempFile != null && finalTempFile.exists()) {
                    finalTempFile.delete();
                    log.debug("临时文件已删除");
                }
            }
        });

        return progress;
    }

    @Override
    public ImportProgress getProgress(String progressId) {
        return progressMap.get(progressId);
    }

    /**
     * 执行实际的导入逻辑
     *
     * EasyExcel.read() 的执行流程：
     * 1. 用 SAX 解析器打开 Excel 文件
     * 2. 逐行读取，每读一行调用 listener.invoke()
     * 3. 全部读完调用 listener.doAfterAllAnalysed()
     * 整个过程内存占用恒定，不会因文件大而 OOM
     */
    private void doImport(File file, ImportProgress progress) {
        log.info("开始导入: file={}", file.getName());

        // 创建监听器（每个导入任务一个独立的监听器实例）
        OrderExcelListener listener = new OrderExcelListener(
                BATCH_SIZE, threadPool, orderMapper, progress);

        // 同步执行读取（在后台线程中运行，不会阻塞主线程）
        EasyExcel.read(file, OrderExcelDTO.class, listener)
                .sheet()
                .doRead();

        log.info("导入流程结束: progressId={}", progress.getProgressId());
    }
}

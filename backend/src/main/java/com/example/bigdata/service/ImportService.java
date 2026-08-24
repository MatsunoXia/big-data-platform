package com.example.bigdata.service;

import com.example.bigdata.dto.ImportProgress;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据导入服务
 *
 * 使用 EasyExcel 流式读取 + 多线程批量插入
 */
public interface ImportService {

    /**
     * 从 Excel 文件导入订单数据
     *
     * @param file 上传的 Excel 文件
     * @return 进度追踪对象（前端轮询读取进度）
     */
    ImportProgress importFromExcel(MultipartFile file);

    /**
     * 查询导入进度
     *
     * @param progressId 进度ID
     * @return 进度对象，不存在返回 null
     */
    ImportProgress getProgress(String progressId);
}

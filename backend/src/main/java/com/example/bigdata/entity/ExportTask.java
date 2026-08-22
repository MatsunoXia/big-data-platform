package com.example.bigdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导出任务表 - 记录异步导出任务状态
 */
@Data
@TableName("t_export_task")
public class ExportTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号 */
    private String taskNo;

    /** 导出文件名 */
    private String fileName;

    /** 任务状态：PENDING, PROCESSING, COMPLETED, FAILED */
    private String status;

    /** 总数据量 */
    private Integer totalCount;

    /** 已处理数据量 */
    private Integer processedCount;

    /** 文件路径 */
    private String filePath;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime finishTime;
}

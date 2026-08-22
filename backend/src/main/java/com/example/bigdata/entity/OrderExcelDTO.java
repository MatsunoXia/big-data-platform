package com.example.bigdata.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单 Excel 导入/导出 DTO
 */
@Data
public class OrderExcelDTO {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("用户ID")
    private Long userId;

    @ExcelProperty("用户名")
    private String userName;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("商品分类")
    private String category;

    @ExcelProperty("订单金额")
    private BigDecimal amount;

    @ExcelProperty("商品数量")
    private Integer quantity;

    @ExcelProperty("订单状态")
    private String status;

    @ExcelProperty("省份")
    private String province;

    @ExcelProperty("城市")
    private String city;

    @ExcelProperty("创建时间")
    private String createTime;
}

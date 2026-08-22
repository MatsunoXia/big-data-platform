package com.example.bigdata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 - 百万级数据量核心表
 */
@Data
@TableName("t_order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，格式：ORD + 时间戳 + 随机数 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String userName;

    /** 商品名称 */
    private String productName;

    /** 商品分类：电子、服饰、食品、家居、图书 */
    private String category;

    /** 订单金额 */
    private BigDecimal amount;

    /** 商品数量 */
    private Integer quantity;

    /** 订单状态：待付款、已付款、已发货、已完成、已取消 */
    private String status;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}

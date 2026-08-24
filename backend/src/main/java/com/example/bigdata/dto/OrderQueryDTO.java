package com.example.bigdata.dto;

import lombok.Data;

/**
 * 订单查询参数 DTO
 *
 * 统一封装前端传来的查询条件，避免 Controller 方法参数过多
 */
@Data
public class OrderQueryDTO {

    /** 订单号（精确匹配） */
    private String orderNo;

    /** 订单状态：待付款/已付款/已发货/已完成/已取消 */
    private String status;

    /** 商品分类：电子产品/服饰鞋包/食品饮料/家居用品/图书文具 */
    private String category;

    /** 用户ID */
    private Long userId;

    /** 省份 */
    private String province;

    /** 分页方式：offset（传统）/ cursor（游标） */
    private String pageType = "cursor";

    /** 页码（传统分页用），从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 游标值（游标分页用），即上一页最后一条的 ID */
    private Long cursorId;

    /** 是否使用缓存 */
    private Boolean useCache = true;
}

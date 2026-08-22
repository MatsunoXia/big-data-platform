-- ============================================================
-- 大数据量优化演示平台 - 数据库初始化脚本
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS big_data_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE big_data_platform;

-- 2. 订单表（核心表，百万级数据量）
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    order_no    VARCHAR(64)     NOT NULL                 COMMENT '订单号',
    user_id     BIGINT          NOT NULL                 COMMENT '用户ID',
    user_name   VARCHAR(32)     NOT NULL                 COMMENT '用户名',
    product_name VARCHAR(64)    NOT NULL                 COMMENT '商品名称',
    category    VARCHAR(16)     NOT NULL                 COMMENT '商品分类：电子产品/服饰鞋包/食品饮料/家居用品/图书文具',
    amount      DECIMAL(12,2)   NOT NULL                 COMMENT '订单金额',
    quantity    INT             NOT NULL DEFAULT 1       COMMENT '商品数量',
    status      VARCHAR(16)     NOT NULL                 COMMENT '订单状态：待付款/已付款/已发货/已完成/已取消',
    province    VARCHAR(16)     NOT NULL                 COMMENT '省份',
    city        VARCHAR(16)     NOT NULL                 COMMENT '城市',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT         NOT NULL DEFAULT 0       COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 索引设计说明（面试高频考点）
-- ============================================================

-- 索引1：订单号唯一索引
-- 场景：根据订单号精确查询
-- 类型：唯一索引，保证订单号不重复
CREATE UNIQUE INDEX idx_order_no ON t_order(order_no);

-- 索引2：用户ID索引
-- 场景：查询某个用户的所有订单
-- 类型：普通索引
CREATE INDEX idx_user_id ON t_order(user_id);

-- 索引3：创建时间索引
-- 场景：按时间范围查询（如：最近7天的订单）
-- 类型：普通索引
CREATE INDEX idx_create_time ON t_order(create_time);

-- 索引4：状态+分类联合索引（最常用的组合查询条件）
-- 场景：筛选"已完成"的"电子产品"订单
-- 类型：联合索引，遵循最左前缀原则
-- 字段顺序：status在前（选择性更高），category在后
CREATE INDEX idx_status_category ON t_order(status, category);

-- 索引5：省份+城市联合索引
-- 场景：按地区筛选订单
CREATE INDEX idx_province_city ON t_order(province, city);


-- 3. 导出任务表（记录异步导出任务状态）
DROP TABLE IF EXISTS t_export_task;
CREATE TABLE t_export_task (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    task_no         VARCHAR(64)     NOT NULL                 COMMENT '任务编号',
    file_name       VARCHAR(128)    DEFAULT NULL             COMMENT '导出文件名',
    status          VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/COMPLETED/FAILED',
    total_count     INT             DEFAULT 0                COMMENT '总数据量',
    processed_count INT             DEFAULT 0                COMMENT '已处理数据量',
    file_path       VARCHAR(256)    DEFAULT NULL             COMMENT '文件存储路径',
    error_msg       VARCHAR(512)    DEFAULT NULL             COMMENT '错误信息',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finish_time     DATETIME        DEFAULT NULL             COMMENT '完成时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='导出任务表';

CREATE UNIQUE INDEX idx_task_no ON t_export_task(task_no);


-- ============================================================
-- 索引设计要点总结（面试用）
-- ============================================================
--
-- 1. B+树索引原理：
--    - 非叶子节点只存索引，叶子节点存数据（聚簇索引）或主键（二级索引）
--    - 叶子节点之间有双向链表，支持范围查询
--
-- 2. 最左前缀原则：
--    - 联合索引 (a, b, c) 等价于 (a)、(a,b)、(a,b,c) 三个索引
--    - 查询条件必须从最左列开始才能命中索引
--    - 例：WHERE status='已完成' AND category='电子产品' → 命中 idx_status_category
--    - 例：WHERE category='电子产品' → 无法命中（跳过了status）
--
-- 3. 覆盖索引：
--    - 当查询的字段全部包含在索引中时，无需回表
--    - 例：SELECT status, category FROM t_order WHERE status='已完成'
--    - idx_status_category 已包含这两个字段，直接从索引返回
--
-- 4. 索引失效场景：
--    - 对索引列使用函数：WHERE YEAR(create_time) = 2024
--    - 隐式类型转换：WHERE order_no = 123（order_no是varchar）
--    - LIKE左模糊：WHERE order_no LIKE '%123'
--    - OR连接非索引列
-- ============================================================

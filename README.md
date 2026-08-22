# 大数据量优化演示平台

## 项目简介

通过一个电商订单管理后台，演示和实践 5 大大数据量场景的优化方案。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 2.7 + MyBatis Plus 3.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 前端 | Vue 3 + Axios + ECharts |
| Excel | EasyExcel 3.3 |
| 多线程 | ThreadPoolExecutor + CompletableFuture |

## 模块规划

| 模块 | 内容 | 核心知识点 |
|------|------|-----------|
| 模块一 | 百万数据检索 | 索引、游标分页、缓存、并行查询 |
| 模块二 | 大量数据导入 | EasyExcel流式读取、多线程批量插入 |
| 模块三 | 大量数据导出 | 异步任务、分段查询、流式写入 |
| 模块四 | 前端大数据表格 | 虚拟滚动、无限滚动 |
| 模块五 | ECharts可视化 | 降采样、Web Worker、渐进式渲染 |

## 快速启动

### 1. 初始化数据库

```bash
# 执行 sql/init.sql 创建数据库和表
mysql -u root -p < sql/init.sql
```

### 2. 修改配置

编辑 `backend/src/main/resources/application.yml`，修改数据库和 Redis 连接信息。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

### 4. 生成测试数据

```bash
# 生成10万条数据（开发阶段）
curl -X POST "http://localhost:8080/api/data/generate?count=100000&batchSize=5000&threadCount=4"

# 生成100万条数据（演示阶段）
curl -X POST "http://localhost:8080/api/data/generate?count=1000000&batchSize=5000&threadCount=4"
```

### 5. 查看数据统计

```bash
curl http://localhost:8080/api/data/stats
```

## 项目结构

```
big-data-platform/
├── sql/
│   └── init.sql                    # 数据库DDL + 索引设计 + 面试要点
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/bigdata/
│       │   ├── BigDataPlatformApplication.java
│       │   ├── config/
│       │   │   ├── MybatisPlusConfig.java    # 分页插件
│       │   │   ├── ThreadPoolConfig.java     # 线程池配置
│       │   │   ├── RedisConfig.java          # Redis序列化
│       │   │   └── WebConfig.java            # 跨域配置
│       │   ├── entity/
│       │   │   ├── Order.java                # 订单实体
│       │   │   ├── ExportTask.java           # 导出任务实体
│       │   │   └── OrderExcelDTO.java        # Excel导入导出DTO
│       │   ├── mapper/
│       │   │   ├── OrderMapper.java
│       │   │   └── ExportTaskMapper.java
│       │   ├── service/
│       │   │   ├── OrderService.java
│       │   │   └── impl/OrderServiceImpl.java
│       │   ├── controller/
│       │   │   └── DataGeneratorController.java
│       │   └── util/
│       │       └── DataGenerator.java        # 测试数据生成器
│       └── resources/
│           ├── application.yml
│           └── mapper/OrderMapper.xml
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js                   # 入口：Element Plus + Router
        ├── App.vue                    # 布局：Header + Aside + Main
        ├── router/index.js            # 路由：7个页面
        ├── api/index.js               # Axios封装 + 耗时拦截器
        └── views/
            ├── Home.vue               # 首页概览 + 数据统计
            ├── DataManage.vue         # 数据管理：生成测试数据
            ├── ModuleSearch.vue       # 模块一：数据检索（待实现）
            ├── ModuleImport.vue       # 模块二：数据导入（待实现）
            ├── ModuleExport.vue       # 模块三：数据导出（待实现）
            ├── ModuleTable.vue        # 模块四：虚拟滚动（待实现）
            └── ModuleChart.vue        # 模块五：ECharts（待实现）
```

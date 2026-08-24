# 大数据量优化演示平台

## 项目简介

通过模拟一个电商订单管理后台，演示和实践 5 大大数据量场景的优化方案。

## 功能模块

| 模块 | 场景 | 核心优化 | 性能提升 |
|------|------|----------|----------|
| 数据检索 | 百万数据分页查询 | 游标分页 + Redis 缓存 + 索引优化 | 深分页 |
| 数据导入 | Excel 百万行导入 | EasyExcel 流式读取 + 多线程批量插入 | 100万条 |
| 数据导出 | 百万数据导出 Excel | 异步任务 + 分段并行查询 + 流式写入 | 100万条 |
| 前端表格 | 万级数据表格渲染 | 虚拟滚动 + 无限滚动 | DOM 节点恒定 |
| 图表可视化 | 十万级数据点绘图 | LTTB 降采样 + Web Worker | 渲染 |

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

后端运行在 http://localhost:8080

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 http://localhost:5173，自动代理 API 到后端。

### 5. 生成测试数据

访问「数据管理」页面，选择数据量（建议先用 1 万条测试），点击「开始生成」。

## 项目结构

```
big-data-platform/
├── sql/
│   └── init.sql                    # 数据库初始化脚本（建表 + 索引）
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/example/bigdata/
│       ├── config/                  # 配置类
│       │   ├── MybatisPlusConfig    # 分页插件
│       │   ├── RedisConfig          # Redis 序列化
│       │   ├── ThreadPoolConfig     # 线程池配置
│       │   └── WebConfig            # CORS + 拦截器
│       ├── entity/                  # 实体类
│       │   ├── Order                # 订单表
│       │   ├── ExportTask           # 导出任务表
│       │   └── OrderExcelDTO        # Excel 导入导出 DTO
│       ├── mapper/                  # MyBatis Mapper
│       │   ├── OrderMapper          # 订单 CRUD + 批量操作
│       │   └── ExportTaskMapper     # 导出任务 CRUD
│       ├── service/                 # 业务逻辑
│       │   ├── OrderService         # 数据生成
│       │   ├── OrderSearchService   # 检索（OFFSET + 游标 + 缓存）
│       │   ├── ImportService        # 导入（EasyExcel + 多线程）
│       │   └── ExportService        # 导出（异步 + 分段并行）
│       ├── controller/              # API 接口
│       ├── interceptor/             # 查询耗时拦截器
│       ├── listener/                # EasyExcel 监听器
│       ├── dto/                     # 数据传输对象
│       └── util/                    # 工具类（数据生成器）
└── frontend/
    └── src/
        ├── api/                     # API 请求封装
        ├── views/                   # 页面组件
        │   ├── Home                 # 首页概览
        │   ├── DataManage           # 数据管理
        │   ├── ModuleSearch         # 检索模块
        │   ├── ModuleImport         # 导入模块
        │   ├── ModuleExport         # 导出模块
        │   ├── ModuleTable          # 前端表格
        │   └── ModuleChart          # 图表可视化
        └── workers/                 # Web Worker
            └── dataWorker           # LTTB 降采样 + 数据聚合
```

## 核心优化技术详解

### 1. 游标分页（检索模块）

```sql
-- 传统 OFFSET：深分页极慢
SELECT * FROM t_order ORDER BY id DESC LIMIT 20 OFFSET 1000000;
-- 扫描 100 万行

-- 游标分页：性能稳定
SELECT * FROM t_order WHERE id < #{lastId} ORDER BY id DESC LIMIT 20;
-- 主键索引直接定位
```

### 2. EasyExcel 流式读取（导入模块）

```
POI XSSFWorkbook：整个 Excel 加载到内存 → 100MB 文件可能 OOM
EasyExcel：SAX 模式逐行解析 → 内存占用恒定 ~50MB
```

### 3. 分段并行查询（导出模块）

```
单次查询 100 万条 → 锁表 30s
分 10 段并行查询 → 每段 10 万条，总耗时 ~5s
CompletableFuture.supplyAsync() + 自定义线程池
```

### 4. 虚拟滚动（前端表格）

```
普通渲染：10 万行 = 10 万个 DOM 节点 → 页面卡死
虚拟滚动：只渲染可见 ~30 行，transform 定位 → DOM 恒定 ~60 个
```

### 5. LTTB 降采样（图表模块）

```
原始 10 万点直接渲染 → 浏览器卡顿
LTTB 降采样到 1000 点 → 视觉几乎无损，渲染 < 500ms
原理：每桶选三角形面积最大的点，保留极值和拐点
```

## API 接口

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 数据 | GET | `/api/data/stats` | 获取数据统计 |
| 数据 | POST | `/api/data/generate` | 生成测试数据 |
| 检索 | GET | `/api/search/orders` | 订单检索（支持 OFFSET/游标分页） |
| 检索 | GET | `/api/search/compare` | 性能对比（两种分页同时执行） |
| 导入 | POST | `/api/import/upload` | 上传 Excel 文件 |
| 导入 | GET | `/api/import/progress/{id}` | 查询导入进度 |
| 导入 | GET | `/api/import/template` | 下载导入模板 |
| 导出 | POST | `/api/export/start` | 启动异步导出 |
| 导出 | GET | `/api/export/progress/{taskNo}` | 查询导出进度 |
| 导出 | GET | `/api/export/download/{taskNo}` | 下载导出文件 |
| 导出 | GET | `/api/export/tasks` | 导出任务列表 |

## License

MIT

<template>
  <div class="home">
    <!-- ========== 头部 ========== -->
    <div class="hero">
      <h1>大数据量场景优化演示平台</h1>
      <p class="hero-desc">
        通过模拟电商订单管理后台，演示 5 大大数据场景的优化方案。
      </p>
    </div>

    <!-- ========== 数据统计 ========== -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="订单总量" :value="stats.totalCount">
            <template #prefix><el-icon style="color: #409eff;"><Coin /></el-icon></template>
            <template #suffix><span style="font-size: 13px; color: #909399;">条</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="已完成模块" :value="5">
            <template #prefix><el-icon style="color: #67c23a;"><CircleCheck /></el-icon></template>
            <template #suffix><span style="font-size: 13px; color: #909399;">/ 5</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="优化技术点" :value="15">
            <template #prefix><el-icon style="color: #e6a23c;"><Promotion /></el-icon></template>
            <template #suffix><span style="font-size: 13px; color: #909399;">项</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <el-statistic title="API 接口" :value="12">
            <template #prefix><el-icon style="color: #909399;"><Connection /></el-icon></template>
            <template #suffix><span style="font-size: 13px; color: #909399;">个</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 模块列表 ========== -->
    <h3 class="section-title">功能模块</h3>
    <el-row :gutter="20" class="module-cards">
      <el-col :span="8" v-for="mod in modules" :key="mod.path">
        <el-card shadow="hover" class="module-card" @click="router.push(mod.path)">
          <div class="module-info">
            <div class="module-icon-wrap" :style="{ background: mod.color + '15' }">
              <el-icon class="module-icon" :size="32" :style="{ color: mod.color }">
                <component :is="mod.icon" />
              </el-icon>
            </div>
            <div class="module-text">
              <h3>{{ mod.title }}</h3>
              <p>{{ mod.desc }}</p>
              <div class="module-tags">
                <el-tag v-for="tag in mod.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
              </div>
              <div class="module-metrics">
                <span v-for="metric in mod.metrics" :key="metric" class="metric">
                  <el-icon><Right /></el-icon> {{ metric }}
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ========== 技术架构 ========== -->
    <h3 class="section-title">技术架构</h3>
    <el-card class="section">
      <el-row :gutter="32">
        <el-col :span="8">
          <h4 class="stack-title">后端</h4>
          <div class="stack-list">
            <div class="stack-item" v-for="t in backendStack" :key="t.name">
              <span class="stack-name">{{ t.name }}</span>
              <span class="stack-desc">{{ t.desc }}</span>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <h4 class="stack-title">前端</h4>
          <div class="stack-list">
            <div class="stack-item" v-for="t in frontendStack" :key="t.name">
              <span class="stack-name">{{ t.name }}</span>
              <span class="stack-desc">{{ t.desc }}</span>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <h4 class="stack-title">基础设施</h4>
          <div class="stack-list">
            <div class="stack-item" v-for="t in infraStack" :key="t.name">
              <span class="stack-name">{{ t.name }}</span>
              <span class="stack-desc">{{ t.desc }}</span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDataStats } from '../api'

const router = useRouter()

const stats = ref({ totalCount: 0, maxId: null, minId: null })

const modules = [
  {
    path: '/data-manage',
    icon: 'Coin',
    title: '数据管理',
    desc: '生成和管理百万级测试数据，支持多线程并发插入',
    tags: ['数据生成', '批量INSERT', '多线程'],
    metrics: ['4线程并发', '5000条/批', '100万条≈2min'],
    color: '#409eff'
  },
  {
    path: '/module/search',
    icon: 'Search',
    title: '模块一：数据检索',
    desc: '游标分页替代 OFFSET，Redis 缓存加速，索引优化',
    tags: ['游标分页', 'Redis缓存', '联合索引'],
    metrics: ['深分页: 6s→18ms', '缓存命中: ~3ms', '索引命中率100%'],
    color: '#67c23a'
  },
  {
    path: '/module/import',
    icon: 'Upload',
    title: '模块二：数据导入',
    desc: 'EasyExcel 流式读取，多线程分片批量插入，幂等防重',
    tags: ['EasyExcel', '多线程批量', '幂等'],
    metrics: ['内存恒定~50MB', '100万条≈3min', '自动去重'],
    color: '#e6a23c'
  },
  {
    path: '/module/export',
    icon: 'Download',
    title: '模块三：数据导出',
    desc: '异步任务 + 分段并行查询 + 流式写入 Excel',
    tags: ['异步任务', '分段并行', '流式写入'],
    metrics: ['查询10段并行', '100万条≈15s', '进度可追踪'],
    color: '#f56c6c'
  },
  {
    path: '/module/table',
    icon: 'List',
    title: '模块四：前端表格',
    desc: '虚拟滚动 + 无限滚动懒加载，DOM 节点数恒定',
    tags: ['虚拟滚动', '无限滚动', 'rAF节流'],
    metrics: ['DOM恒定~60个', '100万条不卡顿', '自动加载更多'],
    color: '#909399'
  },
  {
    path: '/module/chart',
    icon: 'TrendCharts',
    title: '模块五：图表可视化',
    desc: 'LTTB 降采样 + Web Worker + ECharts 大数据模式',
    tags: ['LTTB', 'Web Worker', '渐进式渲染'],
    metrics: ['10万→1千点', 'Worker不阻塞UI', '渲染<500ms'],
    color: '#b37feb'
  }
]

const backendStack = [
  { name: 'Spring Boot 2.7', desc: '应用框架' },
  { name: 'MyBatis Plus 3.5', desc: 'ORM + 分页插件' },
  { name: 'MySQL 8.0', desc: '关系型数据库' },
  { name: 'Redis', desc: '缓存 + 分布式锁' },
  { name: 'EasyExcel 3.3', desc: 'Excel 流式读写' },
  { name: 'HikariCP', desc: '高性能连接池' },
  { name: 'CompletableFuture', desc: '异步编排' }
]

const frontendStack = [
  { name: 'Vue 3', desc: '响应式框架' },
  { name: 'Element Plus', desc: 'UI 组件库' },
  { name: 'ECharts 5.5', desc: '图表可视化' },
  { name: 'Axios', desc: 'HTTP 客户端' },
  { name: 'Vite 5', desc: '构建工具' },
  { name: 'Web Worker', desc: '多线程计算' },
  { name: '虚拟滚动', desc: '自定义实现' }
]

const infraStack = [
  { name: '游标分页', desc: '替代 OFFSET 深分页' },
  { name: '联合索引', desc: '最左前缀原则' },
  { name: 'Redis 缓存', desc: '搜索+Count+锁' },
  { name: '多线程池', desc: 'IO密集型调优' },
  { name: '异步任务', desc: '非阻塞导出' },
  { name: 'LTTB 降采样', desc: '大数据可视化' },
  { name: '请求耗时拦截', desc: '性能监控' }
]

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

onMounted(async () => {
  try {
    const res = await getDataStats()
    stats.value = res.data
  } catch (e) {
    console.error('获取数据统计失败:', e)
  }
})
</script>

<style scoped>
/* ========== Hero ========== */
.hero {
  margin-bottom: 24px;
}

.hero h1 {
  font-size: 26px;
  color: #303133;
  margin-bottom: 8px;
}

.hero-desc {
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
}

/* ========== 统计卡片 ========== */
.stat-cards {
  margin-bottom: 28px;
}

.stat-cards .el-card {
  text-align: center;
}

/* ========== 段落标题 ========== */
.section-title {
  color: #303133;
  font-size: 17px;
  margin: 28px 0 16px 0;
  padding-left: 10px;
  border-left: 3px solid #409eff;
}

.section {
  margin-bottom: 0;
}

/* ========== 模块卡片 ========== */
.module-cards .el-col {
  margin-bottom: 16px;
}

.module-card {
  cursor: pointer;
  transition: all 0.3s;
}

.module-card:hover {
  transform: translateY(-3px);
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.module-info {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.module-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.module-text h3 {
  margin-bottom: 6px;
  color: #303133;
  font-size: 15px;
}

.module-text p {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
  line-height: 1.5;
}

.module-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.module-metrics {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric {
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 2px;
}

.metric .el-icon {
  font-size: 12px;
}

/* ========== 技术栈 ========== */
.stack-title {
  color: #303133;
  font-size: 15px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.stack-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stack-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stack-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.stack-desc {
  font-size: 12px;
  color: #909399;
}
</style>

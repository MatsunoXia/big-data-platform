<template>
  <div class="home">
    <h2>项目概览</h2>
    <p class="subtitle">通过一个电商订单管理后台，演示 5 大大数据量场景的优化方案</p>

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><Coin /></el-icon>
              <span>订单总量</span>
            </div>
          </template>
          <div class="stat-value">{{ formatNumber(stats.totalCount) }}</div>
          <div class="stat-label">条</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><Top /></el-icon>
              <span>最大ID</span>
            </div>
          </template>
          <div class="stat-value">{{ formatNumber(stats.maxId) }}</div>
          <div class="stat-label">自增主键</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><Bottom /></el-icon>
              <span>最小ID</span>
            </div>
          </template>
          <div class="stat-value">{{ formatNumber(stats.minId) }}</div>
          <div class="stat-label">自增主键</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模块列表 -->
    <el-row :gutter="20" class="module-cards">
      <el-col :span="6" v-for="mod in modules" :key="mod.path">
        <el-card shadow="hover" class="module-card" @click="router.push(mod.path)">
          <div class="module-info">
            <el-icon class="module-icon" :size="40"><component :is="mod.icon" /></el-icon>
            <div>
              <h3>{{ mod.title }}</h3>
              <p>{{ mod.desc }}</p>
              <div class="module-tags">
                <el-tag v-for="tag in mod.tags" :key="tag" size="small" type="info">{{ tag }}</el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDataStats } from '../api'

const router = useRouter()

const stats = ref({
  totalCount: 0,
  maxId: null,
  minId: null
})

const modules = [
  {
    path: '/module/search',
    icon: 'Search',
    title: '模块一：数据检索',
    desc: '索引优化、游标分页、缓存、并行查询',
    tags: ['MySQL索引', '游标分页', 'Redis缓存']
  },
  {
    path: '/module/import',
    icon: 'Upload',
    title: '模块二：数据导入',
    desc: 'EasyExcel流式读取、多线程批量插入',
    tags: ['EasyExcel', '多线程', '批量INSERT']
  },
  {
    path: '/module/export',
    icon: 'Download',
    title: '模块三：数据导出',
    desc: '异步任务、分段查询、流式写入',
    tags: ['异步任务', 'SXSSFWorkbook', '进度追踪']
  },
  {
    path: '/module/table',
    icon: 'List',
    title: '模块四：前端表格',
    desc: '虚拟滚动、无限滚动懒加载',
    tags: ['虚拟滚动', 'Intersection Observer']
  },
  {
    path: '/module/chart',
    icon: 'TrendCharts',
    title: '模块五：图表可视化',
    desc: '降采样、Web Worker、渐进式渲染',
    tags: ['ECharts', 'LTTB降采样', 'Web Worker']
  }
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
h2 {
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  color: #909399;
  margin-bottom: 24px;
}

.stat-cards {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #409eff;
  text-align: center;
}

.stat-label {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.module-cards .el-col {
  margin-bottom: 16px;
}

.module-card {
  cursor: pointer;
  transition: all 0.3s;
}

.module-card:hover {
  transform: translateY(-2px);
  border-color: #409eff;
}

.module-info {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.module-icon {
  color: #409eff;
  flex-shrink: 0;
}

.module-info h3 {
  margin-bottom: 8px;
  color: #303133;
  font-size: 16px;
}

.module-info p {
  color: #606266;
  font-size: 13px;
  margin-bottom: 8px;
}

.module-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
</style>

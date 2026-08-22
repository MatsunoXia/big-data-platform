<template>
  <div class="data-manage">
    <h2>数据管理</h2>
    <p class="subtitle">生成和管理测试数据</p>

    <!-- 当前数据统计 -->
    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>当前数据统计</span>
          <el-button type="primary" size="small" @click="refreshStats" :loading="statsLoading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="订单总量">
          <el-tag type="primary" size="large">{{ formatNumber(stats.totalCount) }} 条</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最大ID">{{ stats.maxId ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="最小ID">{{ stats.minId ?? '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 生成数据 -->
    <el-card class="section">
      <template #header>
        <span>生成测试数据</span>
      </template>

      <el-form :model="form" label-width="120px" style="max-width: 600px;">
        <el-form-item label="生成条数">
          <el-input-number
            v-model="form.count"
            :min="1000"
            :max="10000000"
            :step="10000"
            :controls-position="'right'"
          />
          <div class="form-tip">建议：开发用 1万，测试用 10万，演示用 100万</div>
        </el-form-item>

        <el-form-item label="每批条数">
          <el-input-number
            v-model="form.batchSize"
            :min="500"
            :max="20000"
            :step="500"
            :controls-position="'right'"
          />
          <div class="form-tip">批量INSERT的每批大小，一般 2000~5000 较优</div>
        </el-form-item>

        <el-form-item label="线程数">
          <el-input-number
            v-model="form.threadCount"
            :min="1"
            :max="16"
            :step="1"
            :controls-position="'right'"
          />
          <div class="form-tip">并发插入线程数，一般为 CPU核数×2</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleGenerate"
            :loading="generating"
            :icon="generating ? 'Loading' : 'Plus'"
          >
            {{ generating ? '生成中...' : '开始生成' }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 生成进度/结果 -->
      <el-alert
        v-if="result"
        :title="result.success ? '生成成功' : '生成失败'"
        :type="result.success ? 'success' : 'error'"
        :closable="false"
        show-icon
        style="margin-top: 16px;"
      >
        <template #default>
          <div v-if="result.success">
            <p>插入数据：<strong>{{ formatNumber(result.inserted) }}</strong> 条</p>
            <p>耗时：<strong>{{ result.costTimeMs }}ms</strong>（{{ (result.costTimeMs / 1000).toFixed(1) }}秒）</p>
            <p>当前总量：<strong>{{ formatNumber(result.totalCount) }}</strong> 条</p>
            <p>配置：每批 {{ result.batchSize }} 条，{{ result.threadCount }} 线程并发</p>
          </div>
          <div v-else>请查看控制台日志排查问题</div>
        </template>
      </el-alert>
    </el-card>

    <!-- 常用预设 -->
    <el-card class="section">
      <template #header>
        <span>快速预设</span>
      </template>
      <el-space wrap>
        <el-button @click="applyPreset(10000, 2000, 2)">1万条（开发）</el-button>
        <el-button @click="applyPreset(100000, 5000, 4)">10万条（测试）</el-button>
        <el-button @click="applyPreset(1000000, 5000, 4)">100万条（演示）</el-button>
        <el-button @click="applyPreset(5000000, 10000, 8)">500万条（极限）</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDataStats, generateData } from '../api'

const stats = ref({ totalCount: 0, maxId: null, minId: null })
const statsLoading = ref(false)
const generating = ref(false)
const result = ref(null)

const form = reactive({
  count: 100000,
  batchSize: 5000,
  threadCount: 4
})

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

async function refreshStats() {
  statsLoading.value = true
  try {
    const res = await getDataStats()
    stats.value = res.data
  } catch (e) {
    console.error('获取统计失败:', e)
  } finally {
    statsLoading.value = false
  }
}

async function handleGenerate() {
  generating.value = true
  result.value = null
  try {
    const res = await generateData({
      count: form.count,
      batchSize: form.batchSize,
      threadCount: form.threadCount
    })
    result.value = res.data
    // 刷新统计
    await refreshStats()
  } catch (e) {
    result.value = { success: false }
    console.error('生成失败:', e)
  } finally {
    generating.value = false
  }
}

function applyPreset(count, batchSize, threadCount) {
  form.count = count
  form.batchSize = batchSize
  form.threadCount = threadCount
}

onMounted(refreshStats)
</script>

<style scoped>
.data-manage {
  max-width: 1000px;
}

h2 {
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  color: #909399;
  margin-bottom: 20px;
}

.section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

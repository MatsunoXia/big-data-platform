<template>
  <div class="module-export">
    <h2>模块三：大量数据导出</h2>
    <p class="subtitle">异步任务 + 分段并行查询 + EasyExcel 流式写入</p>

    <!-- ========== 导出配置 ========== -->
    <el-card class="section">
      <template #header>
        <span>导出配置</span>
      </template>
      <el-form :model="exportForm" label-width="100px" style="max-width: 600px;">
        <el-form-item label="按状态筛选">
          <el-select v-model="exportForm.status" placeholder="全部状态" clearable>
            <el-option label="待付款" value="待付款" />
            <el-option label="已付款" value="已付款" />
            <el-option label="已发货" value="已发货" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item label="按分类筛选">
          <el-select v-model="exportForm.category" placeholder="全部分类" clearable>
            <el-option label="电子产品" value="电子产品" />
            <el-option label="服饰鞋包" value="服饰鞋包" />
            <el-option label="食品饮料" value="食品饮料" />
            <el-option label="家居用品" value="家居用品" />
            <el-option label="图书文具" value="图书文具" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleStartExport"
            :loading="starting"
          >
            <el-icon><Download /></el-icon> 启动异步导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ========== 当前任务进度 ========== -->
    <el-card v-if="currentTask" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>导出进度</span>
          <el-tag :type="taskStatusType" size="small">{{ taskStatusText }}</el-tag>
        </div>
      </template>

      <!-- 进度条 -->
      <el-progress
        :percentage="currentTask.progressPercent || 0"
        :status="progressStatus"
        :stroke-width="20"
        striped
        striped-flow
      />

      <!-- 统计信息 -->
      <el-descriptions :column="4" border size="small" style="margin-top: 16px;">
        <el-descriptions-item label="任务编号">
          <code>{{ currentTask.taskNo }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="总数据量">
          {{ formatNumber(currentTask.totalCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="已处理">
          {{ formatNumber(currentTask.processedCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ formatTime(currentTask.costMs) }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 下载按钮 -->
      <div v-if="currentTask.status === 'COMPLETED'" style="margin-top: 16px;">
        <el-button type="success" @click="handleDownload(currentTask.taskNo)">
          <el-icon><Download /></el-icon> 下载文件
        </el-button>
        <span style="margin-left: 12px; color: #909399; font-size: 13px;">
          文件名：{{ currentTask.fileName }}
        </span>
      </div>

      <!-- 错误信息 -->
      <el-alert
        v-if="currentTask.errorMsg"
        :title="'导出失败: ' + currentTask.errorMsg"
        type="error"
        show-icon
        style="margin-top: 16px;"
        :closable="false"
      />
    </el-card>

    <!-- ========== 历史任务列表 ========== -->
    <el-card class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>导出历史</span>
          <el-button size="small" @click="refreshTasks" :loading="tasksLoading">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="taskList" border stripe style="width: 100%;" v-loading="tasksLoading">
        <el-table-column prop="taskNo" label="任务编号" width="160">
          <template #default="{ row }">
            <code style="font-size: 12px;">{{ row.taskNo }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总数据量" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.totalCount) }}</template>
        </el-table-column>
        <el-table-column prop="processedCount" label="已处理" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.processedCount) }}</template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="180" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'COMPLETED'"
              type="primary"
              link
              size="small"
              @click="handleDownload(row.taskNo)"
            >
              下载
            </el-button>
            <el-button
              v-if="row.status === 'COMPLETED' || row.status === 'FAILED'"
              type="info"
              link
              size="small"
              @click="currentTask = row"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ========== 技术说明 ========== -->
    <el-card class="section" style="margin-top: 16px;">
      <template #header>
        <span>优化策略说明</span>
      </template>
      <el-collapse>
        <el-collapse-item title="1. 异步任务设计" name="async">
          <div class="explain-content">
            <p><strong>问题：</strong>100 万条数据导出需要 1-2 分钟，HTTP 请求超时</p>
            <p><strong>方案：</strong>POST 接口立即返回 taskNo，后台线程执行导出，前端轮询进度</p>
            <p><strong>流程：</strong>创建任务(PENDING) → 线程池执行 → 分段查询 → 写入文件 → 标记完成(COMPLETED)</p>
            <p><strong>好处：</strong>用户可以离开页面，回来后继续下载</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="2. 分段并行查询" name="parallel">
          <div class="explain-content">
            <p><strong>问题：</strong>单次 SELECT * FROM t_order（100 万条）锁表时间长，占用大量内存</p>
            <p><strong>方案：</strong>按 ID 范围分成 10 段，CompletableFuture 并行查询</p>
            <p><strong>效果：</strong>每段只查 10 万条，查询时间从 30s 降到 ~5s（并行）</p>
            <p><strong>关键：</strong>用 CompletableFuture.supplyAsync() + 自定义线程池</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="3. 流式写入" name="stream">
          <div class="explain-content">
            <p><strong>问题：</strong>把 100 万条数据全部加载到内存再写入，可能 OOM</p>
            <p><strong>方案：</strong>每查完一段立即写入文件，内存中只保留当前段的数据</p>
            <p><strong>工具：</strong>EasyExcel 的 write() 支持多次 write 调用，自动追加到同一个 Sheet</p>
            <p><strong>内存：</strong>100 万条导出，内存占用仅 ~100MB（当前段的数据）</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { startExport, getExportProgress, downloadExport, getExportTasks } from '../api'

// ==================== 状态 ====================
const starting = ref(false)
const currentTask = ref(null)
const taskList = ref([])
const tasksLoading = ref(false)
let pollTimer = null

const exportForm = reactive({
  status: '',
  category: ''
})

// ==================== 计算属性 ====================

const taskStatusText = computed(() => {
  if (!currentTask.value) return ''
  return statusLabel(currentTask.value.status)
})

const taskStatusType = computed(() => {
  if (!currentTask.value) return ''
  return getStatusType(currentTask.value.status)
})

const progressStatus = computed(() => {
  if (!currentTask.value) return ''
  if (currentTask.value.status === 'COMPLETED') return 'success'
  if (currentTask.value.status === 'FAILED') return 'exception'
  return ''
})

// ==================== 方法 ====================

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

function formatTime(ms) {
  if (!ms || ms <= 0) return '-'
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return Math.floor(ms / 60000) + 'm ' + Math.round((ms % 60000) / 1000) + 's'
}

function statusLabel(status) {
  const map = { PENDING: '等待中', PROCESSING: '导出中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}

function getStatusType(status) {
  const map = { PENDING: 'info', PROCESSING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || ''
}

/** 启动导出 */
async function handleStartExport() {
  starting.value = true
  try {
    const params = {}
    if (exportForm.status) params.status = exportForm.status
    if (exportForm.category) params.category = exportForm.category

    const res = await startExport(params)
    const data = res.data

    if (data.success) {
      ElMessage.success('导出任务已创建')
      currentTask.value = {
        taskNo: data.taskNo,
        status: 'PENDING',
        totalCount: 0,
        processedCount: 0,
        progressPercent: 0,
        costMs: 0
      }
      startPolling(data.taskNo)
      refreshTasks()
    } else {
      ElMessage.error(data.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('请求失败: ' + (e.message || '未知错误'))
  } finally {
    starting.value = false
  }
}

/** 轮询进度 */
function startPolling(taskNo) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await getExportProgress(taskNo)
      const data = res.data
      if (data.success) {
        currentTask.value = data
      }
      if (data.finished) {
        stopPolling()
        if (data.status === 'COMPLETED') {
          ElMessage.success('导出完成！共 ' + data.totalCount + ' 条数据')
        } else {
          ElMessage.error('导出失败: ' + (data.errorMsg || '未知错误'))
        }
        refreshTasks()
      }
    } catch (e) {
      console.error('轮询进度失败:', e)
    }
  }, 1000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

/** 下载文件 */
async function handleDownload(taskNo) {
  try {
    const res = await downloadExport(taskNo)
    // 创建 Blob 下载
    const blob = new Blob([res.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = res.headers['content-disposition']
      ? decodeURIComponent(res.headers['content-disposition'].split('filename=')[1])
      : '导出数据.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (e) {
    ElMessage.error('下载失败')
  }
}

/** 刷新任务列表 */
async function refreshTasks() {
  tasksLoading.value = true
  try {
    const res = await getExportTasks(10)
    taskList.value = res.data
  } catch (e) {
    console.error('获取任务列表失败:', e)
  } finally {
    tasksLoading.value = false
  }
}

onMounted(refreshTasks)
onBeforeUnmount(stopPolling)
</script>

<style scoped>
h2 {
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  color: #909399;
  margin-bottom: 20px;
}

.section {
  margin-bottom: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.explain-content p {
  margin: 6px 0;
  line-height: 1.8;
  color: #606266;
}

.explain-content code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  color: #e6a23c;
}
</style>

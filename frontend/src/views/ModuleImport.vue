<template>
  <div class="module-import">
    <h2>模块二：大量数据导入</h2>
    <p class="subtitle">EasyExcel 流式读取 + 多线程分片批量插入</p>

    <!-- ========== 文件上传 ========== -->
    <el-card class="section">
      <template #header>
        <span>上传 Excel 文件</span>
      </template>
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :on-remove="handleFileRemove"
        accept=".xlsx,.xls"
        :disabled="importing"
      >
        <el-icon class="el-icon--upload" :size="48"><Upload /></el-icon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx / .xls 文件，建议单文件不超过 200MB<br>
            Excel 列顺序：订单号、用户ID、用户名、商品名称、分类、金额、数量、状态、省份、城市、创建时间
          </div>
        </template>
      </el-upload>

      <div style="margin-top: 16px; display: flex; gap: 12px;">
        <el-button
          type="primary"
          @click="handleUpload"
          :loading="importing"
          :disabled="!selectedFile"
        >
          <el-icon><Upload /></el-icon>
          {{ importing ? '导入中...' : '开始导入' }}
        </el-button>
        <el-button v-if="importing" @click="showProgressDetail = !showProgressDetail">
          {{ showProgressDetail ? '隐藏详情' : '显示详情' }}
        </el-button>
        <el-button type="success" @click="downloadTemplate">
          <el-icon><Download /></el-icon> 下载导入模板
        </el-button>
      </div>
    </el-card>

    <!-- ========== 导入进度 ========== -->
    <el-card v-if="progress" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>导入进度</span>
          <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
        </div>
      </template>

      <!-- 进度条 -->
      <el-progress
        :percentage="progress.progressPercent"
        :status="progressStatus"
        :stroke-width="20"
        striped
        striped-flow
        :duration="10"
      />

      <!-- 统计数据 -->
      <el-descriptions :column="4" border size="small" style="margin-top: 16px;">
        <el-descriptions-item label="文件名">
          {{ progress.fileName }}
        </el-descriptions-item>
        <el-descriptions-item label="总行数">
          {{ formatNumber(progress.totalRows) }}
        </el-descriptions-item>
        <el-descriptions-item label="已处理">
          {{ formatNumber(progress.successCount + progress.duplicateCount + progress.failCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ formatTime(progress.costMs) }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 详细统计 -->
      <el-row :gutter="16" style="margin-top: 16px;">
        <el-col :span="8">
          <el-statistic title="成功插入" :value="progress.successCount">
            <template #suffix>
              <span style="font-size: 14px; color: #67c23a;">条</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="重复跳过" :value="progress.duplicateCount">
            <template #suffix>
              <span style="font-size: 14px; color: #e6a23c;">条</span>
            </template>
          </el-statistic>
        </el-col>
        <el-col :span="8">
          <el-statistic title="插入失败" :value="progress.failCount">
            <template #suffix>
              <span style="font-size: 14px; color: #f56c6c;">条</span>
            </template>
          </el-statistic>
        </el-col>
      </el-row>

      <!-- 详情（可折叠） -->
      <el-collapse-transition>
        <div v-if="showProgressDetail" style="margin-top: 16px;">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="进度ID">
              <code>{{ progress.progressId }}</code>
            </el-descriptions-item>
            <el-descriptions-item label="状态">{{ progress.status }}</el-descriptions-item>
            <el-descriptions-item label="成功插入率">
              {{ progress.totalRows > 0 ? ((progress.successCount / progress.totalRows) * 100).toFixed(1) : 0 }}%
            </el-descriptions-item>
            <el-descriptions-item label="处理速度">
              {{ progress.costMs > 0 ? Math.round(progress.successCount / (progress.costMs / 1000)) : 0 }} 条/秒
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-collapse-transition>

      <!-- 错误信息 -->
      <el-alert
        v-if="progress.errorMsg"
        :title="'导入失败: ' + progress.errorMsg"
        type="error"
        show-icon
        style="margin-top: 16px;"
        :closable="false"
      />
    </el-card>

    <!-- ========== 技术说明 ========== -->
    <el-card class="section" style="margin-top: 16px;">
      <template #header>
        <span>优化策略说明</span>
      </template>
      <el-collapse>
        <el-collapse-item title="1. EasyExcel 流式读取" name="easyexcel">
          <div class="explain-content">
            <p><strong>问题：</strong>POI 的 XSSFWorkbook 将整个 Excel 加载到内存，100MB 文件可能 OOM</p>
            <p><strong>方案：</strong>EasyExcel 基于 SAX 流式解析，逐行读取，内存占用恒定 ~50MB</p>
            <p><strong>原理：</strong>每读一行触发回调 invoke()，不在内存中积累全部数据</p>
            <p><strong>效果：</strong>100MB Excel（约 100 万行）内存占用稳定，不会 OOM</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="2. 多线程批量插入" name="batch">
          <div class="explain-content">
            <p><strong>单线程逐条 INSERT：</strong>100 万条 ≈ 30 分钟（每条一次网络往返）</p>
            <p><strong>多线程批量 INSERT：</strong>4 线程 + batch=5000 → 100 万条 ≈ 2-3 分钟</p>
            <p><strong>为什么批量快？</strong>减少网络往返次数和事务提交次数</p>
            <p><strong>批量大小怎么选？</strong>5000 条最佳，太大 MySQL 报 packet too large</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="3. 幂等防重" name="idempotent">
          <div class="explain-content">
            <p><strong>问题：</strong>同一批数据重复上传，不能产生重复订单</p>
            <p><strong>方案：</strong>每批插入前先查 order_no 是否已存在，过滤后再插入</p>
            <p><strong>优化：</strong>批量查询（IN 子句，每 1000 个一批），不用逐条查</p>
            <p><strong>效果：</strong>重复数据被跳过并计数，不影响已有数据</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadExcel, getImportProgress, downloadImportTemplate } from '../api'

// ==================== 状态 ====================
const uploadRef = ref(null)
const selectedFile = ref(null)
const importing = ref(false)
const progress = ref(null)
const showProgressDetail = ref(false)
let pollTimer = null

// ==================== 计算属性 ====================

const statusText = computed(() => {
  if (!progress.value) return ''
  const map = {
    READING: '读取 Excel...',
    INSERTING: '批量插入中...',
    COMPLETED: '导入完成',
    FAILED: '导入失败'
  }
  return map[progress.value.status] || progress.value.status
})

const statusTagType = computed(() => {
  if (!progress.value) return ''
  const map = {
    READING: 'warning',
    INSERTING: '',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return map[progress.value.status] || ''
})

const progressStatus = computed(() => {
  if (!progress.value) return ''
  if (progress.value.status === 'COMPLETED') return 'success'
  if (progress.value.status === 'FAILED') return 'exception'
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

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

async function downloadTemplate() {
  try {
    const res = await downloadImportTemplate()
    const url = URL.createObjectURL(res.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '订单导入模板.xlsx'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('模板下载失败: ' + (e.message || '未知错误'))
  }
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  importing.value = true
  progress.value = null

  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const res = await uploadExcel(formData)
    const data = res.data

    if (!data.success) {
      ElMessage.error(data.message || '上传失败')
      importing.value = false
      return
    }

    ElMessage.success('文件上传成功，导入已启动')

    progress.value = {
      progressId: data.progressId,
      fileName: data.fileName,
      status: 'READING',
      totalRows: 0,
      successCount: 0,
      duplicateCount: 0,
      failCount: 0,
      progressPercent: 0,
      costMs: 0,
      errorMsg: null,
      finished: false
    }

    // 开始轮询进度
    startPolling(data.progressId)

    // 清除页面上的已选文件
    uploadRef.value?.clearFiles()
    selectedFile.value = null

  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || '未知错误'))
    importing.value = false
  }
}

function startPolling(progressId) {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const res = await getImportProgress(progressId)
      const data = res.data

      if (data.success) {
        progress.value = data
      }

      if (data.finished) {
        stopPolling()
        importing.value = false

        if (data.status === 'COMPLETED') {
          ElMessage.success(
            `导入完成！成功 ${data.successCount} 条，重复 ${data.duplicateCount} 条，失败 ${data.failCount} 条`
          )
        } else {
          ElMessage.error('导入失败: ' + (data.errorMsg || '未知错误'))
        }
      }
    } catch (e) {
      console.error('轮询进度失败:', e)
    }
  }, 500)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.module-import {
  max-width: 900px;
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

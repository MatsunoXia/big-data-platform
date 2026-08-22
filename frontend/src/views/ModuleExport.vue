<template>
  <div class="module-export">
    <h2>模块三：大量数据导出</h2>
    <p class="subtitle">异步任务 + 分段并行查询 + 流式写入 Excel</p>

    <el-card class="section">
      <template #header>
        <span>导出配置</span>
      </template>
      <el-form :model="exportForm" label-width="100px" style="max-width: 600px;">
        <el-form-item label="导出条件">
          <el-select v-model="exportForm.status" placeholder="全部状态" clearable>
            <el-option label="待付款" value="待付款" />
            <el-option label="已付款" value="已付款" />
            <el-option label="已发货" value="已发货" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item label="导出方式">
          <el-radio-group v-model="exportForm.mode">
            <el-radio label="sync">同步导出（小数据量）</el-radio>
            <el-radio label="async">异步导出（大数据量）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Download">开始导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section" style="margin-top: 16px;">
      <el-empty description="导出功能将在模块三开发中实现">
        <template #image>
          <el-icon :size="60" color="#409eff"><Download /></el-icon>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Download } from '@element-plus/icons-vue'

const exportForm = reactive({
  status: '',
  mode: 'async'
})
</script>

<style scoped>
.module-export { max-width: 800px; }
h2 { color: #303133; margin-bottom: 8px; }
.subtitle { color: #909399; margin-bottom: 20px; }
</style>

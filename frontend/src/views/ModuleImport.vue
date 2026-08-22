<template>
  <div class="module-import">
    <h2>模块二：大量数据导入</h2>
    <p class="subtitle">EasyExcel 流式读取 + 多线程批量插入</p>

    <el-card class="section">
      <el-upload
        drag
        action="/api/import/upload"
        :before-upload="beforeUpload"
        :on-success="onSuccess"
        :on-error="onError"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload" :size="40"><Upload /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls 文件，建议单文件不超过 200MB</div>
        </template>
      </el-upload>
    </el-card>

    <el-card class="section" style="margin-top: 16px;">
      <el-empty description="导入功能将在模块二开发中实现">
        <template #image>
          <el-icon :size="60" color="#409eff"><Upload /></el-icon>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
function beforeUpload(file) {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    || file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件')
  }
  return isExcel
}

function onSuccess(response) {
  ElMessage.success('上传成功')
}

function onError() {
  ElMessage.error('上传失败')
}
</script>

<style scoped>
.module-import { max-width: 800px; }
h2 { color: #303133; margin-bottom: 8px; }
.subtitle { color: #909399; margin-bottom: 20px; }
</style>

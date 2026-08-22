<template>
  <div class="module-search">
    <h2>模块一：大数据量检索</h2>
    <p class="subtitle">对比传统分页与优化方案的查询性能差异</p>

    <el-card class="section">
      <template #header>
        <span>查询条件</span>
      </template>
      <el-form :inline="true" :model="queryForm" label-width="80px">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" placeholder="输入订单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="待付款" value="待付款" />
            <el-option label="已付款" value="已付款" />
            <el-option label="已发货" value="已发货" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部" clearable>
            <el-option label="电子产品" value="电子产品" />
            <el-option label="服饰鞋包" value="服饰鞋包" />
            <el-option label="食品饮料" value="食品饮料" />
            <el-option label="家居用品" value="家居用品" />
            <el-option label="图书文具" value="图书文具" />
          </el-select>
        </el-form-item>
        <el-form-item label="分页方式">
          <el-radio-group v-model="queryForm.pageType">
            <el-radio label="offset">传统 OFFSET</el-radio>
            <el-radio label="cursor">游标分页</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 查询结果统计 -->
    <el-card v-if="queryResult" class="section" style="margin-top: 16px;">
      <template #header>
        <span>查询结果</span>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="查询耗时">
          <el-tag type="success">{{ queryResult.queryTime }}ms</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="返回条数">{{ queryResult.list?.length }}</el-descriptions-item>
        <el-descriptions-item label="总条数">{{ formatNumber(queryResult.total) }}</el-descriptions-item>
        <el-descriptions-item label="当前页">{{ queryResult.pageNum }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 占位：后续实现 -->
    <el-card class="section" style="margin-top: 16px;">
      <el-empty description="检索功能将在模块一开发中实现">
        <template #image>
          <el-icon :size="60" color="#409eff"><Search /></el-icon>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'

const loading = ref(false)
const queryResult = ref(null)

const queryForm = reactive({
  orderNo: '',
  status: '',
  category: '',
  pageType: 'cursor'
})

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

async function handleQuery() {
  loading.value = true
  // TODO: 调用后端接口
  setTimeout(() => {
    loading.value = false
  }, 500)
}
</script>

<style scoped>
.module-search { max-width: 1200px; }
h2 { color: #303133; margin-bottom: 8px; }
.subtitle { color: #909399; margin-bottom: 20px; }
</style>

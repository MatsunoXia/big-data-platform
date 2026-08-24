<template>
  <div class="module-search">
    <h2>模块一：大数据量检索</h2>
    <p class="subtitle">对比传统分页与游标分页的查询性能差异</p>

    <!-- ========== 查询条件 ========== -->
    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>查询条件</span>
          <el-tag :type="queryForm.pageType === 'cursor' ? 'success' : 'warning'" size="small">
            {{ queryForm.pageType === 'cursor' ? '游标分页' : '传统 OFFSET 分页' }}
          </el-tag>
        </div>
      </template>
      <el-form :inline="true" :model="queryForm" label-width="80px">
        <el-form-item label="订单号">
          <el-input v-model="queryForm.orderNo" placeholder="输入订单号" clearable style="width: 200px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待付款" value="待付款" />
            <el-option label="已付款" value="已付款" />
            <el-option label="已发货" value="已发货" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部" clearable style="width: 120px;">
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
        <el-form-item label="每页条数">
          <el-select v-model="queryForm.pageSize" style="width: 80px;">
            <el-option :value="10" label="10" />
            <el-option :value="20" label="20" />
            <el-option :value="50" label="50" />
            <el-option :value="100" label="100" />
          </el-select>
        </el-form-item>
        <el-form-item label="缓存">
          <el-switch v-model="queryForm.useCache" active-text="开" inactive-text="关" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleCompare" :loading="compareLoading" type="warning">
            <el-icon><DataAnalysis /></el-icon> 性能对比
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- ========== 性能元信息面板 ========== -->
    <el-card v-if="result" class="section" style="margin-top: 16px;">
      <template #header>
        <span>查询元信息</span>
      </template>
      <el-descriptions :column="5" border size="small">
        <el-descriptions-item label="查询耗时">
          <el-tag :type="result.queryTimeMs < 50 ? 'success' : result.queryTimeMs < 200 ? 'warning' : 'danger'" size="large">
            {{ result.queryTimeMs }}ms
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="返回条数">{{ result.list?.length ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="总条数">{{ formatNumber(result.total) }}</el-descriptions-item>
        <el-descriptions-item label="缓存状态">
          <el-tag :type="result.cacheHit ? 'success' : 'info'" size="small">
            {{ result.cacheHit ? '命中 (Redis)' : '未命中' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="索引命中">
          <el-tag type="primary" size="small">{{ result.indexHit || '-' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 8px; color: #909399; font-size: 12px;">
        分页方式：{{ result.pageTypeDesc }}
        <template v-if="result.pageNum">
          | 当前第 {{ result.pageNum }} 页 / 共 {{ result.totalPages }} 页
        </template>
        <template v-if="result.hasNext !== undefined">
          | {{ result.hasNext ? '有下一页' : '已是最后一页' }}
        </template>
      </div>
    </el-card>

    <!-- ========== 性能对比结果 ========== -->
    <el-card v-if="compareResult" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>性能对比结果</span>
          <el-tag type="danger" size="small">
            游标分页快 {{ compareResult.speedup }}
          </el-tag>
        </div>
      </template>
      <el-table :data="compareTableData" border style="width: 100%;">
        <el-table-column prop="metric" label="指标" width="180" />
        <el-table-column prop="offset" label="传统 OFFSET 分页" align="center">
          <template #default="{ row }">
            <span :class="row.offsetClass">{{ row.offset }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cursor" label="游标分页" align="center">
          <template #default="{ row }">
            <span :class="row.cursorClass">{{ row.cursor }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-alert
        title="为什么游标分页更快？"
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 16px;"
      >
        <template #default>
          <p>传统 OFFSET：<code>LIMIT 20 OFFSET 1000000</code> → MySQL 需要扫描并丢弃前 100 万行，只返回 20 行</p>
          <p>游标分页：<code>WHERE id &lt; #{cursorId} LIMIT 20</code> → 主键索引直接定位，不扫描前面的行</p>
          <p>结论：深分页场景下游标分页耗时稳定，传统分页随页码线性增长</p>
        </template>
      </el-alert>
    </el-card>

    <!-- ========== 数据表格 ========== -->
    <el-card v-if="result" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>查询结果</span>
          <div>
            <!-- 传统分页的翻页控件 -->
            <template v-if="queryForm.pageType === 'offset'">
              <el-pagination
                v-model:current-page="queryForm.pageNum"
                :page-size="queryForm.pageSize"
                :total="result.total || 0"
                layout="prev, pager, next, jumper, total"
                @current-change="handlePageChange"
                :pager-count="7"
                small
              />
            </template>
            <!-- 游标分页的翻页控件 -->
            <template v-else>
              <el-space>
                <el-button size="small" @click="handleFirstPage" :disabled="!cursorHistory.length">
                  <el-icon><RefreshLeft /></el-icon> 首页
                </el-button>
                <el-button size="small" @click="handlePrevPage" :disabled="!cursorHistory.length">
                  <el-icon><ArrowLeft /></el-icon> 上一页
                </el-button>
                <el-button size="small" @click="handleNextPage" :disabled="!result.hasNext">
                  下一页 <el-icon><ArrowRight /></el-icon>
                </el-button>
                <span style="color: #909399; font-size: 12px; margin-left: 8px;">
                  第 {{ cursorPageIndex + 1 }} 页
                </span>
              </el-space>
            </template>
          </div>
        </div>
      </template>

      <el-table
        :data="result.list"
        border
        stripe
        style="width: 100%;"
        :max-height="500"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" sortable width="80" />
        <el-table-column prop="orderNo" label="订单号" width="240" />
        <el-table-column prop="userName" label="用户" />
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="category" label="分类">
          <template #default="{ row }">
            <el-tag size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" align="right">
          <template #default="{ row }">
            ¥{{ Number(row.amount).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" align="center" width="80" />
        <el-table-column prop="status" label="状态" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="province" label="省份" />
        <el-table-column prop="city" label="城市" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
    </el-card>

    <!-- ========== 优化说明 ========== -->
    <el-card class="section" style="margin-top: 16px;">
      <template #header>
        <span>优化策略说明</span>
      </template>
      <el-collapse>
        <el-collapse-item title="1. 游标分页原理" name="cursor">
          <div class="explain-content">
            <p><strong>问题：</strong>传统 <code>LIMIT offset, size</code> 在深分页时性能急剧下降</p>
            <p><strong>原因：</strong>MySQL 必须扫描并丢弃前 offset 行，offset 越大越慢</p>
            <p><strong>方案：</strong>用 <code>WHERE id &lt; #{lastId} ORDER BY id DESC LIMIT size</code> 代替</p>
            <p><strong>效果：</strong>走主键索引直接定位，无论第几页耗时都稳定在 ~20ms</p>
            <p><strong>限制：</strong>只能"上一页/下一页"，不能跳页</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="2. 索引设计" name="index">
          <div class="explain-content">
            <p><code>idx_order_no</code> — 订单号唯一索引，精确查询</p>
            <p><code>idx_user_id</code> — 用户ID索引，查某用户所有订单</p>
            <p><code>idx_create_time</code> — 时间索引，范围查询</p>
            <p><code>idx_status_category</code> — 联合索引（status, category），最常用组合条件</p>
            <p><strong>最左前缀原则：</strong>联合索引 (a,b,c) 等价于 (a)、(a,b)、(a,b,c)</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="3. Redis 缓存策略" name="cache">
          <div class="explain-content">
            <p><strong>搜索结果缓存：</strong>Key = 查询条件 MD5，TTL = 5 分钟</p>
            <p><strong>Count 缓存：</strong>Key = 条件 MD5，TTL = 10 分钟（全表 COUNT 变化慢）</p>
            <p><strong>缓存穿透防护：</strong>无结果时缓存空值（短 TTL）</p>
            <p><strong>缓存击穿防护：</strong>热门 Key 过期时用分布式锁重建</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { searchOrders, comparePerformance } from '../api'

// ==================== 状态 ====================
const loading = ref(false)
const compareLoading = ref(false)
const result = ref(null)
const compareResult = ref(null)

// 游标分页历史记录（用于"上一页"）
const cursorHistory = ref([])
const cursorPageIndex = ref(0)

const queryForm = reactive({
  orderNo: '',
  status: '',
  category: '',
  pageType: 'cursor',
  pageNum: 1,
  pageSize: 20,
  cursorId: null,
  useCache: true
})

// ==================== 计算属性 ====================

const compareTableData = computed(() => {
  if (!compareResult.value) return []
  const o = compareResult.value.offset
  const c = compareResult.value.cursor
  return [
    {
      metric: '查询耗时',
      offset: o.queryTimeMs + 'ms',
      cursor: c.queryTimeMs + 'ms',
      offsetClass: o.queryTimeMs > 100 ? 'text-danger' : '',
      cursorClass: 'text-success'
    },
    {
      metric: '返回条数',
      offset: o.list?.length ?? 0,
      cursor: c.list?.length ?? 0,
      offsetClass: '',
      cursorClass: ''
    },
    {
      metric: '总条数',
      offset: formatNumber(o.total),
      cursor: formatNumber(c.total),
      offsetClass: '',
      cursorClass: ''
    },
    {
      metric: '缓存状态',
      offset: o.cacheHit ? '命中' : '未命中',
      cursor: c.cacheHit ? '命中' : '未命中',
      offsetClass: '',
      cursorClass: ''
    },
    {
      metric: '索引命中',
      offset: o.indexHit || '-',
      cursor: c.indexHit || '-',
      offsetClass: '',
      cursorClass: ''
    },
    {
      metric: '分页方式',
      offset: 'LIMIT ' + (o.pageNum - 1) * o.pageSize + ', ' + o.pageSize,
      cursor: 'WHERE id < cursor LIMIT ' + c.pageSize,
      offsetClass: '',
      cursorClass: ''
    }
  ]
})

// ==================== 方法 ====================

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

function statusType(status) {
  const map = {
    '待付款': 'warning',
    '已付款': '',
    '已发货': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || ''
}

/** 查询 */
async function handleSearch() {
  loading.value = true
  compareResult.value = null

  try {
    const params = {
      pageType: queryForm.pageType,
      pageSize: queryForm.pageSize,
      useCache: queryForm.useCache,
      orderNo: queryForm.orderNo || undefined,
      status: queryForm.status || undefined,
      category: queryForm.category || undefined
    }

    if (queryForm.pageType === 'offset') {
      params.pageNum = queryForm.pageNum
    } else {
      params.cursorId = queryForm.cursorId
    }

    const res = await searchOrders(params)
    result.value = res.data

    // 重置游标历史（首页查询时）
    if (queryForm.pageType === 'cursor' && !queryForm.cursorId) {
      cursorHistory.value = []
      cursorPageIndex.value = 0
    }
  } catch (e) {
    ElMessage.error('查询失败: ' + (e.message || '未知错误'))
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 性能对比 */
async function handleCompare() {
  compareLoading.value = true
  try {
    const params = {
      pageNum: queryForm.pageNum,
      pageSize: queryForm.pageSize,
      useCache: false, // 对比时不走缓存，看真实性能
      orderNo: queryForm.orderNo || undefined,
      status: queryForm.status || undefined,
      category: queryForm.category || undefined
    }
    const res = await comparePerformance(params)
    compareResult.value = res.data
    ElMessage.success('对比完成')
  } catch (e) {
    ElMessage.error('对比失败: ' + (e.message || '未知错误'))
    console.error(e)
  } finally {
    compareLoading.value = false
  }
}

/** 传统分页 — 页码变化 */
function handlePageChange(page) {
  queryForm.pageNum = page
  handleSearch()
}

/** 游标分页 — 下一页 */
function handleNextPage() {
  if (!result.value?.hasNext) return
  // 保存当前游标到历史
  cursorHistory.value.push(queryForm.cursorId)
  // 用当前页最后一条 ID 作为下一页游标
  queryForm.cursorId = result.value.nextCursorId
  cursorPageIndex.value++
  handleSearch()
}

/** 游标分页 — 上一页 */
function handlePrevPage() {
  if (!cursorHistory.value.length) return
  // 弹出上一个游标
  queryForm.cursorId = cursorHistory.value.pop()
  cursorPageIndex.value--
  handleSearch()
}

/** 游标分页 — 回到首页 */
function handleFirstPage() {
  queryForm.cursorId = null
  cursorHistory.value = []
  cursorPageIndex.value = 0
  handleSearch()
}
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

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

.text-success {
  color: #67c23a;
  font-weight: 600;
}
</style>

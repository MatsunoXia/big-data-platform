<template>
  <div class="module-table">
    <h2>模块四：前端大数据表格</h2>
    <p class="subtitle">虚拟滚动 + 无限滚动懒加载，解决万级 DOM 渲染卡顿问题</p>

    <!-- ========== 控制面板 ========== -->
    <el-card class="section">
      <template #header>
        <div class="card-header">
          <span>渲染模式</span>
          <el-tag :type="mode === 'virtual' ? 'success' : 'warning'" size="small">
            {{ mode === 'virtual' ? '虚拟滚动' : '普通渲染' }}
          </el-tag>
        </div>
      </template>
      <el-space wrap>
        <el-button
          :type="mode === 'virtual' ? 'primary' : ''"
          @click="switchMode('virtual')"
        >
          虚拟滚动模式
        </el-button>
        <el-button
          :type="mode === 'normal' ? 'primary' : ''"
          @click="switchMode('normal')"
        >
          普通渲染模式
        </el-button>
        <el-divider direction="vertical" />
        <el-button @click="resetData" :icon="Refresh">重置数据</el-button>
      </el-space>

      <!-- 性能指标 -->
      <el-descriptions border size="small" style="margin-top: 16px;" label-width="150px">
        <el-descriptions-item label="已加载数据">
          <el-tag type="primary">{{ formatNumber(loadedData.length) }} 条</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="DOM 节点数">
          <el-tag :type="domCount > 1000 ? 'danger' : 'success'">
            {{ formatNumber(domCount) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="渲染耗时">
          <el-tag :type="renderTime > 100 ? 'danger' : 'success'">
            {{ renderTime }}ms
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="可见行数">
          {{ visibleCount }}
        </el-descriptions-item>
        <el-descriptions-item label="滚动位置">
          {{ formatNumber(scrollTop) }}px
        </el-descriptions-item>
      </el-descriptions>

      <!-- 优化说明 -->
      <el-alert
        v-if="mode === 'virtual'"
        title="虚拟滚动：只渲染可见区域的 ~30 行，DOM 节点数恒定，不随数据量增长"
        type="success"
        :closable="false"
        show-icon
        style="margin-top: 12px;"
      />
      <el-alert
        v-if="mode === 'normal'"
        title="普通渲染：所有数据都创建 DOM 节点，数据量大时页面明显卡顿"
        type="warning"
        :closable="false"
        show-icon
        style="margin-top: 12px;"
      />
    </el-card>

    <!-- ========== 虚拟滚动表格 ========== -->
    <el-card v-if="mode === 'virtual'" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>虚拟滚动表格（无限滚动加载）</span>
          <el-tag size="small" type="info">
            滚动到底部自动加载更多
          </el-tag>
        </div>
      </template>

      <!-- 表头（固定） -->
      <div class="vs-header">
        <div class="vs-cell" style="width: 80px;">ID</div>
        <div class="vs-cell" style="width: 170px;">订单号</div>
        <div class="vs-cell" style="width: 80px;">用户</div>
        <div class="vs-cell" style="flex: 1;">商品</div>
        <div class="vs-cell" style="width: 90px;">分类</div>
        <div class="vs-cell" style="width: 90px; text-align: right;">金额</div>
        <div class="vs-cell" style="width: 60px; text-align: center;">数量</div>
        <div class="vs-cell" style="width: 80px; text-align: center;">状态</div>
        <div class="vs-cell" style="width: 70px;">省份</div>
        <div class="vs-cell" style="width: 70px;">城市</div>
        <div class="vs-cell" style="width: 140px;">创建时间</div>
      </div>

      <!-- 虚拟滚动容器 -->
      <div
        class="vs-container"
        ref="vsContainer"
        @scroll="onScroll"
        :style="{ height: containerHeight + 'px' }"
      >
        <!-- 撑高滚动条的占位 div -->
        <div class="vs-spacer" :style="{ height: totalHeight + 'px' }">
          <!-- 只渲染可见行 -->
          <div
            class="vs-row"
            v-for="item in visibleItems"
            :key="item.id"
            :style="{ transform: 'translateY(' + item._offset + 'px)', height: rowHeight + 'px' }"
          >
            <div class="vs-cell" style="width: 80px;">{{ item.id }}</div>
            <div class="vs-cell" style="width: 170px; font-size: 12px;">{{ item.orderNo }}</div>
            <div class="vs-cell" style="width: 80px;">{{ item.userName }}</div>
            <div class="vs-cell"  style="flex: 1;">{{ item.productName }}</div>
            <div class="vs-cell" style="width: 90px;">
              <el-tag size="small">{{ item.category }}</el-tag>
            </div>
            <div class="vs-cell" style="width: 90px; text-align: right;">
              ¥{{ Number(item.amount).toFixed(2) }}
            </div>
            <div class="vs-cell" style="width: 60px; text-align: center;">{{ item.quantity }}</div>
            <div class="vs-cell" style="width: 80px; text-align: center;">
              <el-tag :type="statusType(item.status)" size="small">{{ item.status }}</el-tag>
            </div>
            <div class="vs-cell" style="width: 70px;">{{ item.province }}</div>
            <div class="vs-cell" style="width: 70px;">{{ item.city }}</div>
            <div class="vs-cell" style="width: 140px;font-size: 12px;">{{ item.createTime }}</div>
          </div>
        </div>

        <!-- 底部加载指示器 -->
        <div v-if="loadingMore" class="vs-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore && loadedData.length > 0" class="vs-loading">
          <span>已加载全部 {{ formatNumber(loadedData.length) }} 条数据</span>
        </div>
      </div>
    </el-card>

    <!-- ========== 普通渲染表格 ========== -->
    <el-card v-if="mode === 'normal'" class="section" style="margin-top: 16px;">
      <template #header>
        <div class="card-header">
          <span>普通渲染表格</span>
          <el-tag size="small" type="warning">
            全部 {{ formatNumber(loadedData.length) }} 行都创建 DOM
          </el-tag>
        </div>
      </template>

      <div class="normal-table-wrapper" :style="{ height: containerHeight + 'px' }">
        <el-table
          :data="loadedData"
          border
          stripe
          style="width: 100%;"
          :max-height="containerHeight"
          v-loading="loadingMore && loadedData.length === 0"
        >
          <el-table-column prop="id" label="ID" width="80" sortable />
          <el-table-column prop="orderNo" label="订单号" width="240" />
          <el-table-column prop="userName" label="用户" width="80" />
          <el-table-column prop="productName" label="商品" />
          <el-table-column prop="category" label="分类" width="90">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="90" align="right">
            <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="60" align="center" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="province" label="省份" width="70" />
          <el-table-column prop="city" label="城市" width="70" />
          <el-table-column prop="createTime" label="创建时间" width="170" />
        </el-table>
      </div>

      <div style="margin-top: 12px; text-align: center;">
        <el-button
          v-if="hasMore"
          @click="loadMore"
          :loading="loadingMore"
          type="primary"
        >
          加载更多（已加载 {{ formatNumber(loadedData.length) }} 条）
        </el-button>
        <span v-else style="color: #909399;">
          已加载全部 {{ formatNumber(loadedData.length) }} 条
        </span>
      </div>
    </el-card>

    <!-- ========== 技术说明 ========== -->
    <el-card class="section" style="margin-top: 16px;">
      <template #header>
        <span>优化策略说明</span>
      </template>
      <el-collapse>
        <el-collapse-item title="1. 虚拟滚动原理" name="virtual">
          <div class="explain-content">
            <p><strong>问题：</strong>10 万行数据 = 10 万个 TR 节点 = 数千个 DOM 节点，浏览器渲染和重排都很慢</p>
            <p><strong>方案：</strong>只渲染可视区域内的 ~30 行，通过 CSS <code>transform: translateY</code> 定位到正确位置</p>
            <p><strong>关键计算：</strong></p>
            <p>· 总高度 = 数据总量 × 行高（撑出滚动条）</p>
            <p>· 可见起始行 = Math.floor(scrollTop / 行高)</p>
            <p>· 可见结束行 = 可见起始行 + 可见行数 + 缓冲区</p>
            <p>· 每行偏移 = 行索引 × 行高</p>
            <p><strong>效果：</strong>无论 1 万还是 100 万条数据，DOM 节点数恒定 ~60 个</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="2. 无限滚动加载" name="infinite">
          <div class="explain-content">
            <p><strong>原理：</strong>监听滚动事件，当滚动到底部附近时自动加载下一页</p>
            <p><strong>判断条件：</strong>scrollTop + clientHeight >= scrollHeight - 预加载距离</p>
            <p><strong>分页方式：</strong>使用游标分页，性能稳定</p>
            <p><strong>用户体验：</strong>无限滚动，不需要点击"下一页"</p>
          </div>
        </el-collapse-item>
        <el-collapse-item title="3. 性能优化技巧" name="tips">
          <div class="explain-content">
            <p><strong>requestAnimationFrame：</strong>用 rAF 包裹滚动处理，避免频繁重排</p>
            <p><strong>节流（Throttle）：</strong>滚动事件每 16ms 最多触发一次（60fps）</p>
            <p><strong>缓冲区：</strong>上下各多渲染 5 行，避免快速滚动时出现空白</p>
            <p><strong>固定行高：</strong>虚拟滚动要求每行高度一致，否则需要动态计算</p>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Refresh, Loading } from '@element-plus/icons-vue'
import { searchOrders } from '../api'

// ==================== 配置 ====================
const ROW_HEIGHT = 44        // 每行高度（px），与 CSS 保持一致
const CONTAINER_HEIGHT = 500 // 表格可视区域高度
const BUFFER_ROWS = 5        // 上下缓冲行数
const PAGE_SIZE = 200        // 每次加载条数
const LOAD_THRESHOLD = 200   // 距离底部多少 px 时触发加载

// ==================== 状态 ====================
const mode = ref('virtual') // 'virtual' | 'normal'
const loadedData = ref([])      // 所有已加载的数据
const hasMore = ref(true)       // 是否还有更多数据
const loadingMore = ref(false)  // 是否正在加载
const cursorId = ref(null)      // 游标分页的游标值
const scrollTop = ref(0)        // 当前滚动位置
const domCount = ref(0)         // DOM 节点数
const renderTime = ref(0)       // 渲染耗时

const vsContainer = ref(null)   // 虚拟滚动容器 ref
let ticking = false             // rAF 锁

// ==================== 计算属性 ====================

const containerHeight = CONTAINER_HEIGHT

/** 总高度（撑出滚动条） */
const totalHeight = computed(() => loadedData.value.length * ROW_HEIGHT)

/** 可见起始行索引 */
const startIndex = computed(() => {
  const idx = Math.floor(scrollTop.value / ROW_HEIGHT) - BUFFER_ROWS
  return Math.max(0, idx)
})

/** 可见结束行索引 */
const endIndex = computed(() => {
  const visibleCount = Math.ceil(CONTAINER_HEIGHT / ROW_HEIGHT)
  const idx = startIndex.value + visibleCount + BUFFER_ROWS * 2
  return Math.min(loadedData.value.length, idx)
})

/** 可见行数据（带偏移量） */
const visibleItems = computed(() => {
  const items = []
  for (let i = startIndex.value; i < endIndex.value; i++) {
    const item = loadedData.value[i]
    if (item) {
      items.push({
        ...item,
        _offset: i * ROW_HEIGHT  // 每行的绝对偏移
      })
    }
  }
  return items
})

/** 可见行数 */
const visibleCount = computed(() => endIndex.value - startIndex.value)

// ==================== 方法 ====================

function formatNumber(num) {
  if (num === null || num === undefined) return '-'
  return num.toLocaleString()
}

function statusType(status) {
  const map = {
    '待付款': 'warning', '已付款': '', '已发货': 'primary',
    '已完成': 'success', '已取消': 'info'
  }
  return map[status] || ''
}

/** 切换渲染模式 */
function switchMode(newMode) {
  const start = performance.now()
  mode.value = newMode
  nextTick(() => {
    renderTime.value = Math.round(performance.now() - start)
    updateDomCount()
  })
}

/** 重置数据 */
function resetData() {
  loadedData.value = []
  cursorId.value = null
  hasMore.value = true
  scrollTop.value = 0
  if (vsContainer.value) vsContainer.value.scrollTop = 0
  loadMore()
}

/** 加载下一页数据 */
async function loadMore() {
  if (loadingMore.value || !hasMore.value) return

  loadingMore.value = true
  const start = performance.now()

  try {
    const params = {
      pageType: 'cursor',
      pageSize: PAGE_SIZE,
      useCache: false
    }
    if (cursorId.value) {
      params.cursorId = cursorId.value
    }

    const res = await searchOrders(params)
    const data = res.data

    if (data.list && data.list.length > 0) {
      loadedData.value = [...loadedData.value, ...data.list]
      cursorId.value = data.nextCursorId
      hasMore.value = data.hasNext === true
    } else {
      hasMore.value = false
    }

    renderTime.value = Math.round(performance.now() - start)

    nextTick(updateDomCount)
  } catch (e) {
    console.error('加载数据失败:', e)
  } finally {
    loadingMore.value = false
  }
}

/** 滚动事件处理（用 rAF 节流） */
function onScroll(e) {
  if (!ticking) {
    ticking = true
    requestAnimationFrame(() => {
      scrollTop.value = e.target.scrollTop
      ticking = false

      // 检查是否需要加载更多
      const { scrollTop: st, clientHeight: ch, scrollHeight: sh } = e.target
      if (st + ch >= sh - LOAD_THRESHOLD) {
        loadMore()
      }
    })
  }
}

/** 更新 DOM 节点计数 */
function updateDomCount() {
  domCount.value = document.querySelectorAll('*').length
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadMore()
})

onBeforeUnmount(() => {
  ticking = false
})
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

/* ========== 虚拟滚动表头 ========== */
.vs-header {
  display: flex;
  align-items: center;
  height: 40px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-bottom: 2px solid #ebeef5;
  font-weight: 600;
  font-size: 13px;
  color: #909399;
  padding: 0 4px;
}

/* ========== 虚拟滚动容器 ========== */
.vs-container {
  position: relative;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-top: none;
}

.vs-spacer {
  position: relative;
}

/* ========== 虚拟滚动行 ========== */
.vs-row {
  position: absolute;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  font-size: 13px;
  color: #606266;
  padding: 0 4px;
  transition: background 0.1s;
}

.vs-row:hover {
  background: #f5f7fa;
}

.vs-row:nth-child(even) {
  background: #fafafa;
}

.vs-row:nth-child(even):hover {
  background: #f0f0f0;
}

/* ========== 单元格 ========== */
.vs-cell {
  padding: 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 44px;
}

/* ========== 加载指示器 ========== */
.vs-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: #909399;
  font-size: 13px;
}

/* ========== 普通渲染表格容器 ========== */
.normal-table-wrapper {
  overflow: auto;
}

/* ========== 说明区域 ========== */
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

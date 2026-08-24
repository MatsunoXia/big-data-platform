import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 300000  // 5分钟，数据生成可能耗时较长
})

// 响应拦截器：提取查询耗时
request.interceptors.response.use(
  response => {
    const queryTime = response.headers['x-query-time']
    if (queryTime) {
      response.queryTime = parseInt(queryTime)
    }
    return response
  },
  error => {
    return Promise.reject(error)
  }
)

// ==================== 数据管理 ====================

/** 获取数据统计 */
export function getDataStats() {
  return request.get('/data/stats')
}

/** 生成测试数据 */
export function generateData(params) {
  return request.post('/data/generate', null, { params })
}

// ==================== 检索模块 ====================

/**
 * 订单检索
 * @param {Object} params 查询参数
 * @param {string} params.pageType - 'offset' 或 'cursor'
 * @param {number} params.pageNum - 页码（offset 分页用）
 * @param {number} params.cursorId - 游标 ID（cursor 分页用）
 * @param {number} params.pageSize - 每页条数
 * @param {string} params.orderNo - 订单号
 * @param {string} params.status - 状态
 * @param {string} params.category - 分类
 * @param {boolean} params.useCache - 是否使用缓存
 */
export function searchOrders(params) {
  return request.get('/search/orders', { params })
}

/**
 * 性能对比：同时用两种分页方式查询
 */
export function comparePerformance(params) {
  return request.get('/search/compare', { params })
}

// ==================== 数据导入 ====================

/**
 * 上传 Excel 文件
 * @param {FormData} formData - 包含 file 字段的 FormData
 * @returns {Promise} - 返回 progressId
 */
export function uploadExcel(formData) {
  return request.post('/import/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000  // 上传超时 1 分钟
  })
}

/**
 * 查询导入进度
 * @param {string} progressId - 进度ID
 */
export function getImportProgress(progressId) {
  return request.get(`/import/progress/${progressId}`)
}

/** 下载 Excel 导入模板 */
export function downloadImportTemplate() {
  return request.get('/import/template', { responseType: 'blob' })
}

// ==================== 数据导出 ====================

/**
 * 启动异步导出
 * @param {Object} params - { status, category }
 */
export function startExport(params) {
  return request.post('/export/start', null, { params })
}

/**
 * 查询导出进度
 * @param {string} taskNo - 任务编号
 */
export function getExportProgress(taskNo) {
  return request.get(`/export/progress/${taskNo}`)
}

/**
 * 下载导出文件
 * @param {string} taskNo - 任务编号
 */
export function downloadExport(taskNo) {
  return request.get(`/export/download/${taskNo}`, { responseType: 'blob' })
}

/**
 * 获取最近的导出任务列表
 */
export function getExportTasks(limit = 10) {
  return request.get('/export/tasks', { params: { limit } })
}

export default request

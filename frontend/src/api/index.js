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

export default request

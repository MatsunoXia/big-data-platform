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
      response.queryTime = queryTime
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

export default request

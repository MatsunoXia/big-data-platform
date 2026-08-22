import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/data-manage',
    name: 'DataManage',
    component: () => import('../views/DataManage.vue')
  },
  {
    path: '/module/search',
    name: 'ModuleSearch',
    component: () => import('../views/ModuleSearch.vue')
  },
  {
    path: '/module/import',
    name: 'ModuleImport',
    component: () => import('../views/ModuleImport.vue')
  },
  {
    path: '/module/export',
    name: 'ModuleExport',
    component: () => import('../views/ModuleExport.vue')
  },
  {
    path: '/module/table',
    name: 'ModuleTable',
    component: () => import('../views/ModuleTable.vue')
  },
  {
    path: '/module/chart',
    name: 'ModuleChart',
    component: () => import('../views/ModuleChart.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

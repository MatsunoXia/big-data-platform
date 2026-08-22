<template>
  <el-container class="app-container">
    <!-- 顶部导航 -->
    <el-header class="app-header">
      <div class="logo">
        <el-icon><DataAnalysis /></el-icon>
        <span>大数据量优化演示平台</span>
      </div>
    </el-header>

    <el-container>
      <!-- 左侧菜单 -->
      <el-aside width="220px" class="app-aside">
        <el-menu
          :default-active="route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/">
            <el-icon><HomeFilled /></el-icon>
            <span>首页概览</span>
          </el-menu-item>

          <el-menu-item index="/data-manage">
            <el-icon><Coin /></el-icon>
            <span>数据管理</span>
          </el-menu-item>

          <el-sub-menu index="modules">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>优化模块</span>
            </template>
            <el-menu-item index="/module/search">
              <el-icon><Search /></el-icon>
              <span>数据检索</span>
            </el-menu-item>
            <el-menu-item index="/module/import">
              <el-icon><Upload /></el-icon>
              <span>数据导入</span>
            </el-menu-item>
            <el-menu-item index="/module/export">
              <el-icon><Download /></el-icon>
              <span>数据导出</span>
            </el-menu-item>
            <el-menu-item index="/module/table">
              <el-icon><List /></el-icon>
              <span>前端表格</span>
            </el-menu-item>
            <el-menu-item index="/module/chart">
              <el-icon><TrendCharts /></el-icon>
              <span>图表可视化</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

.app-container {
  height: 100vh;
}

.app-header {
  background-color: #304156;
  display: flex;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.logo .el-icon {
  font-size: 24px;
}

.app-aside {
  background-color: #304156;
  overflow-y: auto;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>

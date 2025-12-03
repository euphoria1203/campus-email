<template>
  <el-container class="main-layout">
    <!-- 左侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon :size="28" color="#409EFF"><Message /></el-icon>
        <span>Campus Mail</span>
      </div>

      <!-- 写邮件按钮 -->
      <div class="compose-btn">
        <el-button type="primary" size="large" @click="$router.push('/compose')">
          <el-icon><Edit /></el-icon>
          写邮件
        </el-button>
      </div>

      <!-- 文件夹列表 -->
      <el-menu
        :default-active="activeFolder"
        class="folder-menu"
        @select="handleFolderSelect"
      >
        <el-menu-item index="inbox">
          <el-icon><MessageBox /></el-icon>
          <span>收件箱</span>
          <el-badge v-if="unreadCount > 0" :value="unreadCount" class="folder-badge" />
        </el-menu-item>

        <el-menu-item index="sent">
          <el-icon><Promotion /></el-icon>
          <span>发件箱</span>
        </el-menu-item>

        <el-menu-item index="starred">
          <el-icon><StarFilled /></el-icon>
          <span>星标邮件</span>
        </el-menu-item>

        <el-menu-item index="drafts">
          <el-icon><Document /></el-icon>
          <span>草稿箱</span>
          <el-badge v-if="draftsCount > 0" :value="draftsCount" class="folder-badge" type="info" />
        </el-menu-item>

        <el-menu-item index="trash">
          <el-icon><Delete /></el-icon>
          <span>垃圾箱</span>
        </el-menu-item>

        <el-menu-item index="contacts">
          <el-icon><User /></el-icon>
          <span>联系人簿</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容区 -->
    <el-container class="right-container">
      <!-- 顶部导航栏 -->
      <el-header class="top-header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleDropdownCommand">
            <div class="user-profile">
              <el-avatar :size="32">{{ userInitial }}</el-avatar>
              <div class="user-details">
                <span class="user-name">{{ username }}</span>
                <span class="user-email">{{ currentAccountEmail }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <!-- 邮箱账号切换区域 -->
                <el-dropdown-item disabled class="section-title">
                  <span>切换邮箱账号</span>
                </el-dropdown-item>
                <el-dropdown-item 
                  v-for="account in mailAccounts" 
                  :key="account.id"
                  :command="'account-' + account.id"
                  :class="{ 'is-active': account.id === currentAccountId }"
                >
                  <div class="account-item">
                    <el-avatar :size="24" :style="{ background: getAccountColor(account.id) }">
                      {{ account.emailAddress.charAt(0).toUpperCase() }}
                    </el-avatar>
                    <span class="account-email">{{ account.emailAddress }}</span>
                    <el-tag v-if="account.isDefault" size="small" type="success">默认</el-tag>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="manage">
                  <el-icon><Setting /></el-icon>
                  管理邮箱账号
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Message, Edit, MessageBox, Promotion, Document, Delete, SwitchButton, ArrowDown, User, StarFilled, Setting
} from '@element-plus/icons-vue'
import { mailApi, mailAccountApi } from '@/api/mail'

const router = useRouter()
const route = useRoute()

// 邮件统计数据
const unreadCount = ref(0)
const draftsCount = ref(0)
const isRefreshing = ref(false)

// 邮箱账号相关
const mailAccounts = ref([])
const currentAccountId = ref(null)

// 账号颜色
const accountColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#00BCD4', '#9C27B0']
const getAccountColor = (accountId) => {
  const index = mailAccounts.value.findIndex(a => a.id === accountId)
  return accountColors[index % accountColors.length]
}

// 当前选中的邮箱地址
const currentAccountEmail = computed(() => {
  if (currentAccountId.value) {
    const account = mailAccounts.value.find(a => a.id === currentAccountId.value)
    return account?.emailAddress || userEmail.value
  }
  const defaultAccount = mailAccounts.value.find(a => a.isDefault)
  return defaultAccount?.emailAddress || userEmail.value
})

const activeFolder = computed(() => {
  return route.meta?.folder || 'inbox'
})

const pageTitle = computed(() => {
  return route.meta?.title || '邮件'
})

const username = computed(() => {
  return localStorage.getItem('username') || 'User'
})

const userEmail = computed(() => {
  return localStorage.getItem('email') || `${username.value}@campus.mail`
})

const userInitial = computed(() => {
  return username.value.charAt(0).toUpperCase()
})  

// 获取邮件统计信息
const fetchMailStats = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (userId) {
      const data = await mailApi.getStats(userId)
      unreadCount.value = data.unreadCount || 0
      draftsCount.value = data.draftsCount || 0
    }
  } catch (error) {
    console.error('获取邮件统计失败:', error)
  }
}

// 加载邮箱账号列表
const loadMailAccounts = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (userId) {
      const data = await mailAccountApi.listByUser(userId)
      mailAccounts.value = Array.isArray(data) ? data : []
      // 设置当前选中的账号（默认账号）
      const defaultAccount = mailAccounts.value.find(a => a.isDefault)
      if (defaultAccount && !currentAccountId.value) {
        currentAccountId.value = defaultAccount.id
      }
    }
  } catch (error) {
    console.error('获取邮箱账号失败:', error)
  }
}

// 处理下拉菜单命令
const handleDropdownCommand = (command) => {
  if (command === 'manage') {
    router.push('/accounts')
    return
  }
  if (command === 'logout') {
    handleLogout()
    return
  }
  // 处理账号切换
  if (command.startsWith('account-')) {
    const accountId = Number(command.replace('account-', ''))
    currentAccountId.value = accountId
    localStorage.setItem('currentAccountId', accountId)
    window.dispatchEvent(new CustomEvent('account-switch', { detail: { accountId } }))
  }
}

// 页面加载时获取统计信息
onMounted(() => {
  fetchMailStats()
  loadMailAccounts()
  // 恢复上次选中的账号
  const savedAccountId = localStorage.getItem('currentAccountId')
  if (savedAccountId) {
    currentAccountId.value = Number(savedAccountId)
  }
})

// 监听路由变化，刷新统计信息
watch(() => route.path, () => {
  fetchMailStats()
})

const handleFolderSelect = (folder) => {
  router.push(`/${folder}`)
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('email')
  localStorage.removeItem('nickname')
  localStorage.removeItem('currentAccountId')
  router.push('/login')
}

const goMailAccounts = () => {
  router.push('/accounts')
}

const handleGlobalRefresh = async () => {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    await fetchMailStats()
    window.dispatchEvent(new CustomEvent('mail-refresh'))
  } catch (error) {
    console.error('刷新失败:', error)
  } finally {
    isRefreshing.value = false
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    border-bottom: 1px solid #e4e7ed;

    span {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }

  .compose-btn {
    padding: 16px;

    .el-button {
      width: 100%;
    }
  }

  .folder-menu {
    flex: 1;
    border-right: none;

    .el-menu-item {
      display: flex;
      align-items: center;
      justify-content: flex-start;

      .folder-badge {
        margin-left: auto;
        margin-right: 10px;
        display:flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

.main-content {
  background: #f5f7fa;
  padding: 0;
}

.right-container {
  flex-direction: column;
}

.top-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;

  .header-left {
    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-profile {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 8px;
      transition: background-color 0.2s;

      &:hover {
        background-color: #f5f7fa;
      }

      .user-details {
        display: flex;
        flex-direction: column;
        align-items: flex-end;

        .user-name {
          font-size: 14px;
          font-weight: 500;
          color: #303133;
        }

        .user-email {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

// 下拉菜单样式
:deep(.section-title) {
  font-size: 12px;
  color: #909399;
  cursor: default;
}

:deep(.account-item) {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .account-email {
    flex: 1;
  }
}

:deep(.el-dropdown-menu__item.is-active) {
  background-color: #ecf5ff;
  color: #409EFF;
}
</style>

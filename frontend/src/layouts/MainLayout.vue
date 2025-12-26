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

        <el-menu-item index="scheduled">
          <el-icon><Clock /></el-icon>
          <span>定时发送</span>
        </el-menu-item>

        <el-menu-item index="trash">
          <el-icon><Delete /></el-icon>
          <span>垃圾箱</span>
        </el-menu-item>

        <el-divider style="margin: 10px 0;" />

        <el-menu-item index="statistics">
          <el-icon><DataAnalysis /></el-icon>
          <span>邮件统计</span>
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
              <el-dropdown-menu class="user-dropdown-menu">
                <!-- 邮箱账号切换区域 -->
                <el-dropdown-item disabled class="section-title">
                  <span>切换邮箱账号</span>
                </el-dropdown-item>
                <el-dropdown-item 
                  v-for="account in mailAccounts" 
                  :key="account.id"
                  :command="'account-' + account.id"
                  :class="{ 'is-active': account.id === currentAccountId }"
                  class="account-dropdown-item"
                >
                  <div class="account-item">
                    <el-avatar :size="28" :style="{ background: getAccountColor(account.id) }">
                      {{ account.emailAddress.charAt(0).toUpperCase() }}
                    </el-avatar>
                    <span class="account-email">{{ account.emailAddress }}</span>
                    <el-tag v-if="account.isDefault" size="small" type="success">默认</el-tag>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="manage" class="action-item">
                  <el-icon><Setting /></el-icon>
                  <span>管理邮箱账号</span>
                </el-dropdown-item>
                <el-dropdown-item command="logout" class="action-item">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
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
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { 
  Message, Edit, MessageBox, Promotion, Document, Delete, SwitchButton, ArrowDown, User, StarFilled, Setting, Clock, DataAnalysis
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

const scheduledQueueKey = 'scheduledSendQueue'
const scheduleCheckIntervalMs = 30000
const isCheckingScheduled = ref(false)
let scheduleTimer = null

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
      // 确保当前账号存在，必要时回落到默认账号
      const defaultAccount = mailAccounts.value.find(a => a.isDefault)
      const currentId = currentAccountId.value
      const currentExists = currentId
        ? mailAccounts.value.some(a => a.id === currentId)
        : false
      if (!currentExists) {
        if (defaultAccount) {
          currentAccountId.value = defaultAccount.id
          localStorage.setItem('currentAccountId', defaultAccount.id)
        } else {
          currentAccountId.value = null
          localStorage.removeItem('currentAccountId')
        }
      } else if (!currentAccountId.value && defaultAccount) {
        currentAccountId.value = defaultAccount.id
        localStorage.setItem('currentAccountId', defaultAccount.id)
      }
    }
  } catch (error) {
    console.error('加载邮箱账号失败:', error)
  }
}

const handleAccountSwitchEvent = (event) => {
  const accountId = event?.detail?.accountId
  if (accountId) {
    currentAccountId.value = Number(accountId)
    localStorage.setItem('currentAccountId', accountId)
  } else {
    currentAccountId.value = null
    localStorage.removeItem('currentAccountId')
  }
  loadMailAccounts()
}

const readScheduledQueue = () => {
  try {
    const raw = localStorage.getItem(scheduledQueueKey)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const writeScheduledQueue = (queue) => {
  if (!queue || queue.length === 0) {
    localStorage.removeItem(scheduledQueueKey)
    return
  }
  localStorage.setItem(scheduledQueueKey, JSON.stringify(queue))
}

const checkScheduledQueue = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId || isCheckingScheduled.value) {
    return
  }
  const queue = readScheduledQueue()
  if (queue.length === 0) {
    return
  }
  isCheckingScheduled.value = true
  try {
    const scheduledList = await mailApi.list(userId, 'scheduled')
    const scheduledIds = new Set(
      (Array.isArray(scheduledList) ? scheduledList : []).map(item => item.id)
    )
    const pending = []
    for (const item of queue) {
      if (!item?.id) {
        continue
      }
      if (scheduledIds.has(item.id)) {
        pending.push(item)
        continue
      }
      try {
        const detail = await mailApi.get(item.id)
        const mail = detail?.mail
        if (mail?.folder === 'sent') {
          const subject = mail.subject || item.subject || '(无主题)'
          ElMessage.success(`定时邮件已发送：${subject}`)
        }
      } catch (error) {
        const status = error?.response?.status
        if (status !== 404 && status !== 403) {
          pending.push(item)
        }
      }
    }
    writeScheduledQueue(pending)
  } finally {
    isCheckingScheduled.value = false
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
  const savedAccountId = localStorage.getItem('currentAccountId')
  if (savedAccountId) {
    currentAccountId.value = Number(savedAccountId)
  }
  fetchMailStats()
  loadMailAccounts()
  checkScheduledQueue()
  scheduleTimer = setInterval(checkScheduledQueue, scheduleCheckIntervalMs)
  window.addEventListener('account-switch', handleAccountSwitchEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('account-switch', handleAccountSwitchEvent)
  if (scheduleTimer) {
    clearInterval(scheduleTimer)
    scheduleTimer = null
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
  localStorage.removeItem(scheduledQueueKey)
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

// 下拉菜单样式优化
:deep(.user-dropdown-menu) {
  min-width: 280px;
  padding: 8px 0;

  .el-dropdown-menu__item {
    padding: 0;
    height: auto;
    line-height: normal;

    &.section-title {
      padding: 8px 16px;
      font-size: 12px;
      color: #909399;
      cursor: default;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    &.account-dropdown-item {
      padding: 10px 16px;
      
      &:hover {
        background-color: #f5f7fa;
      }

      &.is-active {
        background-color: #ecf5ff;
        
        .account-item .account-email {
          color: #409EFF;
          font-weight: 500;
        }
      }
    }

    &.action-item {
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 10px;

      .el-icon {
        font-size: 16px;
      }

      span {
        font-size: 14px;
      }

      &:hover {
        background-color: #f5f7fa;
        color: #409EFF;
      }
    }
  }

  .account-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    
    .account-email {
      flex: 1;
      font-size: 14px;
      color: #303133;
    }

    .el-tag {
      flex-shrink: 0;
    }
  }
}
</style>

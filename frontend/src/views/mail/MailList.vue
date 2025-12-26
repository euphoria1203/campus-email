<template>
  <div class="mail-list-container">
    <!-- 列表概览 -->
    <div class="list-summary">
      <div class="summary-left">
        <span class="summary-title">{{ folderTitle }}</span>
        <span class="summary-meta">
          （共 {{ filteredMails.length }} 封<span v-if="unreadCountInList > 0">，其中未读 {{ unreadCountInList }} 封</span>）
        </span>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button-group>
          <el-button :icon="Refresh" @click="handleRefresh" :loading="loading">刷新</el-button>
          <el-button
            :icon="Delete"
            @click="handleDelete"
            :disabled="selectedMails.length === 0"
          >{{ isTrashFolder ? '彻底删除' : '删除' }}</el-button>
          <el-button
            v-if="isTrashFolder"
            :icon="FolderOpened"
            @click="handleRestore"
            :disabled="selectedMails.length === 0"
          >移出垃圾箱</el-button>
        </el-button-group>
        
        <!-- 邮箱账号切换器 -->
        <el-select 
          v-model="selectedAccountId" 
          placeholder="全部邮箱" 
          clearable
          style="width: 200px; margin-left: 12px;"
          @change="handleAccountChange"
        >
          <el-option label="全部邮箱" :value="null">
            <div class="account-option">
              <el-icon><Message /></el-icon>
              <span>全部邮箱</span>
              <el-tag size="small" type="info">{{ mails.length }}</el-tag>
            </div>
          </el-option>
          <el-option 
            v-for="account in mailAccounts" 
            :key="account.id" 
            :label="account.emailAddress"
            :value="account.id"
          >
            <div class="account-option">
              <el-avatar :size="20" :style="{ background: getAccountColor(account.id) }">
                {{ account.emailAddress.charAt(0).toUpperCase() }}
              </el-avatar>
              <span>{{ account.displayName || account.emailAddress }}</span>
              <el-tag size="small" v-if="account.isDefault" type="success">默认</el-tag>
            </div>
          </el-option>
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索邮件"
          :prefix-icon="Search"
          style="width: 240px"
          clearable
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && mails.length === 0" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && filteredMails.length === 0" class="empty-container">
      <el-empty description="暂无邮件" />
    </div>

    <!-- 邮件列表 -->
    <div v-else class="mail-list">
      <div v-for="group in groupedPaginatedMails" :key="group.key" class="date-group">
        <div class="date-group-header">
          <span class="date-group-title">
            {{ group.label }}
            <span class="date-group-count">({{ group.items.length }}封)</span>
          </span>
          <span class="date-group-line"></span>
        </div>

        <div
          v-for="mail in group.items"
          :key="mail.id"
          class="mail-item"
          :class="{ unread: !mail.isRead, selected: selectedMails.includes(mail.id) }"
          @click="handleMailClick(mail)"
        >
          <el-checkbox
            :model-value="selectedMails.includes(mail.id)"
            @change="(val) => handleMailSelect(mail.id, val)"
            @click.stop
          />

          <el-icon
            class="star-icon"
            :class="{ starred: mail.isStarred }"
            @click.stop="toggleStar(mail)"
          >
            <StarFilled v-if="mail.isStarred" />
            <Star v-else />
          </el-icon>

          <div class="mail-sender">{{ formatSender(mail.fromAddress) }}</div>

          <!-- 收件邮箱标识 -->
          <el-tag
            v-if="mailAccounts.length > 1 && getAccountByMail(mail)"
            size="small"
            :color="getAccountColor(mail.accountId)"
            effect="dark"
            class="mail-account-tag"
          >
            {{ getAccountByMail(mail)?.emailAddress?.split('@')[0] }}
          </el-tag>

          <div class="mail-content">
            <span class="mail-subject" v-html="renderSubject(mail)"></span>
            <span class="mail-preview" v-html="renderPreview(mail)"></span>
          </div>

          <div class="mail-attachment" v-if="mail.hasAttachment">
            <el-icon><Paperclip /></el-icon>
          </div>

          <div class="mail-time">{{ formatTime(getMailDisplayTime(mail)) }}</div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination" v-if="filteredMails.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="filteredMails.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  Refresh, Delete, Search, Star, StarFilled, Paperclip, Loading, Message, FolderOpened
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mailApi, mailAccountApi } from '@/api/mail'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const selectedMails = ref([])
const selectAll = ref(false)
const loading = ref(false)
const mails = ref([])
const mailAccounts = ref([])
const selectedAccountId = ref(null)

// 账号颜色映射
const accountColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#00BCD4', '#9C27B0']
const getAccountColor = (accountId) => {
  const index = mailAccounts.value.findIndex(a => a.id === accountId)
  return accountColors[index % accountColors.length]
}

// 根据邮件获取对应的邮箱账号
const getAccountByMail = (mail) => {
  return mailAccounts.value.find(a => a.id === mail.accountId)
}

// 从 localStorage 获取用户ID
const userId = computed(() => {
  return localStorage.getItem('userId')
})

// 当前文件夹
const currentFolder = computed(() => {
  const path = route.path
  if (path.includes('/inbox')) return 'inbox'
  if (path.includes('/sent')) return 'sent'
  if (path.includes('/drafts')) return 'drafts'
  if (path.includes('/scheduled')) return 'scheduled'
  if (path.includes('/trash')) return 'trash'
  if (path.includes('/starred')) return 'starred'
  return 'inbox'
})

const isTrashFolder = computed(() => currentFolder.value === 'trash')

const folderTitle = computed(() => {
  switch (currentFolder.value) {
    case 'inbox':
      return '收件箱'
    case 'sent':
      return '发件箱'
    case 'drafts':
      return '草稿箱'
    case 'scheduled':
      return '定时发送'
    case 'trash':
      return '垃圾箱'
    case 'starred':
      return '星标邮件'
    default:
      return '邮件'
  }
})

// 根据搜索关键词过滤邮件
const filteredMails = computed(() => {
  let result = mails.value
  
  // 当未进行关键字搜索时按账号过滤
  if (selectedAccountId.value && !(searchKeyword.value && searchKeyword.value.trim())) {
    result = result.filter(m => m.accountId === selectedAccountId.value)
  }
  
  // 星标过滤
  if (currentFolder.value === 'starred') {
    result = result.filter(m => m.isStarred)
  }
  
  return result
})

const unreadCountInList = computed(() => {
  if (currentFolder.value !== 'inbox') {
    return 0
  }
  return filteredMails.value.reduce((count, mail) => count + (!mail.isRead ? 1 : 0), 0)
})

// 分页后的邮件列表
const paginatedMails = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredMails.value.slice(start, end)
})

const getMailDisplayTime = (mail) => {
  return mail?.receiveTime || mail?.sendTime || mail?.createdAt || mail?.updatedAt || null
}

const pad2 = (n) => String(n).padStart(2, '0')

const toDateKey = (date) => {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

const getGroupLabel = (date) => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  const weekAgo = new Date(today.getTime() - 6 * 24 * 60 * 60 * 1000)

  if (date >= today) return '今天'
  if (date >= yesterday) return '昨天'

  if (date >= weekAgo) {
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return weekdays[date.getDay()]
  }

  if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const groupedPaginatedMails = computed(() => {
  const groups = []
  const groupIndex = new Map()

  for (const mail of paginatedMails.value) {
    const timeValue = getMailDisplayTime(mail)
    const date = timeValue ? new Date(timeValue) : null
    const key = date && !Number.isNaN(date.getTime()) ? toDateKey(date) : 'unknown'
    const label = date && !Number.isNaN(date.getTime()) ? getGroupLabel(date) : '未知时间'

    if (!groupIndex.has(key)) {
      const group = { key, label, items: [] }
      groupIndex.set(key, group)
      groups.push(group)
    }
    groupIndex.get(key).items.push(mail)
  }

  return groups
})

// 分页事件处理
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1 // 切换每页条数时重置到第一页
}

const handlePageChange = (val) => {
  currentPage.value = val
}

const isIndeterminate = computed(() => {
  return selectedMails.value.length > 0 && selectedMails.value.length < filteredMails.value.length
})

// 获取邮件预览内容
const getPreview = (mail) => {
  const content = mail.plainContent || mail.content || ''
  // 移除HTML标签
  const text = content.replace(/<[^>]*>/g, '')
  return text.substring(0, 100)
}

// 基于关键词生成高亮展示内容（对原文做 HTML 转义后再插入 <mark>）
const escapeHtml = (text = '') =>
  text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')

const escapeRegex = (text = '') => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const highlightText = (text = '') => {
  const keyword = searchKeyword.value.trim()
  const safe = escapeHtml(text)
  if (!keyword) return safe
  const pattern = new RegExp(escapeRegex(keyword), 'gi')
  return safe.replace(pattern, (match) => `<mark>${match}</mark>`)
}

const renderSubject = (mail) => {
  const subject = mail?.subject || '(无主题)'
  return highlightText(subject)
}

const renderPreview = (mail) => {
  const preview = getPreview(mail)
  const highlighted = highlightText(preview)
  return ` - ${highlighted}`
}

// 格式化发件人显示（提取名称或邮箱）
const formatSender = (fromAddress) => {
  if (!fromAddress) return ''
  // 匹配 "名字 <email>" 格式
  const match = fromAddress.match(/^(.+?)\s*<(.+?)>$/)
  if (match) {
    return match[1].trim() // 返回名字部分
  }
  // 如果只是邮箱地址，返回 @ 前面的部分
  const atIndex = fromAddress.indexOf('@')
  if (atIndex > 0) {
    return fromAddress.substring(0, atIndex)
  }
  return fromAddress
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)
  
  if (date >= today) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (date >= yesterday) {
    return `昨天 ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
  } else if (date.getFullYear() === now.getFullYear()) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  } else {
    return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
  }
}

// 加载邮箱账号列表
const loadMailAccounts = async () => {
  const uid = localStorage.getItem('userId')
  if (!uid) return
  
  try {
    const response = await mailAccountApi.listByUser(uid)
    mailAccounts.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('加载邮箱账号失败:', error)
  }
}

// 处理账号切换
const handleAccountChange = () => {
  selectedMails.value = []
  currentPage.value = 1
  loadMails()
}

// 加载邮件列表
const loadMails = async () => {
  const uid = localStorage.getItem('userId')
  if (!uid) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  loading.value = true
  try {
    let response
    if (searchKeyword.value && searchKeyword.value.trim()) {
      response = await mailApi.search({
        keyword: searchKeyword.value.trim(),
        folder: currentFolder.value,
        accountId: selectedAccountId.value,
        page: 0,
        size: 200
      })
    } else {
      response = await mailApi.list(uid, currentFolder.value)
    }
    // 接口返回可能是数组本身
    mails.value = Array.isArray(response) ? response : []
  } catch (error) {
    console.error('加载邮件失败:', error)
    ElMessage.error('加载邮件失败')
  } finally {
    loading.value = false
  }
}

const handleSelectAll = (val) => {
  selectedMails.value = val ? filteredMails.value.map(m => m.id) : []
}

const handleMailSelect = (id, val) => {
  if (val) {
    selectedMails.value.push(id)
  } else {
    selectedMails.value = selectedMails.value.filter(i => i !== id)
  }
  selectAll.value = selectedMails.value.length === filteredMails.value.length
}

const handleMailClick = async (mail) => {
  // 标记为已读
  if (!mail.isRead) {
    try {
      await mailApi.markAsRead(mail.id)
      mail.isRead = true
    } catch (error) {
      console.error('标记已读失败:', error)
    }
  }
  router.push(`/mail/${mail.id}`)
}

const toggleStar = async (mail) => {
  try {
    await mailApi.toggleStar(mail.id)
    mail.isStarred = !mail.isStarred
  } catch (error) {
    console.error('切换星标失败:', error)
    ElMessage.error('操作失败')
  }
}

const handleRefresh = async () => {
  await loadMails()
}

const handleExternalRefresh = () => {
  handleRefresh()
}

const handleDelete = async () => {
  if (selectedMails.value.length === 0) return
  
  try {
    const actionText = isTrashFolder.value ? '彻底删除' : '删除'
    await ElMessageBox.confirm(
      `确定要${actionText}选中的 ${selectedMails.value.length} 封邮件吗？`,
      `确认${actionText}`,
      { type: 'warning' }
    )

    if (isTrashFolder.value) {
      await mailApi.deletePermanently(selectedMails.value)
      ElMessage.success('已彻底删除')
    } else {
      await mailApi.batchDelete(selectedMails.value)
      ElMessage.success('删除成功，已移至垃圾箱')
    }
    selectedMails.value = []
    loadMails()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleRestore = async () => {
  if (selectedMails.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selectedMails.value.length} 封邮件移出垃圾箱吗？`,
      '移出垃圾箱',
      { type: 'warning' }
    )
    await mailApi.restore(selectedMails.value)
    ElMessage.success('已移出垃圾箱')
    selectedMails.value = []
    loadMails()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移出垃圾箱失败:', error)
      ElMessage.error('移出失败')
    }
  }
}

// 监听路由变化，重新加载邮件
watch(() => route.path, () => {
  loadMails()
})

// 搜索关键字变化时重新拉取
watch(searchKeyword, () => {
  currentPage.value = 1
  loadMails()
})

onMounted(() => {
  loadMailAccounts()
  loadMails()
  window.addEventListener('mail-refresh', handleExternalRefresh)
})

onBeforeUnmount(() => {
  window.removeEventListener('mail-refresh', handleExternalRefresh)
})
</script>

<style lang="scss" scoped>
.mail-list-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.loading-container,
.empty-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  color: #909399;
}

.list-summary {
  padding: 10px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;

  .summary-left {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }

  .summary-title {
    font-size: 16px;
    font-weight: 700;
    color: #303133;
  }

  .summary-meta {
    font-size: 13px;
    color: #606266;
  }
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.mail-list {
  flex: 1;
  overflow-y: auto;
}

.date-group {
  .date-group-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px 6px;
    color: #303133;

    .date-group-title {
      font-size: 14px;
      font-weight: 600;
      color: #409eff;
      white-space: nowrap;

      .date-group-count {
        margin-left: 6px;
        font-weight: 500;
        color: #79bbff;
      }
    }

    .date-group-line {
      height: 1px;
      background: #d9ecff;
      flex: 1;
    }
  }
}

.mail-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background: #f5f7fa;
  }

  &.unread {
    background: #ecf5ff;
    
    .mail-sender, .mail-subject {
      font-weight: 600;
    }
  }

  &.selected {
    background: #e6f7ff;
  }

  .star-icon {
    margin: 0 12px;
    font-size: 16px;
    color: #c0c4cc;
    cursor: pointer;

    &.starred {
      color: #f7ba2a;
    }
  }

  .mail-sender {
    width: 160px;
    flex-shrink: 0;
    font-size: 14px;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mail-account-tag {
    flex-shrink: 0;
    margin-right: 8px;
    border: none;
    font-size: 11px;
  }

  .mail-content {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin: 0 12px;

    .mail-subject {
      font-size: 14px;
      color: #303133;
    }

    .mail-preview {
      font-size: 14px;
      color: #909399;
    }
  }

  .mail-attachment {
    margin-right: 12px;
    color: #909399;
  }

  .mail-time {
    width: 80px;
    text-align: right;
    font-size: 12px;
    color: #909399;
    flex-shrink: 0;
  }
}

.pagination {
  padding: 16px;
  display: flex;
  justify-content: center;
  border-top: 1px solid #e4e7ed;
}

.account-option {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .el-tag {
    margin-left: auto;
  }
}
</style>

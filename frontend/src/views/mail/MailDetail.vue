<template>
  <div class="mail-detail-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="mail">
      <!-- 顶部工具栏 -->
      <div class="detail-toolbar">
        <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
        <el-button-group>
          <el-button :icon="Delete" @click="handleDelete">删除</el-button>
            <el-button :icon="Promotion" type="primary" @click="handleForward">转发</el-button>
        </el-button-group>
        <div class="nav-buttons">
          <el-button :icon="ArrowUp" circle />
          <el-button :icon="ArrowDown" circle />
        </div>
      </div>

      <!-- 邮件内容 -->
      <div class="mail-content">
        <!-- 邮件头部 -->
        <div class="mail-header">
          <h1 class="mail-subject">{{ mail.subject || '(无主题)' }}</h1>
          
          <div class="mail-meta">
            <el-avatar :size="48">{{ getAvatarText(mail.fromAddress) }}</el-avatar>
            <div class="meta-info">
              <div class="sender-line">
                <span class="sender-name">{{ getSenderName(mail.fromAddress) }}</span>
                <span class="sender-email">&lt;{{ getSenderEmail(mail.fromAddress) }}&gt;</span>
                <template v-if="!isContact">
                  <el-button 
                    type="primary" 
                    link 
                    :icon="Plus" 
                    size="small" 
                    @click="addToContacts"
                    style="margin-left: 12px;"
                  >添加联系人</el-button>
                </template>
                <template v-else>
                  <el-tag size="small" type="info" style="margin-left: 12px;">已在通讯录</el-tag>
                </template>
              </div>
              <div class="recipient-line">
                <span>收件人：{{ mail.toAddress }}</span>
                <span class="mail-time">{{ formatDateTime(mail.receiveTime || mail.sendTime) }}</span>
              </div>
            </div>
            <el-button 
              :icon="mail.isStarred ? StarFilled : Star" 
              :class="{ starred: mail.isStarred }"
              circle 
              @click="toggleStar"
            />
          </div>
        </div>

        <!-- 邮件正文 -->
        <div class="mail-body" v-html="mail.content || mail.plainContent"></div>

        <!-- 附件区域 -->
        <div class="mail-attachments" v-if="attachments && attachments.length > 0">
          <div class="attachments-header">
            <el-icon><Paperclip /></el-icon>
            <span>附件 ({{ attachments.length }})</span>
          </div>
          <div class="attachment-list">
            <div class="attachment-item" v-for="att in attachments" :key="att.id">
              <el-icon :size="32" color="#409EFF"><Document /></el-icon>
              <div class="attachment-info">
                <span class="attachment-name">{{ att.fileName }}</span>
                <span class="attachment-size">{{ formatFileSize(att.fileSize) }}</span>
              </div>
              <el-button type="primary" link :icon="Download" @click="downloadAttachment(att)">下载</el-button>
            </div>
          </div>
        </div>

        <!-- 草稿操作 -->
        <div class="draft-actions" v-if="isDraft">
          <el-alert
            type="info"
            :closable="false"
            title="这是草稿，可以继续编辑或直接发送"
          />
          <div class="draft-buttons">
            <el-button
              :icon="Edit"
              @click="handleEditDraft"
            >编辑草稿</el-button>
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="sendingDraft"
              @click="handleSendDraft"
            >发送草稿</el-button>
          </div>
        </div>

        <!-- 快速回复（仅收件箱显示） -->
        <div class="quick-reply" v-if="isInbox">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            placeholder="快速回复..."
          />
          <div class="reply-actions">
            <el-checkbox v-model="includeOriginal" style="margin-right: 12px">包含原邮件</el-checkbox>
            <el-button type="primary" :icon="Promotion" @click="handleReply" :loading="sending">发送</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 邮件不存在 -->
    <div v-else class="empty-container">
      <el-empty description="邮件不存在或已被删除" />
      <el-button type="primary" @click="$router.back()">返回</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  ArrowLeft, Delete, ArrowUp, ArrowDown,
  Star, StarFilled, Paperclip, Document, Download, Promotion, Loading, Plus, Edit
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mailApi, attachmentApi, contactApi } from '@/api/mail'

const route = useRoute()
const router = useRouter()
const mailId = route.params.id

const loading = ref(true)
const sending = ref(false)
const sendingDraft = ref(false)
const mail = ref(null)
const attachments = ref([])
const isContact = ref(false)
const replyContent = ref('')
const includeOriginal = ref(true)

const isInbox = computed(() => mail.value?.folder === 'inbox')
const isDraft = computed(() => mail.value?.folder === 'drafts')

// 获取头像文字（从发件人名称首字母）
const getAvatarText = (fromAddress) => {
  if (!fromAddress) return '?'
  // 先尝试从 "名字 <email>" 格式提取名字
  const match = fromAddress.match(/^(.+?)\s*<(.+?)>$/)
  if (match) {
    return match[1].trim().charAt(0).toUpperCase()
  }
  // 否则用邮箱 @ 前的首字母
  const atIndex = fromAddress.indexOf('@')
  const name = atIndex > 0 ? fromAddress.substring(0, atIndex) : fromAddress
  return name.charAt(0).toUpperCase()
}

// 获取发件人名称（从 "名字 <email>" 格式中提取名字）
const getSenderName = (fromAddress) => {
  if (!fromAddress) return '未知'
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

// 获取发件人邮箱地址（从 "名字 <email>" 格式中提取邮箱）
const getSenderEmail = (fromAddress) => {
  if (!fromAddress) return ''
  // 匹配 "名字 <email>" 格式
  const match = fromAddress.match(/^(.+?)\s*<(.+?)>$/)
  if (match) {
    return match[2].trim() // 返回邮箱部分
  }
  return fromAddress
}

// 检查联系人状态
const checkContactStatus = async () => {
  if (!mail.value || !mail.value.fromAddress) return
  const email = getSenderEmail(mail.value.fromAddress)
  if (!email) return
  
  try {
    const userId = localStorage.getItem('userId')
    const res = await contactApi.search(userId, email)
    if (res && res.length > 0) {
      // 检查是否有完全匹配的邮箱
      isContact.value = res.some(c => c.email === email)
    } else {
      isContact.value = false
    }
  } catch (error) {
    console.error('检查联系人状态失败:', error)
  }
}

// 添加到通讯录
const addToContacts = async () => {
  if (!mail.value || !mail.value.fromAddress) return
  
  const name = getSenderName(mail.value.fromAddress)
  const email = getSenderEmail(mail.value.fromAddress)
  
  try {
    await contactApi.create({
      contactName: name,
      email: email,
      userId: localStorage.getItem('userId')
    })
    ElMessage.success(`已将 ${name} 添加到通讯录`)
    isContact.value = true
  } catch (error) {
    console.error('添加联系人失败:', error)
    ElMessage.error('添加联系人失败，可能已存在')
  }
}

// 格式化日期时间
const formatDateTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  while (bytes >= 1024 && i < units.length - 1) {
    bytes /= 1024
    i++
  }
  return `${bytes.toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

// 去除 HTML 标签，生成纯文本
const stripHtml = (text = '') => text.replace(/<[^>]*>/g, '')

// 加载邮件详情
const loadMailDetail = async () => {
  loading.value = true
  try {
    const response = await mailApi.get(mailId)
    // response 已经是拦截器返回的 data
    mail.value = response.mail
    attachments.value = response.attachments || []
    await checkContactStatus()
  } catch (error) {
    console.error('加载邮件详情失败:', error)
    ElMessage.error('加载邮件详情失败')
  } finally {
    loading.value = false
  }
}

// 切换星标
const toggleStar = async () => {
  try {
    await mailApi.toggleStar(mailId)
    mail.value.isStarred = !mail.value.isStarred
  } catch (error) {
    console.error('切换星标失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除邮件
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这封邮件吗？', '确认删除', { type: 'warning' })
    await mailApi.delete(mailId)
    ElMessage.success('删除成功')
    router.back()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 转发
const handleForward = () => {
  router.push({
    path: '/compose',
    query: { forwardId: mailId }
  })
}

// 下载附件
const downloadAttachment = async (att) => {
  try {
    const response = await attachmentApi.download(att.id)
    const blob = response instanceof Blob ? response : response?.data

    if (!blob) {
      throw new Error('未取得附件内容')
    }

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = att.fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
    ElMessage.error('下载失败')
  }
}

// 发送回复（带上原文）
const handleReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  let finalHtml = replyContent.value.replace(/\n/g, '<br/>')
  let finalPlain = replyContent.value

  if (includeOriginal.value) {
    const originalHtml = mail.value?.content || mail.value?.plainContent || ''
    const originalPlain = stripHtml(mail.value?.plainContent || mail.value?.content || '')
    
    const dateStr = formatDateTime(mail.value.receiveTime || mail.value.sendTime)
    const sender = `${getSenderName(mail.value.fromAddress)} <${getSenderEmail(mail.value.fromAddress)}>`
    
    const headerHtml = `
      <br/><br/>
      <div style="background-color: #f6f8fa; padding: 12px; border-left: 4px solid #409eff; margin: 20px 0; font-size: 13px; color: #606266;">
        <div style="font-weight: bold; margin-bottom: 8px; color: #303133;">------------------ 原始邮件 ------------------</div>
        <div style="margin-bottom: 4px;"><b>发件人:</b> ${sender}</div>
        <div style="margin-bottom: 4px;"><b>发送时间:</b> ${dateStr}</div>
        <div style="margin-bottom: 4px;"><b>收件人:</b> ${mail.value.toAddress}</div>
        <div><b>主题:</b> ${mail.value.subject}</div>
      </div>
      <div>${originalHtml}</div>
    `
    
    const headerPlain = `\n\n------------------ 原始邮件 ------------------\n发件人: ${sender}\n发送时间: ${dateStr}\n主题: ${mail.value.subject}\n\n`
    
    finalHtml += headerHtml
    finalPlain += headerPlain + originalPlain
  }

  sending.value = true
  try {
    const userId = localStorage.getItem('userId')
    await mailApi.send({
      userId: userId,
      toAddress: mail.value.fromAddress,
      subject: `Re: ${mail.value.subject || '(无主题)'}`,
      content: finalHtml,
      plainContent: finalPlain
    })
    ElMessage.success('回复发送成功')
    replyContent.value = ''
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复发送失败')
  } finally {
    sending.value = false
  }
}

const handleEditDraft = () => {
  if (!mail.value) return
  // 跳转到写邮件页面并传递草稿ID
  router.push({
    path: '/compose',
    query: { draftId: mail.value.id }
  })
}

const handleSendDraft = async () => {
  if (!mail.value) return

  if (!mail.value.toAddress || !mail.value.toAddress.trim()) {
    ElMessage.warning('草稿缺少收件人，无法发送')
    return
  }

  sendingDraft.value = true
  try {
    await mailApi.sendDraft(mail.value.id)
    ElMessage.success('草稿发送成功')
    mail.value.folder = 'sent'
    router.push('/sent')
  } catch (error) {
    console.error('草稿发送失败:', error)
    ElMessage.error(error.response?.data?.message || '草稿发送失败')
  } finally {
    sendingDraft.value = false
  }
}

onMounted(() => {
  loadMailDetail()
})
</script>

<style lang="scss" scoped>
.mail-detail-container {
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

.detail-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;

  .nav-buttons {
    margin-left: auto;
  }
}

.mail-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.mail-header {
  margin-bottom: 24px;

  .mail-subject {
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 16px 0;
  }

  .mail-meta {
    display: flex;
    align-items: center;
    gap: 16px;

    .starred {
      color: #f7ba2a;
    }

    .meta-info {
      flex: 1;

      .sender-line {
        margin-bottom: 4px;

        .sender-name {
          font-size: 16px;
          font-weight: 500;
          color: #303133;
          margin-right: 8px;
        }

        .sender-email {
          font-size: 14px;
          color: #909399;
        }
      }

      .recipient-line {
        font-size: 13px;
        color: #909399;
        display: flex;
        gap: 16px;
      }
    }
  }
}

.mail-body {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  padding: 16px 0;
  border-bottom: 1px solid #e4e7ed;

  :deep(p) {
    margin: 8px 0;
  }

  :deep(ol), :deep(ul) {
    margin: 12px 0;
    padding-left: 24px;

    li {
      margin: 8px 0;
    }
  }
}

.mail-attachments {
  padding: 16px 0;
  border-bottom: 1px solid #e4e7ed;

  .attachments-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #606266;
    margin-bottom: 12px;
  }

  .attachment-list {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
  }

  .attachment-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    border: 1px solid #e4e7ed;
    border-radius: 8px;
    min-width: 280px;

    .attachment-info {
      flex: 1;

      .attachment-name {
        display: block;
        font-size: 14px;
        color: #303133;
      }

      .attachment-size {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.draft-actions {
  margin-top: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e4e7ed;

  .draft-buttons {
    margin-top: 12px;
    display: flex;
    gap: 12px;
  }
}

.quick-reply {
  padding-top: 16px;

  .reply-actions {
    display: flex;
    justify-content: flex-end;
    margin-top: 12px;
  }
}
</style>

<template>
  <div class="compose-container">
    <!-- 顶部工具栏 -->
    <div class="compose-toolbar">
      <el-button @click="handleCancel">取消</el-button>
      <div class="toolbar-actions">
        <el-button :icon="Document" @click="saveDraft" :loading="saving">
          存草稿
        </el-button>
        <el-button :icon="Clock" @click="openScheduleDialog" :loading="scheduling">
          定时发送
        </el-button>
        <el-button type="primary" :icon="Promotion" @click="sendMail" :loading="sending">
          发送
        </el-button>
      </div>
    </div>

    <!-- 邮件编辑区域 -->
    <el-dialog v-model="scheduleDialogVisible" title="定时发送" width="420px">
      <el-form label-width="90px">
        <el-form-item label="发送时间">
          <el-date-picker
            v-model="scheduledTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="disablePastDate"
            :disabled-time="disablePastTime"
            placeholder="请选择发送时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scheduling" @click="scheduleMail">确定</el-button>
      </template>
    </el-dialog>

    <div class="compose-form">
        <!-- 发件邮箱（从邮箱账号列表中选择，可选） -->
        <div class="form-row">
          <label>发件人</label>
          <div class="input-wrapper">
            <el-select v-model="selectedAccountId" placeholder="默认发件邮箱" style="width: 260px">
              <el-option
                v-for="item in mailAccounts"
                :key="item.id"
                :label="item.displayName ? `${item.displayName} <${item.emailAddress}>` : item.emailAddress"
                :value="item.id"
              />
            </el-select>
          </div>
        </div>

      <!-- 收件人 -->
      <div class="form-row">
        <label>收件人</label>
        <div class="input-wrapper">
          <ContactSelect v-model="mail.to" />
          <el-button link size="small" @click="toggleCcBcc">
            {{ showCcBcc ? '隐藏' : '抄送/密送' }}
          </el-button>
        </div>
      </div>

      <!-- 抄送 -->
      <div class="form-row" v-if="showCcBcc">
        <label>抄送</label>
        <ContactSelect v-model="mail.cc" />
      </div>

      <!-- 密送 -->
      <div class="form-row" v-if="showCcBcc">
        <label>密送</label>
        <ContactSelect v-model="mail.bcc" />
      </div>

      <!-- 主题 -->
      <div class="form-row">
        <label>主题</label>
        <el-input 
          v-model="mail.subject" 
          placeholder="请输入邮件主题"
          maxlength="200"
          show-word-limit
        />
      </div>

      <!-- 附件上传 -->
      <div class="form-row attachments-row">
        <AttachmentUpload v-model="attachments" ref="attachmentRef" />
      </div>

      <!-- 富文本编辑器 -->
      <div class="editor-wrapper">
        <RichEditor 
          v-model="mail.body" 
          ref="editorRef"
          placeholder="请输入邮件正文..."
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Promotion, Clock } from '@element-plus/icons-vue'
import RichEditor from '@/components/RichEditor.vue'
import ContactSelect from '@/components/ContactSelect.vue'
import AttachmentUpload from '@/components/AttachmentUpload.vue'
import { mailApi, mailAccountApi } from '@/api/mail'

const router = useRouter()
const route = useRoute()

const editorRef = ref(null)
const attachmentRef = ref(null)
const showCcBcc = ref(false)
const sending = ref(false)
const scheduling = ref(false)
const scheduleDialogVisible = ref(false)
const scheduledTime = ref('')

const disablePastDate = (time) => {
  const todayStart = new Date()
  todayStart.setHours(0, 0, 0, 0)
  return time.getTime() < todayStart.getTime()
}

const disablePastTime = (date) => {
  if (!date) {
    return {}
  }

  const now = new Date()
  const selected = new Date(date)
  const isToday =
    selected.getFullYear() === now.getFullYear() &&
    selected.getMonth() === now.getMonth() &&
    selected.getDate() === now.getDate()

  if (!isToday) {
    return {}
  }

  const range = (end) => Array.from({ length: end }, (_, i) => i)
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()
  const currentSecond = now.getSeconds()

  return {
    disabledHours: () => range(currentHour),
    disabledMinutes: (hour) => (hour === currentHour ? range(currentMinute) : []),
    disabledSeconds: (hour, minute) =>
      hour === currentHour && minute === currentMinute ? range(currentSecond + 1) : []
  }
}
const saving = ref(false)
const draftId = ref(null) // 正在编辑的草稿ID
const forwardId = ref(null) // 正在转发的邮件ID

// 发件邮箱账号列表与当前选择
const mailAccounts = ref([])
const selectedAccountId = ref(null)

const mail = reactive({
  to: [],
  cc: [],
  bcc: [],
  subject: '',
  body: ''
})

const attachments = ref([])

const hasAnyRecipient = () => {
  return mail.to.length > 0 || mail.cc.length > 0 || mail.bcc.length > 0
}

const formatRecipientField = (list) => {
  return list.length > 0 ? list.join(',') : null
}

const getPayloadAccountId = () => {
  const raw = selectedAccountId.value
  if (raw === null || raw === undefined || raw === '') {
    return null
  }
  // 不转 Number，避免超出 JS 安全整数导致 Snowflake ID 精度丢失
  return raw
}

// 确保附件已全部上传完成
const ensureAttachmentsReady = () => {
  const uploading = attachments.value.some(a => a.status === 'uploading')
  if (uploading) {
    ElMessage.warning('附件正在上传，请稍后再发送')
    return false
  }
  const missingId = attachments.value.some(a => !a.id && a.status !== 'fail')
  if (missingId) {
    ElMessage.warning('附件尚未上传完成，请稍后重试')
    return false
  }
  return true
}

const buildMailData = () => {
  const userId = localStorage.getItem('userId')
  const attachmentIds = attachments.value
    .filter(a => a.id)
    .map(a => a.id)

  return {
    userId: userId,
    accountId: getPayloadAccountId(),
    toAddress: formatRecipientField(mail.to),
    ccAddress: formatRecipientField(mail.cc),
    bccAddress: formatRecipientField(mail.bcc),
    subject: mail.subject || '(无主题)',
    content: mail.body,
    plainContent: mail.body ? mail.body.replace(/<[^>]*>/g, '') : '',
    attachmentIds: attachmentIds,
    id: draftId.value
  }
}

const ensureReadyToSend = async () => {
  if (!hasAnyRecipient()) {
    ElMessage.warning('请至少填写收件人/抄送/密送')
    return false
  }
  if (!ensureAttachmentsReady()) {
    return false
  }
  if (!mail.subject.trim()) {
    try {
      await ElMessageBox.confirm('邮件主题为空，是否继续发送？', '提示', {
        confirmButtonText: '继续发送',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return false
    }
  }
  return true
}

// 切换抄送/密送显示
const toggleCcBcc = () => {
  showCcBcc.value = !showCcBcc.value
}

// 发送邮件
const sendMail = async () => {
  if (!(await ensureReadyToSend())) {
    return
  }

  sending.value = true
  
  try {
    const mailData = buildMailData()
    await mailApi.send(mailData)

    ElMessage.success('发送成功')
    router.push('/sent')
  } catch (error) {
    console.error('发送失败', error)
    ElMessage.error(error.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const scheduledQueueKey = 'scheduledSendQueue'

const addScheduledQueueItem = (mail) => {
  if (!mail?.id) {
    return
  }
  let queue = []
  try {
    const raw = localStorage.getItem(scheduledQueueKey)
    queue = raw ? JSON.parse(raw) : []
  } catch {
    queue = []
  }
  if (!Array.isArray(queue)) {
    queue = []
  }
  if (!queue.some(item => item?.id === mail.id)) {
    queue.push({
      id: mail.id,
      subject: mail.subject || '(无主题)'
    })
    localStorage.setItem(scheduledQueueKey, JSON.stringify(queue))
  }
}

const scheduleMail = async () => {
  if (!(await ensureReadyToSend())) {
    return
  }
  if (!scheduledTime.value) {
    ElMessage.warning('请选择发送时间')
    return
  }
  const parsed = Date.parse(scheduledTime.value)
  if (Number.isNaN(parsed)) {
    ElMessage.warning('发送时间格式不正确')
    return
  }
  if (parsed <= Date.now()) {
    ElMessage.warning('发送时间必须晚于当前时间')
    return
  }

  scheduling.value = true
  try {
    const mailData = buildMailData()
    mailData.scheduledTime = scheduledTime.value
    const response = await mailApi.schedule(mailData)
    addScheduledQueueItem(response)
    ElMessage.success('定时发送已创建')
    scheduleDialogVisible.value = false
    router.push('/scheduled')
  } catch (error) {
    console.error('定时发送失败', error)
    ElMessage.error(error.response?.data?.message || '定时发送失败')
  } finally {
    scheduling.value = false
  }
}

const openScheduleDialog = () => {
  scheduleDialogVisible.value = true
}

const saveDraft = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    ElMessage.error('请先登录后再保存草稿')
    return
  }
  if (!ensureAttachmentsReady()) {
    return
  }

  saving.value = true

  try {
    const attachmentIds = attachments.value
      .filter(a => a.id)
      .map(a => a.id)

    const mailData = {
      userId,
      accountId: getPayloadAccountId(),
      toAddress: formatRecipientField(mail.to),
      ccAddress: formatRecipientField(mail.cc),
      bccAddress: formatRecipientField(mail.bcc),
      subject: mail.subject,
      content: mail.body,
      plainContent: mail.body ? mail.body.replace(/<[^>]*>/g, '') : '',
      attachmentIds
    }

    // 如果是编辑草稿，附加草稿ID
    if (draftId.value) {
      mailData.id = draftId.value
    }

    await mailApi.saveDraft(mailData)

    // 同步一份本地备份，避免意外关闭浏览器导致内容丢失
    localStorage.setItem('mail_draft', JSON.stringify({
      ...mail,
      savedAt: new Date().toISOString()
    }))

    ElMessage.success(draftId.value ? '草稿已更新' : '草稿已保存到服务器')
    router.push('/drafts')
  } catch (error) {
    console.error('保存草稿失败', error)
    ElMessage.error(error.response?.data?.message || '保存草稿失败，请重试')
  } finally {
    saving.value = false
  }
}

// 取消编辑
const handleCancel = async () => {
  // 检查是否有内容
  const hasContent = mail.to.length > 0 || 
                     mail.subject.trim() || 
                     mail.body.trim() || 
                     attachments.value.length > 0

  if (hasContent) {
    try {
      await ElMessageBox.confirm('确定要放弃编辑吗？未保存的内容将丢失。', '提示', {
        confirmButtonText: '放弃',
        cancelButtonText: '继续编辑',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  router.back()
}

// 自动保存草稿
let autoSaveTimer = null
const startAutoSave = () => {
  autoSaveTimer = setInterval(() => {
    if (mail.to.length > 0 || mail.subject || mail.body) {
      localStorage.setItem('mail_draft_auto', JSON.stringify({
        ...mail,
        attachmentNames: attachments.value.map(f => f.name),
        savedAt: new Date().toISOString()
      }))
    }
  }, 30000) // 每30秒自动保存
}

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
  }
})

// 启动自动保存
startAutoSave()

  // 加载当前用户的邮箱账号列表，用于发件人选择
  const loadMailAccounts = async () => {
    const userId = localStorage.getItem('userId')
    const fallbackEmail = localStorage.getItem('email') || ''
    const fallbackName = localStorage.getItem('username') || '默认账号'

    let list = []

    if (userId) {
      try {
        const remoteList = await mailAccountApi.listByUser(userId)
        list = remoteList || []
      } catch (e) {
        console.error('加载邮箱账号失败', e)
      }
    }

    // 如果接口没有返回任何账号，使用当前登录邮箱作为兜底账号
    if (!list.length && fallbackEmail) {
      list = [{
        id: 'fallback-account',
        emailAddress: fallbackEmail,
        displayName: fallbackName,
        isDefault: true,
        isFallback: true
      }]
    }

    mailAccounts.value = list

    const defaultOne = mailAccounts.value.find(a => a.isDefault)
    if (defaultOne) {
      selectedAccountId.value = defaultOne.id
    } else if (mailAccounts.value.length > 0) {
      selectedAccountId.value = mailAccounts.value[0].id
    } else {
      selectedAccountId.value = null
    }
  }

  // 加载草稿内容
  const loadDraft = async (id) => {
    try {
      const response = await mailApi.get(id)
      const draft = response.mail
      
      // 填充表单
      mail.to = draft.toAddress ? draft.toAddress.split(/[,;]/).map(s => s.trim()).filter(Boolean) : []
      mail.cc = draft.ccAddress ? draft.ccAddress.split(/[,;]/).map(s => s.trim()).filter(Boolean) : []
      mail.bcc = draft.bccAddress ? draft.bccAddress.split(/[,;]/).map(s => s.trim()).filter(Boolean) : []
      mail.subject = draft.subject || ''
      mail.body = draft.content || draft.plainContent || ''
      
      // 如果有抄送或密送，显示这些字段
      if (mail.cc.length > 0 || mail.bcc.length > 0) {
        showCcBcc.value = true
      }
      
      // 设置发件账号
      if (draft.accountId) {
        selectedAccountId.value = draft.accountId
      }
      
      // 加载附件列表
      if (response.attachments && response.attachments.length > 0) {
        // 将附件信息转换为前端格式（保留附件ID以便关联）
        attachments.value = response.attachments.map(att => ({
          id: att.id,
          name: att.fileName,
          size: att.fileSize,
          status: 'success',
          // 标记为已存在的附件，避免重复上传
          existing: true
        }))
      }
      
      draftId.value = id
      ElMessage.success('草稿已加载')
    } catch (error) {
      console.error('加载草稿失败:', error)
      ElMessage.error('加载草稿失败')
    }
  }

  // 加载转发内容
  const loadForward = async (id) => {
    try {
      const response = await mailApi.get(id)
      const original = response.mail

      // 清空收件人，用户自行填写
      mail.to = []
      mail.cc = []
      mail.bcc = []

      // 主题加前缀
      const origSubject = original.subject || '(无主题)'
      mail.subject = origSubject.startsWith('转发:') ? origSubject : `转发: ${origSubject}`

      // 构造转发正文，附带原邮件信息
      const formatDateForQuote = (time) => {
        if (!time) return ''
        return new Date(time).toLocaleString('zh-CN', { hour12: false })
      }

      const meta = [
        `发件人: ${original.fromAddress || ''}`,
        `发送时间: ${formatDateForQuote(original.sendTime || original.receiveTime)}`,
        `收件人: ${original.toAddress || ''}`,
        original.ccAddress ? `抄送: ${original.ccAddress}` : null
      ].filter(Boolean).join('<br/>')

      const originalBody = original.content || original.plainContent || ''

      mail.body = `
        <p></p>
        <hr/>
        <p>---------- 转发的邮件 ----------</p>
        <p>${meta}</p>
        <br/>
        ${originalBody}
      `

      // 不自动附带附件，用户可自行重新添加
      attachments.value = []

      forwardId.value = id
      draftId.value = null // 确保不会当作草稿更新
      ElMessage.success('已加载转发内容')
    } catch (error) {
      console.error('加载转发内容失败:', error)
      ElMessage.error('加载转发内容失败')
    }
  }

  onMounted(async () => {
    await loadMailAccounts()
    
    const queryDraftId = route.query.draftId
    const queryForwardId = route.query.forwardId

    if (queryDraftId) {
      await loadDraft(queryDraftId)
      return
    }

    if (queryForwardId) {
      await loadForward(queryForwardId)
      return
    }
  })
</script>

<style lang="scss" scoped>
.compose-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.compose-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;

  .toolbar-actions {
    display: flex;
    gap: 12px;
  }
}

.compose-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.form-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f2f5;

  label {
    width: 60px;
    font-size: 14px;
    color: #606266;
    flex-shrink: 0;
  }

  .input-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;

    .contact-select {
      flex: 1;
    }
  }

  .el-input, .contact-select {
    flex: 1;
  }

  :deep(.el-input__wrapper) {
    box-shadow: none;
  }
}

.attachments-row {
  flex-direction: column;
  align-items: stretch;

  label {
    display: none;
  }
}

.editor-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .rich-editor {
    flex: 1;
    display: flex;
    flex-direction: column;
    border: none;
    border-radius: 0;

    :deep(.editor-content) {
      flex: 1;
    }
  }
}
</style>

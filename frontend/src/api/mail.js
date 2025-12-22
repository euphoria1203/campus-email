import api from './index'

/**
 * 邮件相关 API
 */
export const mailApi = {
  // 发送邮件
  send(data) {
    return api.post('/mails', data)
  },

  // 保存草稿
  saveDraft(data) {
    return api.post('/mails/drafts', data)
  },

  // 发送草稿
  sendDraft(id) {
    return api.post(`/mails/drafts/${id}/send`)
  },

  // 获取用户邮件列表
  list(userId, folder = '') {
    return api.get(`/mails/user/${userId}`, { params: { folder } })
  },

  // 搜索邮件（后端全文检索）
  search({ keyword, folder = '', accountId = null, page = 0, size = 200 }) {
    const params = { keyword, folder, page, size }
    if (accountId !== null && accountId !== undefined && accountId !== '') {
      params.accountId = accountId
    }
    return api.get('/mails/search', { params })
  },

  // 获取邮件详情
  get(id) {
    return api.get(`/mails/${id}`)
  },

  // 标记为已读
  markAsRead(id) {
    return api.put(`/mails/${id}/read`)
  },

  // 切换星标状态
  toggleStar(id) {
    return api.put(`/mails/${id}/star`)
  },

  // 删除邮件
  delete(id) {
    return api.delete(`/mails/${id}`)
  },

  // 批量删除邮件
  batchDelete(ids) {
    return api.delete('/mails/batch', { data: ids })
  },

  // 垃圾箱彻底删除
  deletePermanently(ids) {
    return api.delete('/mails/trash', { data: ids })
  },

  // 移出垃圾箱
  restore(ids) {
    return api.put('/mails/trash/restore', ids)
  },

  // 获取邮件统计信息（未读数、草稿数）
  getStats(userId) {
    return api.get(`/mails/stats/${userId}`)
  }
}

/**
 * 联系人相关 API
 */
export const contactApi = {
  // 搜索联系人
  search(userId, keyword) {
    return api.get('/contacts/search', { params: { userId, keyword } })
  },

  // 获取联系人列表
  list(userId) {
    return api.get(`/contacts/user/${userId}`)
  },

  // 添加联系人
  create(data) {
    return api.post('/contacts', data)
  },

  // 更新联系人
  update(id, data) {
    return api.put(`/contacts/${id}`, data)
  },

  // 删除联系人
  delete(id) {
    return api.delete(`/contacts/${id}`)
  }
}

/**
 * 附件相关 API
 */
export const attachmentApi = {
  // 上传附件
  upload(file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/attachments/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: progressEvent => {
        if (onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    })
  },

  // 下载附件
  download(id) {
    return api.get(`/attachments/download/${id}`, { responseType: 'blob' })
  },

  // 获取附件信息
  getById(id) {
    return api.get(`/attachments/${id}`)
  },

  // 获取邮件的所有附件
  getByMailId(mailId) {
    return api.get(`/attachments/mail/${mailId}`)
  }
}

/**
 * 邮箱账号相关 API
 */
export const mailAccountApi = {
  // 获取指定用户的所有邮箱账号
  listByUser(userId) {
    return api.get(`/mail-accounts/user/${userId}`)
  },

  // 新增邮箱账号
  create(data) {
    return api.post('/mail-accounts', data)
  },

  // 更新邮箱账号（例如修改显示名、切换默认账号）
  update(id, data) {
    return api.put(`/mail-accounts/${id}`, data)
  },

  // 删除邮箱账号
  delete(id) {
    return api.delete(`/mail-accounts/${id}`)
  }
}

/**
 * 认证相关 API
 */
export const authApi = {
  login(data) {
    return api.post('/auth/login', data)
  },

  register(data) {
    return api.post('/auth/register', data)
  }
}

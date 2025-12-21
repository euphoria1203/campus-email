<template>
  <div class="contact-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="left-section">
            <span class="card-title">联系人簿</span>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索姓名、邮箱或电话"
              :prefix-icon="Search"
              clearable
              class="search-input"
            />
          </div>
          <el-button type="primary" size="small" @click="openCreateDialog">
            新增联系人
          </el-button>
        </div>
      </template>

      <el-table
        :data="filteredContacts"
        v-loading="loading"
        style="width: 100%"
        empty-text="暂无联系人"
      >
        <el-table-column prop="contactName" label="姓名" min-width="160" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column prop="phone" label="电话" min-width="140" />
        <el-table-column label="备注" min-width="200">
          <template #default="scope">
            <span class="notes-cell" :title="scope.row.notes || '-'">
              {{ scope.row.notes || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="scope">
            <el-button type="primary" link size="small" @click="openEditDialog(scope.row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="remove(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱地址" maxlength="100" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" placeholder="可选，方便联系" maxlength="30" />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="form.notes"
            type="textarea"
            placeholder="可选，添加备注信息"
            :autosize="{ minRows: 2, maxRows: 4 }"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { contactApi } from '@/api/mail'

const loading = ref(false)
const contacts = ref([])
const searchKeyword = ref('')

const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const editingId = ref(null)

const form = ref({
  contactName: '',
  email: '',
  phone: '',
  notes: ''
})

const rules = {
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] }
  ]
}

const dialogTitle = computed(() => (isEdit.value ? '编辑联系人' : '新增联系人'))

// 保持字符串，避免 Snowflake ID 精度丢失
const userId = localStorage.getItem('userId') || ''

const filteredContacts = computed(() => {
  if (!searchKeyword.value) {
    return contacts.value
  }
  const keyword = searchKeyword.value.trim().toLowerCase()
  return contacts.value.filter((item) => {
    return (
      item.contactName?.toLowerCase().includes(keyword) ||
      item.email?.toLowerCase().includes(keyword) ||
      item.phone?.toLowerCase().includes(keyword) ||
      item.notes?.toLowerCase().includes(keyword)
    )
  })
})

const loadContacts = async () => {
  if (!userId) return
  loading.value = true
  try {
    const res = await contactApi.list(userId)
    contacts.value = Array.isArray(res) ? res : []
  } catch (error) {
    console.error('加载联系人失败:', error)
    ElMessage.error('加载联系人失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    contactName: '',
    email: '',
    phone: '',
    notes: ''
  }
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    contactName: row.contactName || '',
    email: row.email || '',
    phone: row.phone || '',
    notes: row.notes || ''
  }
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = {
        ...form.value,
        userId
      }
      if (isEdit.value && editingId.value) {
        await contactApi.update(editingId.value, payload)
        ElMessage.success('联系人已更新')
      } else {
        await contactApi.create(payload)
        ElMessage.success('联系人已新增')
      }
      dialogVisible.value = false
      await loadContacts()
    } catch (error) {
      console.error('保存联系人失败:', error)
      ElMessage.error('保存失败，请稍后重试')
    }
  })
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除联系人「${row.contactName || row.email}」吗？`, '删除确认', {
      type: 'warning'
    })
    await contactApi.delete(row.id)
    ElMessage.success('联系人已删除')
    await loadContacts()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除联系人失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

onMounted(() => {
  loadContacts()
})
</script>

<style lang="scss" scoped>
.contact-manage {
  padding: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;

    .left-section {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;

      .card-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .search-input {
        max-width: 280px;
      }
    }
  }

  .notes-cell {
    display: block;
    width: 100%;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #606266;
  }
}
</style>

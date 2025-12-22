<template>
  <div class="mail-account-manage">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>邮箱账号管理</span>
          <el-button type="primary" size="small" @click="openCreateDialog">新增邮箱账号</el-button>
        </div>
      </template>

      <el-table :data="accounts" v-loading="loading" style="width: 100%">
        <el-table-column prop="emailAddress" label="邮箱地址" min-width="220" />
        <el-table-column label="显示名称" min-width="160">
          <template #default="scope">
            <span>{{ scope.row.displayName }}</span>
            <el-button 
              type="primary" 
              link 
              :icon="Edit" 
              @click="openEditDialog(scope.row)"
              style="margin-left: 8px;"
            />
          </template>
        </el-table-column>
        <el-table-column label="默认" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.isDefault" type="success" size="small">默认</el-tag>
            <el-button
              v-else
              type="primary"
              link
              size="small"
              @click="setDefault(scope.row)"
            >设为默认</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope">
            <el-button
              type="danger"
              link
              size="small"
              @click="remove(scope.row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑账号对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑邮箱账号' : '新增邮箱账号'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="邮箱账号" prop="emailAddress">
          <el-input v-model="form.emailAddress" placeholder="请输入邮箱地址" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="显示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="发件人显示名称，可选" />
        </el-form-item>
        <el-form-item label="设为默认" prop="isDefault">
          <el-switch v-model="form.isDefault" />
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
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import { mailAccountApi } from '@/api/mail'

const accounts = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const editingId = ref(null)

const form = ref({
  emailAddress: '',
  displayName: '',
  isDefault: false
})

const rules = {
  emailAddress: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入合法邮箱地址', trigger: 'blur' }
  ]
}

// 保持字符串，避免 Snowflake ID 精度丢失
const userId = localStorage.getItem('userId') || ''

const updateCurrentAccount = (accountId) => {
  if (!accountId) return
  localStorage.setItem('currentAccountId', accountId)
  window.dispatchEvent(new CustomEvent('account-switch', { detail: { accountId } }))
}

const clearCurrentAccount = () => {
  localStorage.removeItem('currentAccountId')
  window.dispatchEvent(new CustomEvent('account-switch', { detail: { accountId: null } }))
}


const loadAccounts = async () => {
  if (!userId) return
  loading.value = true
  try {
    const res = await mailAccountApi.listByUser(userId)
    accounts.value = res || []
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.value = {
    emailAddress: '',
    displayName: '',
    isDefault: false
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    emailAddress: row.emailAddress,
    displayName: row.displayName,
    isDefault: row.isDefault
  }
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const data = {
        emailAddress: form.value.emailAddress,
        displayName: form.value.displayName,
        isDefault: form.value.isDefault,
        userId
      }

      let response
      if (isEdit.value) {
        response = await mailAccountApi.update(editingId.value, data)
        ElMessage.success('????')
      } else {
        response = await mailAccountApi.create(data)
        ElMessage.success('????')
      }

      if (form.value.isDefault) {
        const accountId = response?.id || editingId.value
        updateCurrentAccount(accountId)
      }
      
      dialogVisible.value = false
      loadAccounts()
    } catch (e) {
      ElMessage.error(isEdit.value ? '????' : '????')
    }
  })
}

const setDefault = async (row) => {
  try {
    await mailAccountApi.update(row.id, { ...row, isDefault: true })
    ElMessage.success('???????')
    updateCurrentAccount(row.id)
    loadAccounts()
  } catch (e) {
    ElMessage.error('??????????')
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('????????????', '??', {
      type: 'warning'
    })
    await mailAccountApi.delete(row.id)
    const currentId = Number(localStorage.getItem('currentAccountId'))
    if (currentId && currentId === row.id) {
      clearCurrentAccount()
    }
    ElMessage.success('????')
    loadAccounts()
  } catch (e) {
    // ????????????????
  }
}

onMounted(() => {
  loadAccounts()
})
</script>

<style scoped lang="scss">
.mail-account-manage {
  padding: 16px;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}
</style>

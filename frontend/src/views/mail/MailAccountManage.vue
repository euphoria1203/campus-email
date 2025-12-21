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
        <el-form-item label="邮箱账号" prop="emailPrefix">
          <el-input v-model="form.emailPrefix" placeholder="请输入账号前缀" :disabled="isEdit">
            <template #append>@campus.mail</template>
          </el-input>
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
  emailPrefix: '',
  displayName: '',
  isDefault: false
})

const rules = {
  emailPrefix: [
    { required: true, message: '请输入邮箱账号前缀', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._-]+$/, message: '只能包含字母、数字、点、下划线和连字符', trigger: 'blur' }
  ]
}

// 保持字符串，避免 Snowflake ID 精度丢失
const userId = localStorage.getItem('userId') || ''

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
    emailPrefix: '',
    displayName: '',
    isDefault: false
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  // 提取前缀
  const prefix = row.emailAddress.split('@')[0]
  form.value = {
    emailPrefix: prefix,
    displayName: row.displayName,
    isDefault: row.isDefault
  }
  dialogVisible.value = true
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const emailAddress = `${form.value.emailPrefix}@campus.mail`
      const data = {
        emailAddress,
        displayName: form.value.displayName,
        isDefault: form.value.isDefault,
        userId
      }

      if (isEdit.value) {
        await mailAccountApi.update(editingId.value, data)
        ElMessage.success('更新成功')
      } else {
        await mailAccountApi.create(data)
        ElMessage.success('新增成功')
      }
      
      dialogVisible.value = false
      loadAccounts()
    } catch (e) {
      ElMessage.error(isEdit.value ? '更新失败' : '新增失败')
    }
  })
}

const setDefault = async (row) => {
  try {
    await mailAccountApi.update(row.id, { ...row, isDefault: true })
    ElMessage.success('已设为默认邮箱')
    loadAccounts()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该邮箱账号吗？', '提示', {
      type: 'warning'
    })
    await mailAccountApi.delete(row.id)
    ElMessage.success('删除成功')
    loadAccounts()
  } catch (e) {
    // 用户取消或请求失败都不再额外提示
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

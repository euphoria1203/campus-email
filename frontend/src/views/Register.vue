<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="32" color="#409EFF"><Message /></el-icon>
          <h2>注册账号</h2>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input 
            v-model="form.username" 
            placeholder="用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="emailPrefix">
          <el-input 
            v-model="form.emailPrefix" 
            placeholder="邮箱账号"
            :prefix-icon="Message"
            size="large"
          >
            <template #append>
              <span class="email-suffix">@campus.mail</span>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="确认密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            style="width: 100%"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login">立即登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/mail'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  emailPrefix: '',
  password: '',
  confirmPassword: ''
})

// 邮箱后缀
const EMAIL_SUFFIX = '@campus.mail'

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateEmailPrefix = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入邮箱账号'))
  } else if (!/^[a-zA-Z0-9._-]+$/.test(value)) {
    callback(new Error('邮箱账号只能包含字母、数字、点、下划线和连字符'))
  } else if (value.length < 3 || value.length > 30) {
    callback(new Error('邮箱账号长度在 3 到 30 个字符'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  emailPrefix: [
    { required: true, message: '请输入邮箱账号', trigger: 'blur' },
    { validator: validateEmailPrefix, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    
    await authApi.register({
      username: form.username,
      email: form.emailPrefix + EMAIL_SUFFIX,
      password: form.password
    })
    
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.register-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 400px;

  .card-header {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    h2 {
      margin: 0;
      color: #303133;
    }
  }
}

.register-footer {
  text-align: center;
  color: #909399;

  a {
    color: #409EFF;
    margin-left: 8px;
    
    &:hover {
      text-decoration: underline;
    }
  }
}

.email-suffix {
  color: #606266;
  font-weight: 500;
  white-space: nowrap;
}
</style>

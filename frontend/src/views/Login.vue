<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="32" color="#409EFF"><Message /></el-icon>
          <h2>Campus Mail</h2>
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

        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            style="width: 100%"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register">立即注册</router-link>
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
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  try {
    await formRef.value.validate()
    loading.value = true
    
    const response = await authApi.login({
      username: form.username,
      password: form.password
    })
    
    console.log('登录响应:', response)
    
    // 保存 token 和用户信息到 localStorage
    localStorage.setItem('token', response.token)
    localStorage.setItem('userId', response.userId)
    localStorage.setItem('username', response.username)
    localStorage.setItem('email', response.email)
    localStorage.setItem('nickname', response.nickname || response.username)
    
    ElMessage.success('登录成功')
    
    // 使用 setTimeout 确保 localStorage 写入完成后再跳转
    setTimeout(() => {
      router.push('/inbox')
    }, 100)
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名或密码')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
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

.login-footer {
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
</style>

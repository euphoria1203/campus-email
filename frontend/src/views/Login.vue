<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧插画/欢迎区 -->
      <div class="welcome-side">
        <div class="brand">
          <el-icon :size="40" color="#fff"><Message /></el-icon>
          <h1>CampusMail</h1>
        </div>
        <div class="welcome-text">
          <h2>连接校园，<br>沟通无限</h2>
          <p>欢迎回到您的校园邮箱中心</p>
        </div>
        <div class="illustration-bg">
          <div class="circle c1"></div>
          <div class="circle c2"></div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-side">
        <div class="form-header">
          <h3>账号登录</h3>
          <p>请输入您的校园账号</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="0" @submit.prevent="handleLogin" class="login-form">
          <el-form-item prop="username">
            <el-input 
              v-model="form.username" 
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
              class="rounded-input"
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
              class="rounded-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="submit-btn"
              :loading="loading"
              native-type="submit"
              round
            >
              登 录
            </el-button>
          </el-form-item>

          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="link-text">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
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
.login-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f0f2f5;
  background-image: radial-gradient(#e3e8ee 1px, transparent 1px);
  background-size: 20px 20px;
}

.login-container {
  width: 900px;
  height: 550px;
  display: flex;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  .welcome-side {
    width: 45%;
    background: linear-gradient(135deg, #409EFF 0%, #3a8ee6 100%);
    padding: 40px;
    display: flex;
    flex-direction: column;
    color: #fff;
    position: relative;
    overflow: hidden;

    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 60px;
      
      h1 {
        font-size: 24px;
        font-weight: 600;
        margin: 0;
      }
    }

    .welcome-text {
      z-index: 2;
      h2 {
        font-size: 36px;
        line-height: 1.4;
        margin-bottom: 20px;
        font-weight: 700;
      }
      p {
        font-size: 16px;
        opacity: 0.9;
      }
    }

    .illustration-bg {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: 1;

      .circle {
        position: absolute;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.1);
      }

      .c1 {
        width: 300px;
        height: 300px;
        top: -50px;
        right: -100px;
      }

      .c2 {
        width: 200px;
        height: 200px;
        bottom: -50px;
        left: -50px;
      }
    }
  }

  .form-side {
    width: 55%;
    padding: 60px;
    display: flex;
    flex-direction: column;
    justify-content: center;

    .form-header {
      margin-bottom: 40px;
      text-align: center;

      h3 {
        font-size: 28px;
        color: #303133;
        margin-bottom: 10px;
      }

      p {
        color: #909399;
        font-size: 14px;
      }
    }

    .login-form {
      .rounded-input {
        :deep(.el-input__wrapper) {
          border-radius: 20px;
          padding-left: 15px;
          box-shadow: 0 0 0 1px #dcdfe6 inset;
          
          &.is-focus {
            box-shadow: 0 0 0 1px #409EFF inset;
          }
        }
      }

      .submit-btn {
        margin-top: 20px;
        font-weight: 600;
        letter-spacing: 2px;
        background: linear-gradient(90deg, #409EFF 0%, #3a8ee6 100%);
        border: none;
        
        &:hover {
          opacity: 0.9;
          transform: translateY(-1px);
        }
      }
    }

    .form-footer {
      margin-top: 20px;
      text-align: center;
      font-size: 14px;
      color: #606266;

      .link-text {
        color: #409EFF;
        text-decoration: none;
        font-weight: 600;
        margin-left: 5px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .login-container {
    width: 90%;
    height: auto;
    flex-direction: column;

    .welcome-side {
      width: 100%;
      padding: 30px;
      min-height: 200px;
    }

    .form-side {
      width: 100%;
      padding: 40px 20px;
    }
  }
}
</style>

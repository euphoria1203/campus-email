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
          <h2>加入我们，<br>开启新旅程</h2>
          <p>注册您的专属校园邮箱账号</p>
        </div>
        <div class="illustration-bg">
          <div class="circle c1"></div>
          <div class="circle c2"></div>
        </div>
      </div>

      <!-- 右侧注册表单 -->
      <div class="form-side">
        <div class="form-header">
          <h3>创建账号</h3>
          <p>填写以下信息完成注册</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-width="0" class="login-form">
          <el-form-item prop="username">
            <el-input 
              v-model="form.username" 
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
              class="rounded-input"
            />
          </el-form-item>

          <el-form-item prop="emailPrefix">
            <el-input 
              v-model="form.emailPrefix" 
              placeholder="邮箱账号"
              :prefix-icon="Message"
              size="large"
              class="rounded-input-group"
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
              class="rounded-input"
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
              class="rounded-input"
            />
          </el-form-item>

          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
              round
            >
              注 册
            </el-button>
          </el-form-item>

          <div class="form-footer">
            <span>已有账号？</span>
            <router-link to="/login" class="link-text">立即登录</router-link>
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
  height: 600px; /* Slightly taller for register form */
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
    padding: 40px 60px; /* Adjusted padding */
    display: flex;
    flex-direction: column;
    justify-content: center;

    .form-header {
      margin-bottom: 30px;
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

      .rounded-input-group {
        :deep(.el-input__wrapper) {
          border-top-left-radius: 20px;
          border-bottom-left-radius: 20px;
          padding-left: 15px;
        }
        :deep(.el-input-group__append) {
          border-top-right-radius: 20px;
          border-bottom-right-radius: 20px;
          background-color: #f5f7fa;
          color: #606266;
          font-weight: 500;
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

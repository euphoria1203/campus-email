<template>
  <div class="attachment-upload">
    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      :file-list="fileList"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :before-upload="beforeUpload"
      multiple
      drag
      :class="{ 'hide-upload': !showUploadArea }"
    >
      <div class="upload-trigger" v-if="showUploadArea">
        <el-icon :size="40"><Upload /></el-icon>
        <div class="upload-text">
          <p>将文件拖拽到此处，或 <em>点击上传</em></p>
          <p class="upload-tip">单个文件不超过 {{ maxSizeMB }}MB，最多上传 {{ maxCount }} 个文件</p>
        </div>
      </div>
    </el-upload>

    <!-- 附件列表 -->
    <div class="attachment-list" v-if="fileList.length > 0">
      <div class="list-header">
        <span>附件 ({{ fileList.length }})</span>
        <el-button link type="primary" @click="showUploadArea = !showUploadArea">
          {{ showUploadArea ? '收起' : '添加更多' }}
        </el-button>
      </div>
      
      <div class="attachment-items">
        <div 
          v-for="(file, index) in fileList" 
          :key="file.uid"
          class="attachment-item"
        >
          <el-icon :size="24" :color="getFileIconColor(file)">
            <component :is="getFileIcon(file)" />
          </el-icon>
          
          <div class="file-info">
            <span class="file-name" :title="file.name">{{ file.name }}</span>
            <span class="file-size">{{ formatFileSize(file.size) }}</span>
          </div>

          <el-progress 
            v-if="file.status === 'uploading'"
            :percentage="file.percentage || 0"
            :stroke-width="4"
            style="width: 100px"
          />

          <el-icon 
            v-else
            class="remove-btn" 
            @click="handleRemove(file)"
          >
            <Close />
          </el-icon>
        </div>
      </div>
    </div>

    <!-- 快速添加按钮 -->
    <div class="quick-add" v-if="!showUploadArea && fileList.length === 0">
      <el-button :icon="Paperclip" @click="showUploadArea = true">
        添加附件
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Upload, Close, Document, Picture, VideoPlay, Headset, FolderOpened, Paperclip
} from '@element-plus/icons-vue'
import { attachmentApi } from '@/api/mail'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxSizeMB: {
    type: Number,
    default: 25
  },
  maxCount: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['update:modelValue'])

const uploadRef = ref(null)
const fileList = ref([])
const showUploadArea = ref(false)

// 同步外部值
watch(() => props.modelValue, (newVal) => {
  fileList.value = newVal.map(file => ({
    ...file,
    uid: file.uid || Date.now() + Math.random()
  }))
}, { immediate: true, deep: true })

// 文件变化 - 自动上传
const handleChange = async (file, uploadFiles) => {
  // 检查文件数量
  if (uploadFiles.length > props.maxCount) {
    ElMessage.warning(`最多只能上传 ${props.maxCount} 个文件`)
    return
  }

  // 检查文件大小
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.warning(`文件 "${file.name}" 超过 ${props.maxSizeMB}MB 限制`)
    uploadFiles.pop()
    return
  }

  fileList.value = uploadFiles
  
  // 如果是新添加的文件，立即上传
  if (file.status === 'ready' && file.raw) {
    await uploadFile(file)
  }
}

// 上传单个文件
const uploadFile = async (file) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid)
  if (index === -1) return

  fileList.value[index].status = 'uploading'
  fileList.value[index].percentage = 0

  try {
    const response = await attachmentApi.upload(file.raw, (percent) => {
      if (fileList.value[index]) {
        fileList.value[index].percentage = percent
      }
    })
    
    const saved = response?.data ?? response
    if (!saved || !saved.id) {
      throw new Error('附件ID缺失')
    }
    fileList.value[index].status = 'success'
    fileList.value[index].id = saved.id
    fileList.value[index].storagePath = saved.storagePath
    fileList.value[index].percentage = 100
    
    emitFiles()
    ElMessage.success(`${file.name} 上传成功`)
  } catch (error) {
    console.error('上传失败:', error)
    fileList.value[index].status = 'fail'
    ElMessage.error(`${file.name} 上传失败`)
  }
}

// 移除文件
const handleRemove = (file) => {
  const index = fileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) {
    fileList.value.splice(index, 1)
    emitFiles()
  }
}

// 上传前检查
const beforeUpload = (file) => {
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.warning(`文件 "${file.name}" 超过 ${props.maxSizeMB}MB 限制`)
    return false
  }
  return true
}

// 发送文件列表（包含已上传的附件ID）
const emitFiles = () => {
  const files = fileList.value.map(f => ({
    uid: f.uid,
    name: f.name,
    size: f.size,
    id: f.id, // 附件ID
    status: f.status,
    storagePath: f.storagePath
  }))
  emit('update:modelValue', files)
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取文件图标
const getFileIcon = (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)) {
    return Picture
  }
  if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'mkv'].includes(ext)) {
    return VideoPlay
  }
  if (['mp3', 'wav', 'flac', 'aac', 'ogg'].includes(ext)) {
    return Headset
  }
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) {
    return FolderOpened
  }
  return Document
}

// 获取文件图标颜色
const getFileIconColor = (file) => {
  const ext = file.name.split('.').pop()?.toLowerCase()
  
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(ext)) {
    return '#67C23A'
  }
  if (['pdf'].includes(ext)) {
    return '#F56C6C'
  }
  if (['doc', 'docx'].includes(ext)) {
    return '#409EFF'
  }
  if (['xls', 'xlsx'].includes(ext)) {
    return '#67C23A'
  }
  if (['ppt', 'pptx'].includes(ext)) {
    return '#E6A23C'
  }
  if (['zip', 'rar', '7z'].includes(ext)) {
    return '#909399'
  }
  return '#409EFF'
}

// 获取原始文件列表
const getRawFiles = () => {
  return fileList.value.map(f => f.raw || f)
}

// 清空文件列表
const clear = () => {
  fileList.value = []
  emit('update:modelValue', [])
}

defineExpose({
  getRawFiles,
  clear
})
</script>

<style lang="scss" scoped>
.attachment-upload {
  .hide-upload {
    :deep(.el-upload-dragger) {
      display: none;
    }
  }

  :deep(.el-upload-dragger) {
    padding: 20px;
    border-style: dashed;
  }
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #909399;

  .upload-text {
    text-align: center;

    p {
      margin: 4px 0;
    }

    em {
      color: #409eff;
      font-style: normal;
    }

    .upload-tip {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

.attachment-list {
  margin-top: 12px;

  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 14px;
    color: #606266;
  }
}

.attachment-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  max-width: 280px;

  .file-info {
    flex: 1;
    min-width: 0;

    .file-name {
      display: block;
      font-size: 13px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .file-size {
      font-size: 12px;
      color: #909399;
    }
  }

  .remove-btn {
    cursor: pointer;
    color: #909399;
    
    &:hover {
      color: #f56c6c;
    }
  }
}

.quick-add {
  padding: 8px 0;
}
</style>

<template>
  <div class="rich-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <el-button-group>
        <el-tooltip content="加粗">
          <el-button 
            :class="{ active: isActive('bold') }" 
            @click="execCommand('bold')"
          >
            <span class="icon-text bold">B</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="斜体">
          <el-button 
            :class="{ active: isActive('italic') }" 
            @click="execCommand('italic')"
          >
            <span class="icon-text italic">I</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="下划线">
          <el-button 
            :class="{ active: isActive('underline') }" 
            @click="execCommand('underline')"
          >
            <span class="icon-text underline">U</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="删除线">
          <el-button 
            :class="{ active: isActive('strikeThrough') }" 
            @click="execCommand('strikeThrough')"
          >
            <span class="icon-text strikethrough">S</span>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <!-- 字体大小 -->
      <el-select 
        v-model="fontSize" 
        placeholder="字号" 
        style="width: 90px"
        size="small"
        @change="handleFontSize"
      >
        <el-option label="小" value="2" />
        <el-option label="正常" value="3" />
        <el-option label="大" value="4" />
        <el-option label="较大" value="5" />
        <el-option label="很大" value="6" />
        <el-option label="最大" value="7" />
      </el-select>

      <!-- 字体颜色 -->
      <el-color-picker 
        v-model="fontColor" 
        size="small"
        @change="handleFontColor"
      />

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="左对齐">
          <el-button @click="execCommand('justifyLeft')">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M3 3h18v2H3V3zm0 4h12v2H3V7zm0 4h18v2H3v-2zm0 4h12v2H3v-2zm0 4h18v2H3v-2z"/>
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="居中">
          <el-button @click="execCommand('justifyCenter')">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M3 3h18v2H3V3zm3 4h12v2H6V7zm-3 4h18v2H3v-2zm3 4h12v2H6v-2zm-3 4h18v2H3v-2z"/>
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="右对齐">
          <el-button @click="execCommand('justifyRight')">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M3 3h18v2H3V3zm6 4h12v2H9V7zm-6 4h18v2H3v-2zm6 4h12v2H9v-2zm-6 4h18v2H3v-2z"/>
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-button-group>
        <el-tooltip content="无序列表">
          <el-button @click="execCommand('insertUnorderedList')">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M4 4h2v2H4V4zm4 0h12v2H8V4zM4 10h2v2H4v-2zm4 0h12v2H8v-2zM4 16h2v2H4v-2zm4 0h12v2H8v-2z"/>
            </svg>
          </el-button>
        </el-tooltip>
        <el-tooltip content="有序列表">
          <el-button @click="execCommand('insertOrderedList')">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
              <path d="M3 4h2v2H3V4zm4 0h12v2H7V4zM3 10h2v2H3v-2zm4 0h12v2H7v-2zM3 16h2v2H3v-2zm4 0h12v2H7v-2z"/>
              <text x="3.5" y="5.5" font-size="4" fill="currentColor">1</text>
              <text x="3.5" y="11.5" font-size="4" fill="currentColor">2</text>
              <text x="3.5" y="17.5" font-size="4" fill="currentColor">3</text>
            </svg>
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-divider direction="vertical" />

      <el-tooltip content="插入链接">
        <el-button @click="showLinkDialog = true">
          <el-icon><Link /></el-icon>
        </el-button>
      </el-tooltip>

      <el-tooltip content="插入图片">
        <el-button @click="triggerImageUpload">
          <el-icon><Picture /></el-icon>
        </el-button>
      </el-tooltip>

      <input 
        ref="imageInput" 
        type="file" 
        accept="image/*" 
        style="display: none"
        @change="handleImageUpload"
      />
    </div>

    <!-- 编辑区域 -->
    <div 
      ref="editorRef"
      class="editor-content"
      contenteditable="true"
      @input="handleInput"
      @paste="handlePaste"
      @mouseup="updateActiveState"
      @keyup="updateActiveState"
      :placeholder="placeholder"
    ></div>

    <!-- 插入链接对话框 -->
    <el-dialog v-model="showLinkDialog" title="插入链接" width="400px">
      <el-form>
        <el-form-item label="链接文本">
          <el-input v-model="linkText" placeholder="请输入链接文本" />
        </el-form-item>
        <el-form-item label="链接地址">
          <el-input v-model="linkUrl" placeholder="https://" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLinkDialog = false">取消</el-button>
        <el-button type="primary" @click="insertLink">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, h } from 'vue'
import { 
  Link, Picture
} from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入邮件正文...'
  }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const fontSize = ref('3')
const fontColor = ref('#000000')
const showLinkDialog = ref(false)
const linkText = ref('')
const linkUrl = ref('')
const imageInput = ref(null)

// 初始化内容
onMounted(() => {
  if (props.modelValue && editorRef.value) {
    editorRef.value.innerHTML = props.modelValue
  }
})

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (editorRef.value && editorRef.value.innerHTML !== newVal) {
    editorRef.value.innerHTML = newVal
  }
})

// 执行编辑命令
const execCommand = (command, value = null) => {
  editorRef.value?.focus()
  document.execCommand(command, false, value)
}

// 检查命令状态
const isActive = (command) => {
  return document.queryCommandState(command)
}

// 更新激活状态
const updateActiveState = () => {
  // 触发响应式更新
}

// 处理输入
const handleInput = () => {
  emit('update:modelValue', editorRef.value?.innerHTML || '')
}

// 处理粘贴
const handlePaste = (e) => {
  e.preventDefault()
  const text = e.clipboardData.getData('text/html') || e.clipboardData.getData('text/plain')
  document.execCommand('insertHTML', false, text)
}

// 字体大小 - 需要先选中文本
const handleFontSize = (size) => {
  editorRef.value?.focus()
  // 保存当前选区
  const selection = window.getSelection()
  if (selection && selection.rangeCount > 0) {
    document.execCommand('fontSize', false, size)
  }
}

// 字体颜色
const handleFontColor = (color) => {
  execCommand('foreColor', color)
}

// 插入链接
const insertLink = () => {
  if (linkUrl.value) {
    const html = `<a href="${linkUrl.value}" target="_blank">${linkText.value || linkUrl.value}</a>`
    execCommand('insertHTML', html)
  }
  showLinkDialog.value = false
  linkText.value = ''
  linkUrl.value = ''
}

// 触发图片上传
const triggerImageUpload = () => {
  imageInput.value?.click()
}

// 处理图片上传
const handleImageUpload = (e) => {
  const file = e.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (event) => {
      const html = `<img src="${event.target.result}" style="max-width: 100%;" />`
      execCommand('insertHTML', html)
    }
    reader.readAsDataURL(file)
  }
  e.target.value = ''
}

// 获取内容
const getContent = () => {
  return editorRef.value?.innerHTML || ''
}

// 设置内容
const setContent = (html) => {
  if (editorRef.value) {
    editorRef.value.innerHTML = html
  }
}

// 清空内容
const clear = () => {
  if (editorRef.value) {
    editorRef.value.innerHTML = ''
  }
}

defineExpose({
  getContent,
  setContent,
  clear
})
</script>

<style lang="scss" scoped>
.rich-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;

  .el-button {
    padding: 8px 10px;
    min-width: 32px;
    
    &.active {
      background: #e6f7ff;
      color: #409eff;
    }
  }

  .el-divider--vertical {
    height: 20px;
  }

  // 图标文本样式
  .icon-text {
    font-family: Georgia, serif;
    font-size: 14px;
    line-height: 1;
    
    &.bold {
      font-weight: bold;
    }
    
    &.italic {
      font-style: italic;
    }
    
    &.underline {
      text-decoration: underline;
    }
    
    &.strikethrough {
      text-decoration: line-through;
    }
  }

  svg {
    display: block;
  }
}

.editor-content {
  min-height: 300px;
  padding: 16px;
  font-size: 15px;
  line-height: 1.8;
  outline: none;
  overflow-y: auto;

  &:empty::before {
    content: attr(placeholder);
    color: #c0c4cc;
    pointer-events: none;
  }

  &:focus {
    background: #fafafa;
  }

  // 编辑器内部样式
  :deep(a) {
    color: #409eff;
    text-decoration: underline;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
  }

  :deep(ul), :deep(ol) {
    padding-left: 24px;
  }
}
</style>

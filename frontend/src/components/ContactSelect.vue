<template>
  <div class="contact-select">
    <el-select
      v-model="selectedContacts"
      multiple
      filterable
      allow-create
      default-first-option
      :reserve-keyword="false"
      placeholder="输入邮箱地址，按回车添加"
      style="width: 100%"
      popper-class="contact-select-dropdown"
      :teleported="true"
      @change="handleChange"
      @visible-change="onDropdownVisibleChange"
    >
      <el-option
        v-for="contact in filteredContacts"
        :key="contact.id || contact.email"
        :label="formatLabel(contact)"
        :value="contact.email"
      >
        <div class="contact-option">
          <el-avatar :size="24">{{ getInitial(contact) }}</el-avatar>
          <div class="contact-info">
            <span class="contact-name">{{ contact.contactName || contact.email }}</span>
            <span class="contact-email">{{ contact.email }}</span>
          </div>
        </div>
      </el-option>
    </el-select>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { contactApi } from '@/api/mail'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue'])

const selectedContacts = ref([...props.modelValue])
const allContacts = ref([])
const searchKeyword = ref('')

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  selectedContacts.value = [...newVal]
}, { deep: true })

// 加载所有联系人
const loadContacts = async () => {
  try {
    const userId = localStorage.getItem('userId')
    if (userId) {
      const result = await contactApi.list(userId)
      allContacts.value = Array.isArray(result) ? result : []
    }
  } catch (error) {
    console.error('加载联系人失败', error)
    allContacts.value = []
  }
}

// 组件挂载时加载联系人列表
onMounted(() => {
  loadContacts()
})

// 下拉框打开时刷新联系人
const onDropdownVisibleChange = (visible) => {
  if (visible) {
    loadContacts()
  }
}

// 过滤后的联系人列表
const filteredContacts = computed(() => {
  return allContacts.value
})

// 格式化显示标签
const formatLabel = (contact) => {
  if (contact.contactName && contact.contactName !== contact.email) {
    return `${contact.contactName} <${contact.email}>`
  }
  return contact.email
}

// 获取联系人首字母
const getInitial = (contact) => {
  const name = contact.contactName || contact.email
  return name.charAt(0).toUpperCase()
}

// 处理选择变化
const handleChange = (value) => {
  emit('update:modelValue', value)
}
</script>

<style lang="scss" scoped>
.contact-select {
  width: 100%;
}

.contact-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
  width: 100%;
  overflow: hidden;

  .contact-info {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-width: 0;
    overflow: hidden;

    .contact-name {
      font-size: 14px;
      color: #303133;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .contact-email {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}
</style>

<!-- 全局样式，用于下拉框 -->
<style lang="scss">
.contact-select-dropdown {
  min-width: 280px !important;
  max-width: 400px !important;
  
  .el-select-dropdown__item {
    height: auto !important;
    padding: 8px 12px !important;
    line-height: normal !important;
  }
}
</style>

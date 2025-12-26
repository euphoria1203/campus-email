import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/inbox',
    children: [
      {
        path: 'inbox',
        name: 'Inbox',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'inbox', title: '收件箱' }
      },
      {
        path: 'sent',
        name: 'Sent',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'sent', title: '发件箱' }
      },
      {
        path: 'starred',
        name: 'Starred',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'starred', title: '星标邮件' }
      },
      {
        path: 'drafts',
        name: 'Drafts',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'drafts', title: '草稿箱' }
      },
      {
        path: 'scheduled',
        name: 'Scheduled',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'scheduled', title: '定时发送' }
      },
      {
        path: 'trash',
        name: 'Trash',
        component: () => import('@/views/mail/MailList.vue'),
        meta: { folder: 'trash', title: '垃圾箱' }
      },
      {
        path: 'mail/:id',
        name: 'MailDetail',
        component: () => import('@/views/mail/MailDetail.vue')
      },
      {
        path: 'compose',
        name: 'Compose',
        component: () => import('@/views/mail/Compose.vue')
      },
      {
        path: 'accounts',
        name: 'MailAccountManage',
        component: () => import('@/views/mail/MailAccountManage.vue'),
        meta: { title: '邮箱账号管理' }
      },
      {
        path: 'contacts',
        name: 'ContactManage',
        component: () => import('@/views/mail/ContactManage.vue'),
        meta: { title: '联系人簿', folder: 'contacts' }
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/mail/Statistics.vue'),
        meta: { title: '邮件统计' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫（模拟）
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router

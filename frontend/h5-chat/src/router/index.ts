import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/auth/loginView.vue'
import HomeView from '@/views/homeView.vue'
import ChatView from '@/views/chatView.vue'
import MyCenterView from '@/views/myCenterView.vue'
import MyOrdersView from '@/views/myOrdersView.vue'
import ApplyView from '@/views/applyView.vue'
import ApprovalView from '@/views/approvalView.vue'
import App from "@/App.vue";

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: { requiresAuth: false },
    },
    {
        path: '/',
        name: 'Home',
        component: HomeView,
        meta: { requiresAuth: true },
    },
    {
        path: '/chat/:scene?',
        name: 'Chat',
        component: ChatView,
        meta: { requiresAuth: true },
        props: (route) => ({ scene: route.params.scene }),
    },
    {
        path: '/my',
        name: 'MyCenter',
        component: MyCenterView,
        meta: { requiresAuth: true },
    },
    {
        path: '/my/orders',
        name: 'MyOrders',
        component: MyOrdersView,
        meta: { requiresAuth: true },
    },
    {
        path: '/my/apply',
        name: 'Apply',
        component: ApplyView,
        meta: { requiresAuth: true },
    },
    {
        path: '/my/approval',
        name: 'Approval',
        component: ApprovalView,
        meta: { requiresAuth: true },
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

// 简单路由守卫（需登录）
router.beforeEach((to, _from, next) => {
    const isLoggedIn = localStorage.getItem('token') !== null
    if (to.meta.requiresAuth && !isLoggedIn) {
        next('/login')
    } else {
        next()
    }
})

export default router
import {createRouter, createWebHistory} from 'vue-router'

import LayoutView from '@/layout/layout.vue'
import LoginView from '@/views/auth/loginView.vue'
import DashboardView from '@/views/dashboard.vue'
import CompanyView from '@/views/user/companyView.vue'
import DepartmentView from '@/views/user/departmentView.vue'
import EmployeeView from '@/views/user/employeeView.vue'
import UserView from '@/views/user/userView.vue'
import OrderView from '@/views/order/orderView.vue'
import StandardView from '@/views/order/standardView.vue'
import ApprovalView from '@/views/approval/approvalView.vue'
import RagFileView from '@/views/rag/ragFileView.vue'
import PolicyView from '@/views/rag/policyView.vue'
import PromptView from '@/views/rag/promptView.vue'
import SessionView from '@/views/dialog/sessionView.vue'
import RegisterView from "@/views/auth/registerView.vue";

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: {title: '登录'}
    },
    {
        path: '/register',
        name: 'Register',
        component: RegisterView,
        meta: {title: '注册'}
    },
    {
        path: '/',
        name: 'Layout',
        component: LayoutView,
        redirect: '/dashboard',
        children: [
            {
                path: '/dashboard',
                name: 'Dashboard',
                component: DashboardView,
                meta: { title: '数据看板' }
            },
            {
                path: '/company',
                name: 'Company',
                component: CompanyView,
                meta: { title: '企业管理' }
            },
            {
                path: '/department',
                name: 'Department',
                component: DepartmentView,
                meta: { title: '部门管理' }
            },
            {
                path: '/employee',
                name: 'Employee',
                component: EmployeeView,
                meta: { title: '员工管理' }
            },
            {
                path: '/user',
                name: 'User',
                component: UserView,
                meta: {title: '用户管理'}
            },
            {
                path: '/order',
                name: 'Order',
                component: OrderView,
                meta: {title: '订单管理'}
            },
            {
                path: '/standard',
                name: 'Standard',
                component: StandardView,
                meta: {title: '差标管理'}
            },
            {
                path: '/approval',
                name: 'Approval',
                component: ApprovalView,
                meta: {title: '流程审批'}
            },
            {
                path: '/rag',
                name: 'RAG',
                component: RagFileView,
                meta: {title: 'RAG知识库'}
            },
            {
                path: '/policy',
                name: 'Policy',
                component: PolicyView,
                meta: {title: '差旅政策'}
            },
            {
                path: '/prompt',
                name: 'Prompt',
                component: PromptView,
                meta: {title: '提示词管理'}
            },
            {
                path: '/session',
                name: 'Session',
                component: SessionView,
                meta: {title: '会话管理'}
            },
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 简单路由守卫（示例）
router.beforeEach((to, _from, next) => {
    const token = localStorage.getItem('token')
    if (to.name === 'Login' || to.name === 'Register') {
        if (token) {
            next({name: 'Layout'})
        } else {
            next()
        }
    } else if (!token) {
        next({name: 'Login'})
    } else {
        next()
    }
})

export default router
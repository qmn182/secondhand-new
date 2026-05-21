import { createRouter, createWebHistory } from 'vue-router'
import axios from 'axios'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/apply-merchant', name: 'apply-merchant', component: () => import('@/views/ApplyMerchant.vue') },
    { path: '/publish', name: 'publish', component: () => import('@/views/Publish.vue') },
    { path: '/my-products', name: 'my-products', component: () => import('@/views/My_products.vue') },
    { path: '/profile', name: 'profile', component: () => import('@/views/Profile.vue') },
    { path: '/wallet', name: 'wallet', component: () => import('@/views/Wallet.vue') },
    { path: '/cart', name: 'cart', component: () => import('@/views/Cart.vue') },
    
    // ========== 新增：订单相关页面 ==========
    {
      path: '/buyer/orders',
      name: 'buyer-orders',
      component: () => import('@/views/buyer/BuyerOrders.vue'),
      meta: { requiresAuth: true }  // 需要登录
    },
    {
      path: '/merchant/orders',
      name: 'merchant-orders',
      component: () => import('@/views/merchant/MerchantOrders.vue'),
      meta: { requiresAuth: true, roles: [2, 3] }  // 需要商家或管理员身份
    },
    // ========== 原有管理员和店铺路由 ==========
    {
      path: '/admin/merchant-applications',
      name: 'merchant-applications',
      component: () => import('@/views/admin/MerchantApplications.vue')
    },
    {
      path: '/admin/users',
      name: 'user-management',
      component: () => import('@/views/admin/Users.vue')
    },
    {
      path: '/shop/:sellerId',
      name: 'shop',
      component: () => import('@/views/seller/Shop.vue')
    },
    {
      path: '/product-evaluations/:productId',
      name: 'product-evaluations',
      component: () => import('@/views/product/ProductEvaluations.vue')
    },
    {
      path: '/user/buyer-evaluations',
      name: 'buyer-evaluations',
      component: () => import('@/views/buyer/BuyerEvaluations.vue')
    },
    {
      path: '/product/:id',
      name: 'product-detail',
      component: () => import('@/views/product/ProductDetail.vue')
    },
    {
      path: '/product/edit/:id',
      name: 'edit-product',
      component: () => import('@/views/product/EditProduct.vue')
    },
    {
      path: '/order/detail/:orderNo',
      name: 'order-detail',
      component: () => import('@/views/order/OrderDetail.vue')
    },
    // 在 /admin/users 下方添加
    {
      path: '/admin/product-audit',
      name: 'product-audit',
      component: () => import('@/views/admin/ProductAudit.vue'),
      meta: { requiresAuth: true, roles: [3] }  // 仅管理员可访问
    }
  ]
})

// 全局前置守卫：统一处理登录和角色权限
router.beforeEach(async (to, from, next) => {
  // 不需要权限的页面直接放行（例如首页、登录页等）
  if (!to.meta.requiresAuth) {
    next()
    return
  }

  try {
    // 请求当前登录用户信息（后端 session 认证）
    const res = await axios.get('http://localhost:8080/user/current', { withCredentials: true })
    if (res.data.code !== 200 || !res.data.data) {
      // 未登录，跳转到登录页
      next('/login')
      return
    }

    const user = res.data.data
    const requiredRoles = to.meta.roles

    // 如果需要特定角色
    if (requiredRoles && requiredRoles.length > 0) {
      if (requiredRoles.includes(user.role)) {
        next()  // 有权限
      } else {
        // 角色不符，可根据需求跳转到首页或提示页
        alert('无权限访问该页面')
        next('/')
      }
    } else {
      // 仅需登录的页面
      next()
    }
  } catch (error) {
    // 请求失败（未登录或后端异常），跳转到登录页
    console.error('权限校验失败', error)
    next('/login')
  }
})

export default router
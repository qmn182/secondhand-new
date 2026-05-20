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
      component: () => import('@/views/seller/Shop.vue')   // 根据实际文件名和路径调整
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
      ame: 'product-detail',
      component: () => import('@/views/product/ProductDetail.vue')
    },
    {
      path: '/product/edit/:id',
      name: 'edit-product',
      component: () => import('@/views/product/EditProduct.vue')
    }
    // 商品详情页（若有则取消注释）
    // { path: '/product/:id', name: 'product-detail', component: () => import('@/views/ProductDetail.vue') }
  ]
})

// 全局前置守卫：检查管理员权限
router.beforeEach(async (to, from, next) => {
  // 需要管理员权限的页面路径列表
  const adminPages = ['/admin/users', '/admin/merchant-applications']
  // 判断当前路由是否需要管理员权限
  if (adminPages.some(path => to.path.startsWith(path))) {
    try {
      // 向后端请求当前登录用户信息
      const res = await axios.get('http://localhost:8080/user/current', { withCredentials: true })
      // 如果登录成功并且角色为管理员（role === 3），则放行
      if (res.data.code === 200 && res.data.data.role === 3) {
        next()
      } else {
        // 不是管理员，跳转到登录页
        next('/login')
      }
    } catch (error) {
      // 未登录或请求失败，跳转到登录页
      next('/login')
    }
  } else {
    // 其他页面直接放行
    next()
  }
})

export default router
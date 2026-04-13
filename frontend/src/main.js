/**
 * 应用入口：挂载 Vue3 + Pinia + Vue Router + Element Plus。
 * 路由守卫依赖 Pinia，故 createPinia() 须在 app.use(router) 之前执行（与 useUserStore 在 router 模块内导入一致）。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')

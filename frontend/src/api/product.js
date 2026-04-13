/**
 * 商品与分类 API：对应后端 ProductController、CategoryController；均为 GET，游客可调。
 */
import request from '../utils/request'

export function fetchProducts(params) {
  // -> 后端: ProductController#page (GET /api/products)
  // <- 调用方: Home.vue、Search.vue
  return request.get('/products', { params })
}

export function fetchProduct(id) {
  // -> 后端: ProductController#detail (GET /api/products/{id})
  // <- 调用方: ProductDetail.vue
  return request.get(`/products/${id}`)
}

export function fetchCategories() {
  // -> 后端: CategoryController#list (GET /api/categories)
  // <- 调用方: Home.vue、Search.vue、ProductDetail.vue
  return request.get('/categories')
}

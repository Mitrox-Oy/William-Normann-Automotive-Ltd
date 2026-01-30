/**
 * Admin API
 * 
 * API functions for admin/owner operations
 * All endpoints require owner role authentication
 * 
 * Adjust endpoint paths to match your backend
 */

import { api } from './apiClient'
import type { Product, Category } from './shopApi'

// ============================================================================
// PRODUCTS
// ============================================================================

export interface AdminProduct extends Product {
  stockLevel: number
  costPrice?: number
  profitMargin?: number
  sku: string
  status: 'active' | 'draft' | 'archived'
}

// Backend ProductDTO structure
export interface ProductCreateInput {
  name: string // Required, 2-200 chars
  description?: string // Optional, max 1000 chars
  price: number // Required, BigDecimal
  stockQuantity?: number // Optional, default 0
  sku: string // Required, 3-50 chars, unique
  imageUrl?: string // Optional
  active?: boolean // Optional, default true
  featured?: boolean // Optional, default false
  weight?: number // Optional, BigDecimal
  brand?: string // Optional, max 100 chars
  categoryId: number // Required, Long
  // Info sections (1-7)
  infoSection1Title?: string
  infoSection1Content?: string
  infoSection1Enabled?: boolean
  infoSection2Title?: string
  infoSection2Content?: string
  infoSection2Enabled?: boolean
  infoSection3Title?: string
  infoSection3Content?: string
  infoSection3Enabled?: boolean
  infoSection4Title?: string
  infoSection4Content?: string
  infoSection4Enabled?: boolean
  infoSection5Title?: string
  infoSection5Content?: string
  infoSection5Enabled?: boolean
  infoSection6Title?: string
  infoSection6Content?: string
  infoSection6Enabled?: boolean
  infoSection7Title?: string
  infoSection7Content?: string
  infoSection7Enabled?: boolean
}

export interface ProductVariantCreateInput {
  name: string // Required, max 120 chars
  sku: string // Required, max 60 chars, unique
  price?: number // Optional, BigDecimal
  stockQuantity?: number // Optional, default 0
  active?: boolean // Optional, default true
  defaultVariant?: boolean // Optional, default false
  position?: number // Optional
  options?: Record<string, string> // Optional map of option key/value pairs
}

export interface ProductUpdateInput extends Partial<ProductCreateInput> {
  id: string
}

export interface ProductsListParams {
  page?: number
  limit?: number
  status?: 'active' | 'draft' | 'archived'
  category?: string
  search?: string
  sortBy?: 'name' | 'price' | 'stock' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}

export interface ProductsListResponse {
  products: AdminProduct[]
  total: number
  page: number
  limit: number
  totalPages: number
}

/**
 * Fetch products list (admin view)
 * Backend endpoint: GET /api/products (with pagination)
 * Backend returns Spring Page format: { content, totalElements, totalPages, number, size }
 */
export async function getAdminProducts(params: ProductsListParams = {}): Promise<ProductsListResponse> {
  const searchParams = new URLSearchParams()
  
  // Backend uses 0-based page numbers, frontend uses 1-based
  const backendPage = params.page ? (params.page - 1) : 0
  searchParams.append('page', backendPage.toString())
  searchParams.append('size', (params.limit || 20).toString())
  
  if (params.search) {
    searchParams.append('search', params.search)
  }
  if (params.category) {
    searchParams.append('category', params.category)
  }
  if (params.sortBy) {
    searchParams.append('sortBy', params.sortBy === 'createdAt' ? 'createdDate' : params.sortBy)
  }
  if (params.sortOrder) {
    searchParams.append('sortDir', params.sortOrder)
  }
  
  // Backend uses /api/products, not /admin/products
  // Backend returns Spring Page format, we need to transform it
  const backendResponse = await api.get<{
    content: any[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>(`/api/products?${searchParams.toString()}`)
  
  // Transform backend ProductDTO to AdminProduct format
  const products: AdminProduct[] = (backendResponse.content || []).map((p: any) => ({
    id: p.id?.toString() || '',
    name: p.name || '',
    description: p.description || '',
    price: typeof p.price === 'number' ? p.price : parseFloat(p.price) || 0,
    currency: 'USD', // Default, adjust if backend provides currency
    availability: p.stockQuantity > 0 ? 'in_stock' : 'out_of_stock',
    images: p.images?.map((img: any) => img.imageUrl || img.url || '') || [p.imageUrl].filter(Boolean),
    category: p.categoryId?.toString() || '',
    categoryName: p.categoryName || '',
    sku: p.sku || '',
    stockLevel: p.stockQuantity || 0,
    status: p.active ? 'active' : 'draft',
    slug: p.slug || p.sku?.toLowerCase().replace(/\s+/g, '-') || '',
    partNumber: p.sku || '',
    createdAt: p.createdDate || new Date().toISOString(),
    updatedAt: p.updatedDate || new Date().toISOString(),
  }))
  
  // Transform Spring Page format to frontend format
  return {
    products,
    total: backendResponse.totalElements || 0,
    page: (backendResponse.number || 0) + 1, // Convert 0-based to 1-based
    limit: backendResponse.size || 20,
    totalPages: backendResponse.totalPages || 0,
  }
}

/**
 * Get all categories for product creation
 * Backend endpoint: GET /api/categories
 */
export async function getAllCategories(): Promise<Category[]> {
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
  const response = await fetch(`${API_BASE_URL}/api/categories`, {
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  })
  
  if (!response.ok) {
    throw new Error(`Failed to fetch categories: ${response.status}`)
  }
  
  return response.json()
}

/**
 * Get single product (admin view)
 */
export async function getAdminProduct(id: string): Promise<AdminProduct> {
  return api.get<AdminProduct>(`/admin/products/${id}`)
}

/**
 * Create product
 * Backend endpoint: POST /api/products
 */
export async function createProduct(data: ProductCreateInput): Promise<any> {
  return api.post<any>('/api/products', data)
}

/**
 * Create product variant
 * Backend endpoint: POST /api/products/{productId}/variants
 */
export async function createProductVariant(productId: string, data: ProductVariantCreateInput): Promise<any> {
  return api.post<any>(`/api/products/${productId}/variants`, data)
}

/**
 * Upload product image
 * Backend endpoint: POST /api/products/{productId}/images/upload
 */
export async function uploadProductImage(productId: string, file: File, isMain: boolean = false): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('isMain', isMain.toString())
  
  // Use fetch directly for FormData
  const { getAccessToken } = await import('./apiClient')
  const token = getAccessToken()
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
  const response = await fetch(`${baseUrl}/api/products/${productId}/images/upload`, {
    method: 'POST',
    headers: token ? {
      'Authorization': `Bearer ${token}`,
    } : {},
    body: formData,
    credentials: 'include',
  })
  
  if (!response.ok) {
    const error = await response.text()
    throw new Error(`Failed to upload image: ${error}`)
  }
  
  return response.json()
}

/**
 * Update product
 */
export async function updateProduct(id: string, data: Partial<ProductCreateInput>): Promise<AdminProduct> {
  return api.put<AdminProduct>(`/admin/products/${id}`, data)
}

/**
 * Delete product
 */
export async function deleteProduct(id: string): Promise<void> {
  return api.delete<void>(`/admin/products/${id}`)
}

/**
 * Update product stock
 */
export async function updateProductStock(id: string, stockLevel: number): Promise<AdminProduct> {
  return api.patch<AdminProduct>(`/admin/products/${id}/stock`, { stockLevel })
}

// ============================================================================
// CATEGORIES
// ============================================================================

export interface AdminCategory extends Category {
  status: 'active' | 'inactive'
  createdAt: string
  updatedAt: string
}

export interface CategoryCreateInput {
  name: string
  slug: string
  description?: string
  status: 'active' | 'inactive'
}

/**
 * Get all categories (admin view)
 * Backend endpoint: GET /api/categories
 */
export async function getAdminCategories(): Promise<AdminCategory[]> {
  return api.get<AdminCategory[]>('/api/categories')
}

/**
 * Create category
 * Backend endpoint: POST /api/categories
 */
export async function createCategory(data: CategoryCreateInput): Promise<AdminCategory> {
  // Convert status to active boolean
  const payload = {
    name: data.name,
    slug: data.slug,
    description: data.description,
    active: data.status === 'active',
    parentId: data.parentId ? parseInt(data.parentId) : null,
  }
  return api.post<AdminCategory>('/api/categories', payload)
}

/**
 * Update category
 * Backend endpoint: PUT /api/categories/{id}
 */
export async function updateCategory(id: string, data: Partial<CategoryCreateInput>): Promise<AdminCategory> {
  // Convert status to active boolean if present
  const payload: any = {}
  if (data.name !== undefined) payload.name = data.name
  if (data.slug !== undefined) payload.slug = data.slug
  if (data.description !== undefined) payload.description = data.description
  if (data.status !== undefined) payload.active = data.status === 'active'
  if (data.parentId !== undefined) payload.parentId = data.parentId ? parseInt(data.parentId) : null
  
  return api.put<AdminCategory>(`/api/categories/${id}`, payload)
}

/**
 * Delete category
 * Backend endpoint: DELETE /api/categories/{id}
 */
export async function deleteCategory(id: string): Promise<void> {
  return api.delete<void>(`/api/categories/${id}`)
}

// ============================================================================
// ORDERS
// ============================================================================

export interface AdminOrder {
  id: string
  orderNumber: string
  customer: {
    id: string
    name: string
    email: string
  }
  items: {
    id: string
    productId: string
    productName: string
    quantity: number
    price: number
  }[]
  total: number
  currency: string
  status: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled'
  paymentStatus: 'pending' | 'paid' | 'refunded'
  shippingAddress: {
    street: string
    city: string
    state: string
    country: string
    postalCode: string
  }
  createdAt: string
  updatedAt: string
}

export interface OrdersListParams {
  page?: number
  limit?: number
  status?: string
  search?: string
  sortBy?: 'createdAt' | 'total' | 'status'
  sortOrder?: 'asc' | 'desc'
}

export interface OrdersListResponse {
  orders: AdminOrder[]
  total: number
  page: number
  limit: number
  totalPages: number
}

/**
 * Get orders list
 */
export async function getAdminOrders(params: OrdersListParams = {}): Promise<OrdersListResponse> {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) searchParams.append(key, value.toString())
  })
  
  return api.get<OrdersListResponse>(`/admin/orders?${searchParams.toString()}`)
}

/**
 * Get single order
 */
export async function getAdminOrder(id: string): Promise<AdminOrder> {
  return api.get<AdminOrder>(`/admin/orders/${id}`)
}

/**
 * Update order status
 */
export async function updateOrderStatus(
  id: string,
  status: AdminOrder['status']
): Promise<AdminOrder> {
  return api.patch<AdminOrder>(`/admin/orders/${id}/status`, { status })
}

// ============================================================================
// CUSTOMERS
// ============================================================================

export interface AdminCustomer {
  id: string
  name: string
  email: string
  phone?: string
  role: 'customer'
  totalOrders: number
  totalSpent: number
  createdAt: string
  lastOrderAt?: string
  status: 'active' | 'blocked'
}

export interface CustomersListParams {
  page?: number
  limit?: number
  search?: string
  status?: 'active' | 'blocked'
  sortBy?: 'name' | 'totalSpent' | 'totalOrders' | 'createdAt'
  sortOrder?: 'asc' | 'desc'
}

export interface CustomersListResponse {
  customers: AdminCustomer[]
  total: number
  page: number
  limit: number
  totalPages: number
}

/**
 * Get customers list
 */
export async function getAdminCustomers(params: CustomersListParams = {}): Promise<CustomersListResponse> {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) searchParams.append(key, value.toString())
  })
  
  return api.get<CustomersListResponse>(`/admin/customers?${searchParams.toString()}`)
}

/**
 * Get single customer
 */
export async function getAdminCustomer(id: string): Promise<AdminCustomer> {
  return api.get<AdminCustomer>(`/admin/customers/${id}`)
}

/**
 * Update customer status
 */
export async function updateCustomerStatus(
  id: string,
  status: 'active' | 'blocked'
): Promise<AdminCustomer> {
  return api.patch<AdminCustomer>(`/admin/customers/${id}/status`, { status })
}

// ============================================================================
// DASHBOARD STATS
// ============================================================================

export interface DashboardStats {
  totalRevenue: number
  totalOrders: number
  totalCustomers: number
  totalProducts: number
  recentOrders: AdminOrder[]
  lowStockProducts: AdminProduct[]
  revenueByMonth: {
    month: string
    revenue: number
  }[]
}

/**
 * Get dashboard stats
 * Note: Backend endpoint may not exist yet - this will gracefully fail
 * TODO: Update endpoint to match backend when available
 */
export async function getDashboardStats(): Promise<DashboardStats> {
  try {
    // Try the admin endpoint first
    return await api.get<DashboardStats>('/api/admin/dashboard/stats')
  } catch (error: any) {
    // If endpoint doesn't exist, return default stats
    if (error.status === 404 || error.message?.includes('No static resource')) {
      console.warn('Dashboard stats endpoint not available, returning default stats')
      return {
        totalRevenue: 0,
        totalOrders: 0,
        totalCustomers: 0,
        totalProducts: 0,
        recentOrders: [],
        lowStockProducts: [],
        revenueByMonth: [],
      }
    }
    throw error
  }
}


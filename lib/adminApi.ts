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
  stockQuantity?: number
  costPrice?: number
  profitMargin?: number
  sku: string
  status: 'active' | 'draft' | 'archived'
  categoryId?: number
  active?: boolean
  featured?: boolean
  imageUrl?: string
  variants?: any[]
}

// Backend ProductDTO structure
export interface ProductCreateInput {
  name: string // Required, 2-200 chars
  description?: string // Optional, max 1000 chars
  price: number // Required, BigDecimal
  stockQuantity?: number // Optional, default 0
  // Optional on create: backend auto-generates a stable SKU if blank/omitted.
  sku?: string // Optional, 3-50 chars if provided, unique
  imageUrl?: string // Optional
  active?: boolean // Optional, default true
  featured?: boolean // Optional, default false
  quoteOnly?: boolean // Optional, default false (true for new CARS products)
  weight?: number // Optional, BigDecimal
  brand?: string // Optional, max 100 chars
  productType?: string
  condition?: string
  oemType?: string
  compatibilityMode?: string
  compatibleMakes?: string[]
  compatibleModels?: string[]
  compatibleYearStart?: number
  compatibleYearEnd?: number
  vinCompatible?: boolean
  make?: string
  model?: string
  year?: number
  mileage?: number
  fuelType?: string
  transmission?: string
  bodyType?: string
  driveType?: string
  powerKw?: number
  color?: string
  warrantyIncluded?: boolean
  partCategory?: string
  partNumber?: string
  partPosition?: string[]
  material?: string
  reconditioned?: boolean
  toolCategory?: string
  powerSource?: string
  voltage?: number
  torqueMinNm?: number
  torqueMaxNm?: number
  driveSize?: string
  professionalGrade?: boolean
  isKit?: boolean
  customCategory?: string
  styleTags?: string[]
  finish?: string
  streetLegal?: boolean
  installationDifficulty?: string
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
  imageUrl?: string // Optional, variant-specific image
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
  rootCategoryId?: number  // Filter products by topic (root category)
  topic?: string           // Topic slug (cars, parts, tools, custom)
  condition?: string
  productType?: string
  make?: string
  model?: string
  yearMin?: number
  yearMax?: number
}

export interface ProductsListResponse {
  products: AdminProduct[]
  total: number
  page: number
  limit: number
  totalPages: number
}

function parseBooleanFlag(value: unknown): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value === 1
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase()
    return normalized === 'true' || normalized === '1' || normalized === 'yes'
  }
  return false
}

function mapBackendProductToAdminProduct(p: any): AdminProduct {
  const stockQuantity = p.stockQuantity ?? p.stock_level ?? 0
  const availability = stockQuantity > 0 ? 'in_stock' : 'out_of_stock'
  const images = p.images?.map((img: any) => img.imageUrl || img.url || '') || [p.imageUrl].filter(Boolean)
  const quoteOnly = parseBooleanFlag(p.quoteOnly ?? p.quote_only)

  return {
    id: p.id?.toString() || '',
    name: p.name || '',
    description: p.description || '',
    price: typeof p.price === 'number' ? p.price : parseFloat(p.price) || 0,
    currency: 'USD',
    availability,
    quoteOnly,
    images,
    category: p.categoryId?.toString() || p.category?.toString() || '',
    categoryName: p.categoryName || '',
    sku: p.sku || '',
    stockLevel: stockQuantity,
    status: p.active ? 'active' : 'draft',
    slug: p.slug || p.sku?.toLowerCase().replace(/\s+/g, '-') || '',
    brand: p.brand || '',
    manufacturer: p.brand || p.manufacturer || '',
    productType: p.productType,
    condition: p.condition,
    oemType: p.oemType,
    compatibilityMode: p.compatibilityMode,
    compatibleMakes: p.compatibleMakes,
    compatibleModels: p.compatibleModels,
    compatibleYearStart: p.compatibleYearStart,
    compatibleYearEnd: p.compatibleYearEnd,
    vinCompatible: p.vinCompatible,
    make: p.make,
    model: p.model,
    year: p.year,
    mileage: p.mileage,
    fuelType: p.fuelType,
    transmission: p.transmission,
    bodyType: p.bodyType,
    driveType: p.driveType,
    powerKw: p.powerKw,
    color: p.color,
    warrantyIncluded: p.warrantyIncluded,
    partCategory: p.partCategory,
    partNumber: p.partNumber,
    partPosition: p.partPosition,
    material: p.material,
    reconditioned: p.reconditioned,
    toolCategory: p.toolCategory,
    powerSource: p.powerSource,
    voltage: p.voltage,
    torqueMinNm: p.torqueMinNm,
    torqueMaxNm: p.torqueMaxNm,
    driveSize: p.driveSize,
    professionalGrade: p.professionalGrade,
    isKit: p.isKit,
    customCategory: p.customCategory,
    styleTags: p.styleTags,
    finish: p.finish,
    streetLegal: p.streetLegal,
    installationDifficulty: p.installationDifficulty,
    weight: typeof p.weight === 'number' ? p.weight : p.weight ? parseFloat(p.weight) : undefined,
    createdAt: p.createdDate || new Date().toISOString(),
    updatedAt: p.updatedDate || new Date().toISOString(),
    // Keep backend fields used by admin edit pages
    active: p.active ?? true,
    featured: p.featured ?? false,
    categoryId: p.categoryId,
    imageUrl: p.imageUrl,
    variants: p.variants || [],
    infoSection1Title: p.infoSection1Title,
    infoSection1Content: p.infoSection1Content,
    infoSection1Enabled: p.infoSection1Enabled,
    infoSection2Title: p.infoSection2Title,
    infoSection2Content: p.infoSection2Content,
    infoSection2Enabled: p.infoSection2Enabled,
    infoSection3Title: p.infoSection3Title,
    infoSection3Content: p.infoSection3Content,
    infoSection3Enabled: p.infoSection3Enabled,
    infoSection4Title: p.infoSection4Title,
    infoSection4Content: p.infoSection4Content,
    infoSection4Enabled: p.infoSection4Enabled,
    infoSection5Title: p.infoSection5Title,
    infoSection5Content: p.infoSection5Content,
    infoSection5Enabled: p.infoSection5Enabled,
    infoSection6Title: p.infoSection6Title,
    infoSection6Content: p.infoSection6Content,
    infoSection6Enabled: p.infoSection6Enabled,
    infoSection7Title: p.infoSection7Title,
    infoSection7Content: p.infoSection7Content,
    infoSection7Enabled: p.infoSection7Enabled,
    infoSection8Title: p.infoSection8Title,
    infoSection8Content: p.infoSection8Content,
    infoSection8Enabled: p.infoSection8Enabled,
    infoSection9Title: p.infoSection9Title,
    infoSection9Content: p.infoSection9Content,
    infoSection9Enabled: p.infoSection9Enabled,
    infoSection10Title: p.infoSection10Title,
    infoSection10Content: p.infoSection10Content,
    infoSection10Enabled: p.infoSection10Enabled,
  } as AdminProduct
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
  if (params.condition) searchParams.append('condition', params.condition)
  if (params.productType) searchParams.append('productType', params.productType)
  if (params.make) searchParams.append('make', params.make)
  if (params.model) searchParams.append('model', params.model)
  if (params.yearMin !== undefined) searchParams.append('yearMin', params.yearMin.toString())
  if (params.yearMax !== undefined) searchParams.append('yearMax', params.yearMax.toString())
  // Topic filtering: pass rootCategoryId to filter products by topic
  if (params.rootCategoryId) {
    searchParams.append('rootCategoryId', params.rootCategoryId.toString())
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
  const products: AdminProduct[] = (backendResponse.content || []).map(mapBackendProductToAdminProduct)

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
  const product = await api.get<any>(`/api/products/${id}`)
  return mapBackendProductToAdminProduct(product)
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
  console.log('Creating variant for product', productId, 'with data:', JSON.stringify(data, null, 2))
  try {
    const response = await api.post<any>(`/api/products/${productId}/variants`, data)
    console.log('Variant creation response:', response)
    return response
  } catch (error) {
    console.error('Variant creation error:', error)
    throw error
  }
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

export interface AdminProductImage {
  id: string
  imageUrl: string
  position: number
  isMain: boolean
  createdDate?: string
  updatedDate?: string
}

export interface ImagePositionInput {
  imageId: string
  position: number
}

/**
 * Get product images (admin view)
 * Backend endpoint: GET /api/products/{productId}/images
 */
export async function getProductImages(productId: string): Promise<AdminProductImage[]> {
  const images = await api.get<any[]>(`/api/products/${productId}/images`)
  return (images || []).map((img: any) => ({
    id: img.id?.toString() || '',
    imageUrl: img.imageUrl || img.url || '',
    position: typeof img.position === 'number' ? img.position : Number.parseInt(img.position, 10) || 0,
    isMain: parseBooleanFlag(img.isMain),
    createdDate: img.createdDate,
    updatedDate: img.updatedDate,
  }))
}

/**
 * Reorder product images
 * Backend endpoint: PUT /api/products/{productId}/images/reorder
 */
export async function reorderProductImages(productId: string, order: ImagePositionInput[]): Promise<AdminProductImage[]> {
  const payload = order.map((o) => ({ imageId: Number.parseInt(o.imageId, 10), position: o.position }))
  const images = await api.put<any[]>(`/api/products/${productId}/images/reorder`, payload)
  return (images || []).map((img: any) => ({
    id: img.id?.toString() || '',
    imageUrl: img.imageUrl || img.url || '',
    position: typeof img.position === 'number' ? img.position : Number.parseInt(img.position, 10) || 0,
    isMain: parseBooleanFlag(img.isMain),
    createdDate: img.createdDate,
    updatedDate: img.updatedDate,
  }))
}

/**
 * Replace a product image file (keeps position and isMain)
 * Backend endpoint: PUT /api/products/{productId}/images/{imageId}/replace
 */
export async function replaceProductImage(productId: string, imageId: string, file: File): Promise<AdminProductImage> {
  const formData = new FormData()
  formData.append('file', file)

  const { getAccessToken } = await import('./apiClient')
  const token = getAccessToken()
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'

  const response = await fetch(`${baseUrl}/api/products/${productId}/images/${imageId}/replace`, {
    method: 'PUT',
    headers: token ? {
      'Authorization': `Bearer ${token}`,
    } : {},
    body: formData,
    credentials: 'include',
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(`Failed to replace image: ${error}`)
  }

  const img = await response.json()
  return {
    id: img.id?.toString() || '',
    imageUrl: img.imageUrl || img.url || '',
    position: typeof img.position === 'number' ? img.position : Number.parseInt(img.position, 10) || 0,
    isMain: parseBooleanFlag(img.isMain),
    createdDate: img.createdDate,
    updatedDate: img.updatedDate,
  }
}

/**
 * Upload variant image
 * Backend endpoint: POST /api/products/{productId}/variants/{variantId}/image
 */
export async function uploadVariantImage(productId: string, variantId: string, file: File): Promise<any> {
  const formData = new FormData()
  formData.append('file', file)

  const { getAccessToken } = await import('./apiClient')
  const token = getAccessToken()
  const baseUrl = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
  const response = await fetch(`${baseUrl}/api/products/${productId}/variants/${variantId}/image`, {
    method: 'POST',
    headers: token ? {
      'Authorization': `Bearer ${token}`,
    } : {},
    body: formData,
    credentials: 'include',
  })

  if (!response.ok) {
    const error = await response.text()
    throw new Error(`Failed to upload variant image: ${error}`)
  }

  return response.json()
}

/**
 * Update product
 */
export async function updateProduct(id: string, data: Partial<ProductCreateInput>): Promise<AdminProduct> {
  return api.put<AdminProduct>(`/api/products/${id}`, data)
}

/**
 * Delete product image
 * Backend endpoint: DELETE /api/products/{productId}/images/{imageId}
 */
export async function deleteProductImage(productId: string, imageId: string): Promise<void> {
  return api.delete<void>(`/api/products/${productId}/images/${imageId}`)
}

/**
 * Set main product image
 * Backend endpoint: PUT /api/products/{productId}/images/{imageId}/main
 */
export async function setMainProductImage(productId: string, imageId: string): Promise<void> {
  return api.put<void>(`/api/products/${productId}/images/${imageId}/main`, {})
}

/**
 * Update product variant
 * Backend endpoint: PUT /api/products/{productId}/variants/{variantId}
 */
export async function updateProductVariant(productId: string, variantId: string, data: Partial<ProductVariantCreateInput>): Promise<any> {
  return api.put<any>(`/api/products/${productId}/variants/${variantId}`, data)
}

/**
 * Delete product variant
 * Backend endpoint: DELETE /api/products/{productId}/variants/{variantId}
 */
export async function deleteProductVariant(productId: string, variantId: string): Promise<void> {
  return api.delete<void>(`/api/products/${productId}/variants/${variantId}`)
}

/**
 * Delete product
 */
export async function deleteProduct(id: string): Promise<void> {
  return api.delete<void>(`/api/products/${id}`)
}

/**
 * Update product stock
 */
export async function updateProductStock(id: string, stockLevel: number): Promise<AdminProduct> {
  return api.patch<AdminProduct>(`/api/products/${id}/stock`, { stockLevel })
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
  parentId?: string | number | null  // Parent category ID for topic hierarchy
}

/**
 * Get all categories (admin view)
 * Backend endpoint: GET /api/categories
 */
export async function getAdminCategories(): Promise<AdminCategory[]> {
  const cats = await api.get<any[]>('/api/categories')
  return (cats || []).map((c: any) => ({
    id: typeof c.id === 'number' ? c.id : Number.parseInt(String(c.id || '0'), 10),
    slug: c.slug || '',
    name: c.name || '',
    description: c.description || undefined,
    imageUrl: c.imageUrl || undefined,
    productCount: c.productCount ?? undefined,
    parentId: c.parentId ?? c.parent_id ?? c.parent?.id ?? undefined,
    children: c.children ?? undefined,
    status: (c.active === false ? 'inactive' : 'active') as AdminCategory['status'],
    createdAt: c.createdDate || c.createdAt || new Date().toISOString(),
    updatedAt: c.updatedDate || c.updatedAt || c.createdDate || new Date().toISOString(),
  }))
}

/**
 * Get root categories (topics: cars, parts, tools, custom)
 * Backend endpoint: GET /api/categories/root
 */
export async function getRootCategories(): Promise<Category[]> {
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
  const response = await fetch(`${API_BASE_URL}/api/categories/root`, {
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  })

  if (!response.ok) {
    console.warn(`Root categories endpoint returned ${response.status}, falling back to slug filter`)
    // Fallback: get all categories and filter root ones by slug
    const allCategories = await getAllCategories()
    return allCategories.filter(c => ['cars', 'parts', 'tools', 'custom'].includes(c.slug))
  }

  return response.json()
}

/**
 * Get category by slug
 * Backend endpoint: GET /api/categories/slug/{slug}
 */
export async function getCategoryBySlug(slug: string): Promise<Category | null> {
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
  try {
    const response = await fetch(`${API_BASE_URL}/api/categories/slug/${slug}`, {
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
    })

    if (!response.ok) {
      return null
    }

    return response.json()
  } catch (error) {
    console.error(`Failed to fetch category by slug ${slug}:`, error)
    return null
  }
}

/**
 * Get categories filtered by topic (root category)
 * Returns all categories that are descendants of the specified root category
 */
export async function getCategoriesByTopic(topicSlug: string): Promise<Category[]> {
  // First get the root category ID for the topic
  const rootCategory = await getCategoryBySlug(topicSlug)
  if (!rootCategory) {
    console.warn(`Topic category not found: ${topicSlug}`)
    return []
  }

  // Get all categories and filter to only those in this topic's subtree
  const allCategories = await getAllCategories()

  // Build a map of category IDs to their parent IDs
  const categoryMap = new Map<number, Category>()
  allCategories.forEach(c => categoryMap.set(c.id, c))

  // Check if a category is a descendant of the root
  function isDescendantOf(category: Category, rootId: number): boolean {
    if (category.id === rootId) return true
    if (!category.parentId) return false
    const parent = categoryMap.get(category.parentId)
    if (!parent) return false
    return isDescendantOf(parent, rootId)
  }

  // Filter categories that belong to this topic
  return allCategories.filter(c => isDescendantOf(c, rootCategory.id))
}

/**
 * Create category
 * Backend endpoint: POST /api/categories
 */
export async function createCategory(data: CategoryCreateInput): Promise<AdminCategory> {
  // Convert status to active boolean
  const resolvedParentId =
    data.parentId === null || data.parentId === undefined || data.parentId === ''
      ? null
      : (typeof data.parentId === 'string' ? Number.parseInt(data.parentId, 10) : data.parentId)
  const payload = {
    name: data.name,
    slug: data.slug,
    description: data.description,
    active: data.status === 'active',
    parentId: resolvedParentId,
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
  if (data.parentId !== undefined) {
    payload.parentId =
      data.parentId === null || data.parentId === '' || data.parentId === undefined
        ? null
        : (typeof data.parentId === 'string' ? Number.parseInt(data.parentId, 10) : data.parentId)
  }

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
  status:
  | 'pending'
  | 'confirmed'
  | 'checkout_created'
  | 'paid'
  | 'failed'
  | 'processing'
  | 'shipped'
  | 'delivered'
  | 'cancelled'
  | 'refunded'
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
  const page = params.page ? Math.max(params.page - 1, 0) : 0
  const size = params.limit ?? 20

  searchParams.set('page', page.toString())
  searchParams.set('size', size.toString())
  if (params.status) searchParams.set('status', params.status)
  if (params.search) searchParams.set('search', params.search)

  const response = await api.get<any>(`/api/orders/owner?${searchParams.toString()}`)

  const orders: AdminOrder[] = (response.content || []).map((order: any) => ({
    id: String(order.id),
    orderNumber: order.orderNumber,
    customer: {
      id: String(order.userId ?? ''),
      name: order.username || 'Customer',
      email: order.username || '',
    },
    items: (order.orderItems || []).map((item: any) => ({
      id: String(item.id),
      productId: String(item.productId),
      productName: item.productName,
      quantity: item.quantity,
      price: Number(item.unitPrice),
    })),
    total: Number(order.totalAmount ?? 0),
    currency: (order.currency || 'eur').toUpperCase(),
    status: (String(order.status || 'pending').toLowerCase() as AdminOrder['status']),
    paymentStatus: order.paymentIntentId ? 'paid' : 'pending',
    shippingAddress: {
      street: order.shippingAddress || '',
      city: order.shippingCity || '',
      state: '',
      country: order.shippingCountry || '',
      postalCode: order.shippingPostalCode || '',
    },
    createdAt: order.createdDate || order.orderDate || new Date().toISOString(),
    updatedAt: order.updatedDate || order.createdDate || new Date().toISOString(),
  }))

  return {
    orders,
    total: response.totalElements ?? orders.length,
    page: (response.number ?? 0) + 1,
    limit: response.size ?? size,
    totalPages: response.totalPages ?? 1,
  }
}

/**
 * Get single order
 */
export async function getAdminOrder(id: string): Promise<AdminOrder> {
  const order = await api.get<any>(`/api/orders/owner/orders/${id}`)
  return {
    id: String(order.id),
    orderNumber: order.orderNumber,
    customer: {
      id: String(order.userId ?? ''),
      name: order.username || 'Customer',
      email: order.username || '',
    },
    items: (order.orderItems || []).map((item: any) => ({
      id: String(item.id),
      productId: String(item.productId),
      productName: item.productName,
      quantity: item.quantity,
      price: Number(item.unitPrice),
    })),
    total: Number(order.totalAmount ?? 0),
    currency: (order.currency || 'eur').toUpperCase(),
    status: (String(order.status || 'pending').toLowerCase() as AdminOrder['status']),
    paymentStatus: order.paymentIntentId ? 'paid' : 'pending',
    shippingAddress: {
      street: order.shippingAddress || '',
      city: order.shippingCity || '',
      state: '',
      country: order.shippingCountry || '',
      postalCode: order.shippingPostalCode || '',
    },
    createdAt: order.createdDate || order.orderDate || new Date().toISOString(),
    updatedAt: order.updatedDate || order.createdDate || new Date().toISOString(),
  }
}

/**
 * Update order status
 */
export async function updateOrderStatus(
  id: string,
  status: AdminOrder['status']
): Promise<AdminOrder> {
  const order = await api.patch<any>(`/api/orders/owner/orders/${id}/status`, { status: status.toUpperCase() })
  return {
    id: String(order.id),
    orderNumber: order.orderNumber,
    customer: {
      id: String(order.userId ?? ''),
      name: order.username || 'Customer',
      email: order.username || '',
    },
    items: (order.orderItems || []).map((item: any) => ({
      id: String(item.id),
      productId: String(item.productId),
      productName: item.productName,
      quantity: item.quantity,
      price: Number(item.unitPrice),
    })),
    total: Number(order.totalAmount ?? 0),
    currency: (order.currency || 'eur').toUpperCase(),
    status: (String(order.status || 'pending').toLowerCase() as AdminOrder['status']),
    paymentStatus: order.paymentIntentId ? 'paid' : 'pending',
    shippingAddress: {
      street: order.shippingAddress || '',
      city: order.shippingCity || '',
      state: '',
      country: order.shippingCountry || '',
      postalCode: order.shippingPostalCode || '',
    },
    createdAt: order.createdDate || order.orderDate || new Date().toISOString(),
    updatedAt: order.updatedDate || order.createdDate || new Date().toISOString(),
  }
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
  const page = params.page ? Math.max(params.page - 1, 0) : 0
  const size = params.limit ?? 20

  searchParams.set('page', page.toString())
  searchParams.set('size', size.toString())
  if (params.search) searchParams.set('search', params.search)
  if (params.status) searchParams.set('status', params.status)

  const response = await api.get<any>(`/api/owner/customers?${searchParams.toString()}`)

  const customers: AdminCustomer[] = (response.content || []).map((c: any) => ({
    id: String(c.id),
    name: c.name || c.email,
    email: c.email,
    phone: undefined,
    role: 'customer',
    totalOrders: Number(c.totalOrders ?? 0),
    totalSpent: Number(c.totalSpent ?? 0),
    createdAt: c.createdAt || new Date().toISOString(),
    lastOrderAt: c.lastOrderAt || undefined,
    status: c.status === 'blocked' ? 'blocked' : 'active',
  }))

  return {
    customers,
    total: response.totalElements ?? customers.length,
    page: (response.number ?? 0) + 1,
    limit: response.size ?? size,
    totalPages: response.totalPages ?? 1,
  }
}

/**
 * Get single customer
 */
export async function getAdminCustomer(id: string): Promise<AdminCustomer> {
  const c = await api.get<any>(`/api/owner/customers/${id}`)
  return {
    id: String(c.id),
    name: c.name || c.email,
    email: c.email,
    phone: undefined,
    role: 'customer',
    totalOrders: Number(c.totalOrders ?? 0),
    totalSpent: Number(c.totalSpent ?? 0),
    createdAt: c.createdAt || new Date().toISOString(),
    lastOrderAt: c.lastOrderAt || undefined,
    status: c.status === 'blocked' ? 'blocked' : 'active',
  }
}

/**
 * Update customer status
 */
export async function updateCustomerStatus(
  id: string,
  status: 'active' | 'blocked'
): Promise<AdminCustomer> {
  const c = await api.patch<any>(`/api/owner/customers/${id}/status`, { status })
  return {
    id: String(c.id),
    name: c.name || c.email,
    email: c.email,
    phone: undefined,
    role: 'customer',
    totalOrders: Number(c.totalOrders ?? 0),
    totalSpent: Number(c.totalSpent ?? 0),
    createdAt: c.createdAt || new Date().toISOString(),
    lastOrderAt: c.lastOrderAt || undefined,
    status: c.status === 'blocked' ? 'blocked' : 'active',
  }
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
    const data = await api.get<any>('/api/admin/dashboard/stats')

    const recentOrders: AdminOrder[] = (data.recentOrders || []).map((order: any) => ({
      id: String(order.id),
      orderNumber: order.orderNumber,
      customer: {
        id: String(order.userId ?? ''),
        name: order.username || 'Customer',
        email: order.username || '',
      },
      items: (order.orderItems || []).map((item: any) => ({
        id: String(item.id),
        productId: String(item.productId),
        productName: item.productName,
        quantity: item.quantity,
        price: Number(item.unitPrice),
      })),
      total: Number(order.totalAmount ?? 0),
      currency: (order.currency || 'eur').toUpperCase(),
      status: (String(order.status || 'pending').toLowerCase() as AdminOrder['status']),
      paymentStatus: order.paymentIntentId ? 'paid' : 'pending',
      shippingAddress: {
        street: order.shippingAddress || '',
        city: order.shippingCity || '',
        state: '',
        country: order.shippingCountry || '',
        postalCode: order.shippingPostalCode || '',
      },
      createdAt: order.createdDate || order.orderDate || new Date().toISOString(),
      updatedAt: order.updatedDate || order.createdDate || new Date().toISOString(),
    }))

    const lowStockProducts: AdminProduct[] = (data.lowStockProducts || []).map((p: any) => ({
      id: String(p.id),
      slug: '',
      name: p.name,
      description: '',
      shortDescription: undefined,
      price: 0,
      currency: 'USD',
      availability: 'in_stock',
      availabilityText: undefined,
      images: [],
      category: '',
      categoryName: undefined,
      partNumber: undefined,
      manufacturer: undefined,
      specifications: undefined,
      compatibleVehicles: undefined,
      leadTime: undefined,
      minQuantity: undefined,
      stockLevel: p.stockLevel ?? 0,
      createdAt: undefined,
      updatedAt: undefined,
      costPrice: undefined,
      profitMargin: undefined,
      sku: p.sku,
      status: 'active',
    }))

    return {
      totalRevenue: Number(data.totalRevenue ?? 0),
      totalOrders: Number(data.totalOrders ?? 0),
      totalCustomers: Number(data.totalCustomers ?? 0),
      totalProducts: Number(data.totalProducts ?? 0),
      recentOrders,
      lowStockProducts,
      revenueByMonth: data.revenueByMonth ?? [],
    }
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

// ============================================================================
// LOAD TEST TOOLS (Admin)
// ============================================================================

export interface LoadTestCreateProductsRequest {
  count?: number
}

export interface LoadTestCreateProductsResponse {
  runId: string
  countRequested: number
  countCreated: number
  skuPrefix: string
  sampleSkus: string[]
}

export interface LoadTestDeleteProductsResponse {
  runId: string
  hardDeleteRequested: boolean
  matched: number
  hardDeleted: number
  softDeleted: number
  failed: number
}

export async function loadTestCreateProducts(
  request: LoadTestCreateProductsRequest = {}
): Promise<LoadTestCreateProductsResponse> {
  return api.post<LoadTestCreateProductsResponse>('/api/admin/tools/loadtest/products', request)
}

export async function loadTestDeleteProducts(
  runId: string,
  hardDelete: boolean = false
): Promise<LoadTestDeleteProductsResponse> {
  const params = new URLSearchParams({ runId, hardDelete: hardDelete ? 'true' : 'false' })
  return api.delete<LoadTestDeleteProductsResponse>(`/api/admin/tools/loadtest/products?${params.toString()}`)
}

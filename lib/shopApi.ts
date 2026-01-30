/**
 * Shop API Integration Layer
 * 
 * Base URL should be set in environment variable: NEXT_PUBLIC_SHOP_API_BASE_URL
 * Example: https://api.yourdomain.com or http://localhost:8080
 * 
 * API endpoints (adjust as needed):
 * - GET /products?query=&category=&page=&limit=
 * - GET /products/:slug
 * - GET /categories
 */

import { apiRequest } from './apiClient'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'

export interface ProductVariant {
  id: number
  name: string
  sku: string
  price?: number
  stockQuantity: number
  active: boolean
  defaultVariant: boolean
  position: number
  options: Record<string, string>
  createdDate?: string
  updatedDate?: string
}

export interface Product {
  id: string
  slug: string
  name: string
  description: string
  shortDescription?: string
  price: number
  currency: string
  availability: 'in_stock' | 'low_stock' | 'out_of_stock' | 'pre_order'
  availabilityText?: string
  images: string[]
  category: string
  categoryName?: string
  partNumber?: string
  manufacturer?: string
  specifications?: Record<string, string>
  compatibleVehicles?: string[]
  leadTime?: string
  minQuantity?: number
  stockLevel?: number
  variants?: ProductVariant[]
  createdAt?: string
  updatedAt?: string
}

export interface Category {
  id: string
  slug: string
  name: string
  description?: string
  productCount?: number
  parentId?: string
}

export interface ProductsResponse {
  products: Product[]
  total: number
  page: number
  limit: number
  totalPages: number
}

export interface SearchParams {
  query?: string
  category?: string
  page?: number
  limit?: number
  minPrice?: number
  maxPrice?: number
  availability?: string
  sortBy?: 'price_asc' | 'price_desc' | 'name_asc' | 'name_desc' | 'newest'
}

/**
 * Fetch products with filters and pagination
 */
export async function fetchProducts(params: SearchParams = {}): Promise<ProductsResponse> {
  const searchParams = new URLSearchParams()

  if (params.query) searchParams.append('query', params.query)
  if (params.category) searchParams.append('category', params.category)
  if (params.page) searchParams.append('page', params.page.toString())
  if (params.limit) searchParams.append('limit', params.limit.toString())
  if (params.minPrice) searchParams.append('minPrice', params.minPrice.toString())
  if (params.maxPrice) searchParams.append('maxPrice', params.maxPrice.toString())
  if (params.availability) searchParams.append('availability', params.availability)
  if (params.sortBy) searchParams.append('sortBy', params.sortBy)

  // Backend uses 0-based page numbers, frontend uses 1-based
  const backendPage = params.page ? (params.page - 1) : 0
  searchParams.set('page', backendPage.toString())
  searchParams.set('size', (params.limit || 12).toString())

  // Map frontend params to backend params
  if (params.category) {
    searchParams.set('category', params.category)
  }
  if (params.query) {
    searchParams.set('search', params.query)
  }
  if (params.sortBy) {
    const sortMap: Record<string, string> = {
      'price_asc': 'price',
      'price_desc': 'price',
      'name_asc': 'name',
      'name_desc': 'name',
      'newest': 'createdDate',
    }
    searchParams.set('sortBy', sortMap[params.sortBy] || 'createdDate')
    searchParams.set('sortDir', params.sortBy.includes('_desc') ? 'desc' : 'asc')
  }

  const url = `${API_BASE_URL}/api/products?${searchParams.toString()}`

  try {
    const response = await fetch(url, {
      next: { revalidate: 60 }, // Revalidate every 60 seconds
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit', // Shop endpoints are public - don't send auth cookies
    })

    if (!response.ok) {
      // Don't throw for 401/400 on public endpoints - return empty result
      if (response.status === 401 || response.status === 400) {
        console.warn(`Products endpoint returned ${response.status} - backend may require public endpoint configuration`)
        return {
          products: [],
          total: 0,
          page: params.page || 1,
          limit: params.limit || 12,
          totalPages: 0,
        }
      }
      throw new Error(`Failed to fetch products: ${response.status} ${response.statusText}`)
    }

    // Backend returns Spring Page format: { content, totalElements, totalPages, number, size }
    const backendData = await response.json()

    // Transform backend ProductDTO to frontend Product format
    const products: Product[] = (backendData.content || []).map((p: any) => {
      // Extract images from ProductImageResponse array or use imageUrl
      const images: string[] = []
      if (p.images && Array.isArray(p.images)) {
        images.push(...p.images.map((img: any) => img.imageUrl || img.url || '').filter(Boolean))
      }
      if (p.imageUrl && !images.includes(p.imageUrl)) {
        images.unshift(p.imageUrl)
      }

      // Determine availability
      const stockQty = p.stockQuantity || 0
      let availability: 'in_stock' | 'low_stock' | 'out_of_stock' | 'pre_order' = 'out_of_stock'
      if (stockQty > 10) {
        availability = 'in_stock'
      } else if (stockQty > 0) {
        availability = 'low_stock'
      }

      return {
        id: p.id?.toString() || '',
        slug: p.slug || p.sku?.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '') || p.id?.toString() || '',
        name: p.name || '',
        description: p.description || '',
        price: typeof p.price === 'number' ? p.price : parseFloat(p.price) || 0,
        currency: 'USD', // Default, adjust if backend provides currency
        availability,
        images: images.length > 0 ? images : [''],
        category: p.categoryId?.toString() || '',
        categoryName: p.categoryName || '',
        partNumber: p.sku || '',
        stockLevel: stockQty,
        createdAt: p.createdDate || new Date().toISOString(),
        updatedAt: p.updatedDate || new Date().toISOString(),
      }
    })

    // Transform Spring Page format to frontend format
    return {
      products,
      total: backendData.totalElements || 0,
      page: (backendData.number || 0) + 1, // Convert 0-based to 1-based
      limit: backendData.size || 12,
      totalPages: backendData.totalPages || 0,
    }
  } catch (error) {
    console.error('Error fetching products:', error)
    // Return empty result set instead of throwing to prevent UI breakage
    return {
      products: [],
      total: 0,
      page: params.page || 1,
      limit: params.limit || 12,
      totalPages: 0,
    }
  }
}

/**
 * Fetch a single product by slug (using SKU endpoint)
 */
export async function fetchProductBySlug(slug: string): Promise<Product | null> {
  const url = `${API_BASE_URL}/api/products/sku/${slug}`

  try {
    const response = await fetch(url, {
      next: { revalidate: 300 }, // Revalidate every 5 minutes
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit', // Shop endpoints are public - don't send auth cookies
    })

    if (!response.ok) {
      if (response.status === 404) {
        return null
      }
      // Don't throw for 401/400 on public endpoints
      if (response.status === 401 || response.status === 400) {
        console.warn(`Product endpoint returned ${response.status} - backend may require public endpoint configuration`)
        return null
      }
      throw new Error(`Failed to fetch product: ${response.status} ${response.statusText}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error(`Error fetching product ${slug}:`, error)
    return null
  }
}

/**
 * Fetch all categories
 */
export async function fetchCategories(): Promise<Category[]> {
  // Skip fetch if API URL is not configured
  if (!API_BASE_URL || API_BASE_URL === 'http://localhost:8080') {
    // Only skip in production if URL is still default
    if (typeof window === 'undefined') {
      return []
    }
  }

  const url = `${API_BASE_URL}/api/categories`

  try {
    const response = await fetch(url, {
      next: { revalidate: 300 }, // Revalidate every 5 minutes
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit', // Shop endpoints are public - don't send auth cookies
    })

    if (!response.ok) {
      // Don't throw for 401/400 on public endpoints - just return empty array
      if (response.status === 401 || response.status === 400) {
        console.warn(`Categories endpoint returned ${response.status} - backend may require public endpoint configuration`)
        return []
      }
      throw new Error(`Failed to fetch categories: ${response.status} ${response.statusText}`)
    }

    const data = await response.json()
    return data
  } catch (error) {
    console.error('Error fetching categories:', error)
    // Return empty array on error to allow UI to render
    return []
  }
}

/**
 * Format currency for display
 */
export function formatCurrency(amount: number, currency: string = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
  }).format(amount)
}

/**
 * Get availability badge variant
 */
export function getAvailabilityBadge(availability: Product['availability']): {
  label: string
  variant: 'default' | 'secondary' | 'destructive' | 'outline'
} {
  switch (availability) {
    case 'in_stock':
      return { label: 'In Stock', variant: 'default' }
    case 'low_stock':
      return { label: 'Low Stock', variant: 'secondary' }
    case 'out_of_stock':
      return { label: 'Out of Stock', variant: 'destructive' }
    case 'pre_order':
      return { label: 'Pre-Order', variant: 'outline' }
    default:
      return { label: 'Contact for Availability', variant: 'outline' }
  }
}

/**
 * Checkout API Functions
 */

export interface CreateOrderRequest {
  shippingAddress: string
  shippingCity: string
  shippingPostalCode: string
  shippingCountry: string
  shippingAmount?: number
  taxAmount?: number
}

export interface CreateOrderResponse {
  orderId: number
  orderNumber: string
  totalCents: number
  currency: string
}

export interface CreateSessionResponse {
  id: string
  url: string
}

/**
 * Create an order from the user's cart
 * This is the first step in the checkout process
 */
export async function createCheckoutOrder(
  orderData: CreateOrderRequest,
): Promise<CreateOrderResponse> {
  return apiRequest<CreateOrderResponse>('/api/checkout/create-order', {
    method: 'POST',
    body: JSON.stringify(orderData),
  })
}

/**
 * Create a Stripe Checkout Session for an existing order
 * Returns the session ID to redirect to Stripe hosted checkout
 */
export async function createCheckoutSession(orderId: string): Promise<CreateSessionResponse> {
  return apiRequest<CreateSessionResponse>('/api/checkout/create-session', {
    method: 'POST',
    body: JSON.stringify({ orderId }),
  })
}

export interface OrderSummary {
  id: number
  orderNumber: string
  status: string
  totalAmount: number
  currency: string
  stripeCheckoutSessionId?: string
}

export async function getOrder(orderId: number | string): Promise<OrderSummary> {
  return apiRequest<OrderSummary>(`/api/orders/${orderId}`)
}

export async function getOrderByCheckoutSession(sessionId: string): Promise<OrderSummary> {
  return apiRequest<OrderSummary>(`/api/orders/checkout-session/${sessionId}`)
}

export async function getLatestOrder(): Promise<OrderSummary> {
  return apiRequest<OrderSummary>('/api/orders/me/latest')
}

export async function finalizeCheckout(orderId: number | string, sessionId?: string): Promise<OrderSummary> {
  return apiRequest<OrderSummary>(`/api/orders/${orderId}/finalize`, {
    method: 'POST',
    body: sessionId ? JSON.stringify({ sessionId }) : undefined,
  })
}

/**
 * Sync cart items from localStorage to backend
 * This is needed before checkout since the backend creates orders from its own cart
 */
export async function syncCartToBackend(items: { productId: string, quantity: number }[]): Promise<void> {
  console.log('Syncing cart to backend:', items)

  // Try to clear backend cart first, but don't fail if it doesn't exist
  try {
    await apiRequest('/api/cart', { method: 'DELETE' })
    console.log('Backend cart cleared')
  } catch (error: any) {
    // Ignore errors - cart might not exist yet or be empty
    console.log('Cart clear skipped (may not exist):', error?.message)
  }

  // Add each item to backend cart
  for (const item of items) {
    console.log('Adding item to backend cart:', item)
    const result = await apiRequest('/api/cart/items', {
      method: 'POST',
      body: JSON.stringify({
        productId: parseInt(item.productId),
        quantity: item.quantity,
      }),
    })
    console.log('Item added to backend cart:', result)
  }

  console.log('Cart sync completed')
}

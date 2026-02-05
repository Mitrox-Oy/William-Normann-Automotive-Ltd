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

function parseBooleanFlag(value: unknown): boolean {
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') return value === 1
  if (typeof value === 'string') {
    const normalized = value.trim().toLowerCase()
    return normalized === 'true' || normalized === '1' || normalized === 'yes'
  }
  return false
}

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
  quoteOnly?: boolean
  images: string[]
  category: string
  categoryName?: string
  partNumber?: string
  manufacturer?: string
  brand?: string
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
  partNumberRaw?: string
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
  weight?: number
  specifications?: Record<string, string>
  compatibleVehicles?: string[]
  leadTime?: string
  minQuantity?: number
  stockLevel?: number
  variants?: ProductVariant[]
  infoSections?: Array<{ title: string; content: string }>
  createdAt?: string
  updatedAt?: string
}

export interface Category {
  id: number
  slug: string
  name: string
  description?: string
  imageUrl?: string
  productCount?: number
  parentId?: number
  children?: Category[]
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
  rootCategoryId?: number
  page?: number
  limit?: number
  minPrice?: number
  maxPrice?: number
  brand?: string
  condition?: string
  productType?: string
  make?: string
  model?: string
  yearMin?: number
  yearMax?: number
  mileageMin?: number
  mileageMax?: number
  fuelType?: string
  transmission?: string
  bodyType?: string
  driveType?: string
  powerMin?: number
  powerMax?: number
  warrantyIncluded?: boolean
  compatibilityMode?: string
  compatibleMake?: string
  compatibleModel?: string
  compatibleYear?: number
  oemType?: string
  partCategory?: string
  partNumber?: string
  partPosition?: string[]
  toolCategory?: string
  powerSource?: string
  voltageMin?: number
  voltageMax?: number
  torqueMin?: number
  torqueMax?: number
  driveSize?: string
  professionalGrade?: boolean
  isKit?: boolean
  styleTags?: string[]
  finish?: string
  streetLegal?: boolean
  installationDifficulty?: string
  customCategory?: string
  availability?: string
  sortBy?: 'price_asc' | 'price_desc' | 'name_asc' | 'name_desc' | 'newest'
}

// Valid topic slugs for the shop
export const SHOP_TOPICS = ['cars', 'parts', 'tools', 'custom'] as const
export type ShopTopic = typeof SHOP_TOPICS[number]

export function isValidTopic(slug: string): slug is ShopTopic {
  return SHOP_TOPICS.includes(slug as ShopTopic)
}

// Topic display information
export const TOPIC_INFO: Record<ShopTopic, { label: string; description: string; image: string }> = {
  cars: {
    label: 'CARS',
    description: 'Browse our selection of cars and vehicles',
    image: '/images/topics/cars.jpg'
  },
  parts: {
    label: 'PARTS',
    description: 'OEM and aftermarket automotive parts',
    image: '/images/topics/parts.jpg'
  },
  tools: {
    label: 'TOOLS',
    description: 'Professional automotive tools and equipment',
    image: '/images/topics/tools.jpg'
  },
  custom: {
    label: 'CUSTOM',
    description: 'Custom parts and modifications',
    image: '/images/topics/custom.jpg'
  }
}

export async function fetchBrands(rootCategoryId?: number): Promise<string[]> {
  let url = `${API_BASE_URL}/api/products/brands`
  if (rootCategoryId) {
    url += `?rootCategoryId=${rootCategoryId}`
  }

  try {
    const response = await fetch(url, {
      next: { revalidate: 60 },
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
    })

    if (!response.ok) {
      console.warn(`Brands endpoint returned ${response.status}`)
      return []
    }

    return await response.json()
  } catch (error) {
    console.error('Failed to fetch brands:', error)
    return []
  }
}

/**
 * Fetch products with filters and pagination
 */
export async function fetchProducts(params: SearchParams = {}): Promise<ProductsResponse> {
  const searchParams = new URLSearchParams()

  if (params.query) searchParams.append('query', params.query)
  if (params.page) searchParams.append('page', params.page.toString())
  if (params.limit) searchParams.append('limit', params.limit.toString())
  if (params.availability) searchParams.append('availability', params.availability)
  if (params.sortBy) searchParams.append('sortBy', params.sortBy)

  // Backend uses 0-based page numbers, frontend uses 1-based
  const backendPage = params.page ? (params.page - 1) : 0
  searchParams.set('page', backendPage.toString())
  searchParams.set('size', (params.limit || 12).toString())

  // Map frontend params to backend params
  const sortMap: Record<string, string> = {
    'price_asc': 'price',
    'price_desc': 'price',
    'name_asc': 'name',
    'name_desc': 'name',
    'newest': 'createdDate',
  }
  if (params.sortBy) {
    searchParams.set('sortBy', sortMap[params.sortBy] || 'createdDate')
    searchParams.set('sortDir', params.sortBy.includes('_desc') ? 'desc' : 'asc')
  }

  if (params.category) {
    searchParams.set('category', params.category)
  }
  if (params.rootCategoryId) {
    searchParams.set('rootCategoryId', params.rootCategoryId.toString())
  }
  if (params.query) {
    searchParams.set('search', params.query)
  }

  const hasMinPrice = params.minPrice !== undefined && Number.isFinite(params.minPrice)
  const hasMaxPrice = params.maxPrice !== undefined && Number.isFinite(params.maxPrice)
  if (hasMinPrice) {
    searchParams.set('minPrice', params.minPrice!.toString())
  }
  if (hasMaxPrice) {
    searchParams.set('maxPrice', params.maxPrice!.toString())
  }
  if (params.brand) {
    searchParams.set('brand', params.brand)
  }
  if (params.condition) searchParams.set('condition', params.condition)
  if (params.productType) searchParams.set('productType', params.productType)
  if (params.make) searchParams.set('make', params.make)
  if (params.model) searchParams.set('model', params.model)
  if (params.yearMin !== undefined) searchParams.set('yearMin', params.yearMin.toString())
  if (params.yearMax !== undefined) searchParams.set('yearMax', params.yearMax.toString())
  if (params.mileageMin !== undefined) searchParams.set('mileageMin', params.mileageMin.toString())
  if (params.mileageMax !== undefined) searchParams.set('mileageMax', params.mileageMax.toString())
  if (params.fuelType) searchParams.set('fuelType', params.fuelType)
  if (params.transmission) searchParams.set('transmission', params.transmission)
  if (params.bodyType) searchParams.set('bodyType', params.bodyType)
  if (params.driveType) searchParams.set('driveType', params.driveType)
  if (params.powerMin !== undefined) searchParams.set('powerMin', params.powerMin.toString())
  if (params.powerMax !== undefined) searchParams.set('powerMax', params.powerMax.toString())
  if (params.warrantyIncluded !== undefined) searchParams.set('warrantyIncluded', String(params.warrantyIncluded))
  if (params.compatibilityMode) searchParams.set('compatibilityMode', params.compatibilityMode)
  if (params.compatibleMake) searchParams.set('compatibleMake', params.compatibleMake)
  if (params.compatibleModel) searchParams.set('compatibleModel', params.compatibleModel)
  if (params.compatibleYear !== undefined) searchParams.set('compatibleYear', params.compatibleYear.toString())
  if (params.oemType) searchParams.set('oemType', params.oemType)
  if (params.partCategory) searchParams.set('partCategory', params.partCategory)
  if (params.partNumber) searchParams.set('partNumber', params.partNumber)
  if (params.partPosition && params.partPosition.length > 0) searchParams.set('partPosition', params.partPosition.join(','))
  if (params.toolCategory) searchParams.set('toolCategory', params.toolCategory)
  if (params.powerSource) searchParams.set('powerSource', params.powerSource)
  if (params.voltageMin !== undefined) searchParams.set('voltageMin', params.voltageMin.toString())
  if (params.voltageMax !== undefined) searchParams.set('voltageMax', params.voltageMax.toString())
  if (params.torqueMin !== undefined) searchParams.set('torqueMin', params.torqueMin.toString())
  if (params.torqueMax !== undefined) searchParams.set('torqueMax', params.torqueMax.toString())
  if (params.driveSize) searchParams.set('driveSize', params.driveSize)
  if (params.professionalGrade !== undefined) searchParams.set('professionalGrade', String(params.professionalGrade))
  if (params.isKit !== undefined) searchParams.set('isKit', String(params.isKit))
  if (params.styleTags && params.styleTags.length > 0) searchParams.set('styleTags', params.styleTags.join(','))
  if (params.finish) searchParams.set('finish', params.finish)
  if (params.streetLegal !== undefined) searchParams.set('streetLegal', String(params.streetLegal))
  if (params.installationDifficulty) searchParams.set('installationDifficulty', params.installationDifficulty)
  if (params.customCategory) searchParams.set('customCategory', params.customCategory)
  if (params.availability === 'in_stock') {
    searchParams.set('inStockOnly', 'true')
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
        quoteOnly: parseBooleanFlag((p as any).quoteOnly ?? (p as any).quote_only),
        images: images.length > 0 ? images : [''],
        category: p.categoryId?.toString() || '',
        categoryName: p.categoryName || '',
        partNumber: p.sku || '',
        manufacturer: p.brand || p.manufacturer || '',
        brand: p.brand || p.manufacturer || '',
        productType: p.productType || undefined,
        condition: p.condition || undefined,
        oemType: p.oemType || undefined,
        compatibilityMode: p.compatibilityMode || undefined,
        compatibleMakes: p.compatibleMakes || undefined,
        compatibleModels: p.compatibleModels || undefined,
        compatibleYearStart: p.compatibleYearStart ?? undefined,
        compatibleYearEnd: p.compatibleYearEnd ?? undefined,
        vinCompatible: parseBooleanFlag(p.vinCompatible),
        make: p.make || undefined,
        model: p.model || undefined,
        year: p.year ?? undefined,
        mileage: p.mileage ?? undefined,
        fuelType: p.fuelType || undefined,
        transmission: p.transmission || undefined,
        bodyType: p.bodyType || undefined,
        driveType: p.driveType || undefined,
        powerKw: p.powerKw ?? undefined,
        color: p.color || undefined,
        warrantyIncluded: p.warrantyIncluded ?? undefined,
        partCategory: p.partCategory || undefined,
        partNumberRaw: p.partNumber || undefined,
        partPosition: p.partPosition || undefined,
        material: p.material || undefined,
        reconditioned: p.reconditioned ?? undefined,
        toolCategory: p.toolCategory || undefined,
        powerSource: p.powerSource || undefined,
        voltage: p.voltage ?? undefined,
        torqueMinNm: p.torqueMinNm ?? undefined,
        torqueMaxNm: p.torqueMaxNm ?? undefined,
        driveSize: p.driveSize || undefined,
        professionalGrade: p.professionalGrade ?? undefined,
        isKit: p.isKit ?? undefined,
        customCategory: p.customCategory || undefined,
        styleTags: p.styleTags || undefined,
        finish: p.finish || undefined,
        streetLegal: p.streetLegal ?? undefined,
        installationDifficulty: p.installationDifficulty || undefined,
        weight: typeof p.weight === 'number' ? p.weight : p.weight ? parseFloat(p.weight) : undefined,
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

    const images: string[] = []
    if (data.images && Array.isArray(data.images)) {
      images.push(...data.images.map((img: any) => img.imageUrl || img.url || '').filter(Boolean))
    }
    if (data.imageUrl && !images.includes(data.imageUrl)) {
      images.unshift(data.imageUrl)
    }

    const stockQty = data.stockQuantity || 0
    let availability: 'in_stock' | 'low_stock' | 'out_of_stock' | 'pre_order' = 'out_of_stock'
    if (stockQty > 10) {
      availability = 'in_stock'
    } else if (stockQty > 0) {
      availability = 'low_stock'
    }

    const infoSections: Array<{ title: string; content: string }> = []
    for (let i = 1; i <= 10; i += 1) {
      const title = data[`infoSection${i}Title`]
      const content = data[`infoSection${i}Content`]
      const enabled = data[`infoSection${i}Enabled`]
      if (enabled && title && content) {
        infoSections.push({ title, content })
      }
    }

    return {
      id: data.id?.toString() || '',
      slug: data.slug || data.sku?.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '') || data.id?.toString() || '',
      name: data.name || '',
      description: data.description || '',
      shortDescription: data.shortDescription || '',
      price: typeof data.price === 'number' ? data.price : parseFloat(data.price) || 0,
      currency: 'USD',
      availability,
      quoteOnly: parseBooleanFlag((data as any).quoteOnly ?? (data as any).quote_only),
      images: images.length > 0 ? images : [''],
      category: data.categoryId?.toString() || '',
      categoryName: data.categoryName || '',
      partNumber: data.sku || '',
      manufacturer: data.brand || data.manufacturer || '',
      brand: data.brand || data.manufacturer || '',
      productType: data.productType || undefined,
      condition: data.condition || undefined,
      oemType: data.oemType || undefined,
      compatibilityMode: data.compatibilityMode || undefined,
      compatibleMakes: data.compatibleMakes || undefined,
      compatibleModels: data.compatibleModels || undefined,
      compatibleYearStart: data.compatibleYearStart ?? undefined,
      compatibleYearEnd: data.compatibleYearEnd ?? undefined,
      vinCompatible: parseBooleanFlag(data.vinCompatible),
      make: data.make || undefined,
      model: data.model || undefined,
      year: data.year ?? undefined,
      mileage: data.mileage ?? undefined,
      fuelType: data.fuelType || undefined,
      transmission: data.transmission || undefined,
      bodyType: data.bodyType || undefined,
      driveType: data.driveType || undefined,
      powerKw: data.powerKw ?? undefined,
      color: data.color || undefined,
      warrantyIncluded: data.warrantyIncluded ?? undefined,
      partCategory: data.partCategory || undefined,
      partNumberRaw: data.partNumber || undefined,
      partPosition: data.partPosition || undefined,
      material: data.material || undefined,
      reconditioned: data.reconditioned ?? undefined,
      toolCategory: data.toolCategory || undefined,
      powerSource: data.powerSource || undefined,
      voltage: data.voltage ?? undefined,
      torqueMinNm: data.torqueMinNm ?? undefined,
      torqueMaxNm: data.torqueMaxNm ?? undefined,
      driveSize: data.driveSize || undefined,
      professionalGrade: data.professionalGrade ?? undefined,
      isKit: data.isKit ?? undefined,
      customCategory: data.customCategory || undefined,
      styleTags: data.styleTags || undefined,
      finish: data.finish || undefined,
      streetLegal: data.streetLegal ?? undefined,
      installationDifficulty: data.installationDifficulty || undefined,
      weight: typeof data.weight === 'number' ? data.weight : data.weight ? parseFloat(data.weight) : undefined,
      specifications: data.specifications || undefined,
      compatibleVehicles: data.compatibleVehicles || undefined,
      leadTime: data.leadTime || undefined,
      minQuantity: data.minQuantity || undefined,
      stockLevel: stockQty,
      variants: data.variants || undefined,
      infoSections,
      createdAt: data.createdDate || new Date().toISOString(),
      updatedAt: data.updatedDate || new Date().toISOString(),
    }
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
 * Fetch root categories (topics: cars, parts, tools, custom)
 */
export async function fetchRootCategories(): Promise<Category[]> {
  const url = `${API_BASE_URL}/api/categories/root`

  try {
    const response = await fetch(url, {
      next: { revalidate: 300 },
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
    })

    if (!response.ok) {
      console.warn(`Root categories endpoint returned ${response.status}`)
      return []
    }

    return await response.json()
  } catch (error) {
    console.error('Error fetching root categories:', error)
    return []
  }
}

/**
 * Fetch category by slug (for topic routing)
 */
export async function fetchCategoryBySlug(slug: string): Promise<Category | null> {
  const url = `${API_BASE_URL}/api/categories/slug/${slug}`

  try {
    const response = await fetch(url, {
      next: { revalidate: 300 },
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
    })

    if (!response.ok) {
      if (response.status === 404) {
        return null
      }
      console.warn(`Category by slug endpoint returned ${response.status}`)
      return null
    }

    return await response.json()
  } catch (error) {
    console.error(`Error fetching category ${slug}:`, error)
    return null
  }
}

/**
 * Fetch children of a category (for topic sub-things row)
 */
export async function fetchCategoryChildren(categoryId: string | number): Promise<Category[]> {
  const url = `${API_BASE_URL}/api/categories/${categoryId}/children`

  try {
    const response = await fetch(url, {
      next: { revalidate: 300 },
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'omit',
    })

    if (!response.ok) {
      console.warn(`Category children endpoint returned ${response.status}`)
      return []
    }

    return await response.json()
  } catch (error) {
    console.error(`Error fetching category children for ${categoryId}:`, error)
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

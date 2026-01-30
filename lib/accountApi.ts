/**
 * Account API
 * 
 * API functions for customer account operations
 * All endpoints require customer authentication
 * 
 * Adjust endpoint paths to match your backend
 */

import { api } from './apiClient'
import type { Product } from './shopApi'
import type { User } from './authApi'

// ============================================================================
// PROFILE
// ============================================================================

export interface ProfileUpdateInput {
  name?: string
  email?: string
  phone?: string
  avatar?: string
}

export interface PasswordChangeInput {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

/**
 * Get user profile
 */
export async function getProfile(): Promise<User> {
  return api.get<User>('/api/account/profile')
}

/**
 * Update user profile
 */
export async function updateProfile(data: ProfileUpdateInput): Promise<User> {
  return api.put<User>('/api/account/profile', data)
}

/**
 * Change password
 */
export async function changePassword(data: PasswordChangeInput): Promise<void> {
  return api.post<void>('/api/account/password', data)
}

// ============================================================================
// ORDERS
// ============================================================================

export interface CustomerOrder {
  id: string
  orderNumber: string
  items: {
    id: string
    productId: string
    productName: string
    productImage?: string
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
  trackingNumber?: string
  createdAt: string
  updatedAt: string
  estimatedDelivery?: string
}

export interface OrdersListParams {
  page?: number
  limit?: number
  status?: string
}

export interface OrdersListResponse {
  orders: CustomerOrder[]
  total: number
  page: number
  limit: number
  totalPages: number
}

/**
 * Get user's orders
 */
export async function getOrders(params: OrdersListParams = {}): Promise<OrdersListResponse> {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined) searchParams.append(key, value.toString())
  })
  
  return api.get<OrdersListResponse>(`/api/orders?${searchParams.toString()}`)
}

/**
 * Get single order
 */
export async function getOrder(id: string): Promise<CustomerOrder> {
  return api.get<CustomerOrder>(`/api/orders/${id}`)
}

/**
 * Cancel order
 */
export async function cancelOrder(id: string): Promise<CustomerOrder> {
  return api.post<CustomerOrder>(`/api/orders/${id}/cancel`, {})
}

// ============================================================================
// WISHLIST
// ============================================================================

export interface WishlistItem {
  id: string
  product: Product
  addedAt: string
}

/**
 * Get wishlist
 */
export async function getWishlist(): Promise<WishlistItem[]> {
  return api.get<WishlistItem[]>('/api/wishlist')
}

/**
 * Add to wishlist
 */
export async function addToWishlist(productId: string): Promise<WishlistItem> {
  return api.post<WishlistItem>('/api/wishlist', { productId })
}

/**
 * Remove from wishlist
 */
export async function removeFromWishlist(id: string): Promise<void> {
  return api.delete<void>(`/api/wishlist/${id}`)
}

/**
 * Check if product is in wishlist
 */
export async function isInWishlist(productId: string): Promise<boolean> {
  try {
    const wishlist = await getWishlist()
    return wishlist.some((item) => item.product.id === productId)
  } catch {
    return false
  }
}

// ============================================================================
// ADDRESSES
// ============================================================================

export interface Address {
  id: string
  label: string // e.g., "Home", "Work", "Shipping"
  isDefault: boolean
  recipientName: string
  phone: string
  street: string
  city: string
  state: string
  country: string
  postalCode: string
  createdAt: string
  updatedAt: string
}

export interface AddressCreateInput {
  label: string
  isDefault?: boolean
  recipientName: string
  phone: string
  street: string
  city: string
  state: string
  country: string
  postalCode: string
}

/**
 * Get all addresses
 */
export async function getAddresses(): Promise<Address[]> {
  return api.get<Address[]>('/api/shipping/addresses')
}

/**
 * Get single address
 */
export async function getAddress(id: string): Promise<Address> {
  return api.get<Address>(`/api/shipping/addresses/${id}`)
}

/**
 * Create address
 */
export async function createAddress(data: AddressCreateInput): Promise<Address> {
  return api.post<Address>('/api/shipping/addresses', data)
}

/**
 * Update address
 */
export async function updateAddress(id: string, data: Partial<AddressCreateInput>): Promise<Address> {
  return api.put<Address>(`/api/shipping/addresses/${id}`, data)
}

/**
 * Delete address
 */
export async function deleteAddress(id: string): Promise<void> {
  return api.delete<void>(`/api/shipping/addresses/${id}`)
}

/**
 * Set default address
 */
export async function setDefaultAddress(id: string): Promise<Address> {
  return api.post<Address>(`/api/shipping/addresses/${id}/set-default`, {})
}

// ============================================================================
// NOTIFICATIONS
// ============================================================================

export interface Notification {
  id: string
  title: string
  message: string
  type: 'order' | 'product' | 'system'
  read: boolean
  createdAt: string
}

/**
 * Get notifications
 */
export async function getNotifications(): Promise<Notification[]> {
  return api.get<Notification[]>('/api/account/notifications')
}

/**
 * Mark notification as read
 */
export async function markNotificationRead(id: string): Promise<void> {
  return api.post<void>(`/api/account/notifications/${id}/read`, {})
}

/**
 * Mark all notifications as read
 */
export async function markAllNotificationsRead(): Promise<void> {
  return api.post<void>('/api/account/notifications/read-all', {})
}


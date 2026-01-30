import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Convert relative image URL to absolute backend URL
 * @param imageUrl - Image URL (can be relative like "/uploads/..." or legacy "/api/images/..." or absolute, or an object with imageUrl property)
 * @returns Absolute URL pointing to the backend server
 */
export function getImageUrl(imageUrl: string | undefined | null | any): string {
  if (!imageUrl) {
    return '/placeholder-product.png' // Fallback image
  }

  // If it's an object (ProductImageResponse), extract the imageUrl property
  if (typeof imageUrl === 'object' && imageUrl.imageUrl) {
    imageUrl = imageUrl.imageUrl
  }

  // If it's still not a string, try to convert or return fallback
  if (typeof imageUrl !== 'string') {
    console.warn('getImageUrl received non-string value:', imageUrl)
    return '/placeholder-product.png'
  }

  // If already absolute URL, return as-is
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl
  }

  // Backend base URL
  const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'

  // Legacy path support: /api/images/{file} -> /uploads/products/{file}
  if (imageUrl.startsWith('/api/images/')) {
    const fileName = imageUrl.replace('/api/images/', '')
    return `${API_BASE_URL}/uploads/products/${fileName}`
  }

  // If stored as "products/filename"
  if (!imageUrl.startsWith('/') && imageUrl.startsWith('products/')) {
    return `${API_BASE_URL}/uploads/${imageUrl}`
  }

  // If it's just a filename, assume products directory
  if (!imageUrl.startsWith('/')) {
    return `${API_BASE_URL}/uploads/products/${imageUrl}`
  }

  // For any other relative URL, prepend backend URL
  return `${API_BASE_URL}${imageUrl}`
}

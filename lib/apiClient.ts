/**
 * API Client
 * 
 * Centralized fetch wrapper that handles:
 * - Authorization headers
 * - Error handling
 * - Request/response interceptors
 * 
 * Token Strategy:
 * - Prefers httpOnly cookies (backend sets via Set-Cookie header)
 * - Falls back to memory storage if backend returns token in JSON
 * - Cookie approach is more secure (not accessible to JavaScript)
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_SHOP_API_BASE_URL || 'http://localhost:8080'
const AUTH_API_BASE_URL = process.env.NEXT_PUBLIC_AUTH_API_BASE_URL || API_BASE_URL

// In-memory token storage (only used if backend doesn't use httpOnly cookies)
let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken(): string | null {
  return accessToken
}

export interface ApiError {
  message: string
  status: number
  errors?: Record<string, string[]>
}

export class ApiException extends Error {
  status: number
  errors?: Record<string, string[]>

  constructor(message: string, status: number, errors?: Record<string, string[]>) {
    super(message)
    this.name = 'ApiException'
    this.status = status
    this.errors = errors
  }
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean
  baseUrl?: string
  allow401?: boolean // For login endpoints - 401 means invalid credentials, not unauthorized request
}

/**
 * Centralized fetch function with auth header injection
 */
export async function apiRequest<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { skipAuth = false, baseUrl, allow401 = false, ...fetchOptions } = options
  
  const url = `${baseUrl || API_BASE_URL}${endpoint}`
  
  const headers = new Headers(fetchOptions.headers || {})
  
  // Set Content-Type if not already set and body exists
  if (fetchOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  
  // Add Authorization header if token exists and not skipped
  if (!skipAuth && accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }
  
  // Include credentials to send/receive cookies
  const config: RequestInit = {
    ...fetchOptions,
    headers,
    credentials: 'include', // Important: allows cookies to be sent/received
  }

  try {
    const response = await fetch(url, config)
    
    // Handle different response statuses
    if (response.status === 401) {
      // For login endpoints, 401 means invalid credentials (not unauthorized request)
      if (allow401) {
        // Try to parse error message from response
        let errorMessage = 'Invalid email or password'
        let errorErrors: Record<string, string[]> | undefined
        
        try {
          const contentType = response.headers.get('content-type')
          if (contentType && contentType.includes('application/json')) {
            const errorData = await response.json()
            // Backend ErrorResponse structure: { message, status, timestamp }
            errorMessage = errorData.message || errorData.error || errorMessage
            errorErrors = errorData.errors
          } else {
            const text = await response.text()
            if (text) {
              try {
                // Try parsing as JSON even if content-type is wrong
                const parsed = JSON.parse(text)
                errorMessage = parsed.message || parsed.error || errorMessage
              } catch {
                errorMessage = text
              }
            }
          }
        } catch (parseError) {
          console.warn('Failed to parse error response:', parseError)
          // Use default message if parsing fails
        }
        
        throw new ApiException(errorMessage, 401, errorErrors)
      }
      // For other endpoints, 401 means unauthorized - clear token
      setAccessToken(null)
      throw new ApiException('Unauthorized', 401)
    }
    
    if (response.status === 403) {
      throw new ApiException('Forbidden', 403)
    }
    
    if (response.status === 404) {
      throw new ApiException('Not found', 404)
    }
    
    if (!response.ok) {
      // Try to parse error response
      try {
        const errorData = await response.json()
        throw new ApiException(
          errorData.message || `Request failed with status ${response.status}`,
          response.status,
          errorData.errors
        )
      } catch (e) {
        if (e instanceof ApiException) throw e
        throw new ApiException(`Request failed with status ${response.status}`, response.status)
      }
    }
    
    // Handle empty responses (204 No Content, etc.)
    if (response.status === 204 || response.headers.get('content-length') === '0') {
      return {} as T
    }
    
    // Parse JSON response
    const data = await response.json()
    return data
  } catch (error) {
    // Network errors or other fetch failures
    if (error instanceof ApiException) {
      throw error
    }
    
    console.error('API Request Error:', error)
    throw new ApiException(
      error instanceof Error ? error.message : 'Network error',
      0
    )
  }
}

/**
 * Convenience methods for different HTTP verbs
 */
export const api = {
  get: <T>(endpoint: string, options?: RequestOptions) =>
    apiRequest<T>(endpoint, { ...options, method: 'GET' }),
  
  post: <T>(endpoint: string, body?: any, options?: RequestOptions) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  put: <T>(endpoint: string, body?: any, options?: RequestOptions) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  patch: <T>(endpoint: string, body?: any, options?: RequestOptions) =>
    apiRequest<T>(endpoint, {
      ...options,
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),
  
  delete: <T>(endpoint: string, options?: RequestOptions) =>
    apiRequest<T>(endpoint, { ...options, method: 'DELETE' }),
}

/**
 * Auth-specific API client (uses AUTH_API_BASE_URL)
 */
export const authApi = {
  get: <T>(endpoint: string, options?: RequestOptions) =>
    apiRequest<T>(endpoint, { ...options, method: 'GET', baseUrl: AUTH_API_BASE_URL, skipAuth: true }),
  
  post: <T>(endpoint: string, body?: any, options?: RequestOptions) => {
    // Login endpoint should allow 401 (invalid credentials)
    const isLoginEndpoint = endpoint.includes('/login')
    return apiRequest<T>(endpoint, {
      ...options,
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
      baseUrl: AUTH_API_BASE_URL,
      skipAuth: true, // Auth endpoints don't require auth
      // Use explicit allow401 from options if provided, otherwise default to isLoginEndpoint
      allow401: options?.allow401 !== undefined ? options.allow401 : isLoginEndpoint,
    })
  },
}


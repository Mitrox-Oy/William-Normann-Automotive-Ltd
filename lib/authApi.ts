/**
 * Authentication API
 * 
 * Handles user authentication with the backend
 * 
 * Backend endpoints:
 * - POST /api/auth/login     { email, password } -> { user, accessToken? }
 * - POST /api/auth/logout    -> 204
 * - GET  /api/auth/profile   -> { id, email, role, name }
 * - POST /api/auth/refresh   -> { accessToken } (optional)
 */

import { authApi, setAccessToken } from './apiClient'
import { z } from 'zod'

export type UserRole = 'customer' | 'owner'

export interface User {
  id: string
  email: string
  name: string
  role: UserRole
  avatar?: string
  phone?: string
  createdAt?: string
}

// Zod schemas for validation
const UserSchema = z.object({
  id: z.string(),
  email: z.string().email(),
  name: z.string(),
  role: z.enum(['customer', 'owner']),
  avatar: z.string().optional(),
  phone: z.string().optional(),
  createdAt: z.string().optional(),
})

interface LoginRequest {
  email: string
  password: string
}

/**
 * Backend AuthResponse structure:
 * {
 *   token: string (JWT),
 *   type: "Bearer",
 *   id: number,
 *   username: string (email),
 *   role: "CUSTOMER" | "OWNER" | "ADMIN",
 *   firstName: string,
 *   lastName: string
 * }
 */
interface BackendAuthResponse {
  token: string
  type?: string
  id: number
  username: string
  role: 'CUSTOMER' | 'OWNER' | 'ADMIN'
  firstName?: string
  lastName?: string
}

/**
 * Login user (customer or owner)
 * 
 * Automatically tries regular login first, then owner login if needed.
 * Backend returns JWT token in response body.
 * We store it in memory and include it in Authorization header for subsequent requests.
 */
export async function login(email: string, password: string): Promise<User> {
  // Debug logging (remove in production)
  console.log('Attempting login:', { email, passwordLength: password.length })
  
  // Try regular customer login first
  let customerError: any = null
  try {
    const response = await authApi.post<BackendAuthResponse>('/api/auth/login', {
      username: email, // Backend expects 'username' field, not 'email'
      password,
    })
    
    console.log('Login successful (customer):', { id: response.id, username: response.username, role: response.role })
    
    // Store token in memory
    if (response.token) {
      setAccessToken(response.token)
    }
    
    // Map backend response to frontend User format
    const user: User = {
      id: response.id.toString(),
      email: response.username, // Backend uses 'username' as email
      name: `${response.firstName || ''} ${response.lastName || ''}`.trim() || response.username,
      role: response.role === 'OWNER' || response.role === 'ADMIN' ? 'owner' : 'customer',
      createdAt: new Date().toISOString(), // Backend doesn't return this in login response
    }
    
    // Validate user data
    const validatedUser = UserSchema.parse(user)
    return validatedUser
  } catch (error: any) {
    // Customer login failed - save error and try owner login as fallback
    customerError = error
    console.log('Customer login failed, trying owner login as fallback...')
  }
  
  // Try owner login as fallback (owner accounts get "Invalid username or password" from customer endpoint)
  try {
    console.log('Attempting owner login with:', { email, passwordLength: password.length })
    const ownerResponse = await authApi.post<BackendAuthResponse>('/api/auth/owner/login', {
      email: email, // Owner login uses 'email' field
      password,
    }, {
      allow401: true, // Allow 401 for invalid credentials
    })
    
    console.log('Login successful (owner):', { id: ownerResponse.id, username: ownerResponse.username, role: ownerResponse.role })
    
    // Store token in memory
    if (ownerResponse.token) {
      setAccessToken(ownerResponse.token)
    }
    
    // Map backend response to frontend User format
    const user: User = {
      id: ownerResponse.id.toString(),
      email: ownerResponse.username, // Backend uses 'username' as email
      name: `${ownerResponse.firstName || ''} ${ownerResponse.lastName || ''}`.trim() || ownerResponse.username,
      role: ownerResponse.role === 'OWNER' || ownerResponse.role === 'ADMIN' ? 'owner' : 'customer',
      createdAt: new Date().toISOString(),
    }
    
    // Validate user data
    const validatedUser = UserSchema.parse(user)
    return validatedUser
  } catch (ownerError: any) {
    // Both logins failed - log both errors for debugging
    console.error('Both customer and owner login failed')
    console.error('Customer error:', customerError?.message || customerError)
    console.error('Owner error:', ownerError?.message || ownerError)
    // Throw the most descriptive error
    throw ownerError || customerError
  }
}

/**
 * Register new user
 * 
 * Backend returns AuthResponse with JWT token, so user is automatically logged in.
 */
export async function register(
  email: string,
  password: string,
  firstName: string,
  lastName: string
): Promise<User> {
  try {
    console.log('Attempting registration:', { email, firstName, lastName })
    
    const response = await authApi.post<BackendAuthResponse>('/api/auth/register', {
      username: email, // Backend expects 'username' field, not 'email'
      password,
      firstName,
      lastName,
      role: 'CUSTOMER', // Backend always creates CUSTOMER, but we include it for clarity
    })
    
    console.log('Registration successful:', { id: response.id, username: response.username, role: response.role })
    
    // Store token in memory (user is automatically logged in)
    if (response.token) {
      setAccessToken(response.token)
    }
    
    // Map backend response to frontend User format
    const user: User = {
      id: response.id.toString(),
      email: response.username, // Backend uses 'username' as email
      name: `${response.firstName || ''} ${response.lastName || ''}`.trim() || response.username,
      role: response.role === 'OWNER' || response.role === 'ADMIN' ? 'owner' : 'customer',
      createdAt: new Date().toISOString(),
    }
    
    // Validate user data
    const validatedUser = UserSchema.parse(user)
    return validatedUser
  } catch (error: any) {
    console.error('Registration error:', error)
    // Re-throw with user-friendly message
    if (error.message?.includes('already taken')) {
      throw new Error('An account with this email already exists')
    }
    throw error
  }
}

/**
 * Logout user
 */
export async function logout(): Promise<void> {
  try {
    await authApi.post('/api/auth/logout', {})
    setAccessToken(null)
  } catch (error) {
    // Even if logout fails on backend, clear local token
    setAccessToken(null)
    console.error('Logout error:', error)
  }
}

/**
 * Get current user from session
 * Note: Backend uses /api/auth/profile, not /api/auth/me
 */
export async function getCurrentUser(): Promise<User | null> {
  try {
    const response = await authApi.get<any>('/api/auth/profile')
    
    // Map backend response to frontend User format
    // Backend returns: { id, username, firstName, lastName, role, createdAt }
    // Frontend expects: { id, email, name, role, ... }
    const user: User = {
      id: response.id,
      email: response.username, // Backend uses 'username' as email
      name: `${response.firstName || ''} ${response.lastName || ''}`.trim() || response.username,
      role: response.role?.toLowerCase() === 'owner' ? 'owner' : 'customer',
      createdAt: response.createdAt,
    }
    
    const validatedUser = UserSchema.parse(user)
    return validatedUser
  } catch (error: any) {
    // 401 means not authenticated
    if (error.status === 401) {
      return null
    }
    console.error('Get current user error:', error)
    return null
  }
}

/**
 * Refresh access token (if backend supports it)
 */
export async function refreshAccessToken(): Promise<string | null> {
  try {
    const response = await authApi.post<{ accessToken: string }>('/api/auth/refresh', {})
    if (response.accessToken) {
      setAccessToken(response.accessToken)
      return response.accessToken
    }
    return null
  } catch (error) {
    console.error('Token refresh error:', error)
    return null
  }
}

/**
 * Check if user has required role
 */
export function hasRole(user: User | null, roles: UserRole[]): boolean {
  if (!user) return false
  return roles.includes(user.role)
}

/**
 * Check if user is admin/owner
 */
export function isOwner(user: User | null): boolean {
  return hasRole(user, ['owner'])
}

/**
 * Check if user is customer
 */
export function isCustomer(user: User | null): boolean {
  return hasRole(user, ['customer'])
}


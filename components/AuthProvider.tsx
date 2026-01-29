"use client"

import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { login as apiLogin, logout as apiLogout, register as apiRegister, getCurrentUser, type User } from '@/lib/authApi'
import { ApiException } from '@/lib/apiClient'

interface AuthContextType {
  user: User | null
  loading: boolean
  error: string | null
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
  isOwner: boolean
  isCustomer: boolean
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const router = useRouter()
  const pathname = usePathname()

  /**
   * Fetch current user on mount
   */
  const refreshUser = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const currentUser = await getCurrentUser()
      setUser(currentUser)
    } catch (err) {
      console.error('Failed to fetch user:', err)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refreshUser()
  }, [refreshUser])

  /**
   * Login function
   */
  const login = async (email: string, password: string) => {
    try {
      setLoading(true)
      setError(null)
      
      const loggedInUser = await apiLogin(email, password)
      setUser(loggedInUser)
      
      // Redirect based on role
      // Read 'next' query param for redirect, or use default based on role
      const params = new URLSearchParams(window.location.search)
      const next = params.get('next')
      
      if (next) {
        router.push(next)
      } else if (loggedInUser.role === 'owner') {
        router.push('/admin')
      } else {
        router.push('/account')
      }
    } catch (err) {
      const message = err instanceof ApiException ? err.message : 'Login failed'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * Register function
   */
  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    try {
      setLoading(true)
      setError(null)
      
      // Register returns user with token, so user is automatically logged in
      const registeredUser = await apiRegister(email, password, firstName, lastName)
      setUser(registeredUser)
      
      // Redirect to account page after registration
      router.push('/account')
    } catch (err) {
      const message = err instanceof ApiException ? err.message : 'Registration failed'
      setError(message)
      throw err
    } finally {
      setLoading(false)
    }
  }

  /**
   * Logout function
   */
  const logout = async () => {
    try {
      setLoading(true)
      await apiLogout()
      setUser(null)
      router.push('/')
    } catch (err) {
      console.error('Logout error:', err)
      // Clear user anyway
      setUser(null)
      router.push('/')
    } finally {
      setLoading(false)
    }
  }

  const value: AuthContextType = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    refreshUser,
    isOwner: user?.role === 'owner',
    isCustomer: user?.role === 'customer',
    isAuthenticated: user !== null,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

/**
 * Client-side route guard component
 * Use this to protect pages that require authentication
 */
interface RequireAuthProps {
  children: React.ReactNode
  allowedRoles?: ('customer' | 'owner')[]
  fallback?: React.ReactNode
}

export function RequireAuth({ children, allowedRoles, fallback }: RequireAuthProps) {
  const { user, loading } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (!loading && !user) {
      // Not authenticated, redirect to login
      router.push(`/login?next=${encodeURIComponent(pathname)}`)
    } else if (!loading && user && allowedRoles && !allowedRoles.includes(user.role)) {
      // Authenticated but wrong role
      if (user.role === 'owner') {
        router.push('/admin')
      } else {
        router.push('/account')
      }
    }
  }, [user, loading, allowedRoles, router, pathname])

  if (loading) {
    return (
      fallback || (
        <div className="flex min-h-screen items-center justify-center">
          <div className="text-center">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent mx-auto mb-4" />
            <p className="text-muted-foreground">Loading...</p>
          </div>
        </div>
      )
    )
  }

  if (!user) {
    return null
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return null
  }

  return <>{children}</>
}


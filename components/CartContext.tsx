"use client"

import React, { createContext, useContext, useState, useEffect } from 'react'
import type { Product } from '@/lib/shopApi'

export interface CartItem {
  product: Product
  quantity: number
}

interface CartContextType {
  items: CartItem[]
  addItem: (product: Product, quantity?: number) => void
  removeItem: (productId: string) => void
  updateQuantity: (productId: string, quantity: number) => void
  clearCart: () => void
  getTotalItems: () => number
  getTotalPrice: () => number
  isOpen: boolean
  openCart: () => void
  closeCart: () => void
}

const CartContext = createContext<CartContextType | undefined>(undefined)

const CART_STORAGE_KEY = 'wna-cart'

function isQuoteOnlyProduct(product: Product | null | undefined): boolean {
  if (!product) return false
  const flag = (product as any).quoteOnly ?? (product as any).quote_only
  if (typeof flag === 'boolean') return flag
  if (typeof flag === 'number') return flag === 1
  if (typeof flag === 'string') {
    const normalized = flag.trim().toLowerCase()
    return normalized === 'true' || normalized === '1' || normalized === 'yes'
  }
  return false
}

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([])
  const [isOpen, setIsOpen] = useState(false)
  const [isHydrated, setIsHydrated] = useState(false)

  // Load cart from localStorage on mount
  useEffect(() => {
    const storedCart = localStorage.getItem(CART_STORAGE_KEY)
    if (storedCart) {
      try {
        const parsed = JSON.parse(storedCart)
        const sanitized = Array.isArray(parsed)
          ? parsed.filter((item) => !isQuoteOnlyProduct(item?.product))
          : []
        setItems(sanitized)
      } catch (error) {
        console.error('Failed to parse cart from localStorage:', error)
      }
    }
    setIsHydrated(true)
  }, [])

  // Save cart to localStorage whenever it changes
  useEffect(() => {
    if (isHydrated) {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items))
    }
  }, [items, isHydrated])

  const addItem = (product: Product, quantity: number = 1) => {
    if (isQuoteOnlyProduct(product)) {
      console.warn('Blocked add-to-cart for quote-only product:', product?.id)
      return
    }

    setItems((prevItems) => {
      const existingItem = prevItems.find((item) => item.product.id === product.id)
      
      if (existingItem) {
        return prevItems.map((item) =>
          item.product.id === product.id
            ? { ...item, quantity: item.quantity + quantity }
            : item
        )
      }
      
      return [...prevItems, { product, quantity }]
    })
    setIsOpen(true)
  }

  const removeItem = (productId: string) => {
    setItems((prevItems) => prevItems.filter((item) => item.product.id !== productId))
  }

  const updateQuantity = (productId: string, quantity: number) => {
    if (quantity <= 0) {
      removeItem(productId)
      return
    }

    setItems((prevItems) =>
      prevItems.map((item) =>
        item.product.id === productId ? { ...item, quantity } : item
      )
    )
  }

  const clearCart = () => {
    setItems([])
  }

  const getTotalItems = () => {
    return items.reduce((total, item) => total + item.quantity, 0)
  }

  const getTotalPrice = () => {
    return items.reduce((total, item) => total + item.product.price * item.quantity, 0)
  }

  const openCart = () => setIsOpen(true)
  const closeCart = () => setIsOpen(false)

  const value = {
    items,
    addItem,
    removeItem,
    updateQuantity,
    clearCart,
    getTotalItems,
    getTotalPrice,
    isOpen,
    openCart,
    closeCart,
  }

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const context = useContext(CartContext)
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}


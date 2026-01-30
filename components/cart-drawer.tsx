"use client"

import { useCart } from "@/components/CartContext"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import { formatCurrency } from "@/lib/shopApi"
import { Minus, Plus, ShoppingCart, Trash2, X } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { Badge } from "@/components/ui/badge"

export function CartDrawer() {
  const { items, removeItem, updateQuantity, getTotalItems, getTotalPrice, isOpen, closeCart, openCart } = useCart()
  const totalItems = getTotalItems()

  return (
    <Sheet open={isOpen} onOpenChange={(open) => (open ? openCart() : closeCart())}>
      <SheetTrigger asChild>
        <Button variant="ghost" size="sm" className="relative text-primary bg-white hover:bg-white/90">
          <ShoppingCart className="h-5 w-5" />
          {totalItems > 0 && (
            <Badge
              variant="destructive"
              className="absolute -right-2 -top-2 flex h-5 w-5 items-center justify-center rounded-full p-0 text-xs"
            >
              {totalItems}
            </Badge>
          )}
        </Button>
      </SheetTrigger>
      <SheetContent className="flex w-full flex-col sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Shopping Cart ({totalItems})</SheetTitle>
        </SheetHeader>

        {items.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 text-center">
            <ShoppingCart className="h-16 w-16 text-muted-foreground" />
            <div>
              <p className="text-lg font-semibold">Your cart is empty</p>
              <p className="text-sm text-muted-foreground">Add products to get started</p>
            </div>
            <Button asChild onClick={closeCart}>
              <Link href="/shop">Browse Shop</Link>
            </Button>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-auto">
              <div className="space-y-4">
                {items.map((item) => (
                  <div key={item.product.id} className="flex gap-4 rounded-lg border p-4">
                    {item.product.images[0] && (
                      <div className="relative h-20 w-20 flex-shrink-0 overflow-hidden rounded bg-muted">
                        <Image
                          src={item.product.images[0]}
                          alt={item.product.name}
                          fill
                          className="object-cover"
                        />
                      </div>
                    )}
                    <div className="flex flex-1 flex-col">
                      <div className="flex justify-between gap-2">
                        <div>
                          <h4 className="font-semibold text-sm">{item.product.name}</h4>
                          {item.product.partNumber && (
                            <p className="text-xs text-muted-foreground">Part #: {item.product.partNumber}</p>
                          )}
                        </div>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => removeItem(item.product.id)}
                          className="h-8 w-8 p-0"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                      <div className="mt-2 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                            className="h-7 w-7 p-0"
                            disabled={item.quantity <= 1}
                          >
                            <Minus className="h-3 w-3" />
                          </Button>
                          <span className="w-8 text-center text-sm font-medium">{item.quantity}</span>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                            className="h-7 w-7 p-0"
                          >
                            <Plus className="h-3 w-3" />
                          </Button>
                        </div>
                        <p className="font-semibold">{formatCurrency(item.product.price * item.quantity, item.product.currency)}</p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="space-y-4 border-t pt-4">
              <div className="flex justify-between text-lg font-bold">
                <span>Total</span>
                <span>{formatCurrency(getTotalPrice(), items[0]?.product.currency || 'GBP')}</span>
              </div>
              <div className="space-y-2">
                <Button asChild className="w-full" size="lg">
                  <Link href="/cart" onClick={closeCart}>
                    View Cart
                  </Link>
                </Button>
                <p className="text-center text-xs text-muted-foreground">
                  Checkout coming soon. Request a quote for these items.
                </p>
              </div>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  )
}


"use client"

import { useState, useEffect } from "react"
import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { RequireAuth } from "@/components/AuthProvider"
import { useCart } from "@/components/CartContext"
import { getWishlist, removeFromWishlist, type WishlistItem } from "@/lib/accountApi"
import { formatCurrency, getAvailabilityBadge } from "@/lib/shopApi"
import { getImageUrl } from "@/lib/utils"
import { Heart, ShoppingCart, Trash2, ChevronLeft } from "lucide-react"
import Link from "next/link"
import Image from "next/image"

function WishlistPageContent() {
  const [wishlist, setWishlist] = useState<WishlistItem[]>([])
  const [loading, setLoading] = useState(true)
  const { addItem } = useCart()

  useEffect(() => {
    loadWishlist()
  }, [])

  async function loadWishlist() {
    try {
      setLoading(true)
      const items = await getWishlist()
      setWishlist(items)
    } catch (error) {
      console.error("Failed to load wishlist:", error)
      setWishlist([])
    } finally {
      setLoading(false)
    }
  }

  async function handleRemove(id: string) {
    try {
      await removeFromWishlist(id)
      setWishlist((prev) => prev.filter((item) => item.id !== id))
    } catch (error) {
      console.error("Failed to remove item:", error)
    }
  }

  function handleAddToCart(item: WishlistItem) {
    addItem(item.product)
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link href="/account" className="inline-flex items-center text-sm text-muted-foreground hover:text-primary">
            <ChevronLeft className="mr-1 h-4 w-4" />
            Back to Account
          </Link>
        </div>

        <SectionHeading
          title="My Wishlist"
          subtitle="Items you've saved for later"
          className="mb-8"
        />

        {loading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <Card key={i}>
                <CardContent className="p-0">
                  <Skeleton className="aspect-square w-full" />
                  <div className="p-4 space-y-2">
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-4 w-1/2" />
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        ) : wishlist.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {wishlist.map((item) => {
              const availabilityBadge = getAvailabilityBadge(item.product.availability)

              return (
                <Card key={item.id} className="group overflow-hidden">
                  <CardContent className="p-0">
                    <Link href={`/shop/${item.product.slug}`} className="block relative aspect-square">
                      {item.product.images[0] ? (
                        <Image
                          src={getImageUrl(item.product.images[0])}
                          alt={item.product.name}
                          fill
                          className="object-cover transition-transform group-hover:scale-105"
                        />
                      ) : (
                        <div className="flex h-full items-center justify-center bg-muted">
                          <ShoppingCart className="h-16 w-16 text-muted-foreground" />
                        </div>
                      )}
                      <Badge variant={availabilityBadge.variant} className="absolute right-2 top-2">
                        {availabilityBadge.label}
                      </Badge>
                    </Link>

                    <div className="p-4 space-y-3">
                      <Link href={`/shop/${item.product.slug}`}>
                        <h3 className="font-semibold line-clamp-2 hover:text-primary">
                          {item.product.name}
                        </h3>
                      </Link>

                      <p className="text-lg font-bold">
                        {formatCurrency(item.product.price, item.product.currency)}
                      </p>

                      <div className="flex gap-2">
                        <Button
                          onClick={() => handleAddToCart(item)}
                          disabled={item.product.availability === "out_of_stock"}
                          className="flex-1"
                          size="sm"
                        >
                          <ShoppingCart className="mr-2 h-4 w-4" />
                          Add to Cart
                        </Button>
                        <Button
                          onClick={() => handleRemove(item.id)}
                          variant="outline"
                          size="sm"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              )
            })}
          </div>
        ) : (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-16 text-center">
              <Heart className="mb-4 h-16 w-16 text-muted-foreground" />
              <h3 className="mb-2 text-lg font-semibold">Your wishlist is empty</h3>
              <p className="mb-6 text-sm text-muted-foreground">
                Save items you're interested in for later
              </p>
              <Button asChild>
                <Link href="/shop">Browse Shop</Link>
              </Button>
            </CardContent>
          </Card>
        )}
      </Container>
    </section>
  )
}

export default function WishlistPage() {
  return (
    <RequireAuth allowedRoles={["customer", "owner"]}>
      <WishlistPageContent />
    </RequireAuth>
  )
}


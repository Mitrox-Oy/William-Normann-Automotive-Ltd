"use client"

import { Container } from "@/components/container"
import { SectionHeading } from "@/components/section-heading"
import { Card, CardContent } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { useCart } from "@/components/CartContext"
import { useAuth } from "@/components/AuthProvider"
import { formatCurrency, createCheckoutOrder, createCheckoutSession } from "@/lib/shopApi"
import { ShoppingCart, Minus, Plus, Trash2, ArrowLeft, FileText, Package, CreditCard, Loader2 } from "lucide-react"
import Image from "next/image"
import Link from "next/link"
import { useRouter } from "next/navigation"
import { motion } from "framer-motion"
import { loadStripe } from "@stripe/stripe-js"
import { useState } from "react"

// Initialize Stripe
const stripePublishableKey = process.env.NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY || ''
const stripePromise = loadStripe(stripePublishableKey)

export default function CartPage() {
  const { items, removeItem, updateQuantity, clearCart, getTotalItems, getTotalPrice } = useCart()
  const { isAuthenticated } = useAuth()
  const router = useRouter()
  const [isProcessingCheckout, setIsProcessingCheckout] = useState(false)
  const [checkoutError, setCheckoutError] = useState<string>("")

  const totalItems = getTotalItems()
  const totalPrice = getTotalPrice()
  const currency = items[0]?.product.currency || 'GBP'

  const handleRequestQuote = () => {
    const cartSummary = items
      .map((item) => `${item.product.name} (Part #: ${item.product.partNumber || 'N/A'}) x ${item.quantity}`)
      .join(', ')

    const queryParams = new URLSearchParams({
      cart_items: cartSummary,
      total_items: totalItems.toString(),
    })

    router.push(`/#contact?${queryParams.toString()}`)
  }

  const handleCheckout = async () => {
    if (!isAuthenticated || !token) {
      router.push('/login?redirect=/cart')
      return
    }

    setIsProcessingCheckout(true)
    setCheckoutError("")

    if (!stripePublishableKey) {
      setCheckoutError("Stripe key missing: configure NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY")
      setIsProcessingCheckout(false)
      return
    }

    try {
      // Step 1: Create order from cart
      const orderResponse = await createCheckoutOrder({
        shippingAddress: "To be provided",
        shippingCity: "To be provided",
        shippingPostalCode: "To be provided",
        shippingCountry: "GB",
        shippingAmount: 0,
        taxAmount: 0,
      })

      if (!orderResponse?.orderId) {
        throw new Error('Failed to create order')
      }

      // Step 2: Create Stripe Checkout Session
      const sessionResponse = await createCheckoutSession(String(orderResponse.orderId))

      if (!sessionResponse?.id) {
        throw new Error('Failed to create checkout session')
      }

      // Step 3: Redirect to Stripe Checkout
      const stripe = await stripePromise
      if (!stripe) {
        throw new Error('Failed to initialize payment system')
      }

      const result = await stripe.redirectToCheckout({
        sessionId: sessionResponse.id
      })

      if (result.error) {
        throw new Error(result.error.message || 'Failed to redirect to checkout')
      }
    } catch (error: any) {
      console.error('Error proceeding to checkout:', error)
      let errorMessage = 'Failed to proceed to checkout. Please try again.'

      if (error?.message) {
        errorMessage = error.message
      } else if (error?.error) {
        errorMessage = error.error
      }

      setCheckoutError(errorMessage)
    } finally {
      setIsProcessingCheckout(false)
    }
  }

  if (items.length === 0) {
    return (
      <section className="py-24 lg:py-32">
        <Container>
          <Card>
            <CardContent className="flex flex-col items-center justify-center gap-6 py-20 text-center">
              <ShoppingCart className="h-24 w-24 text-muted-foreground" />
              <div>
                <h2 className="mb-2 text-2xl font-bold">Your cart is empty</h2>
                <p className="text-muted-foreground">
                  Add some products from our shop to get started
                </p>
              </div>
              <Button asChild size="lg">
                <Link href="/shop">
                  Browse Shop
                </Link>
              </Button>
            </CardContent>
          </Card>
        </Container>
      </section>
    )
  }

  return (
    <section className="py-24 lg:py-32">
      <Container>
        <div className="mb-8">
          <Link
            href="/shop"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="mr-2 h-4 w-4" />
            Continue Shopping
          </Link>
        </div>

        <SectionHeading
          title={`Shopping Cart (${totalItems} ${totalItems === 1 ? 'item' : 'items'})`}
          subtitle="Review your items and request a quote"
          className="mb-12"
        />

        <div className="grid gap-8 lg:grid-cols-3">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {items.map((item, index) => (
              <motion.div
                key={item.product.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
              >
                <Card>
                  <CardContent className="p-6">
                    <div className="flex gap-6">
                      {/* Image */}
                      <Link
                        href={`/shop/${item.product.slug}`}
                        className="relative h-24 w-24 flex-shrink-0 overflow-hidden rounded-lg bg-muted"
                      >
                        {item.product.images[0] ? (
                          <Image
                            src={item.product.images[0]}
                            alt={item.product.name}
                            fill
                            className="object-cover"
                          />
                        ) : (
                          <div className="flex h-full items-center justify-center">
                            <Package className="h-8 w-8 text-muted-foreground" />
                          </div>
                        )}
                      </Link>

                      {/* Product Info */}
                      <div className="flex flex-1 flex-col">
                        <div className="flex justify-between gap-4">
                          <div className="flex-1">
                            <Link
                              href={`/shop/${item.product.slug}`}
                              className="font-semibold hover:text-primary transition-colors"
                            >
                              {item.product.name}
                            </Link>
                            {item.product.partNumber && (
                              <p className="mt-1 text-sm text-muted-foreground">
                                Part #: {item.product.partNumber}
                              </p>
                            )}
                            {item.product.leadTime && (
                              <p className="mt-1 text-xs text-muted-foreground">
                                Lead time: {item.product.leadTime}
                              </p>
                            )}
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => removeItem(item.product.id)}
                            className="h-8 w-8 text-destructive hover:text-destructive"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>

                        <div className="mt-4 flex items-center justify-between">
                          {/* Quantity Controls */}
                          <div className="flex items-center gap-3">
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => updateQuantity(item.product.id, item.quantity - 1)}
                              disabled={item.quantity <= (item.product.minQuantity || 1)}
                              className="h-8 w-8"
                            >
                              <Minus className="h-4 w-4" />
                            </Button>
                            <span className="w-12 text-center font-semibold">
                              {item.quantity}
                            </span>
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => updateQuantity(item.product.id, item.quantity + 1)}
                              className="h-8 w-8"
                            >
                              <Plus className="h-4 w-4" />
                            </Button>
                          </div>

                          {/* Price */}
                          <div className="text-right">
                            <p className="text-lg font-bold">
                              {formatCurrency(item.product.price * item.quantity, item.product.currency)}
                            </p>
                            <p className="text-xs text-muted-foreground">
                              {formatCurrency(item.product.price, item.product.currency)} each
                            </p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}

            {/* Clear Cart Button */}
            <Button
              variant="outline"
              onClick={clearCart}
              className="w-full text-destructive hover:bg-destructive hover:text-destructive-foreground"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              Clear Cart
            </Button>
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <Card className="sticky top-24">
              <CardContent className="p-6 space-y-6">
                <h3 className="text-xl font-bold">Order Summary</h3>

                <Separator />

                <div className="space-y-3">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Subtotal</span>
                    <span className="font-medium">{formatCurrency(totalPrice, currency)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Total Items</span>
                    <span className="font-medium">{totalItems}</span>
                  </div>
                  <Separator />
                  <div className="flex justify-between">
                    <span className="font-semibold">Estimated Total</span>
                    <span className="text-xl font-bold">{formatCurrency(totalPrice, currency)}</span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    Final pricing, shipping, and duties will be calculated in your quote
                  </p>
                </div>

                <Separator />

                {checkoutError && (
                  <div className="rounded-lg border border-red-200 bg-red-50 p-3">
                    <p className="text-sm text-red-900">{checkoutError}</p>
                  </div>
                )}

                <div className="space-y-3">
                  <Button
                    onClick={handleCheckout}
                    size="lg"
                    className="w-full"
                    disabled={isProcessingCheckout}
                  >
                    {isProcessingCheckout ? (
                      <>
                        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                        Processing...
                      </>
                    ) : (
                      <>
                        <CreditCard className="mr-2 h-5 w-5" />
                        Proceed to Checkout
                      </>
                    )}
                  </Button>
                  <Button onClick={handleRequestQuote} variant="outline" size="lg" className="w-full">
                    <FileText className="mr-2 h-5 w-5" />
                    Request Quote Instead
                  </Button>
                  {!isAuthenticated && (
                    <p className="text-center text-xs text-muted-foreground">
                      You'll be asked to sign in to complete checkout
                    </p>
                  )}
                </div>

                <Separator />

                {/* Info Cards */}
                <div className="space-y-3">
                  <div className="rounded-lg border p-3">
                    <p className="text-sm font-medium">What happens next?</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      We'll review your quote request and respond within 4 business hours with detailed pricing, lead times, and shipping options.
                    </p>
                  </div>

                  <div className="rounded-lg bg-muted p-3">
                    <p className="text-xs text-muted-foreground">
                      <span className="font-medium">✓</span> Transparent pricing with no hidden fees
                      <br />
                      <span className="font-medium">✓</span> Multiple shipping options available
                      <br />
                      <span className="font-medium">✓</span> Customs & duties calculated upfront
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Trust Section */}
        <Card className="mt-12 border-primary/20 bg-primary/5">
          <CardContent className="grid gap-6 p-8 sm:grid-cols-3">
            <div className="text-center">
              <div className="mb-2 flex justify-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                  <FileText className="h-6 w-6 text-primary" />
                </div>
              </div>
              <h4 className="mb-1 font-semibold">Transparent Quoting</h4>
              <p className="text-sm text-muted-foreground">
                Full breakdown of parts, shipping, and duties
              </p>
            </div>

            <div className="text-center">
              <div className="mb-2 flex justify-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                  <ShoppingCart className="h-6 w-6 text-primary" />
                </div>
              </div>
              <h4 className="mb-1 font-semibold">Quality Guaranteed</h4>
              <p className="text-sm text-muted-foreground">
                OEM and verified aftermarket suppliers only
              </p>
            </div>

            <div className="text-center">
              <div className="mb-2 flex justify-center">
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                  <Package className="h-6 w-6 text-primary" />
                </div>
              </div>
              <h4 className="mb-1 font-semibold">Global Shipping</h4>
              <p className="text-sm text-muted-foreground">
                Tracked delivery to EU, UK, MENA, and Asia
              </p>
            </div>
          </CardContent>
        </Card>
      </Container>
    </section>
  )
}

